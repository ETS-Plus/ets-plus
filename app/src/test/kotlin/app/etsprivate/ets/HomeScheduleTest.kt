package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScheduleTest {
    private fun row(title: String, minutes: Int, added: Boolean) = HomeRow(
        minutes = minutes,
        added = added,
        title = title,
        subtitle = null,
        start = null,
        end = null,
        accentCourse = null,
        onClick = {},
    )

    @Test fun anAddedDateKeepsItsPlaceAmongTheClasses() {
        val shown = shownHomeRows(
            listOf(
                row("morning", 8 * 60, added = false),
                row("later", 15 * 60, added = true),
                row("second", 9 * 60, added = false),
                row("third", 10 * 60, added = false),
            ),
        )
        assertEquals(listOf("morning", "second", "later"), shown.map { it.title })
    }

    @Test fun aDateWithNoHourSitsAboveTheClasses() {
        val shown = shownHomeRows(
            listOf(
                row("class", 9 * 60, added = false),
                row("exam", -1, added = true),
            ),
        )
        assertEquals(listOf("exam", "class"), shown.map { it.title })
    }

    @Test fun addedDatesStayWhenTheDayAlreadyHasThreeClasses() {
        val shown = shownHomeRows(
            listOf(
                row("a", 8 * 60, added = false),
                row("b", 9 * 60, added = false),
                row("c", 10 * 60, added = false),
                row("mine", 11 * 60, added = true),
                row("also", 12 * 60, added = true),
            ),
        )
        assertEquals(listOf("a", "mine", "also"), shown.map { it.title })
    }
}
