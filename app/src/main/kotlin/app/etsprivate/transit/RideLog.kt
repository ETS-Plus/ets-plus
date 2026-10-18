package app.etsprivate.transit

import app.etsprivate.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.etsprivate.MainActivity
import app.etsprivate.plugin.PluginForegroundService
import app.etsprivate.plugin.PluginService
import app.etsprivate.plugin.ServiceShell
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Ride(val file: File) {
    val startedAt: Long get() = file.name.removePrefix("ride-").removeSuffix(".jsonl").toLongOrNull() ?: file.lastModified()
    val bytes: Long get() = file.length()

    val countedPoints: Int? get() = counts[file.path]?.takeIf { it.first == stamp }?.second

    suspend fun countPoints(): Int = countedPoints ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val count = runCatching { file.useLines { lines -> lines.count { it.contains("\"lat\"") } } }.getOrDefault(0)
        counts[file.path] = stamp to count
        count
    }

    private val stamp: Long get() = file.length() * 31 + file.lastModified()

    private companion object {
        val counts = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Int>>()
    }
}

object RideLog {
    var recording by mutableStateOf(false)
        private set

    var points by mutableIntStateOf(0)
        private set

    var startedAt by mutableStateOf(0L)
        private set

    const val ACTION_RECORD = "app.etsprivate.RECORD_RIDE"
    const val ACTION_STOP_RECORDING = "app.etsprivate.STOP_RECORDING"
    private const val CHANNEL = "ride-log"
    private const val NOTIFICATION = 4310

    private var writer: java.io.Writer? = null
    private var counting: kotlinx.coroutines.Job? = null
    private var lastRx = 0L
    private var lastTx = 0L
    private var listeningIn: Context? = null
    private var sensors: SensorManager? = null
    private var heading: Float? = null

    private val readings = java.util.concurrent.ConcurrentHashMap<String, FloatArray>()
    private var satellites: JSONObject? = null

    private fun directory(context: Context): File {
        val external = context.getExternalFilesDir(null)?.let { File(it, "rides") }
            ?: return File(context.filesDir, "rides").apply { mkdirs() }
        external.mkdirs()
        val old = File(context.filesDir, "rides")
        if (!recording) {
            old.listFiles()?.forEach { file ->
                val moved = File(external, file.name)
                if (!moved.exists() && runCatching { file.copyTo(moved) }.isSuccess) file.delete()
            }
        }
        return external
    }

    fun rides(context: Context): List<Ride> =
        directory(context).listFiles { file -> file.extension == "jsonl" }
            .orEmpty()
            .map(::Ride)
            .sortedByDescending { it.startedAt }

    fun start(context: Context) {
        if (recording) return
        val now = System.currentTimeMillis()
        val file = File(directory(context), "ride-$now.jsonl")
        writer = file.bufferedWriter()
        startedAt = now
        points = 0
        recording = true
        write(
            JSONObject()
                .put("type", "start")
                .put("t", now)
                .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                .put("android", Build.VERSION.SDK_INT)
                .put("trip", TripFollow.trip?.milestones?.size ?: 0),
        )
        TripFollow.trip?.let(::trip)

        PluginForegroundService.start(context, TransportModule.id, ACTION_RECORD)
    }

    fun stop(context: Context) {
        if (!recording) return
        write(JSONObject().put("type", "end").put("t", System.currentTimeMillis()).put("points", points))
        recording = false
        detach()
        runCatching { writer?.close() }
        writer = null
        if (!TripFollow.active) PluginForegroundService.stop(context)
    }

    fun delete(ride: Ride) {
        ride.file.delete()
    }

    fun share(context: Context, ride: Ride) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ride.file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, ride.file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                null,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private val fixes = object : LocationListener {
        override fun onLocationChanged(location: Location) = record(location)
        override fun onProviderEnabled(provider: String) = note("provider-on", provider)
        override fun onProviderDisabled(provider: String) = note("provider-off", provider)

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    }

    private val turning = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            readings[event.sensor.name] = event.values.copyOf()
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val matrix = FloatArray(9)
            val angles = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            SensorManager.getOrientation(matrix, angles)
            heading = ((Math.toDegrees(angles[0].toDouble()) + 360) % 360).toFloat()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val sky = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            var strongest = 0f
            var total = 0f
            for (index in 0 until status.satelliteCount) {
                if (status.usedInFix(index)) used++
                val snr = status.getCn0DbHz(index)
                if (snr > strongest) strongest = snr
                total += snr
            }
            satellites = JSONObject()
                .put("visible", status.satelliteCount)
                .put("used", used)
                .put("best", strongest)
                .put("mean", if (status.satelliteCount > 0) total / status.satelliteCount else 0f)
        }
    }

    fun http(method: String, url: String, status: Int, sent: Int, received: Int, millis: Long) {
        if (!recording) return
        write(
            JSONObject()
                .put("type", "http")
                .put("t", System.currentTimeMillis())
                .put("method", method)
                .put("url", url)
                .put("status", status)
                .put("sent", sent)
                .put("received", received)
                .put("ms", millis),
        )
    }

    private fun countBytes(context: Context) {
        val rx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
        val tx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
        if (rx < 0 || tx < 0) return
        val manager = context.getSystemService(android.net.ConnectivityManager::class.java)
        val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork)
        val transport = when {
            capabilities == null -> "none"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "other"
        }
        write(
            JSONObject()
                .put("type", "data")
                .put("t", System.currentTimeMillis())
                .put("rx", rx)
                .put("tx", tx)
                .put("rxSince", if (lastRx == 0L) 0 else rx - lastRx)
                .put("txSince", if (lastTx == 0L) 0 else tx - lastTx)
                .put("network", transport)
                .put("metered", capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false)
                .put("down", capabilities?.linkDownstreamBandwidthKbps ?: JSONObject.NULL)
                .put("up", capabilities?.linkUpstreamBandwidthKbps ?: JSONObject.NULL),
        )
        lastRx = rx
        lastTx = tx
    }

    @SuppressLint("MissingPermission")
    fun attach(context: Context) {
        if (!recording || sensors != null) return
        if (!CurrentPosition.granted(context)) return
        listeningIn = context.applicationContext
        val manager = context.getSystemService(LocationManager::class.java) ?: return
        val providers = buildList {
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        }
        providers.forEach { provider ->
            runCatching { manager.requestLocationUpdates(provider, 1000L, 0f, fixes, Looper.getMainLooper()) }
        }
        runCatching { manager.registerGnssStatusCallback(sky, android.os.Handler(Looper.getMainLooper())) }

        sensors = context.getSystemService(SensorManager::class.java)?.also { service ->
            service.getSensorList(Sensor.TYPE_ALL).forEach { sensor ->
                runCatching { service.registerListener(turning, sensor, SensorManager.SENSOR_DELAY_NORMAL) }
            }
        }
        lastRx = 0
        lastTx = 0
        counting = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()).launch {
            while (true) {
                countBytes(context)
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun detach() {
        val manager = listeningIn?.getSystemService(LocationManager::class.java)
        runCatching { manager?.removeUpdates(fixes) }
        runCatching { manager?.unregisterGnssStatusCallback(sky) }
        sensors?.unregisterListener(turning)
        sensors = null
        counting?.cancel()
        counting = null
        listeningIn = null
    }

    private fun record(location: Location) {
        val line = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("fixedAt", location.time)

            .put("elapsed", location.elapsedRealtimeNanos / 1_000_000)
            .put("provider", location.provider)
            .put("lat", location.latitude)
            .put("lon", location.longitude)
            .put("acc", if (location.hasAccuracy()) location.accuracy else JSONObject.NULL)
            .put("speed", if (location.hasSpeed()) location.speed else JSONObject.NULL)
            .put("bearing", if (location.hasBearing()) location.bearing else JSONObject.NULL)
            .put("alt", if (location.hasAltitude()) location.altitude else JSONObject.NULL)
            .put("heading", heading ?: JSONObject.NULL)
            .put("sats", satellites ?: JSONObject.NULL)
            .put("sensors", sensorSnapshot())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            line.put("speedAcc", if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else JSONObject.NULL)
            line.put("bearingAcc", if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees else JSONObject.NULL)
            line.put("altAcc", if (location.hasVerticalAccuracy()) location.verticalAccuracyMeters else JSONObject.NULL)
        }

        TripFollow.trip?.let { trip ->
            val leg = trip.milestones.firstOrNull { it.isTransit }
            line.put(
                "app",
                JSONObject()
                    .put("following", true)
                    .put("legs", trip.milestones.size)
                    .put("summary", leg?.summary.orEmpty())
                    .put("stop", leg?.arrivalStop.orEmpty()),
            )
        }
        points++
        write(line)
    }

    private fun sensorSnapshot(): JSONObject {
        val all = JSONObject()
        readings.forEach { (name, values) ->
            all.put(name, org.json.JSONArray().apply { values.forEach { put(it.toDouble()) } })
        }
        return all
    }

    fun placed(line: JSONObject, perSecond: Boolean = false) {
        if (!recording) return
        val now = System.currentTimeMillis()

        if (perSecond) {
            if (now - lastPlaced < 1_000) return
            lastPlaced = now
        }
        write(line.put("type", "placed").put("t", now))
    }

    private var lastPlaced = 0L

    fun trip(itinerary: Itinerary) {
        if (!recording) return
        val legs = org.json.JSONArray()
        itinerary.milestones.forEach { m ->
            legs.put(
                JSONObject()
                    .put("summary", m.summary)
                    .put("transit", m.isTransit)
                    .put("rail", m.isRail())
                    .put("vehicle", m.vehicleType ?: JSONObject.NULL)
                    .put("polyline", m.polyline ?: JSONObject.NULL)
                    .put("departLat", m.departPoint?.lat ?: JSONObject.NULL)
                    .put("departLng", m.departPoint?.lng ?: JSONObject.NULL)
                    .put("arriveLat", m.arrivePoint?.lat ?: JSONObject.NULL)
                    .put("arriveLng", m.arrivePoint?.lng ?: JSONObject.NULL)
                    .put("departure", m.departureTime)
                    .put("arrival", m.arrivalTime)
                    .put("duration", m.durationSeconds)
                    .put("stops", m.stopCount ?: JSONObject.NULL)
                    .put("agency", m.agencyCode ?: JSONObject.NULL)
                    .put("route", m.routeId ?: JSONObject.NULL)
                    .put("boardAt", m.departureStopId ?: JSONObject.NULL)
                    .put("arrivalStop", m.arrivalStop ?: JSONObject.NULL),
            )
        }
        write(JSONObject().put("type", "trip").put("t", System.currentTimeMillis()).put("legs", legs))
    }

    private fun note(what: String, provider: String) {
        write(JSONObject().put("type", what).put("t", System.currentTimeMillis()).put("provider", provider))
    }

    private fun write(line: JSONObject) {
        val out = writer ?: return
        runCatching {
            out.write(line.toString())
            out.write("\n")

            out.flush()
        }
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, Plug.string(R.string.ride_log_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun notification(context: Context): Notification {
        val stop = PendingIntent.getService(
            context,
            2,
            Intent(context, PluginForegroundService::class.java)
                .putExtra(PluginForegroundService.EXTRA_MODULE, TransportModule.id)
                .setAction(ACTION_STOP_RECORDING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(Plug.string(R.string.ride_log_running))
            .addAction(0, Plug.string(R.string.ride_log_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()
    }

    fun label(context: Context, ride: Ride): String {
        val format = SimpleDateFormat(app.etsprivate.ui.Clock.withTime(context, "d MMM HH:mm"), Plug.resources(context).configuration.locales[0] ?: Locale.getDefault())
        return format.format(Date(ride.startedAt))
    }
}

object TransportWork : PluginService {
    private const val NOTIFICATION = 4310

    private var following = false

    override fun onStart(shell: ServiceShell) {
        RideLog.ensureChannel(shell.context)
        if (TripFollow.active) {
            following = true
            TripFollowWork.onStart(shell)
        } else {
            shell.foreground(NOTIFICATION, RideLog.notification(shell.context))
        }
        RideLog.attach(shell.context)
    }

    override fun onCommand(shell: ServiceShell, action: String?) {
        when (action) {
            RideLog.ACTION_RECORD -> RideLog.attach(shell.context)
            RideLog.ACTION_STOP_RECORDING -> {
                RideLog.stop(shell.context)
                if (!following) shell.stop()
            }
            else -> {
                if (TripFollow.active && !following) {
                    following = true
                    TripFollowWork.onStart(shell)
                }
                TripFollowWork.onCommand(shell, action)
            }
        }
    }

    override fun onStop() {
        RideLog.detach()
        if (following) TripFollowWork.onStop()
        following = false
    }
}
