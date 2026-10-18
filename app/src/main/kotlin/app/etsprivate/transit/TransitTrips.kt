package app.etsprivate.transit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.annotation.SuppressLint
import android.location.LocationListener
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.callbackFlow
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

data class SavedTrip(val origin: Place, val destination: Place, val name: String? = null) {
    val label: String get() = name?.takeIf { it.isNotBlank() } ?: "${origin.name} → ${destination.name}"
}

object SavedTrips {
    private const val PREFS = "transit_trips"
    private const val KEY = "saved"
    private const val BACKUP_KEY = "saved_backup_2026_10_18"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<SavedTrip> = runCatching {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()

        if (!prefs(context).contains(BACKUP_KEY)) prefs(context).edit().putString(BACKUP_KEY, raw).apply()
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            SavedTrip(
                placeFromJson(o.getJSONObject("origin")),
                placeFromJson(o.getJSONObject("destination")),
                o.optString("name").ifBlank { null },
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, trips: List<SavedTrip>) {
        val array = JSONArray()
        trips.forEach { trip ->
            array.put(
                JSONObject()
                    .put("origin", placeToJson(trip.origin))
                    .put("destination", placeToJson(trip.destination))
                    .putOpt("name", trip.name),
            )
        }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    fun add(context: Context, trip: SavedTrip): List<SavedTrip> {
        val existing = load(context).filterNot {
            it.origin.name == trip.origin.name && it.destination.name == trip.destination.name
        }
        val updated = listOf(trip) + existing
        save(context, updated)
        return updated
    }

    fun touch(context: Context, from: String, to: String): List<SavedTrip> {
        val trips = load(context)
        val used = trips.firstOrNull { it.origin.name == from && it.destination.name == to } ?: return trips
        if (trips.first() === used) return trips
        val updated = listOf(used) + trips.filterNot { it === used }
        save(context, updated)
        return updated
    }

    fun remove(context: Context, trip: SavedTrip): List<SavedTrip> {
        val updated = load(context).filterNot {
            it.origin.name == trip.origin.name && it.destination.name == trip.destination.name
        }
        save(context, updated)
        return updated
    }

    private fun placeToJson(p: Place) = JSONObject()
        .put("name", p.name)
        .put("lieuId", p.lieuId ?: JSONObject.NULL)
        .put("lat", p.lat ?: JSONObject.NULL)
        .put("lng", p.lng ?: JSONObject.NULL)
        .put("isTransitStop", p.isTransitStop)

    private fun placeFromJson(o: JSONObject) = Place(
        name = o.optString("name"),
        lieuId = o.optString("lieuId").takeIf { it.isNotBlank() && it != "null" },
        lat = if (o.isNull("lat")) null else o.optDouble("lat"),
        lng = if (o.isNull("lng")) null else o.optDouble("lng"),
        isTransitStop = o.optBoolean("isTransitStop", false),
    )
}

data class PositionFix(
    val point: LatLng,
    val bearing: Float?,
    val speed: Float?,

    val accuracy: Float? = null,

    val elapsedMs: Long = 0,

    val provider: String? = null,

    val cached: Boolean = false,
)

private fun Location.asFix(cached: Boolean = false): PositionFix = PositionFix(
    point = LatLng(latitude, longitude),
    bearing = if (hasBearing()) bearing else null,
    speed = if (hasSpeed()) speed else null,
    accuracy = if (hasAccuracy()) accuracy else null,
    elapsedMs = elapsedRealtimeNanos / 1_000_000,
    provider = provider,
    cached = cached,
)

object CurrentPosition {
    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun updates(context: Context): Flow<PositionFix> = callbackFlow {
        if (!granted(context)) {
            close()
            return@callbackFlow
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            close()
            return@callbackFlow
        }
        val listener = LocationListener { location -> trySend(location.asFix()) }

        val providers = buildList {
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        providers.forEach { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                ?.let { trySend(it.asFix(cached = true)) }

            runCatching { manager.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper()) }
        }
        awaitClose { runCatching { manager.removeUpdates(listener) } }
    }

    suspend fun place(context: Context, label: String): Place? {
        if (!granted(context)) return null
        val fresh = kotlinx.coroutines.withTimeoutOrNull(5_000) {
            updates(context).firstOrNull { !it.cached }
        }
        val point = fresh?.point ?: lastKnown(context) ?: return null
        return Place(name = label, lieuId = null, lat = point.lat, lng = point.lng)
    }

    fun lastKnownPlace(context: Context, label: String): Place? {
        if (!granted(context)) return null
        val point = lastKnown(context) ?: return null
        return Place(name = label, lieuId = null, lat = point.lat, lng = point.lng)
    }

    private fun lastKnown(context: Context): LatLng? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            manager.allProviders
                .mapNotNull { provider ->
                    @Suppress("MissingPermission")
                    runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull(Location::getTime)
                ?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
