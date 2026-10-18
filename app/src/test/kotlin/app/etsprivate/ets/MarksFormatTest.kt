package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MarksFormatTest {
    @Test fun aMarkKeepsItsOwnTotal() {
        assertEquals("85%", formatMark(17.0, 20.0, asPercent = true, Locale.US))
        assertEquals("17/20", formatMark(17.0, 20.0, asPercent = false, Locale.US))
        assertEquals("17,5/20", formatMark(17.5, 20.0, asPercent = false, Locale.FRANCE))
    }

    @Test fun aWholeTotalDropsTheDecimal() {
        assertEquals("20/20", formatMark(20.0, 20.0, asPercent = false, Locale.US))
        assertEquals("100%", formatMark(20.0, 20.0, asPercent = true, Locale.US))
    }

    @Test fun aFinerMarkKeepsBothDecimals() {
        assertEquals("86%", formatMark(8.55, 10.0, asPercent = true, Locale.US))
        assertEquals("8.55/10", formatMark(8.55, 10.0, asPercent = false, Locale.US))
    }

    @Test fun nothingToShowStaysBlank() {
        assertEquals("—", formatMark(null, 10.0, asPercent = true, Locale.US))
        assertEquals("—", formatMark(8.0, 0.0, asPercent = false, Locale.US))
    }
}
