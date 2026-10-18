package app.etsprivate.transit

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TripPoint(val legIndex: Int, val alongMeters: Double, val estimated: Boolean)

class TrackedLeg(
    val milestone: Milestone,
    val points: List<LatLng>,

    val isRail: Boolean,
) {
    val isTransit: Boolean get() = milestone.isTransit
    val length: Double = lengthMeters(points)

    val stations: List<Double> =
        if (isRail) stationsAlong(points).map { alongMeters(points, it.point) } else emptyList()
}

fun trackLegs(itinerary: Itinerary?, isRail: (Milestone) -> Boolean): List<TrackedLeg> =
    itinerary?.milestones.orEmpty().mapNotNull { milestone ->
        val points = milestone.polyline?.let { decodePolyline(it) }?.takeIf { it.size > 1 }
            ?: listOfNotNull(milestone.departPoint, milestone.arrivePoint).takeIf { it.size == 2 }
            ?: return@mapNotNull null
        TrackedLeg(milestone, points, isRail(milestone))
    }

enum class TrackMode {
    WAITING,
    ON_ROUTE,

    OFF_ROUTE,

    UNDERGROUND,
    FINISHED,
}

data class TrackState(
    val point: TripPoint?,
    val mode: TrackMode,
    val why: String,

    val offLine: Double?,

    val travelled: Double,

    val stationsCounted: Int = 0,

    val railStation: Int? = null,

    val railMoving: Boolean = false,
)

class TripTracker(legs: List<TrackedLeg>) {
    var legs: List<TrackedLeg> = legs
        private set

    private class Segment(val leg: Int, val a: LatLng, val b: LatLng, val startInLeg: Double, val length: Double)

    private val segments: List<Segment>
    private val legStart: DoubleArray
    private val firstSegment: IntArray
    private val lastSegment: IntArray
    private val total: Double

    init {
        val all = ArrayList<Segment>()
        legStart = DoubleArray(legs.size)
        firstSegment = IntArray(legs.size)
        lastSegment = IntArray(legs.size)
        var before = 0.0
        legs.forEachIndexed { index, leg ->
            legStart[index] = before
            firstSegment[index] = all.size
            var inLeg = 0.0
            for (i in 0 until leg.points.size - 1) {
                val length = distanceMeters(leg.points[i], leg.points[i + 1])
                all.add(Segment(index, leg.points[i], leg.points[i + 1], inLeg, length))
                inLeg += length
            }
            lastSegment[index] = all.size - 1
            before += leg.length
        }
        segments = all
        total = before
    }

    private var mode = TrackMode.WAITING
    private var why = "no-fix"
    private var segment = 0
    private var leg = 0
    private var along = 0.0
    private var estimated = false
    private var offLine: Double? = null

    private var lastGood: LatLng? = null
    private var lastGoodAt = 0L
    private var legReachedAt = 0L

    private var busLeftAt: Long? = null

    private var bestToEnd = Double.MAX_VALUE

    private var moveAway = 0
    private var lastAwayDistance = 0.0

    private var pendingJump: Match? = null

    private var stationsCounted = 0

    private var railStation = 0

    private var railMoving = false

    private var railSince = 0L

    val state: TrackState
        get() = TrackState(
            point = if (mode == TrackMode.WAITING || legs.isEmpty()) null else TripPoint(leg, along, estimated),
            mode = mode,
            why = why,
            offLine = offLine,
            travelled = travelled(),
            stationsCounted = stationsCounted,
            railStation = if (legs.isNotEmpty() && legs[leg].isRail && legs[leg].stations.isNotEmpty()) railStation else null,
            railMoving = railMoving,
        )

    private fun travelled(): Double = if (legs.isEmpty()) 0.0 else legStart[leg] + along

    fun retime(newLegs: List<TrackedLeg>) {
        if (newLegs.size != legs.size) return
        legs = newLegs
    }

    private class Match(val segment: Int, val leg: Int, val alongInLeg: Double, val travelled: Double, val off: Double)

    fun onFix(fix: PositionFix, now: Long) {
        if (legs.isEmpty() || segments.isEmpty() || mode == TrackMode.FINISHED) return
        val position = fix.point
        val accuracy = fix.accuracy ?: 0f

        if (legs[leg].isRail && accuracy > RAIL_WORST_ACCURACY) return
        val moving = (fix.speed ?: 0f) > BOARDED_SPEED || steadySince(now) != null

        val match = boardable(
            when {
                mode == TrackMode.WAITING -> earliest(0, segments.size, position, accuracy)
                mode == TrackMode.OFF_ROUTE -> confirmed(earliest(segmentAt(travelled() - BACKTRACK_METERS), segments.size, position, accuracy))

                estimated -> closest(firstSegment[leg], lastSegment[windowEndLeg()] + 1, position, accuracy)
                else -> windowed(position, accuracy)
            },
            position,
            accuracy,
            moving,
        )
        offLine = match?.off ?: nearestOff(position)

        if (match == null) {
            if (!gotOff(position, fix.speed, now) && !ownWay(position, now)) movingAway(position, fix.speed)
            return
        }
        moveAway = 0
        lastAwayDistance = 0.0
        lastGood = position
        lastGoodAt = now
        if (!legs[leg].isTransit) bestToEnd = min(bestToEnd, distanceMeters(position, legs[leg].points.last()))

        if (!estimated && mode != TrackMode.WAITING && match.travelled < travelled()) {
            why = "held"
            return
        }

        val current = legs[leg]
        if (current.isRail && railMoving && match.leg == leg && current.stations.size >= 2) {
            val station = current.stations.indices.minByOrNull { abs(current.stations[it] - match.alongInLeg) }!!
            if (station <= railStation) return
        }
        moveTo(match, now)
        estimated = false

        if (legs[leg].isTransit && !legs[leg].isRail && busLeftAt == null &&
            ((fix.speed ?: 0f) > BOARDED_SPEED || distanceMeters(position, legs[leg].points.first()) > WAITING_AREA_METERS)
        ) {
            busLeftAt = now
        }

        if (legs[leg].isTransit && !legs[leg].isRail && busLeftAt == null) {
            along = 0.0
            segment = firstSegment[leg]
        }
        mode = when {
            leg == legs.lastIndex && (travelled() >= total - FINISH_METERS || distanceMeters(position, legs[leg].points.last()) <= ARRIVED_METERS) -> TrackMode.FINISHED

            legs[leg].isRail && mode == TrackMode.UNDERGROUND && fix.provider != "gps" -> TrackMode.UNDERGROUND
            else -> TrackMode.ON_ROUTE
        }
        why = "on-line"

        val rail = legs[leg]
        if (rail.isRail && rail.stations.isNotEmpty()) {
            val station = rail.stations.indices.minByOrNull { abs(rail.stations[it] - along) }!!
            if (abs(rail.stations[station] - along) <= max(STATION_SNAP_METERS, accuracy.toDouble())) {
                along = rail.stations[station]
                segment = segmentAt(legStart[leg] + along)
                if (station != railStation || railMoving) railSince = now
                railStation = station
                railMoving = false
                why = "at-station"
            }
        }
    }

    private fun moveTo(match: Match, now: Long) {
        if (match.leg != leg || mode == TrackMode.WAITING) {
            legReachedAt = now
            stationsCounted = 0
            busLeftAt = null
            bestToEnd = Double.MAX_VALUE
            boardRail(match.leg, match.alongInLeg, now)
        }
        segment = match.segment
        leg = match.leg
        along = match.alongInLeg
    }

    private fun boardRail(legIndex: Int, alongInLeg: Double, now: Long) {
        val rail = legs[legIndex]
        railStation = rail.stations.indices.minByOrNull { abs(rail.stations[it] - alongInLeg) } ?: 0
        railMoving = false
        railSince = now
    }

    private fun windowEndLeg(): Int {
        var end = min(leg + 1, legs.lastIndex)
        while (end < legs.lastIndex && !legs[end].isTransit) end++
        return end
    }

    private fun windowed(position: LatLng, accuracy: Float): Match? {
        val reach = travelled() + NEAR_AHEAD_METERS
        var near = min(segment + 1, segments.size)
        while (near < min(segment + 3, segments.size) && legStart[segments[near].leg] + segments[near].startInLeg <= reach) near++
        val end = lastSegment[windowEndLeg()] + 1
        val match = closest(segment, near, position, accuracy) ?: closest(near, end, position, accuracy)

        if (match != null && match.leg == leg && leg < legs.lastIndex && legs[leg].length - match.alongInLeg < HANDOFF_METERS) {
            earliest(firstSegment[leg + 1], end, position, accuracy)?.let { return it }
        }
        return match
    }

    private fun closest(from: Int, to: Int, position: LatLng, accuracy: Float): Match? {
        var best: Match? = null
        for (i in from until to) {
            val m = project(i, position)
            if (m.off <= radius(segments[i].leg, accuracy) && (best == null || m.off < best.off)) best = m
        }
        return best
    }

    private fun earliest(from: Int, to: Int, position: LatLng, accuracy: Float): Match? {
        var best: Match? = null
        for (i in from until to) {
            val m = project(i, position)
            if (m.off > radius(segments[i].leg, accuracy)) {
                if (best != null) return best
                continue
            }
            if (best == null || m.off < best.off) best = m
        }
        return best
    }

    private fun confirmed(candidate: Match?): Match? {
        val previous = pendingJump
        pendingJump = candidate
        if (candidate == null || previous == null) return null
        return candidate.takeIf { abs(it.travelled - previous.travelled) < CONFIRM_METERS }
    }

    private fun boardable(match: Match?, position: LatLng, accuracy: Float, moving: Boolean): Match? {
        if (match == null || match.leg == leg && mode != TrackMode.WAITING || moving) return match
        val ride = legs[match.leg]
        if (!ride.isTransit || ride.isRail || legs.getOrNull(match.leg - 1)?.isTransit != false) return match
        if (match.alongInLeg <= max(STOP_REACH_METERS, accuracy.toDouble())) return match
        val walk = match.leg - 1
        return closest(firstSegment[walk], lastSegment[walk] + 1, position, accuracy)
            ?: match.takeIf { it.alongInLeg > HANDOFF_METERS }
    }

    private fun ownWay(position: LatLng, now: Long): Boolean {
        val walk = legs[leg]
        if (walk.isTransit || mode == TrackMode.WAITING) return false
        val toEnd = distanceMeters(position, walk.points.last())
        if (toEnd > walk.length + OWN_WAY_SLACK_METERS || toEnd > bestToEnd + OWN_WAY_NOISE_METERS) return false
        bestToEnd = min(bestToEnd, toEnd)
        moveAway = 0
        lastAwayDistance = 0.0
        lastGood = position
        lastGoodAt = now
        along = max(along, (walk.length - toEnd).coerceIn(0.0, walk.length))
        segment = segmentAt(legStart[leg] + along)
        estimated = false
        mode = if (leg == legs.lastIndex && toEnd <= ARRIVED_METERS) TrackMode.FINISHED else TrackMode.ON_ROUTE
        why = "own-way"
        return true
    }

    private fun gotOff(position: LatLng, speed: Float?, now: Long): Boolean {
        val ride = legs[leg]
        val walk = legs.getOrNull(leg + 1) ?: return false

        if (!ride.isTransit || ride.isRail || walk.isTransit || mode == TrackMode.WAITING || busLeftAt == null) return false
        val atEnd = ride.length - along <= ARRIVING_METERS && distanceMeters(position, ride.points.last()) <= ARRIVING_METERS
        if ((speed ?: 0f) > BOARDED_SPEED || !atEnd && !walkingNow(now)) return false
        leg += 1
        along = 0.0
        segment = firstSegment[leg]
        legReachedAt = now
        busLeftAt = null
        bestToEnd = Double.MAX_VALUE
        stationsCounted = 0
        estimated = false
        mode = TrackMode.ON_ROUTE
        why = "got-off"
        moveAway = 0
        lastAwayDistance = 0.0
        lastGoodAt = now
        ownWay(position, now)
        return true
    }

    private fun project(index: Int, position: LatLng): Match {
        val s = segments[index]
        val (point, fraction) = nearestOnSegment(s.a, s.b, position)
        val alongInLeg = s.startInLeg + s.length * fraction
        return Match(index, s.leg, alongInLeg, legStart[s.leg] + alongInLeg, distanceMeters(point, position))
    }

    private fun nearestOff(position: LatLng): Double? =
        if (segments.isEmpty()) null else segments.indices.minOf { project(it, position).off }

    private fun segmentAt(travelled: Double): Int {
        val index = segments.indexOfFirst { legStart[it.leg] + it.startInLeg + it.length >= travelled }
        return if (index < 0) segments.size - 1 else index
    }

    private fun radius(legIndex: Int, accuracy: Float): Double {
        val l = legs[legIndex]
        val threshold = when {
            !l.isTransit -> 30.0
            l.isRail -> 60.0
            else -> 50.0
        }
        return max(threshold, accuracy.toDouble())
    }

    private fun movingAway(position: LatLng, speed: Float?) {
        if (mode == TrackMode.WAITING || mode == TrackMode.OFF_ROUTE) return

        val ride = legs[leg]
        val departed = if (ride.isRail) railMoving || railStation > 0 else busLeftAt != null
        if (ride.isTransit && !departed && distanceMeters(position, ride.points.first()) <= WAITING_AREA_METERS) return
        val here = pointAtMeters(legs[leg].points, along) ?: return
        val distance = distanceMeters(here, position)
        if (abs(distance - lastAwayDistance) < 0.01) return
        moveAway += if ((speed ?: 0f) > 3f) 2 else 1
        lastAwayDistance = distance
        if (moveAway > OFF_ROUTE_COUNT) {
            mode = TrackMode.OFF_ROUTE
            why = "off-route"
            pendingJump = null
        }
    }

    fun onTick(now: Long) {
        if (legs.isEmpty() || mode == TrackMode.WAITING || mode == TrackMode.FINISHED || mode == TrackMode.OFF_ROUTE) return
        val sinceFix = now - lastGoodAt
        val current = legs[leg]

        val next = legs.getOrNull(leg + 1)
        if (!current.isRail && next?.isRail == true && sinceFix > SKY_LOST_MS && current.length - along < STATION_REACH_METERS) {
            enterRail(lastGoodAt)
        }
        val riding = legs[leg]
        when {
            riding.isRail && sinceFix > SKY_LOST_MS -> {
                mode = TrackMode.UNDERGROUND
                placeUnderground(now)
            }

            riding.isRail -> Unit
            riding.isTransit && sinceFix > BUS_CLOCK_MS -> {
                val span = legSpanMs(riding)
                val left = busLeftAt
                if (span > 0 && left != null) {
                    val byClock = riding.length * min((now - left).toDouble() / span, NEARLY_THERE)
                    if (byClock > along) along = byClock
                    segment = segmentAt(legStart[leg] + along)
                    estimated = true
                    why = "timetable"
                }
            }
        }
    }

    private fun enterRail(at: Long) {
        leg += 1
        along = legs[leg].stations.firstOrNull() ?: 0.0
        segment = segmentAt(legStart[leg] + along)
        legReachedAt = at
        stationsCounted = 0
        boardRail(leg, along, at)
        estimated = true
        mode = TrackMode.UNDERGROUND
        why = "entered-station"
    }

    private fun placeUnderground(now: Long) {
        val rail = legs[leg]
        estimated = true
        val stations = rail.stations

        if (!feelsMotion(now) && !railMoving && railStation < stations.lastIndex && now - max(railSince, lastGoodAt) > QUIET_MEANS_LEFT_MS) {
            railMoving = true
            railSince = max(railSince, lastGoodAt) + QUIET_MEANS_LEFT_MS / 2
        }
        if (stations.size < 2) {
            if (!railMoving) {
                why = "waiting-for-train"
                return
            }
            val span = legSpanMs(rail).takeIf { it > 0 } ?: (rail.length / TRAIN_SPEED_MPS * 1000).toLong()
            val byClock = rail.length * (max(0L, now - railSince).toDouble() / span).coerceAtMost(NEARLY_THERE)
            if (byClock > along) along = byClock
            segment = segmentAt(legStart[leg] + along)
            why = "clock-from-departure"
            return
        }
        if (!railMoving) {
            along = stations[railStation]
            segment = segmentAt(legStart[leg] + along)
            why = if (railStation == 0) "waiting-for-train" else "at-station"
            return
        }

        val from = stations[railStation]
        val to = stations[railStation + 1]
        val stretch = segmentTimeMs(rail, railStation)
        val elapsed = now - railSince

        val stillRunning = steadySince(now) != null && elapsed < 2 * stretch + ARRIVAL_GRACE_MS
        if (elapsed >= stretch + ARRIVAL_GRACE_MS && !stillRunning) {
            railStation += 1
            railMoving = false
            railSince = railSince + stretch
            along = to
            why = "arrived-by-clock"
        } else {
            along = max(along, from + (to - from) * (elapsed.toDouble() / stretch).coerceIn(0.0, NEARLY_THERE))
            why = "between-stations"
        }
        segment = segmentAt(legStart[leg] + along)
    }

    private fun segmentTimeMs(rail: TrackedLeg, index: Int): Long {
        val span = legSpanMs(rail).takeIf { it > 0 } ?: (rail.length / TRAIN_SPEED_MPS * 1000).toLong()
        val stations = rail.stations
        val whole = (stations.last() - stations.first()).takeIf { it > 0 } ?: return span
        val share = (stations[index + 1] - stations[index]) / whole
        return (span * share).toLong().coerceAtLeast(MIN_STRETCH_MS)
    }

    private val shakes = ArrayDeque<Pair<Long, Double>>()
    private var stillSince: Long? = null

    private fun feelsMotion(now: Long) = shakes.lastOrNull()?.let { now - it.first <= MOTION_FRESH_MS } == true

    private fun steadySince(now: Long): Long? {
        if (shakes.size < STEADY_SECONDS || !feelsMotion(now)) return null
        val first = shakes.first().first
        if (now - first > (STEADY_SECONDS + 2) * 1000L) return null
        val calm = shakes.map { it.second }.filter { it < MotionStops.WALKING_RMS }
        if (calm.size < STEADY_SECONDS - STEADY_JOLTS) return null
        if (calm.count { it < STEADY_LEAST } > 1) return null
        val median = calm.sorted()[calm.size / 2]
        val change = calm.zipWithNext { a, b -> abs(a - b) }.sorted()[(calm.size - 1) / 2]
        return first.takeIf { median >= STEADY_MEDIAN && change <= STEADY_CHANGE * median }
    }

    private fun walkingNow(now: Long): Boolean =
        feelsMotion(now) && shakes.takeLast(10).count { MotionStops.feel(it.second) == MotionStops.Feel.WALKING } >= WALKING_SECONDS

    fun onShake(now: Long, rms: Double) {
        shakes.addLast(now to rms)
        while (shakes.size > STEADY_SECONDS) shakes.removeFirst()
        if (MotionStops.feel(rms) == MotionStops.Feel.STILL) {
            if (stillSince == null) stillSince = now
        } else {
            stillSince = null
        }
        if (legs.isEmpty() || mode == TrackMode.WAITING || mode == TrackMode.FINISHED) return
        val steady = steadySince(now)

        val next = legs.getOrNull(leg + 1)
        if (!legs[leg].isRail && next?.isRail == true && legs[leg].length - along < STATION_REACH_METERS &&
            steady != null && now - lastGoodAt >= QUIET_AFTER_LEAVING_MS
        ) {
            enterRail(lastGoodAt)
        }
        val rail = legs[leg]
        if (!rail.isRail) return
        val stations = rail.stations

        if (!railMoving && steady != null && steady >= railSince &&
            now - lastGoodAt >= QUIET_AFTER_LEAVING_MS && (stations.size < 2 || railStation < stations.lastIndex)
        ) {
            railMoving = true
            railSince = steady
            if (mode == TrackMode.UNDERGROUND) placeUnderground(now)
            return
        }
        val still = stillSince
        if (railMoving && still != null && now - still >= STOPPED_MS && still - railSince >= MIN_STRETCH_MS &&
            stations.size >= 2 && railStation < stations.lastIndex
        ) {
            railStation += 1
            railMoving = false
            railSince = still
            stationsCounted += 1
            if (mode == TrackMode.UNDERGROUND) {
                along = max(along, stations[railStation])
                segment = segmentAt(legStart[leg] + along)
                why = "stopped-at-station"
            }
        }
    }

    private fun legSpanMs(l: TrackedLeg): Long =
        ((l.milestone.arrivalTime - l.milestone.departureTime) * 1000).takeIf { it > 0 }
            ?: (l.milestone.durationSeconds * 1000).toLong()

    private companion object {
        const val OFF_ROUTE_COUNT = 10
        const val BACKTRACK_METERS = 50.0
        const val CONFIRM_METERS = 150.0
        const val FINISH_METERS = 30.0

        const val RAIL_WORST_ACCURACY = 100f
        const val STATION_SNAP_METERS = 150.0
        const val HANDOFF_METERS = 150.0
        const val NEAR_AHEAD_METERS = 300.0

        const val QUIET_MEANS_LEFT_MS = 40_000L
        const val ARRIVAL_GRACE_MS = 10_000L
        const val MIN_STRETCH_MS = 45_000L
        const val QUIET_AFTER_LEAVING_MS = 10_000L
        const val STEADY_SECONDS = 20
        const val STEADY_LEAST = 0.3
        const val STEADY_MEDIAN = 0.6
        const val STEADY_JOLTS = 3

        const val STEADY_CHANGE = 0.2

        const val WALKING_SECONDS = 7
        const val MOTION_FRESH_MS = 5_000L

        const val BOARDED_SPEED = 4f

        const val STOP_REACH_METERS = 25.0
        const val WAITING_AREA_METERS = 150.0
        const val ARRIVING_METERS = 200.0

        const val ARRIVED_METERS = 40.0

        const val OWN_WAY_SLACK_METERS = 100.0

        const val OWN_WAY_NOISE_METERS = 15.0
        const val STOPPED_MS = 5_000L
        const val SKY_LOST_MS = 20_000L
        const val BUS_CLOCK_MS = 30_000L
        const val STATION_REACH_METERS = 150.0
        const val TRAIN_SPEED_MPS = 9.0

        const val NEARLY_THERE = 0.97
    }
}
