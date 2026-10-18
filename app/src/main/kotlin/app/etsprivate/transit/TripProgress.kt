package app.etsprivate.transit

fun cumulativeMeters(points: List<LatLng>): List<Double> {
    val out = ArrayList<Double>(points.size)
    var total = 0.0
    points.forEachIndexed { index, point ->
        if (index > 0) total += distanceMeters(points[index - 1], point)
        out.add(total)
    }
    return out
}

fun alongMeters(points: List<LatLng>, position: LatLng): Double {
    if (points.size < 2) return 0.0
    val cumulative = cumulativeMeters(points)
    var best = 0.0
    var bestOff = Double.MAX_VALUE
    for (i in 0 until points.size - 1) {
        val (point, fraction) = nearestOnSegment(points[i], points[i + 1], position)
        val off = distanceMeters(point, position)
        if (off < bestOff) {
            bestOff = off
            best = cumulative[i] + (cumulative[i + 1] - cumulative[i]) * fraction
        }
    }
    return best
}

fun offLineMeters(points: List<LatLng>, position: LatLng): Double {
    if (points.isEmpty()) return Double.MAX_VALUE
    if (points.size == 1) return distanceMeters(points[0], position)
    var best = Double.MAX_VALUE
    for (i in 0 until points.size - 1) {
        val (point, _) = nearestOnSegment(points[i], points[i + 1], position)
        best = minOf(best, distanceMeters(point, position))
    }
    return best
}

fun pointAtMeters(points: List<LatLng>, meters: Double): LatLng? {
    if (points.isEmpty()) return null
    if (points.size == 1) return points[0]
    val cumulative = cumulativeMeters(points)
    val wanted = meters.coerceIn(0.0, cumulative.last())
    for (i in 0 until points.size - 1) {
        if (wanted <= cumulative[i + 1]) {
            val span = cumulative[i + 1] - cumulative[i]
            val fraction = if (span <= 0.0) 0.0 else (wanted - cumulative[i]) / span
            return LatLng(
                points[i].lat + (points[i + 1].lat - points[i].lat) * fraction,
                points[i].lng + (points[i + 1].lng - points[i].lng) * fraction,
            )
        }
    }
    return points.last()
}

fun splitAtMeters(points: List<LatLng>, meters: Double): Pair<List<LatLng>, List<LatLng>> {
    val at = pointAtMeters(points, meters) ?: return emptyList<LatLng>() to points
    val cumulative = cumulativeMeters(points)
    val behind = ArrayList<LatLng>()
    val ahead = ArrayList<LatLng>()
    points.forEachIndexed { index, point ->
        if (cumulative[index] <= meters) behind.add(point) else ahead.add(point)
    }
    behind.add(at)
    ahead.add(0, at)
    return behind to ahead
}

fun lengthMeters(points: List<LatLng>): Double = cumulativeMeters(points).lastOrNull() ?: 0.0

fun bearingAtMeters(points: List<LatLng>, meters: Double): Float? {
    if (points.size < 2) return null
    val cumulative = cumulativeMeters(points)
    val wanted = meters.coerceIn(0.0, cumulative.last())
    for (i in 0 until points.size - 1) {
        if (wanted <= cumulative[i + 1]) return bearingBetween(points[i], points[i + 1])
    }
    return bearingBetween(points[points.size - 2], points.last())
}

private fun bearingBetween(a: LatLng, b: LatLng): Float {
    val squeeze = kotlin.math.cos(Math.toRadians((a.lat + b.lat) / 2))
    val dx = (b.lng - a.lng) * squeeze
    val dy = b.lat - a.lat
    return ((Math.toDegrees(kotlin.math.atan2(dx, dy)) + 360) % 360).toFloat()
}

internal fun nearestOnSegment(start: LatLng, end: LatLng, position: LatLng): Pair<LatLng, Double> {
    val squeeze = kotlin.math.cos(Math.toRadians((start.lat + end.lat) / 2))
    val ax = start.lng * squeeze
    val ay = start.lat
    val bx = end.lng * squeeze
    val by = end.lat
    val px = position.lng * squeeze
    val py = position.lat
    val dx = bx - ax
    val dy = by - ay
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0.0) return start to 0.0
    val fraction = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
    return LatLng(ay + dy * fraction, (ax + dx * fraction) / squeeze) to fraction
}
