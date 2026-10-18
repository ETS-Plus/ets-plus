package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleShareTest {
    private fun c(acronym: String, day: Int, start: String, end: String, group: String = "01", kind: String = "Cours") =

        SharedClass(acronym, group, kind, "2026-10-%02d".format(4 + day), start, end)

    @Test fun aScheduleSurvivesTheTrip() {
        val schedule = SharedSchedule("Camille", "camille@ens.etsmtl.ca", "A2026", 1_000L, listOf(c("LOG121", 1, "13:30", "17:00")))
        assertEquals(schedule, sharedScheduleFromJson(schedule.toJson()))
    }

    @Test fun anythingElseIsNotASchedule() {
        assertNull(sharedScheduleFromJson("{\"format\":1,\"classes\":[]}"))
        assertNull(sharedScheduleFromJson("not json"))
    }

    @Test fun theSameClassAtTheSameTimeIsInCommon() {
        val me = listOf(c("LOG121", 1, "13:30", "17:00"), c("MAT350", 1, "08:30", "12:00"))
        val friend = listOf(c("LOG121", 1, "13:30", "17:00"), c("GTI100", 1, "08:30", "12:00"))
        val monday = compareTerms(listOf(me, friend)).single()
        assertEquals(listOf("LOG121"), monday.commonClasses.map { it.acronym })
    }

    @Test fun anotherGroupIsNotInCommon() {
        val me = listOf(c("LOG121", 1, "13:30", "17:00", group = "01"))
        val friend = listOf(c("LOG121", 1, "13:30", "17:00", group = "02"))
        assertTrue(compareTerms(listOf(me, friend)).single().commonClasses.isEmpty())
    }

    @Test fun aBreakIsWhenEveryoneIsFreeBetweenClasses() {
        val me = listOf(c("MAT350", 2, "08:30", "12:00"), c("LOG121", 2, "13:30", "17:00"))
        val friend = listOf(c("GTI100", 2, "09:00", "11:00"), c("ING150", 2, "13:30", "16:00"))
        val tuesday = compareTerms(listOf(me, friend)).single()
        assertEquals(listOf(TimeSpan("12:00", "13:30")), tuesday.commonBreaks)
    }

    @Test fun noBreakBeforeSomeoneArrivesOrAfterSomeoneLeaves() {
        val me = listOf(c("MAT350", 3, "08:30", "10:00"), c("LOG121", 3, "15:00", "17:00"))
        val friend = listOf(c("GTI100", 3, "13:00", "14:00"))
        val wednesday = compareTerms(listOf(me, friend)).single()
        assertTrue(wednesday.commonBreaks.isEmpty())
    }

    @Test fun onlyDaysEveryoneIsAtSchool() {
        val me = listOf(c("A", 1, "08:30", "10:00"), c("B", 4, "08:30", "10:00"))
        val friend = listOf(c("C", 4, "11:00", "12:00"))
        assertEquals(listOf("2026-10-08"), compareTerms(listOf(me, friend)).map { it.date })
    }

    @Test fun threePeopleShareOnlyWhatAllThreeShare() {
        val a = listOf(c("LOG121", 1, "13:30", "17:00"), c("X", 1, "08:30", "10:00"))
        val b = listOf(c("LOG121", 1, "13:30", "17:00"), c("Y", 1, "09:00", "10:30"))
        val third = listOf(c("Z", 1, "10:00", "11:00"))
        val monday = compareTerms(listOf(a, b, third)).single()
        assertTrue(monday.commonClasses.isEmpty())
    }
}

class TermShareTest {
    @Test fun signetsSessionsBecomeDatedClasses() {
        val occurrences = listOf(
            SignetsActivityOccurrence("LOG121-03", "Conception", "Cours", "A-1234", "2026-10-05T13:30:00", "2026-10-05T17:00:00"),
            SignetsActivityOccurrence("LOG121-03", "Conception", "Laboratoire", "A-1234", "2026-10-07T08:30:00", "2026-10-07T10:30:00"),
        )
        val classes = sharedClassesOf(occurrences)
        assertEquals(SharedClass("LOG121", "03", "Cours", "2026-10-05", "13:30", "17:00"), classes.first())
        assertEquals(1, classes.first().weekday)
        assertEquals(listOf(1, 3), typicalWeek(classes).keys.toList())
    }

    @Test fun theSameWeekdayOnDifferentWeeksIsOneLineOfTheTypicalWeek() {
        val term = listOf(
            SharedClass("LOG121", "03", "Cours", "2026-10-05", "13:30", "17:00"),
            SharedClass("LOG121", "03", "Cours", "2026-10-12", "13:30", "17:00"),
        )
        assertEquals(1, typicalWeek(term).getValue(1).size)
    }
}
