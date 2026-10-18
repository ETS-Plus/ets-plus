package app.etsprivate.transit

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.Charsets
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val BASE = "https://svcchronomobile.artm.quebec"

data class Place(val name: String, val lieuId: String?, val lat: Double?, val lng: Double?, val isTransitStop: Boolean = false)

data class LatLng(val lat: Double, val lng: Double)

private fun JSONObject.point(latKey: String, lngKey: String): LatLng? {
    val lat = optDouble(latKey, Double.NaN)
    val lng = optDouble(lngKey, Double.NaN)
    return if (lat.isNaN() || lng.isNaN()) null else LatLng(lat, lng)
}

data class Milestone(
    val summary: String,
    val isTransit: Boolean,
    val mode: String?,
    val routeShortName: String?,

    val title: String?,

    val vehicleType: String?,
    val color: String?,
    val directionTransit: String?,
    val departureStop: String?,
    val arrivalStop: String?,
    val departureTime: Long,
    val arrivalTime: Long,
    val durationSeconds: Double,
    val distanceMeters: Double,

    val polyline: String?,
    val departPoint: LatLng?,
    val arrivePoint: LatLng?,

    val agencyCode: String?,

    val stopCount: Int?,

    val routeId: String? = null,

    val departureStopId: String? = null,
)

data class Vehicle(val tripId: String, val point: LatLng)

data class TripStop(
    val stopId: String,

    val plannedSecond: Int,

    val passing: String?,

    val countdown: String?,
)

data class Itinerary(
    val departureTime: Long,
    val arrivalTime: Long,
    val durationText: String?,
    val milestones: List<Milestone>,

    val southWest: LatLng? = null,
    val northEast: LatLng? = null,
)

class ChronoClient(private val appContext: Context) {
    val context: Context get() = appContext

    val preferences: android.content.SharedPreferences get() = prefs

    @Volatile var language: String = "fr"

    @Volatile private var accessToken: String? = null
    @Volatile private var refreshTokenVal: String? = null

    @Volatile var blockNetwork: Boolean = false

    private val prefs get() = appContext.getSharedPreferences("chrono_client", Context.MODE_PRIVATE)

    fun ensureAuthenticated() {
        if (accessToken != null) return

        val storedRefresh = prefs.getString("refreshToken", null)
        if (storedRefresh != null) {
            refreshTokenVal = storedRefresh
            try {
                doRefresh()
                return
            } catch (e: Exception) {
            }
        }

        val (s1, b1) = postRaw(
            "/ws/profils",
            """{"organisation":"RTM","langue":"$language","abonnementCourrielARTM":false,"abonnementCourrielRTL":false,"abonnementCourrielRTM":false,"abonnementCourrielSTL":false,"version":"1"}""",
            authed = false,
        )
        check(s1 == 201) { "signup failed: $s1 $b1" }
        val anonToken = JSONObject(b1).getJSONObject("reponse").getString("jetonAnonyme")

        val conn = openConn("/ws/profils/authentifier", "POST")
        writeBody(conn, """{"jetonAnonyme":"$anonToken"}""")
        val status2 = conn.responseCode
        val ssId = conn.headerFields["Set-Cookie"]
            ?.firstOrNull { it.startsWith("ss-id=") }
            ?.substringAfter("ss-id=")?.substringBefore(";")
        conn.disconnect()
        check(status2 == 200) { "login failed: $status2" }
        requireNotNull(ssId) { "no ss-id cookie in login response" }

        val tokConn = openConn("/ws/profils/token", "GET")
        tokConn.setRequestProperty("Cookie", "ss-id=$ssId")
        val status3 = tokConn.responseCode
        val body3 = tokConn.inputStream.readBodyText(tokConn)
        tokConn.disconnect()
        check(status3 == 200) { "token fetch failed: $status3" }
        storeTokens(JSONObject(body3).getJSONObject("reponse"))
    }

    private fun doRefresh() {
        val conn = openConn("/ws/profils/token", "POST")
        writeBody(conn, """{"refreshToken":"$refreshTokenVal"}""")
        val status = conn.responseCode
        val stream = if (status < 400) conn.inputStream else conn.errorStream
        val body = stream.readBodyText(conn)
        conn.disconnect()
        check(status == 200) { "refresh failed: $status $body" }
        storeTokens(JSONObject(body).getJSONObject("reponse"))
    }

    private fun storeTokens(reponse: JSONObject) {
        accessToken = reponse.getString("accessToken")
        refreshTokenVal = reponse.getString("refreshToken")
        prefs.edit().putString("refreshToken", refreshTokenVal).apply()
    }

    fun suggestPlaces(term: String): List<Place> {
        if (term.isBlank()) return emptyList()
        val q = URLEncoder.encode(term, "UTF-8")
        val (status, body) = get("/ws/planificateur/lieux/suggestions?TermeRecherche=$q&Langue=$language")
        if (status != 200) return emptyList()
        val arr = JSONObject(body).optJSONArray("reponse") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Place(o.getString("nom"), o.optString("lieuId", null), null, null, o.optBoolean("estTransit", false))
        }
    }

    fun getItineraries(
        origin: Place,
        destination: Place,
        departAt: Long? = null,
        arriveBy: Long? = null,
    ): List<Itinerary> {
        val params = StringBuilder("/ws/planificateur/trajets?")
        fun addPlace(prefix: String, p: Place) {
            if (p.lieuId != null) {
                params.append("${prefix}LieuId=${URLEncoder.encode(p.lieuId, "UTF-8")}&")
            } else if (p.lat != null && p.lng != null) {
                params.append("${prefix}Latitude=${p.lat}&${prefix}Longitude=${p.lng}&")
            }
        }
        addPlace("Origine", origin)
        addPlace("Destination", destination)
        when {
            arriveBy != null -> params.append("HeureArriveeUTC=$arriveBy&")
            departAt != null -> params.append("HeureDepartUTC=$departAt&")
        }
        params.append("UtiliserMetro=true&UtiliserBus=true&UtiliserTrain=true&UtiliserVelo=false&Langue=$language")

        val (status, body) = get(params.toString())
        check(status == 200) { "itinerary request failed: $status $body" }
        val reponse = JSONObject(body).getJSONObject("reponse")
        check(reponse.getString("statut") == "OK") { "planner status: ${reponse.getString("statut")}" }
        val itinerairesArr = reponse.optJSONArray("itineraires") ?: JSONArray()
        return (0 until itinerairesArr.length()).map { i ->
            val it = itinerairesArr.getJSONObject(i)
            val jalonsArr = it.optJSONArray("jalons") ?: JSONArray()
            val milestones = (0 until jalonsArr.length()).map { j ->
                val m = jalonsArr.getJSONObject(j)
                Milestone(
                    summary = m.optString("sommaire", ""),
                    isTransit = m.optBoolean("estTransit", false),
                    mode = m.optString("mode", null),
                    routeShortName = m.optString("routeNomCourt", null),
                    title = m.optString("titre", null),
                    vehicleType = m.optString("typeVehicule", null),
                    color = m.optString("couleur", null),
                    directionTransit = m.optString("directionTransit", null),
                    departureStop = m.optString("arretDepart", null),
                    arrivalStop = m.optString("arretArrivee", null),
                    departureTime = m.optLong("heureDepart", 0),
                    arrivalTime = m.optLong("heureArrivee", 0),
                    durationSeconds = m.optDouble("dureeSecondes", 0.0),
                    distanceMeters = m.optDouble("distanceMetres", 0.0),
                    polyline = m.optString("pointsPolyligne", null),
                    departPoint = m.point("departLatitude", "departLongitude"),
                    arrivePoint = m.point("arriveeLatitude", "arriveeLongitude"),
                    agencyCode = m.optString("agenceCode", null),
                    stopCount = m.optInt("nbArrets", 0).takeIf { it > 0 },
                    routeId = m.optString("routeID", "").ifBlank { null },
                    departureStopId = m.optString("arretDepartID", "").ifBlank { null },
                )
            }
            Itinerary(
                departureTime = it.optLong("heureDepart", 0),
                arrivalTime = it.optLong("heureArrivee", 0),
                durationText = it.optString("dureeTexte", null),
                milestones = milestones,
                southWest = it.point("limiteSudOuestLatitude", "limiteSudOuestLongitude"),
                northEast = it.point("limiteNordEstLatitude", "limiteNordEstLongitude"),
            )
        }
    }

    fun nextDepartures(leg: Milestone, limit: Int = 2): List<Long> {
        val from = leg.departPoint ?: return emptyList()
        val to = leg.arrivePoint ?: return emptyList()
        val route = leg.routeShortName?.trim().orEmpty()
        val itineraries = runCatching {
            getItineraries(
                origin = Place(leg.departureStop.orEmpty(), null, from.lat, from.lng),
                destination = Place(leg.arrivalStop.orEmpty(), null, to.lat, to.lng),
                departAt = System.currentTimeMillis() / 1000,
            )
        }.getOrNull().orEmpty()
        return itineraries
            .flatMap { it.milestones }
            .filter { it.isTransit && it.departureTime > 0 }
            .filter { route.isEmpty() || it.routeShortName?.trim() == route }
            .map { it.departureTime }
            .distinct()
            .sorted()
            .take(limit)
    }

    fun vehiclePositions(agency: String, routeId: String): List<Vehicle> {
        val (status, body) = get("/ws/realtime/positions/${enc(agency)}/${enc(routeId)}")
        check(status == 200) { "positions failed: $status" }
        val data = JSONObject(body).optJSONObject("reponse")?.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val v = data.getJSONObject(i)
            val trip = v.optString("voyageID", "").ifBlank { return@mapNotNull null }
            val lat = v.optDouble("latitude", Double.NaN)
            val lng = v.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) null else Vehicle(trip, LatLng(lat, lng))
        }
    }

    fun tripStops(agency: String, routeId: String, tripId: String): List<TripStop> {
        val (status, body) = get("/ws/realtime/infosvoyages/${enc(agency)}/${enc(routeId)}?Voyage=${enc(tripId)}")
        check(status == 200) { "trip stops failed: $status" }
        val data = JSONObject(body).optJSONObject("reponse")?.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).map { i ->
            val stop = data.getJSONObject(i)
            TripStop(
                stopId = stop.optString("arretID", ""),
                plannedSecond = stop.optInt("heurePlanifieSec", -1),
                passing = stop.optString("heurePassage", "").ifBlank { null },
                countdown = stop.optString("decompte", "").ifBlank { null },
            )
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun get(path: String): Pair<Int, String> {
        if (blockNetwork) throw IllegalStateException("network blocked (local apply)")
        val started = System.currentTimeMillis()
        val conn = openConn(path, "GET")
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        val status = conn.responseCode
        val stream = if (status < 400) conn.inputStream else conn.errorStream
        val body = stream.readBodyText(conn)
        conn.disconnect()
        RideLog.http("GET", path, status, 0, body.length, System.currentTimeMillis() - started)
        return status to body
    }

    fun postRaw(path: String, jsonBody: String, authed: Boolean): Pair<Int, String> {
        if (blockNetwork) throw IllegalStateException("network blocked (local apply)")
        val started = System.currentTimeMillis()
        val conn = openConn(path, "POST")
        if (authed) conn.setRequestProperty("Authorization", "Bearer $accessToken")
        writeBody(conn, jsonBody)
        val status = conn.responseCode
        val stream = if (status < 400) conn.inputStream else conn.errorStream
        val body = stream.readBodyText(conn)
        conn.disconnect()
        RideLog.http("POST", path, status, jsonBody.length, body.length, System.currentTimeMillis() - started)
        return status to body
    }

    internal fun java.io.InputStream.readBodyText(conn: HttpURLConnection): String {
        val bytes = readBytes()
        val declared = conn.contentType
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', ' ')
            ?.let { runCatching { java.nio.charset.Charset.forName(it) }.getOrNull() }
        val decoded = if (declared != null) {
            String(bytes, declared)
        } else {
            val asUtf8 = String(bytes, Charsets.UTF_8)
            if (asUtf8.contains('\uFFFD')) String(bytes, Charsets.ISO_8859_1) else asUtf8
        }
        return repairMojibake(decoded)
    }

    private fun repairMojibake(text: String): String {
        if (!text.contains('\u00C3') && !text.contains('\u00C2')) return text
        val repaired = runCatching {
            String(text.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        }.getOrNull() ?: return text
        if (repaired.contains('\uFFFD')) return text
        val before = text.count { it == '\u00C3' || it == '\u00C2' }
        val after = repaired.count { it == '\u00C3' || it == '\u00C2' }
        return if (after < before) repaired else text
    }

    private fun openConn(path: String, method: String): HttpURLConnection {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        if (method == "POST") conn.doOutput = true
        return conn
    }

    private fun writeBody(conn: HttpURLConnection, json: String) {
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(json) }
    }
}
