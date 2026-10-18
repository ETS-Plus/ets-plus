package app.etsprivate.transit

import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import app.etsprivate.ui.AppFeedback
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Star
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.MyLocation
import app.etsprivate.ui.bleed
import app.etsprivate.R
import androidx.compose.ui.draw.clip
import android.annotation.SuppressLint
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import app.etsprivate.cache.MemoryCache
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.theme.AppShapes
import app.etsprivate.ui.theme.Spacing
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import app.etsprivate.ui.FullScreenLoading
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.MultiPoint
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng as MapLatLng

enum class MapTheme(val labelRes: Int) {
    APP(R.string.settings_map_theme_app),
    DAY_NIGHT(R.string.settings_map_theme_daynight),
    LIGHT(R.string.settings_map_theme_light),
    DARK(R.string.settings_map_theme_dark),
}

private object MaterialColors {
    const val stopFill = 0xFF5F6368.toInt()
    const val stopStroke = 0xFFFFFFFF.toInt()
    const val labelText = 0xFF202124.toInt()
    const val labelHalo = 0xFFFFFFFF.toInt()
}

private val MONTREAL = LatLng(45.5019, -73.5674)

private const val CAMERA_CENTER = "transit/mapCenter"
private const val CAMERA_ZOOM = "transit/mapZoom"

private const val TRIP_SOURCE = "trip"
private const val STOP_SOURCE = "stops"
private const val POSITION_SOURCE = "position"
private const val LINE_STOPS_LAYER = "line-stops"
private const val LINE_STOP_LABELS_LAYER = "line-stop-labels"
private const val STATION_SOURCE = "stations"
private const val POSITION_ICON = "position-beam"

private fun headingBeam(): android.graphics.Bitmap {
    val size = 192
    val centre = size / 2f
    val reach = size / 2f - 4f
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val blue = android.graphics.Color.parseColor("#1A73E8")
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(
            centre, centre, reach,
            intArrayOf(
                android.graphics.Color.argb(150, android.graphics.Color.red(blue), android.graphics.Color.green(blue), android.graphics.Color.blue(blue)),
                android.graphics.Color.argb(0, android.graphics.Color.red(blue), android.graphics.Color.green(blue), android.graphics.Color.blue(blue)),
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
    }

    canvas.drawArc(android.graphics.RectF(centre - reach, centre - reach, centre + reach, centre + reach), -90f - 35f, 70f, true, paint)
    return bitmap
}

private enum class RiddenVehicle(val image: String) { BUS("vehicle-bus"), METRO("vehicle-metro"), TRAIN("vehicle-train") }

private fun Milestone.riddenVehicle(): RiddenVehicle {
    val type = (vehicleType ?: mode).orEmpty().uppercase()
    return when {
        listOf("METRO", "MÉTRO", "SUBWAY").any { it in type } -> RiddenVehicle.METRO
        listOf("REM", "TRAIN", "RAIL", "TRAM").any { it in type } -> RiddenVehicle.TRAIN
        else -> RiddenVehicle.BUS
    }
}

private fun vehicleSprite(kind: RiddenVehicle): android.graphics.Bitmap {
    val size = 176
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val c = size / 2f
    data class Look(val width: Float, val length: Float, val side: Int, val body: Int, val roof: Int, val stripe: Int?, val cars: Int)
    val look = when (kind) {
        RiddenVehicle.BUS -> Look(54f, 124f, 0xFF1557B0.toInt(), 0xFF1A73E8.toInt(), 0xFF5E9BEF.toInt(), null, 1)

        RiddenVehicle.METRO -> Look(50f, 156f, 0xFF0B4FA8.toInt(), 0xFFE9EEF5.toInt(), 0xFFFFFFFF.toInt(), 0xFF1A73E8.toInt(), 3)
        RiddenVehicle.TRAIN -> Look(52f, 156f, 0xFF6B7280.toInt(), 0xFFD7DCE2.toInt(), 0xFFF3F4F6.toInt(), 0xFF2E9C6A.toInt(), 2)
    }
    val left = c - look.width / 2
    val right = c + look.width / 2
    val top = c - look.length / 2
    val bottom = c + look.length / 2
    val radius = look.width * 0.28f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.argb(90, 0, 0, 0)
    paint.maskFilter = android.graphics.BlurMaskFilter(7f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    canvas.drawRoundRect(left + 3f, top + 7f, right + 3f, bottom + 7f, radius, radius, paint)
    paint.maskFilter = null

    paint.shader = android.graphics.LinearGradient(
        left, 0f, right, 0f,
        intArrayOf(look.side, look.body, look.body, look.side),
        floatArrayOf(0f, 0.3f, 0.7f, 1f),
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
    paint.shader = null

    paint.color = android.graphics.Color.argb(170, 20, 28, 40)
    canvas.drawRect(left + 2f, top + 26f, left + 6f, bottom - 14f, paint)
    canvas.drawRect(right - 6f, top + 26f, right - 2f, bottom - 14f, paint)

    paint.color = look.roof
    val inset = look.width * 0.16f
    canvas.drawRoundRect(left + inset, top + 22f, right - inset, bottom - 8f, radius * 0.6f, radius * 0.6f, paint)
    look.stripe?.let { stripe ->
        paint.color = stripe
        canvas.drawRect(c - 3f, top + 24f, c + 3f, bottom - 10f, paint)
    }

    if (look.cars > 1) {
        paint.color = android.graphics.Color.argb(120, 0, 0, 0)
        val carLength = (bottom - top) / look.cars
        for (i in 1 until look.cars) {
            val y = top + carLength * i
            canvas.drawRect(left + 1f, y - 1.5f, right - 1f, y + 1.5f, paint)
        }
    }

    if (kind == RiddenVehicle.BUS) {
        paint.color = android.graphics.Color.argb(110, 255, 255, 255)
        canvas.drawRoundRect(c - 12f, c - 18f, c + 12f, c + 4f, 4f, 4f, paint)
        canvas.drawRoundRect(c - 10f, c + 22f, c + 10f, c + 38f, 4f, 4f, paint)
    }

    paint.shader = android.graphics.LinearGradient(
        0f, top, 0f, top + 24f,
        intArrayOf(0xFF3B4C63.toInt(), 0xFF111827.toInt()),
        null,
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRoundRect(left + 4f, top + 3f, right - 4f, top + 22f, radius * 0.7f, radius * 0.7f, paint)
    paint.shader = null

    paint.color = 0xFFFFF4C2.toInt()
    canvas.drawCircle(left + 9f, top + 3f, 2.5f, paint)
    canvas.drawCircle(right - 9f, top + 3f, 2.5f, paint)
    return bitmap
}

fun mapStyleUrl(dark: Boolean): String =
    if (dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/bright"

private data class DrawnLeg(
    val milestone: Milestone,
    val points: List<LatLng>,
    val color: Color,
)

@SuppressLint("MissingPermission")
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TransitMapScreen(
    itinerary: Itinerary?,
    onBack: (() -> Unit)?,
    visible: Boolean = true,

    overlay: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)?,
) {
    val context = LocalContext.current
    val ridePlaceholder = MaterialTheme.colorScheme.primary
    val walkColor = MaterialTheme.colorScheme.outline
    val appIsDark = MaterialTheme.colorScheme.surface.isDark()

    val dark = remember(appIsDark, TransportSettings.mapThemeChanges.intValue, visible) {
        when (TransportSettings.mapTheme(context)) {
            MapTheme.APP -> appIsDark
            MapTheme.DAY_NIGHT -> isNight()
            MapTheme.LIGHT -> false
            MapTheme.DARK -> true
        }
    }

    val trackedLegs = remember(itinerary) { trackLegs(itinerary) { it.isRail() } }
    val legs = remember(trackedLegs, ridePlaceholder, walkColor) {
        trackedLegs.map { tracked ->
            val milestone = tracked.milestone
            DrawnLeg(milestone, tracked.points, milestone.serverColor() ?: if (milestone.isTransit) ridePlaceholder else walkColor)
        }
    }

    val localTracker = remember(trackedLegs) { TripTracker(trackedLegs) }
    val currentLocalTracker by rememberUpdatedState(localTracker)
    var localFixes by remember { mutableIntStateOf(0) }
    val fixFilter = remember { FixFilter() }

    var position by remember {
        mutableStateOf(CurrentPosition.lastKnownPlace(context, "")?.let { place ->
            place.lat?.let { lat -> place.lng?.let { lng -> LatLng(lat, lng) } }
        })
    }
    var granted by remember { mutableStateOf(CurrentPosition.granted(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = CurrentPosition.granted(context)
    }

    LaunchedEffect(Unit) { if (!CurrentPosition.granted(context)) permission.launch(CurrentPosition.PERMISSIONS) }
    var gpsBearing by remember { mutableStateOf<Float?>(null) }
    var compass by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(granted) {
        if (granted) {
            CurrentPosition.updates(context).collect { fix ->

                if (!fixFilter.accept(fix, android.os.SystemClock.elapsedRealtime())) return@collect
                position = fix.point
                currentLocalTracker.onFix(fix, System.currentTimeMillis())
                localFixes++

                gpsBearing = if (fix.bearing != null && (fix.speed ?: 0f) > 3f) fix.bearing else null
            }
        }
    }

    LaunchedEffect(Unit) { DeviceHeading.updates(context).collect { compass = it } }
    val bearing = gpsBearing ?: compass

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    var centredOnUser by remember { mutableStateOf(itinerary != null) }

    val following = TripFollow.active && itinerary != null

    var cameraFollows by remember { mutableStateOf(true) }

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(following) {
        while (following) {
            kotlinx.coroutines.delay(1_000)
            tick++
        }
    }

    val localState = remember(localTracker, localFixes) { localTracker.state }
    val followedHere = following && itinerary != null && TripFollow.isSameTrip(itinerary)
    val progress = (if (followedHere) TripFollow.state else localState)?.point

    val waitingFor = remember(legs, progress, position, tick) {
        val now = System.currentTimeMillis() / 1000
        val toCome = legs.filter { it.milestone.isTransit && it.milestone.arrivalTime > now }

        val atTheStop = position?.let { here ->
            toCome.firstOrNull { leg ->
                leg.milestone.departPoint?.let { distanceMeters(it, here) < 80 } == true
            }
        }
        if (atTheStop != null) return@remember atTheStop
        val riding = progress?.legIndex?.let { legs.getOrNull(it) }?.takeIf { it.milestone.isTransit }
        if (riding != null) null else toCome.firstOrNull()
    }

    LaunchedEffect(waitingFor, tick / 5) {
        if (!RideLog.recording) return@LaunchedEffect
        val leg = waitingFor?.milestone
        RideLog.placed(
            org.json.JSONObject()
                .put("kind", "waiting")
                .put("waiting", leg?.summary ?: org.json.JSONObject.NULL)
                .put("departsAt", leg?.departureTime ?: org.json.JSONObject.NULL)
                .put(
                    "toDepartStop",
                    position?.let { here -> leg?.departPoint?.let { distanceMeters(it, here) } }
                        ?: org.json.JSONObject.NULL,
                )
                .put("riding", progress?.legIndex ?: org.json.JSONObject.NULL),
        )
    }

    var departures by remember { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(waitingFor, following, tick / 180) {
        val leg = waitingFor?.milestone
        if (leg == null || !following) {
            departures = emptyList()
            return@LaunchedEffect
        }
        val cacheKey = "transit/departures/${leg.departureStop}/${leg.routeShortName}"
        val cached = MemoryCache.get<Pair<Long, List<Long>>>(cacheKey)
        val now = System.currentTimeMillis()

        if (cached != null && now - cached.first < 170_000 && cached.second.any { it * 1000 > now }) {
            departures = cached.second.filter { it * 1000 > now }
            return@LaunchedEffect
        }
        val client = ChronoClient(context.applicationContext)
        val fresh = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                client.ensureAuthenticated()
                client.nextDepartures(leg)
            }.getOrDefault(emptyList())
        }
        if (fresh.isNotEmpty()) {
            MemoryCache.put(cacheKey, now to fresh)
            departures = fresh
        }
    }

    LaunchedEffect(departures, waitingFor, following) {
        val waiting = waitingFor ?: return@LaunchedEffect
        val plan = itinerary ?: return@LaunchedEffect
        if (!following) return@LaunchedEffect
        val now = System.currentTimeMillis() / 1000
        val earlier = departures.filter { it > now }.minOrNull() ?: return@LaunchedEffect
        val plannedStart = waiting.milestone.departureTime

        if (earlier >= plannedStart - 60) return@LaunchedEffect
        val index = plan.milestones.indexOfFirst { it === waiting.milestone }
        if (index < 0) return@LaunchedEffect
        val shift = plannedStart - earlier
        val adjusted = plan.copy(
            milestones = plan.milestones.mapIndexed { i, milestone ->
                if (i == index) {
                    milestone.copy(departureTime = milestone.departureTime - shift, arrivalTime = milestone.arrivalTime - shift)
                } else {
                    milestone
                }
            },
        )
        TripFollow.start(context, adjusted)
    }

    LaunchedEffect(map, styleReady, legs, progress) {
        val ready = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        ready.style?.let { style ->
            (style.getSourceAs<GeoJsonSource>(TRIP_SOURCE))?.setGeoJson(tripFeatures(legs, progress))
            (style.getSourceAs<GeoJsonSource>(STOP_SOURCE))?.setGeoJson(stopFeatures(legs, progress))
        }
    }

    val onTransitLeg = progress?.legIndex?.let { legs.getOrNull(it)?.milestone?.isTransit } == true

    val snapped = remember(progress, position, onTransitLeg) {
        if (onTransitLeg) {
            progress?.let { p -> pointAtMeters(legs[p.legIndex].points, p.alongMeters) }
        } else {
            position
        }
    }
    val smoothLat by animateFloatAsState(
        targetValue = (snapped?.lat ?: 0.0).toFloat(),
        animationSpec = if (onTransitLeg) tween(900) else snap(),
        label = "lat",
    )
    val smoothLng by animateFloatAsState(
        targetValue = (snapped?.lng ?: 0.0).toFloat(),
        animationSpec = if (onTransitLeg) tween(900) else snap(),
        label = "lng",
    )

    val effectiveBearing = if (onTransitLeg) {
        progress?.let { p -> bearingAtMeters(legs[p.legIndex].points, p.alongMeters) } ?: bearing
    } else {
        bearing
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(following, map, styleReady, visible, position != null, resumed) {
        val ready = map ?: return@LaunchedEffect
        if (!visible || !following || !styleReady) return@LaunchedEffect

        val here = snapped ?: position ?: return@LaunchedEffect
        cameraFollows = true
        ready.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(MapLatLng(here.lat, here.lng))
                    .zoom(ready.cameraPosition.zoom.coerceAtLeast(17.0))
                    .bearing(effectiveBearing?.toDouble() ?: ready.cameraPosition.bearing)
                    .tilt(45.0)
                    .build(),
            ),
            600,
        )
    }

    val ridden = progress?.takeIf { following && onTransitLeg && it.alongMeters > 25 }
        ?.let { legs.getOrNull(it.legIndex)?.milestone?.riddenVehicle() }
    LaunchedEffect(map, styleReady, smoothLat, smoothLng, following, effectiveBearing, ridden) {
        val ready = map ?: return@LaunchedEffect
        if (snapped == null) return@LaunchedEffect
        val here = LatLng(smoothLat.toDouble(), smoothLng.toDouble())
        if (!styleReady) return@LaunchedEffect
        ready.style?.getSourceAs<GeoJsonSource>(POSITION_SOURCE)?.setGeoJson(
            Feature.fromGeometry(Point.fromLngLat(here.lng, here.lat)).apply {
                addNumberProperty("bearing", effectiveBearing ?: 0f)
                addBooleanProperty("facing", effectiveBearing != null)

                addStringProperty("vehicle", ridden?.image ?: "")
            },
        )

        val zoom = if (following && cameraFollows) maxOf(ready.cameraPosition.zoom, 17.0) else ready.cameraPosition.zoom
        val close = zoom >= 16.0
        when {
            cameraFollows && (following || close) -> ready.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(MapLatLng(here.lat, here.lng))
                        .zoom(zoom)
                        .bearing(if (close) effectiveBearing?.toDouble() ?: ready.cameraPosition.bearing else ready.cameraPosition.bearing)
                        .tilt(if (following && close) 45.0 else 0.0)
                        .build(),
                ),
            )
            !centredOnUser && cameraFollows -> {
                centredOnUser = true
                ready.animateCamera(CameraUpdateFactory.newLatLngZoom(MapLatLng(here.lat, here.lng), 16.0))
            }
        }
    }

    LaunchedEffect(map, styleReady, legs, progress?.legIndex) {
        val ready = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val ridden = progress?.legIndex?.let { legs.getOrNull(it)?.takeIf { leg -> leg.milestone.isTransit } }
        val lines = if (ridden != null) listOf(ridden) else legs.filter { it.milestone.isTransit }
        val layer = ready.style?.getLayer(LINE_STOPS_LAYER) as? CircleLayer ?: return@LaunchedEffect
        val busLines = lines.filter { !it.milestone.isRail() }

        val railLines = legs.filter { it.milestone.isRail() }

        val visibility = if (legs.isEmpty()) Property.VISIBLE else Property.NONE
        listOf("poi_transit", "poi_r1", "poi_r7", "poi_r20").forEach { id ->
            ready.style?.getLayer(id)?.setProperties(PropertyFactory.visibility(visibility))
        }

        val tripEnds = legs.filter { it.milestone.isTransit }
            .flatMap { listOfNotNull(it.points.firstOrNull(), it.points.lastOrNull()) }
        val stations = railLines.flatMap { stationsAlong(it.points) }
            .distinct()
            .filterNot { station -> tripEnds.any { distanceMeters(it, station.point) < 120 } }
        ready.style?.getSourceAs<GeoJsonSource>(STATION_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(
                stations.map { station ->
                    Feature.fromGeometry(Point.fromLngLat(station.lng, station.lat)).apply {
                        addStringProperty("name", station.name)
                    }
                },
            ),
        )

        val ends = legs.filter { it.milestone.isTransit }
            .flatMap { listOfNotNull(it.points.firstOrNull(), it.points.lastOrNull()) }
        layer.setFilter(
            busLines.takeIf { it.isNotEmpty() }?.let { lines ->
                val nearLine = onLine(lines, BUS_STOPS, 25)
                if (ends.isEmpty()) {
                    nearLine
                } else {
                    Expression.all(
                        nearLine,
                        Expression.gt(
                            Expression.distance(
                                MultiPoint.fromLngLats(ends.map { Point.fromLngLat(it.lng, it.lat) }),
                            ),
                            Expression.literal(140),
                        ),
                    )
                }
            } ?: Expression.literal(false),
        )
    }

    val immersive = itinerary != null && onBack != null

    androidx.compose.runtime.DisposableEffect(visible, dark) {
        val owner = Any()
        if (visible) app.etsprivate.ui.Immersive.claimStatusBar(owner, light = !dark)
        onDispose { app.etsprivate.ui.Immersive.releaseStatusBar(owner) }
    }
    val statusTopPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(androidx.compose.ui.platform.LocalDensity.current)

    val density = androidx.compose.ui.platform.LocalDensity.current
    var headPx by remember(following) { mutableIntStateOf(0) }
    val navInset = if (immersive) {
        androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }
    val peek = when {
        itinerary == null -> 0.dp
        headPx == 0 -> (if (following) 112.dp else 236.dp) + navInset

        else -> 48.dp + with(density) { headPx.toDp() } + navInset
    }

    var endsPx by remember { mutableIntStateOf(0) }
    val fitPadding = with(density) {
        val margin = 40.dp.roundToPx()
        intArrayOf(margin, statusTopPx + endsPx + 12.dp.roundToPx() + margin, margin, peek.roundToPx() + margin)
    }
    val currentFit by rememberUpdatedState(fitPadding)

    androidx.compose.runtime.SideEffect {
        app.etsprivate.ui.Immersive.snackbarLift = if (immersive && visible) (peek - navInset).coerceAtLeast(0.dp) else 0.dp
    }

    var wasFollowing by remember { mutableStateOf(following) }
    LaunchedEffect(following) {
        val stopped = wasFollowing && !following
        wasFollowing = following
        if (!stopped) return@LaunchedEffect
        val ready = map ?: return@LaunchedEffect

        kotlinx.coroutines.delay(280)
        val bounds = legs.flatMap { it.points }.boundsOrNull() ?: return@LaunchedEffect
        val (l, t, r, b) = currentFit.toList()
        ready.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0.0, 0.0, l, t, r, b), 700)
    }

    LaunchedEffect(fitPadding.toList(), styleReady) {
        if (following || itinerary == null || !styleReady) return@LaunchedEffect
        val ready = map ?: return@LaunchedEffect
        val bounds = legs.flatMap { it.points }.boundsOrNull() ?: return@LaunchedEffect
        val (l, t, r, b) = fitPadding.toList()
        ready.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, l, t, r, b), 300)
    }

    LaunchedEffect(legs, dark) {
        if (legs.isEmpty()) return@LaunchedEffect
        if (!app.etsprivate.settings.Settings.getPrepareTripsOnWifi(context)) return@LaunchedEffect
        if (!NetworkCost.free(context)) return@LaunchedEffect
        val points = legs.flatMap { it.points }
        MapDownload.downloadTrip(context, points, dark)
    }

    val activity = context as? android.app.Activity
    val onLockScreen = remember { app.etsprivate.settings.Settings.getMapOnLockScreen(context) }
    DisposableEffect(following, onLockScreen) {
        if (following) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (onLockScreen && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(true)
            }
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(false)
            }
        }
    }

    BackHandler(enabled = visible && onBack != null) {
        onBack?.invoke()
    }

    androidx.compose.runtime.DisposableEffect(immersive, visible) {
        app.etsprivate.ui.Immersive.hideBottomBar = immersive && visible
        onDispose {
            app.etsprivate.ui.Immersive.hideBottomBar = false
            app.etsprivate.ui.Immersive.snackbarLift = 0.dp
        }
    }
    val sheet = androidx.compose.material3.rememberBottomSheetScaffoldState(
        bottomSheetState = androidx.compose.material3.rememberStandardBottomSheetState(skipHiddenState = true),
    )

    var layoutPx by remember { mutableIntStateOf(0) }
    val peekPx = with(density) { peek.toPx() }
    val revealPx = with(density) { 64.dp.toPx() }
    val stepsShown: () -> Float = {
        val offset = runCatching { sheet.bottomSheetState.requireOffset() }.getOrNull()
        if (offset == null || layoutPx == 0) 0f else ((layoutPx - peekPx - offset) / revealPx).coerceIn(0f, 1f)
    }
    val sheetScope = androidx.compose.runtime.rememberCoroutineScope()

    val sheetScroll = androidx.compose.foundation.rememberScrollState()
    LaunchedEffect(sheet) {
        androidx.compose.runtime.snapshotFlow { sheet.bottomSheetState.targetValue }.collect { target ->
            if (target == androidx.compose.material3.SheetValue.PartiallyExpanded && sheetScroll.value > 0) {
                sheetScroll.animateScrollTo(0)
            }
        }
    }

    val toastLift by androidx.compose.animation.core.animateDpAsState(
        if (AppFeedback.hostState.currentSnackbarData != null) 72.dp else 0.dp,
        label = "map-fab-lift",
    )

    var favorites by remember { mutableStateOf(SavedTrips.load(context)) }
    var removingFavorite by remember { mutableStateOf<SavedTrip?>(null) }
    val savedMessage = pluginString(R.string.transit_trip_saved)
    val endsTrip = MemoryCache.get<Place>("transit/originPlace")?.let { from ->
        MemoryCache.get<Place>("transit/destPlace")?.let { to -> SavedTrip(from, to) }
    }
    val favoriteTrip = endsTrip?.let { trip ->
        favorites.firstOrNull { it.origin.name == trip.origin.name && it.destination.name == trip.destination.name }
    }
    removingFavorite?.takeIf { visible }?.let { trip ->
        app.etsprivate.ui.ConfirmDialog(
            title = pluginString(R.string.transit_remove_trip_title),
            onDismissRequest = { removingFavorite = null },
            confirmLabel = pluginString(R.string.transit_remove_trip),
            onConfirm = {
                favorites = SavedTrips.remove(context, trip)
                removingFavorite = null
            },
            dismissLabel = pluginString(R.string.action_cancel),
        ) {
        }
    }
    val focusOn: (LatLng) -> Unit = { point -> map?.animateCamera(CameraUpdateFactory.newLatLngZoom(MapLatLng(point.lat, point.lng), 16.0)) }
    androidx.compose.material3.BottomSheetScaffold(
        modifier = Modifier.layout { measurable, constraints ->
            val extra = statusTopPx
            val tall = if (constraints.hasBoundedHeight) constraints.maxHeight + extra else constraints.maxHeight
            val placeable = measurable.measure(constraints.copy(minHeight = tall, maxHeight = tall))
            layout(placeable.width, constraints.maxHeight) { placeable.place(0, -extra) }
        },
        scaffoldState = sheet,
        sheetPeekHeight = peek,
        sheetContainerColor = app.etsprivate.ui.groupColor,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetShadowElevation = 8.dp,
        sheetDragHandle = if (itinerary == null) null else ({ androidx.compose.material3.BottomSheetDefaults.DragHandle() }),
        containerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            val sheetRoom = with(density) { (layoutPx - statusTopPx).coerceAtLeast(0).toDp() } - 48.dp
            if (itinerary != null) Box(modifier = if (layoutPx > 0) Modifier.heightIn(max = sheetRoom) else Modifier) {
                if (following) {
                    FollowSheet(
                        itinerary = itinerary,
                        legs = legs,
                        activeLeg = progress?.legIndex,

                        arrivalTime = itinerary.arrivalTime.takeIf { it > 0 }
                            ?: legs.lastOrNull { it.milestone.arrivalTime > 0 }?.milestone?.arrivalTime
                            ?: 0,
                        remaining = remainingMeters(legs, progress),
                        onStop = {
                            TripFollow.stop(context)
                            cameraFollows = true

                            sheetScope.launch { sheet.bottomSheetState.partialExpand() }
                        },
                        onFocus = focusOn,
                        onHeadHeight = { headPx = it },
                        stepsShown = stepsShown,
                        scroll = sheetScroll,
                        onSteps = { sheetScope.launch { sheet.bottomSheetState.expand() } },
                    )
                } else {
                    RoutePanel(
                        itinerary = itinerary,
                        legs = legs,
                        onStart = { TripFollow.start(context, itinerary) },
                        onClose = onBack,
                        favorite = favoriteTrip != null,
                        onFavorite = endsTrip?.let { trip ->
                            {
                                val kept = favoriteTrip
                                if (kept != null) {
                                    removingFavorite = kept
                                } else {
                                    favorites = SavedTrips.add(context, trip)
                                    AppFeedback.show(savedMessage)
                                }
                            }
                        },
                        onFocus = focusOn,
                        onHeadHeight = { headPx = it },
                        stepsShown = stepsShown,
                        scroll = sheetScroll,
                    )
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().onSizeChanged { layoutPx = it.height }) {
            MapLibreView(
                dark = dark,
                visible = visible,
                loadColor = MaterialTheme.colorScheme.surface.toArgb(),
                onRelease = {
                    map = null
                    styleReady = false
                },
                onMap = { ready, style ->
                    map = ready

                    ready.uiSettings.isAttributionEnabled = false
                    ready.uiSettings.isLogoEnabled = false

                    ready.uiSettings.isCompassEnabled = false
                    style.addSource(GeoJsonSource(TRIP_SOURCE, tripFeatures(legs, progress)))
                    style.addSource(GeoJsonSource(STOP_SOURCE, stopFeatures(legs)))
                    style.addSource(GeoJsonSource(POSITION_SOURCE))
                    style.addSource(GeoJsonSource(STATION_SOURCE))
                    style.addLayer(
                        LineLayer("trip-line", TRIP_SOURCE).withProperties(
                            PropertyFactory.lineColor(org.maplibre.android.style.expressions.Expression.get("color")),
                            PropertyFactory.lineOpacity(org.maplibre.android.style.expressions.Expression.get("opacity")),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                    )
                    style.addLayer(
                        CircleLayer("trip-stops", STOP_SOURCE).withProperties(
                            PropertyFactory.circleColor(MaterialColors.stopStroke),
                            PropertyFactory.circleRadius(6f),
                            PropertyFactory.circleStrokeColor(org.maplibre.android.style.expressions.Expression.get("color")),
                            PropertyFactory.circleStrokeWidth(3f),
                        ),
                    )

                    style.addLayer(
                        CircleLayer(LINE_STOPS_LAYER, "openmaptiles").apply {
                            sourceLayer = "poi"
                            minZoom = 13f
                            setFilter(Expression.literal(false))
                        }.withProperties(
                            PropertyFactory.circleRadius(4f),
                            PropertyFactory.circleColor(MaterialColors.stopFill),
                            PropertyFactory.circleStrokeColor(MaterialColors.stopStroke),
                            PropertyFactory.circleStrokeWidth(2f),
                        ),
                    )

                    style.addLayer(
                        SymbolLayer("trip-stop-labels", STOP_SOURCE).apply { minZoom = 13.5f }.withProperties(
                            PropertyFactory.textField(org.maplibre.android.style.expressions.Expression.get("name")),
                            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                            PropertyFactory.textSize(11f),
                            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                            PropertyFactory.textOffset(arrayOf(0f, -0.9f)),
                            PropertyFactory.textColor(MaterialColors.labelText),
                            PropertyFactory.textHaloColor(MaterialColors.labelHalo),
                            PropertyFactory.textHaloWidth(1.4f),
                            PropertyFactory.textOptional(true),
                        ),
                    )
                    style.addLayer(
                        CircleLayer("stations", STATION_SOURCE).apply { minZoom = 12f }.withProperties(
                            PropertyFactory.circleRadius(5f),
                            PropertyFactory.circleColor(MaterialColors.stopStroke),
                            PropertyFactory.circleStrokeColor(MaterialColors.stopFill),
                            PropertyFactory.circleStrokeWidth(3f),
                        ),
                    )
                    style.addLayer(
                        SymbolLayer(LINE_STOP_LABELS_LAYER, STATION_SOURCE).apply { minZoom = 13.5f }.withProperties(
                            PropertyFactory.textField(Expression.get("name")),
                            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                            PropertyFactory.textSize(11f),
                            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                            PropertyFactory.textOffset(arrayOf(0f, -0.8f)),
                            PropertyFactory.textColor(MaterialColors.labelText),

                            PropertyFactory.textHaloColor(MaterialColors.labelHalo),
                            PropertyFactory.textHaloWidth(1.4f),
                            PropertyFactory.textAllowOverlap(false),
                            PropertyFactory.textOptional(true),
                        ),
                    )

                    style.addImage(POSITION_ICON, headingBeam())
                    RiddenVehicle.entries.forEach { style.addImage(it.image, vehicleSprite(it)) }
                    val onBoard = org.maplibre.android.style.expressions.Expression.neq(
                        org.maplibre.android.style.expressions.Expression.get("vehicle"),
                        org.maplibre.android.style.expressions.Expression.literal(""),
                    )
                    fun hiddenOnBoard(shown: Float) = org.maplibre.android.style.expressions.Expression.switchCase(
                        onBoard,
                        org.maplibre.android.style.expressions.Expression.literal(0f),
                        org.maplibre.android.style.expressions.Expression.literal(shown),
                    )

                    style.addLayer(
                        SymbolLayer("position-heading", POSITION_SOURCE).withProperties(
                            PropertyFactory.iconImage(POSITION_ICON),
                            PropertyFactory.iconRotate(org.maplibre.android.style.expressions.Expression.get("bearing")),
                            PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                            PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconOpacity(
                                org.maplibre.android.style.expressions.Expression.switchCase(
                                    onBoard,
                                    org.maplibre.android.style.expressions.Expression.literal(0f),
                                    org.maplibre.android.style.expressions.Expression.get("facing"),
                                    org.maplibre.android.style.expressions.Expression.literal(1f),
                                    org.maplibre.android.style.expressions.Expression.literal(0f),
                                ),
                            ),
                        ),
                    )

                    style.addLayer(
                        CircleLayer("position-shadow", POSITION_SOURCE).withProperties(
                            PropertyFactory.circleColor(android.graphics.Color.BLACK),
                            PropertyFactory.circleOpacity(hiddenOnBoard(0.18f)),
                            PropertyFactory.circleRadius(12f),
                            PropertyFactory.circleBlur(0.8f),
                            PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                        ),
                    )

                    style.addLayer(
                        CircleLayer("position-dot", POSITION_SOURCE).withProperties(
                            PropertyFactory.circleColor(android.graphics.Color.parseColor("#1A73E8")),
                            PropertyFactory.circleRadius(8f),
                            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            PropertyFactory.circleStrokeWidth(3f),
                            PropertyFactory.circleOpacity(hiddenOnBoard(1f)),
                            PropertyFactory.circleStrokeOpacity(hiddenOnBoard(1f)),
                            PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP),
                        ),
                    )

                    style.addLayer(
                        SymbolLayer("position-vehicle", POSITION_SOURCE).withProperties(
                            PropertyFactory.iconImage(org.maplibre.android.style.expressions.Expression.get("vehicle")),
                            PropertyFactory.iconRotate(org.maplibre.android.style.expressions.Expression.get("bearing")),
                            PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                            PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconSize(
                                org.maplibre.android.style.expressions.Expression.interpolate(
                                    org.maplibre.android.style.expressions.Expression.linear(),
                                    org.maplibre.android.style.expressions.Expression.zoom(),
                                    org.maplibre.android.style.expressions.Expression.stop(13, 0.45f),
                                    org.maplibre.android.style.expressions.Expression.stop(17, 0.8f),
                                ),
                            ),
                        ),
                    )

                    style.getLayer("highway-name-minor")?.minZoom = 13.5f
                    style.getLayer("highway-name-path")?.minZoom = 14.5f
                    style.getLayer("highway-name-major")?.minZoom = 11f
                    styleReady = true

                    val here = position

                    val followAt = here ?: snapped ?: legs.firstOrNull()?.points?.firstOrNull()
                    if (following && followAt != null) {
                        ready.cameraPosition = CameraPosition.Builder()
                            .target(MapLatLng(followAt.lat, followAt.lng))
                            .zoom(17.0)
                            .tilt(45.0)
                            .build()
                        centredOnUser = true
                    } else {
                        val tripBounds = legs.flatMap { it.points }.boundsOrNull()
                        if (tripBounds != null) {
                            val (l, t, r, b) = currentFit.toList()
                            ready.animateCamera(CameraUpdateFactory.newLatLngBounds(tripBounds, l, t, r, b))
                        } else {
                            ready.cameraPosition = CameraPosition.Builder()
                                .target(MapLatLng(here?.lat ?: MONTREAL.lat, here?.lng ?: MONTREAL.lng))
                                .zoom(if (here != null) 15.0 else 13.0)
                                .build()
                            if (here != null) centredOnUser = true
                        }
                    }
                    ready.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            cameraFollows = false
                        }
                    }
                    ready.addOnCameraIdleListener {
                        val camera = ready.cameraPosition
                        MemoryCache.put(CAMERA_CENTER, LatLng(camera.target?.latitude ?: 0.0, camera.target?.longitude ?: 0.0))
                        MemoryCache.put(CAMERA_ZOOM, camera.zoom)
                    }
                },
            )

            val activeLeg = progress?.legIndex?.let { legs.getOrNull(it) }
            if (following) {
                NavigationBanner(
                    leg = activeLeg?.milestone ?: waitingFor?.milestone,
                    departures = departures,
                    onStop = {
                        TripFollow.stop(context)
                        cameraFollows = true
                    },
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(Spacing.default),
                )
            } else if (itinerary != null) {
                RouteEnds(
                    itinerary,
                    onBack,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .onSizeChanged { endsPx = it.height },
                )
            } else if (overlay != null) {
                overlay()
            } else if (onBack != null) {
                androidx.compose.material3.FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = pluginString(R.string.action_back))
                }
            }

            if (!styleReady) {
                FullScreenLoading(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
            }

            var showingSources by remember { mutableStateOf(false) }

            Surface(
                onClick = { showingSources = true },
                shape = AppShapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = peek + 8.dp)
                    .offset(y = -toastLift),
            ) {
                Text(
                    pluginString(R.string.transit_map_credit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.half, vertical = 2.dp),
                )
            }

            if (showingSources) {
                AppBottomSheet(
                    title = pluginString(R.string.transit_map_sources),
                    onDismissRequest = { showingSources = false },
                    dismissLabel = pluginString(R.string.action_cancel),
                ) { close ->
                    MAP_SOURCES.forEach { (name, licence, url) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bleed(Spacing.half)
                                .clip(AppShapes.small)
                                .clickable {
                                    close {
                                        showingSources = false
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(url),
                                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = Spacing.half, vertical = Spacing.default),
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                licence,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    val here = position
                    cameraFollows = true
                    if (here != null) {
                        val zoom = (map?.cameraPosition?.zoom ?: 17.0).coerceAtLeast(17.0)
                        if (following) {
                            map?.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(MapLatLng(here.lat, here.lng))
                                        .zoom(zoom)
                                        .bearing(effectiveBearing?.toDouble() ?: map?.cameraPosition?.bearing ?: 0.0)
                                        .tilt(45.0)
                                        .build(),
                                ),
                            )
                        } else {
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(MapLatLng(here.lat, here.lng), zoom))
                        }
                    } else if (!granted) {
                        permission.launch(CurrentPosition.PERMISSIONS)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = peek + 16.dp).offset(y = -toastLift),
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = pluginString(R.string.transit_my_position))
            }
        }
    }
}

@Deprecated("Kept for modules built against it", level = DeprecationLevel.HIDDEN)
@Composable
fun TransitMapScreen(itinerary: Itinerary?, onBack: (() -> Unit)?, visible: Boolean = true) =
    TransitMapScreen(itinerary, onBack, visible, overlay = null)

@Composable
private fun MapLibreView(
    dark: Boolean,
    visible: Boolean,
    loadColor: Int,
    onRelease: () -> Unit,
    onMap: (MapLibreMap, Style) -> Unit,
) {
    val context = Plug.context(LocalContext.current)
    val styleUrl = mapStyleUrl(dark)

    remember { MapLibre.getInstance(Plug.context(context.applicationContext)) }

    val mapView = remember {
        MapView(context, MapLibreMapOptions.createFromAttributes(context).foregroundLoadColor(loadColor))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            onRelease()
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(styleUrl) {
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl) { style -> onMap(map, style) }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view -> view.visibility = if (visible) View.VISIBLE else View.INVISIBLE },
    )
}

private fun tripFeatures(legs: List<DrawnLeg>, progress: TripPoint?): FeatureCollection {
    fun line(points: List<LatLng>, color: String, opacity: Float) =
        Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })).apply {
            addStringProperty("color", color)
            addNumberProperty("opacity", opacity)
        }

    val features = mutableListOf<Feature>()
    legs.forEachIndexed { index, leg ->
        val color = leg.color.toHex()
        when {
            progress == null || index > progress.legIndex -> features.add(line(leg.points, color, 1f))
            index < progress.legIndex -> features.add(line(leg.points, color, PASSED_OPACITY))
            else -> {
                val (behind, ahead) = splitAtMeters(leg.points, progress.alongMeters)
                if (behind.size > 1) features.add(line(behind, color, PASSED_OPACITY))
                if (ahead.size > 1) features.add(line(ahead, color, 1f))
            }
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private const val PASSED_OPACITY = 0.3f

private fun stopFeatures(legs: List<DrawnLeg>, progress: TripPoint? = null): FeatureCollection =
    FeatureCollection.fromFeatures(
        legs.filter { it.milestone.isTransit }.flatMap { leg ->

            listOfNotNull(
                leg.points.firstOrNull()?.let { it to leg.milestone.departureStop },
                leg.points.lastOrNull()?.let { it to leg.milestone.arrivalStop },
            ).map { (point, name) ->
                Feature.fromGeometry(Point.fromLngLat(point.lng, point.lat)).apply {
                    addStringProperty("color", leg.color.toHex())
                    addStringProperty("name", name.orEmpty())
                }
            }
        },
    )

private fun travelled(legs: List<DrawnLeg>, point: TripPoint): Double =
    legs.take(point.legIndex).sumOf { lengthMeters(it.points) } + point.alongMeters

@Composable
private fun NavigationBanner(
    leg: Milestone?,
    departures: List<Long> = emptyList(),
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (leg?.isTransit == true) leg.icon() else Icons.Outlined.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    leg?.summary ?: pluginString(R.string.transit_follow_running),
                    style = MaterialTheme.typography.titleLarge,
                )
                val detail = leg?.arrivalStop.orEmpty()
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f))
                }
                if (departures.isNotEmpty()) {
                    val clock = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(androidx.compose.ui.platform.LocalContext.current), Locale.getDefault())
                    Text(
                        pluginString(
                            R.string.transit_next_departures,
                            departures.joinToString(" · ") { clock.format(Date(it * 1000)) },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowSheet(
    itinerary: Itinerary,
    legs: List<DrawnLeg>,
    activeLeg: Int?,
    arrivalTime: Long,
    remaining: Double,
    onStop: () -> Unit,
    onFocus: (LatLng) -> Unit,
    onHeadHeight: (Int) -> Unit,
    stepsShown: () -> Float,
    scroll: androidx.compose.foundation.ScrollState,
    onSteps: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = app.etsprivate.settings.LocaleHelper.locale(context)
    val clock = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(context), locale)
    val minutes = ((arrivalTime - System.currentTimeMillis() / 1000) / 60).toInt().coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scroll).navigationBarsPadding().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().onSizeChanged { onHeadHeight(it.height) }.padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onSteps,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = pluginString(R.string.transit_steps))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        arrivalTime <= 0 -> pluginString(R.string.transit_follow_running)

                        minutes >= 60 -> "${minutes / 60} h ${"%02d".format(minutes % 60)}"
                        else -> "$minutes min"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                val under = listOfNotNull(
                    remaining.takeIf { it > 0 }?.let { distanceLabel(it, locale) },
                    arrivalTime.takeIf { it > 0 }?.let { clock.format(Date(it * 1000)) },
                ).joinToString(" · ")
                if (under.isNotEmpty()) {
                    Text(under, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = pluginString(R.string.transit_follow_stop))
            }
        }
        Column(modifier = Modifier.graphicsLayer { alpha = stepsShown() }) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            RouteSteps(itinerary, legs, activeLeg, onFocus)
        }
    }
}

private fun remainingMeters(legs: List<DrawnLeg>, progress: TripPoint?): Double {
    if (legs.isEmpty()) return 0.0
    val total = legs.sumOf { lengthMeters(it.points) }
    val done = progress?.let { travelled(legs, it) } ?: 0.0
    return (total - done).coerceAtLeast(0.0)
}

private fun distanceLabel(meters: Double, locale: Locale = Locale.getDefault()): String =
    if (meters >= 1000) String.format(locale, "%.1f km", meters / 1000) else "${meters.toInt()} m"

private val MAP_SOURCES = listOf(
    Triple("OpenStreetMap", "ODbL 1.0", "https://www.openstreetmap.org/copyright"),
    Triple("OpenFreeMap", "openfreemap.org", "https://openfreemap.org/"),
    Triple("MapLibre", "BSD-2-Clause", "https://maplibre.org/"),
)

private fun onLine(lines: List<DrawnLeg>, subclasses: List<String>, meters: Int): Expression {
    val shape = MultiLineString.fromLineStrings(
        lines.map { line -> LineString.fromLngLats(line.points.map { Point.fromLngLat(it.lng, it.lat) }) },
    )
    return Expression.all(
        Expression.match(
            Expression.get("subclass"),
            Expression.literal(false),
            *subclasses.map { Expression.stop(it, true) }.toTypedArray(),
        ),
        Expression.lt(Expression.distance(shape), Expression.literal(meters)),
    )
}

private val BUS_STOPS = listOf("bus_stop", "bus_station")
private val RAIL_STOPS = listOf("subway", "station", "halt", "tram_stop")

private fun durationLabel(itinerary: Itinerary): String {
    val seconds = if (itinerary.departureTime > 0 && itinerary.arrivalTime > itinerary.departureTime) {
        itinerary.arrivalTime - itinerary.departureTime
    } else {
        itinerary.milestones.sumOf { it.durationSeconds }.toLong()
    }
    val minutes = (seconds / 60).toInt().coerceAtLeast(1)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${"%02d".format(minutes % 60)} min"
}

@Composable
private fun RoutePanel(
    itinerary: Itinerary,
    legs: List<DrawnLeg>,
    onStart: () -> Unit,
    onClose: (() -> Unit)?,

    favorite: Boolean,
    onFavorite: (() -> Unit)?,
    onFocus: (LatLng) -> Unit,
    onHeadHeight: (Int) -> Unit,
    stepsShown: () -> Float,
    scroll: androidx.compose.foundation.ScrollState,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clock = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(context), Locale.getDefault())
    fun at(seconds: Long) = if (seconds <= 0) "" else clock.format(Date(seconds * 1000))
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scroll).navigationBarsPadding().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().onSizeChanged { onHeadHeight(it.height) }.padding(start = 20.dp, end = 16.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    RouteLegs(itinerary)
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = pluginString(R.string.action_back))
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(durationLabel(itinerary), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    if (itinerary.arrivalTime > 0) {
                        Text(pluginString(R.string.transit_arrives, at(itinerary.arrivalTime)), style = MaterialTheme.typography.bodyLarge)
                    }
                    val firstRide = itinerary.milestones.firstOrNull { it.isTransit }
                    val from = firstRide?.departureStop?.takeIf { it.isNotBlank() }
                    if (firstRide != null && from != null && firstRide.departureTime > 0) {
                        Text(
                            pluginString(R.string.transit_leaves_from, at(firstRide.departureTime), from),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(pluginString(R.string.transit_follow_start), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
                if (onFavorite != null) {
                    androidx.compose.material3.FilledTonalButton(onClick = onFavorite, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(
                            if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(pluginString(R.string.transit_favorite), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.graphicsLayer { alpha = stepsShown() }) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            RouteSteps(itinerary, legs, activeLeg = null, onFocus = onFocus)
        }
    }
}

@Composable
private fun RouteSteps(itinerary: Itinerary, legs: List<DrawnLeg>, activeLeg: Int?, onFocus: (LatLng) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clock = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(context), Locale.getDefault())
    fun at(seconds: Long) = if (seconds <= 0) "" else clock.format(Date(seconds * 1000))

    val starts = remember(itinerary) {
        var previousEnd = itinerary.departureTime
        itinerary.milestones.map { leg ->
            val start = if (leg.departureTime > 0) leg.departureTime else previousEnd
            previousEnd = if (leg.arrivalTime > 0) leg.arrivalTime else start + leg.durationSeconds.toLong()
            start
        }
    }
    legs.forEachIndexed { index, leg ->
        val milestone = leg.milestone
        val active = index == activeLeg
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (active) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                .clickable { leg.points.firstOrNull()?.let(onFocus) }
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                .padding(start = 12.dp, end = 20.dp),
        ) {
            Text(
                at(starts.getOrElse(index) { 0L }),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(52.dp).padding(top = 14.dp),
            )
            val lineColor = leg.color
            val walking = !milestone.isTransit
            androidx.compose.foundation.Canvas(modifier = Modifier.width(20.dp).fillMaxHeight()) {
                val x = size.width / 2
                val top = 18.dp.toPx()
                drawCircle(lineColor, radius = 6.dp.toPx(), center = Offset(x, top))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(x, top))
                drawLine(
                    lineColor,
                    Offset(x, top + 7.dp.toPx()),
                    Offset(x, size.height),
                    strokeWidth = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = if (walking) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())) else null,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 16.dp)) {
                milestone.departureStop?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall)
                }
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegChip(milestone)
                    Text(
                        milestone.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                val detail = listOfNotNull(
                    milestone.stopCount?.let { pluginString(R.string.transit_map_stops, it) },
                    milestone.arrivalStop?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun RouteEnds(itinerary: Itinerary, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val from = MemoryCache.get<Place>("transit/originPlace")?.name
        ?: itinerary.milestones.firstOrNull()?.departureStop.orEmpty()
    val to = MemoryCache.get<Place>("transit/destPlace")?.name
        ?: itinerary.milestones.lastOrNull()?.arrivalStop.orEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = app.etsprivate.ui.groupColor,
        shadowElevation = 6.dp,
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = pluginString(R.string.action_back))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp, top = 4.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.padding(horizontal = 4.dp).size(10.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
                    Text(from, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
                }
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(to, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun Milestone.serverColor(): Color? =
    color?.trim()?.takeIf { it.startsWith("#") && (it.length == 7 || it.length == 4) }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun List<LatLng>.boundsOrNull(): LatLngBounds? {
    if (isEmpty()) return null
    val north = maxOf { it.lat } + 0.001
    val south = minOf { it.lat } - 0.001
    val east = maxOf { it.lng } + 0.001
    val west = minOf { it.lng } - 0.001
    return runCatching { LatLngBounds.from(north, east, south, west) }.getOrNull()
}

private fun isNight(): Boolean {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return hour < 7 || hour >= 19
}

private fun Color.isDark(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f
