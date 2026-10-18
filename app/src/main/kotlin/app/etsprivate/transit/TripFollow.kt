package app.etsprivate.transit

import app.etsprivate.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.etsprivate.MainActivity
import app.etsprivate.plugin.PluginForegroundService
import app.etsprivate.plugin.PluginService
import app.etsprivate.plugin.ServiceShell
import app.etsprivate.settings.Settings
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

object TripFollow {
    var trip: Itinerary? by mutableStateOf(null)
        private set

    var state: TrackState? by mutableStateOf(null)
        internal set

    var stopsLeft: Int? by mutableStateOf(null)
        internal set

    fun start(context: Context, itinerary: Itinerary) {
        trip = itinerary

        PluginForegroundService.start(context, TransportModule.id, TripFollowWork.ACTION_FOLLOW)

        RideLog.start(context)
    }

    fun stop(context: Context) {
        trip = null
        state = null
        stopsLeft = null

        context.getSystemService(NotificationManager::class.java)?.let { manager ->
            manager.cancel(TripFollowWork.ALERT_ID)
            manager.cancel(TripFollowWork.ONGOING_ID)
        }
        RideLog.stop(context)
        PluginForegroundService.stop(context)
    }

    val active: Boolean get() = trip != null

    fun isSameTrip(other: Itinerary): Boolean {
        val current = trip ?: return false
        return rides(current) == rides(other)
    }

    private fun rides(itinerary: Itinerary) = itinerary.milestones
        .filter { it.isTransit }
        .map { Triple(it.routeShortName?.trim(), it.departureStop?.trim(), it.arrivalStop?.trim()) }
}

object TripFollowWork : PluginService {
    private var shell: ServiceShell? = null
    private val context: Context get() = shell!!.context

    private var scope: CoroutineScope? = null
    private val alerted = mutableSetOf<Int>()

    private var tracker: TripTracker? = null
    private var trackedTrip: Itinerary? = null
    private var filter = FixFilter()
    private var lastFix: PositionFix? = null
    private var vehicles: VehicleWatch? = null
    private var shown: Pair<Int?, Int?>? = null

    private var sensors: SensorManager? = null
    private var shakeSum = 0.0
    private var shakeCount = 0
    private var lastRms: Double? = null
    private var lastFeel = MotionStops.Feel.STILL

    private val shaking = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
            shakeSum += (x * x + y * y + z * z).toDouble()
            shakeCount++
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onStart(shell: ServiceShell) {
        this.shell = shell
        alerted.clear()
        filter = FixFilter()
        tracker = null
        trackedTrip = null
        shown = null
        vehicles = VehicleWatch(shell.context)
        ensureChannels(shell.context)

        val first = TripFollow.trip?.milestones?.firstOrNull()
        shell.foreground(
            ONGOING_ID,
            ongoingNotification(first?.summary ?: Plug.string(R.string.tab_transit), first?.arrivalStop),
        )

        val running = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = running
        running.launch {
            CurrentPosition.updates(shell.context).collect { fix ->
                if (!filter.accept(fix, SystemClock.elapsedRealtime())) return@collect
                lastFix = fix
                currentTracker()?.onFix(fix, System.currentTimeMillis())
                publish()
                watchForLateness(fix)
            }
        }
        running.launch {
            var second = 0
            while (true) {
                delay(1_000)
                second++
                val now = System.currentTimeMillis()
                feel(now)
                currentTracker()?.onTick(now)
                publish()
                if (second % 10 == 0) launch { askAboutTheBus() }
            }
        }
        sensors = shell.context.getSystemService(SensorManager::class.java)?.also { manager ->
            manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let { sensor ->
                manager.registerListener(shaking, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onCommand(shell: ServiceShell, action: String?) {
        if (action == ACTION_STOP) {
            TripFollow.stop(shell.context)
            shell.stop()
        }
    }

    override fun onStop() {
        sensors?.unregisterListener(shaking)
        sensors = null
        scope?.cancel()
        scope = null
        tracker = null
        trackedTrip = null
        shell = null
    }

    private fun currentTracker(): TripTracker? {
        val trip = TripFollow.trip
        if (trip == null) {
            shell?.stop()
            return null
        }
        if (trip === trackedTrip) return tracker
        val legs = trackLegs(trip) { it.isRail() }
        val existing = tracker
        if (existing != null && trackedTrip?.let { TripFollow.isSameTrip(trip) } == true) {
            existing.retime(legs)
        } else {
            tracker = TripTracker(legs)
            alerted.clear()
        }
        trackedTrip = trip
        RideLog.trip(trip)
        return tracker
    }

    private fun feel(now: Long) {
        if (shakeCount == 0) {
            lastRms = null
            return
        }
        val rms = kotlin.math.sqrt(shakeSum / shakeCount)
        shakeSum = 0.0
        shakeCount = 0
        lastRms = rms
        lastFeel = MotionStops.feel(rms)
        tracker?.onShake(now, rms)
    }

    private suspend fun askAboutTheBus() {
        val watch = vehicles ?: return
        val state = TripFollow.state
        val tracked = tracker?.legs
        val leg = state?.point?.legIndex?.let { tracked?.getOrNull(it) }
            ?.takeIf { it.isTransit && !it.isRail && state.mode == TrackMode.ON_ROUTE }
        val fresh = lastFix?.takeIf { SystemClock.elapsedRealtime() - it.elapsedMs < 15_000 }
        watch.step(leg?.milestone, fresh?.point)
        TripFollow.stopsLeft = watch.stopsLeft
    }

    private fun publish() {
        val tracker = tracker ?: return
        val state = tracker.state
        TripFollow.state = state
        RideLog.placed(
            JSONObject()
                .put("kind", "tracker")
                .put("mode", state.mode.name)
                .put("why", state.why)
                .put("leg", state.point?.legIndex ?: JSONObject.NULL)
                .put("along", state.point?.alongMeters ?: JSONObject.NULL)
                .put("estimated", state.point?.estimated ?: JSONObject.NULL)
                .put("travelled", state.travelled)
                .put("offLine", state.offLine ?: JSONObject.NULL)
                .put("stations", state.stationsCounted)
                .put("rms", lastRms ?: JSONObject.NULL)
                .put("feel", lastFeel.name)
                .put("stopsLeft", TripFollow.stopsLeft ?: JSONObject.NULL),
            perSecond = true,
        )
        val point = state.point ?: return
        val leg = tracker.legs.getOrNull(point.legIndex) ?: return

        val stops = TripFollow.stopsLeft?.takeIf { leg.isTransit && !leg.isRail }
        if (shown != point.legIndex to stops) {
            shown = point.legIndex to stops
            val stop = listOfNotNull(
                leg.milestone.arrivalStop,
                stops?.let { Plug.string(R.string.transit_map_stops, it) },
            ).joinToString(" · ")
            notify(ONGOING_ID, ongoingNotification(leg.milestone.summary, stop))
        }
        if (state.mode == TrackMode.ON_ROUTE || state.mode == TrackMode.UNDERGROUND) alertIfDue(point, leg, state)
    }

    private fun alertIfDue(point: TripPoint, leg: TrackedLeg, state: TrackState) {
        if (!leg.isTransit || point.legIndex in alerted) return
        val rail = leg.isRail
        val wanted = if (rail) Settings.getMetroAlerts(context) else Settings.getBusAlerts(context)
        if (!wanted) return

        val due = if (rail) {
            val station = state.railStation
            val last = leg.stations.lastIndex
            if (station != null && last >= 1) {
                (state.railMoving && station >= last - 1) || station >= last
            } else {
                point.alongMeters >= leg.length - 600
            }
        } else {
            val left = TripFollow.stopsLeft
            val remaining = leg.length - point.alongMeters
            if (left != null) {
                left <= 1 && remaining <= 1_000
            } else {
                val spacing = leg.milestone.stopCount?.let { leg.length / it } ?: 0.0
                remaining <= maxOf(300.0, spacing)
            }
        }
        if (!due) return

        alerted.add(point.legIndex)
        note("alert")
        notify(
            ALERT_ID,
            NotificationCompat.Builder(context, ALERT_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(
                    Plug.string(if (rail) R.string.transit_alert_metro_title else R.string.transit_alert_bus_title),
                )
                .setContentText(leg.milestone.arrivalStop ?: leg.milestone.summary)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setAutoCancel(true)
                .setContentIntent(openApp())
                .build(),
        )
    }

    private fun note(what: String) = RideLog.placed(JSONObject().put("kind", what))

    private var waitingAt: Triple<Milestone, Long, LatLng>? = null

    private fun watchForLateness(fix: PositionFix) {
        if (!Settings.getTrackDelays(context)) return
        val trip = TripFollow.trip ?: return
        val now = System.currentTimeMillis() / 1000
        val waiting = waitingAt

        if (waiting != null) {
            val (_, due, stop) = waiting
            val leftTheStop = distanceMeters(stop, fix.point) > 200 || (fix.speed ?: 0f) > 6f
            if (!leftTheStop) return
            waitingAt = null
            TripStats.record(context, wasLate = now > due + 60)
            return
        }

        val ride = trip.milestones.firstOrNull { leg ->
            leg.isTransit &&
                leg.departureTime > 0 &&
                now > leg.departureTime - 600 &&
                now < leg.departureTime + 3_600 &&
                leg.departPoint?.let { distanceMeters(it, fix.point) < 150 } == true
        } ?: return
        if ((fix.speed ?: 0f) > 3f) return
        waitingAt = Triple(ride, ride.departureTime, ride.departPoint ?: return)
    }

    private fun ongoingNotification(title: String, stop: String?): Notification {
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, PluginForegroundService::class.java)
                .putExtra(PluginForegroundService.EXTRA_MODULE, TransportModule.id)
                .setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, ONGOING_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(stop.orEmpty())

            .addAction(0, Plug.string(R.string.transit_follow_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(openApp())
            .build()
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notify(id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        shell?.notify(id, notification)
    }

    private const val ACTION_STOP = "app.etsprivate.STOP_FOLLOWING"
    const val ACTION_FOLLOW = "app.etsprivate.FOLLOW_TRIP"
    private const val ONGOING_CHANNEL = "trip-following"

    private const val ALERT_CHANNEL = "trip-get-off-loud"
    const val ONGOING_ID = 4301
    const val ALERT_ID = 4302

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                ONGOING_CHANNEL,
                Plug.string(R.string.transit_follow_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL,
                Plug.string(R.string.transit_alert_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
    }
}
