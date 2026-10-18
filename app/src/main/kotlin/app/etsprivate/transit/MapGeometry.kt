package app.etsprivate.transit

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

fun decodePolyline(encoded: String): List<LatLng> {
    val points = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        fun nextValue(): Int {
            var result = 0
            var shift = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20 && index < encoded.length)
            return if (result and 1 != 0) (result shr 1).inv() else result shr 1
        }
        lat += nextValue()
        if (index >= encoded.length && points.isEmpty()) break
        lng += nextValue()
        points.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return points
}

fun distanceMeters(a: LatLng, b: LatLng): Double {
    val earthRadius = 6_371_000.0
    val dLat = (b.lat - a.lat) * PI / 180.0
    val dLng = (b.lng - a.lng) * PI / 180.0
    val meanLat = (a.lat + b.lat) / 2.0 * PI / 180.0
    val x = dLng * cos(meanLat)
    return earthRadius * sqrt(x * x + dLat * dLat)
}
