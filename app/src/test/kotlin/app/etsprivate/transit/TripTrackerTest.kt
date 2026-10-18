package app.etsprivate.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

private const val LAT = 45.5
private const val LNG = -73.57

private const val STILL = 0.1
private const val RUNNING = 0.9

internal fun at(east: Double, north: Double) =
    LatLng(LAT + north / 111_195.0, LNG + east / (111_195.0 * cos(Math.toRadians(LAT))))

internal fun leg(
    transit: Boolean,
    points: List<LatLng>,
    rail: Boolean = false,
    departure: Long = 0,
    arrival: Long = 0,
    stops: Int? = null,
    boardAt: String? = null,
) = TrackedLeg(
    Milestone(
        summary = "", isTransit = transit, mode = null, routeShortName = null, title = null,
        vehicleType = if (rail) "SUBWAY" else if (transit) "BUS" else null, color = null,
        directionTransit = null, departureStop = null, arrivalStop = null,
        departureTime = departure, arrivalTime = arrival, durationSeconds = (arrival - departure).toDouble(),
        distanceMeters = 0.0, polyline = null, departPoint = points.first(), arrivePoint = points.last(),
        agencyCode = null, stopCount = stops, departureStopId = boardAt,
    ),
    points,
    rail,
)

internal fun fix(point: LatLng, second: Long, accuracy: Float = 5f, speed: Float? = null, provider: String = "gps") =
    PositionFix(point, null, speed, accuracy, elapsedMs = second * 1000, provider = provider)

class FixFilterTest {
    @Test fun aNetworkGuessDoesNotReplaceAFreshGpsFix() {
        val filter = FixFilter()
        assertTrue(filter.accept(fix(at(0.0, 0.0), 10), 10_000))
        assertFalse(filter.accept(fix(at(40.0, 0.0), 11, accuracy = 30f, provider = "network"), 11_000))
    }

    @Test fun aNetworkFixIsTakenOnceGpsHasBeenQuietLongEnough() {
        val filter = FixFilter()
        assertTrue(filter.accept(fix(at(0.0, 0.0), 10), 10_000))
        assertTrue(filter.accept(fix(at(40.0, 0.0), 20, accuracy = 30f, provider = "network"), 20_000))
    }

    @Test fun anOlderFixIsRefused() {
        val filter = FixFilter()
        assertTrue(filter.accept(fix(at(0.0, 0.0), 10), 10_000))
        assertFalse(filter.accept(fix(at(0.0, 0.0), 9, accuracy = 1f), 10_000))
    }

    @Test fun anHoursOldCacheIsRefused() {
        val filter = FixFilter()
        val cached = fix(at(0.0, 0.0), 10).copy(cached = true)
        assertFalse(filter.accept(cached, 10_000_000))
    }
}

class TripTrackerTest {
    private fun uTurnTrip() = TripTracker(
        listOf(
            leg(false, listOf(at(0.0, 0.0), at(0.0, 100.0))),
            leg(true, listOf(at(0.0, 100.0), at(1000.0, 100.0), at(1000.0, 160.0), at(0.0, 160.0))),
        ),
    )

    @Test fun followsTheLineForward() {
        val tracker = uTurnTrip()
        var second = 0L
        for (north in 0..100 step 10) tracker.onFix(fix(at(2.0, north.toDouble()), second++), second * 1000)
        for (east in 0..900 step 50) tracker.onFix(fix(at(east.toDouble(), 103.0), second++), second * 1000)
        val point = tracker.state.point!!
        assertEquals(1, point.legIndex)
        assertEquals(900.0, point.alongMeters, 10.0)
        assertEquals(TrackMode.ON_ROUTE, tracker.state.mode)
    }

    @Test fun aFixNearTheWayBackDoesNotJumpTheTripAhead() {
        val tracker = uTurnTrip()
        var second = 0L
        for (north in 0..100 step 20) tracker.onFix(fix(at(0.0, north.toDouble()), second++), second * 1000)
        tracker.onFix(fix(at(100.0, 145.0), second++, accuracy = 20f), second * 1000)
        assertTrue(tracker.state.travelled < 300)
        for (east in 100..500 step 50) tracker.onFix(fix(at(east.toDouble(), 100.0), second++), second * 1000)
        assertEquals(1, tracker.state.point!!.legIndex)
        assertEquals(500.0, tracker.state.point!!.alongMeters, 10.0)
    }

    @Test fun oneStrayFixIsNotLeavingTheRoute() {
        val tracker = uTurnTrip()
        var second = 0L
        for (north in 0..60 step 20) tracker.onFix(fix(at(0.0, north.toDouble()), second++), second * 1000)
        tracker.onFix(fix(at(300.0, -100.0), second++), second * 1000)
        assertEquals(TrackMode.ON_ROUTE, tracker.state.mode)
        for (i in 1..12) tracker.onFix(fix(at(300.0 + i * 20, -100.0), second++), second * 1000)
        assertEquals(TrackMode.OFF_ROUTE, tracker.state.mode)
    }

    @Test fun comingBackToTheRouteAfterLeavingItTakesTwoFixesThatAgree() {
        val tracker = uTurnTrip()
        var second = 0L
        tracker.onFix(fix(at(0.0, 0.0), second++), second * 1000)
        for (i in 1..12) tracker.onFix(fix(at(300.0 + i * 20, 0.0), second++), second * 1000)
        assertEquals(TrackMode.OFF_ROUTE, tracker.state.mode)
        tracker.onFix(fix(at(600.0, 100.0), second++), second * 1000)
        assertEquals(TrackMode.OFF_ROUTE, tracker.state.mode)
        tracker.onFix(fix(at(620.0, 100.0), second++), second * 1000)
        assertEquals(TrackMode.ON_ROUTE, tracker.state.mode)
        assertEquals(620.0, tracker.state.point!!.alongMeters, 10.0)
    }

    private fun metroTrip(departure: Long): TripTracker {
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        val station = line.first()
        val walkStart = LatLng(station.lat - 300 / 111_195.0, station.lng)
        return TripTracker(
            listOf(
                leg(false, listOf(walkStart, station)),
                leg(true, line, rail = true, departure = departure, arrival = departure + 360),
            ),
        )
    }

    @Test fun losingTheSkyAtTheStationPutsTheTripOnTheTrain() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000 + 120)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        val station = line.first()
        for (i in 0..5) {
            val p = LatLng(station.lat - (300 - i * 60) / 111_195.0, station.lng)
            tracker.onFix(fix(p, i.toLong()), start + i * 1000)
        }
        var now = start + 5_000
        repeat(25) { now += 1000; tracker.onTick(now) }
        assertEquals(TrackMode.UNDERGROUND, tracker.state.mode)
        assertEquals(1, tracker.state.point!!.legIndex)

        repeat(300) { now += 1000; tracker.onTick(now) }
        val along = tracker.state.point!!.alongMeters
        assertTrue("along $along", along > 300 && along < 2000)
        assertTrue(tracker.state.point!!.estimated)
    }

    @Test fun theFirstFixOutOfTheMetroCorrectsTheClockEvenBackwards() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        tracker.onFix(fix(line.first(), 0), start)
        var now = start
        repeat(330) { now += 1000; tracker.onTick(now) }
        val guessed = tracker.state.point!!.alongMeters

        val placeDArmes = LatLng(45.50635, -73.55955)
        tracker.onFix(fix(placeDArmes, 331), now)
        val corrected = tracker.state.point!!
        assertEquals(1, corrected.legIndex)
        assertFalse(corrected.estimated)
        assertTrue("guessed $guessed corrected ${corrected.alongMeters}", corrected.alongMeters < guessed)
    }

    @Test fun aStopFeltIsAStationReached() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000 + 600)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        tracker.onFix(fix(line.first(), 0), start)
        var now = start
        fun shake(seconds: Int, rms: Double) = repeat(seconds) { now += 1000; tracker.onShake(now, rms); tracker.onTick(now) }
        shake(12, STILL)
        shake(20, RUNNING)
        assertTrue(tracker.state.railMoving)
        shake(60, RUNNING)
        shake(6, STILL)
        assertEquals(1, tracker.state.railStation)
        assertEquals(1, tracker.state.stationsCounted)
    }

    @Test fun steppingOntoATrainWhileItsStationStillAnswersIsNotLeaving() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000 + 600)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        var now = start

        repeat(30) { i ->
            now += 1000
            if (i % 3 == 0) tracker.onFix(fix(line.first(), now / 1000, accuracy = 15f, provider = "network"), now)
            tracker.onShake(now, RUNNING)
            tracker.onTick(now)
        }
        assertEquals(false, tracker.state.railMoving)
    }

    @Test fun standingOnAPlatformWithNoWifiIsNotLeaving() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000 + 600)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        tracker.onFix(fix(line.first(), 0), start)
        var now = start

        val platform = listOf(0.47, 0.38, 0.74, 0.58, 0.49, 0.37, 0.27, 0.50, 0.58, 0.35, 0.63, 0.50, 0.62, 0.20, 0.61, 1.30, 0.44, 0.34, 0.42, 1.39)
        repeat(240) { i -> now += 1000; tracker.onShake(now, platform[i % platform.size]); tracker.onTick(now) }
        assertEquals(false, tracker.state.railMoving)
        assertEquals(0, tracker.state.railStation)
    }

    @Test fun aLateFixFromTheStationJustLeftDoesNotStopTheTrain() {
        val start = 1_000_000L
        val tracker = metroTrip(departure = start / 1000 + 600)
        val line = decodePolyline("qkutGfp_`MCDmU}V?AcXgSmA_AgUyP[Ug_@zZ")
        tracker.onFix(fix(line.first(), 0), start)
        var now = start
        repeat(12) { now += 1000; tracker.onShake(now, STILL); tracker.onTick(now) }
        repeat(20) { now += 1000; tracker.onShake(now, RUNNING); tracker.onTick(now) }
        assertTrue(tracker.state.railMoving)
        tracker.onFix(fix(line.first(), now / 1000, accuracy = 20f, provider = "network"), now)
        assertTrue(tracker.state.railMoving)
        assertEquals(0, tracker.state.railStation)
    }
}

class StopsLeftTest {
    private fun stop(id: String, countdown: String?) = TripStop(id, 0, null, countdown)

    @Test fun countsFromTheNextStopToTheOneToGetOffAt() {
        val run = listOf(stop("1", null), stop("2", null), stop("3", "< 1 min"), stop("4", "2 min"), stop("5", "4 min"), stop("6", "5 min"))
        val milestone = leg(true, listOf(at(0.0, 0.0), at(1.0, 1.0)), stops = 3, boardAt = "2").milestone

        assertEquals(3, stopsLeft(run, milestone))
    }

    @Test fun aRunThatDoesNotStopWhereTheLegBoardsIsNotTheOneBeingRidden() {
        val run = listOf(stop("1", "1 min"), stop("2", "2 min"))
        val milestone = leg(true, listOf(at(0.0, 0.0), at(1.0, 1.0)), stops = 1, boardAt = "9").milestone
        assertNull(stopsLeft(run, milestone))
    }

    @Test fun theStopIsNextWhenOneIsLeft() {
        val run = listOf(stop("1", null), stop("2", null), stop("3", "1 min"))
        val milestone = leg(true, listOf(at(0.0, 0.0), at(1.0, 1.0)), stops = 2, boardAt = "1").milestone
        assertNotNull(stopsLeft(run, milestone))
        assertEquals(1, stopsLeft(run, milestone))
    }
}
