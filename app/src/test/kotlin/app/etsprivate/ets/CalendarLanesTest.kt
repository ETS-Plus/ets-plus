package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarLanesTest {
    @Test fun aSpanAloneKeepsTheWidth() {
        assertEquals(listOf(0 to 1), assignLanes(listOf(0 to 60)))
    }

    @Test fun overlappingSpansSitBesideEachOther() {
        assertEquals(listOf(0 to 2, 1 to 2), assignLanes(listOf(0 to 60, 30 to 90)))
    }

    @Test fun aSpanAfterAFinishedOneStartsOver() {
        assertEquals(listOf(0 to 1, 0 to 1), assignLanes(listOf(0 to 60, 60 to 120)))
    }

    @Test fun aLaterSpanReusesAFreeLane() {
        assertEquals(
            listOf(0 to 2, 1 to 2, 0 to 2),
            assignLanes(listOf(0 to 60, 30 to 90, 60 to 120)),
        )
    }

    @Test fun overlapKeepsTheFullWidthOutsideTheSharedMinutes() {
        val slices = barSlices(listOf(0 to 60, 30 to 90))
        assertEquals(listOf(BarSlice(0, 30, 0f, 1f), BarSlice(30, 60, 0f, 0.5f)), slices[0])
        assertEquals(listOf(BarSlice(30, 60, 0.5f, 0.5f), BarSlice(60, 90, 0f, 1f)), slices[1])
        assertEquals(BarSlice(0, 30, 0f, 1f), labelSlice(slices[0]))
        assertEquals(BarSlice(60, 90, 0f, 1f), labelSlice(slices[1]))
    }

    @Test fun theSameMinutesStaySideBySide() {
        val slices = barSlices(listOf(0 to 60, 0 to 60))
        assertEquals(listOf(BarSlice(0, 60, 0f, 0.5f)), slices[0])
        assertEquals(listOf(BarSlice(0, 60, 0.5f, 0.5f)), slices[1])
    }

    @Test fun aShortSpanInsideALongOneLeavesTheLongOneItsWidth() {
        val slices = barSlices(listOf(0 to 120, 0 to 30))
        assertEquals(BarSlice(30, 120, 0f, 1f), labelSlice(slices[0]))
        assertEquals(BarSlice(0, 30, 0.5f, 0.5f), labelSlice(slices[1]))
    }

    @Test fun spansThatOnlyTouchEachKeepTheWidth() {
        val slices = barSlices(listOf(0 to 60, 60 to 120))
        assertEquals(listOf(BarSlice(0, 60, 0f, 1f)), slices[0])
        assertEquals(listOf(BarSlice(60, 120, 0f, 1f)), slices[1])
    }

    @Test fun aSliverOfFullWidthDoesNotStealTheLabel() {
        val slices = barSlices(listOf(0 to 120, 0 to 110))
        assertEquals(listOf(BarSlice(0, 110, 0f, 0.5f), BarSlice(110, 120, 0f, 1f)), slices[0])
        assertEquals(BarSlice(0, 110, 0f, 0.5f), labelSlice(slices[0], minWideMinutes = 15))
    }

    @Test fun aFullWidthStretchGetsTheLabelEvenWhenTheNarrowPartIsLonger() {
        val slices = barSlices(listOf(0 to 180, 0 to 150))
        assertEquals(listOf(BarSlice(0, 150, 0f, 0.5f), BarSlice(150, 180, 0f, 1f)), slices[0])
        assertEquals(BarSlice(150, 180, 0f, 1f), labelSlice(slices[0], minWideMinutes = 15))
    }
}
