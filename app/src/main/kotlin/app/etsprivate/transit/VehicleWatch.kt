package app.etsprivate.transit

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun stopsLeft(stops: List<TripStop>, leg: Milestone): Int? {
    val boarded = leg.departureStopId ?: return null
    val count = leg.stopCount ?: return null
    val from = stops.indexOfFirst { it.stopId == boarded }
    if (from < 0) return null
    val getOff = from + count
    if (getOff >= stops.size) return null
    val next = stops.indexOfFirst { it.countdown != null }
    if (next < 0) return null
    return (getOff - next + 1).coerceAtLeast(0)
}

class VehicleWatch(private val context: Context) {
    var stopsLeft: Int? = null
        private set

    private var leg: Milestone? = null
    private var tripId: String? = null
    private var candidate: String? = null
    private var steps = 0

    suspend fun step(riding: Milestone?, rider: LatLng?) {
        if (riding !== leg) {
            leg = riding
            tripId = null
            candidate = null
            stopsLeft = null
            steps = 0
        }
        val current = riding ?: return
        val agency = current.agencyCode ?: return
        val route = current.routeId ?: return
        steps++
        val client = ChronoClient(context.applicationContext)
        val found = tripId
        if (found == null) {
            val here = rider ?: return
            val vehicles = withContext(Dispatchers.IO) {
                runCatching {
                    client.ensureAuthenticated()
                    client.vehiclePositions(agency, route)
                }.getOrDefault(emptyList())
            }
            val nearest = vehicles.minByOrNull { distanceMeters(it.point, here) }
                ?.takeIf { distanceMeters(it.point, here) < SAME_VEHICLE_METERS }
            log(JSONObject().put("vehicles", vehicles.size).put("nearest", nearest?.tripId ?: JSONObject.NULL)
                .put("nearestMeters", nearest?.let { distanceMeters(it.point, here) } ?: JSONObject.NULL))
            tripId = nearest?.tripId?.takeIf { it == candidate }
            candidate = nearest?.tripId
            return
        }
        if (steps % 3 != 0 && stopsLeft != null) return
        val stops = withContext(Dispatchers.IO) {
            runCatching {
                client.ensureAuthenticated()
                client.tripStops(agency, route, found)
            }.getOrDefault(emptyList())
        }
        if (stops.isEmpty()) return
        stopsLeft = stopsLeft(stops, current)
        log(JSONObject().put("trip", found).put("stops", stops.size).put("stopsLeft", stopsLeft ?: JSONObject.NULL)
            .put("boardedAt", stops.indexOfFirst { it.stopId == current.departureStopId })
            .put("next", stops.indexOfFirst { it.countdown != null })
            .put("count", current.stopCount ?: JSONObject.NULL))
    }

    private fun log(line: JSONObject) = RideLog.placed(line.put("kind", "vehicle"))

    private companion object {
        const val SAME_VEHICLE_METERS = 80.0
    }
}
