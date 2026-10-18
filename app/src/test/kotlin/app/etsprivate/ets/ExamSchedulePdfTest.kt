package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamSchedulePdfTest {
    private val schedule = ExamSchedulePdf.parse(
        javaClass.classLoader!!.getResourceAsStream("final-exams-a2026.pdf")!!.readBytes(),
    )

    private fun exam(code: String) = schedule!!.exams.single { it.acronym == code }

    @Test fun readsTheSession() {
        assertNotNull(schedule)
        assertEquals("A2026", schedule!!.session)
        assertTrue(schedule.final)
        println("exams read: ${schedule.exams.size}")
        assertTrue(schedule.exams.size > 250)
    }

    @Test fun placesAnExamInItsDayAndSlot() {
        assertEquals(ExamSchedulePdf.Exam("LOG100", "2026-12-08", "09:00", "12:00"), exam("LOG100"))
        assertEquals(ExamSchedulePdf.Exam("INF111", "2026-12-18", "09:00", "12:00"), exam("INF111"))
        assertEquals(ExamSchedulePdf.Exam("AER300", "2026-12-08", "13:30", "16:30"), exam("AER300"))
    }

    @Test fun shortensAndLengthensWhatTheNotesSay() {
        assertEquals("10:30", exam("COM210").end)
        assertEquals("12:30", exam("DGA1032").end)
        assertEquals("13:00", exam("COM400").end)
    }

    @Test fun joinsACodeDrawnInTwoPieces() {
        assertEquals(ExamSchedulePdf.Exam("AER600", "2026-12-14", "09:00", "11:00"), exam("AER600"))
    }

    @Test fun everyCodeInTheGrid() {
        assertEquals(286, schedule!!.exams.size)
    }

    @Test fun keepsBothSittingsOfACourseOnTwoSlots() {
        val sittings = schedule!!.exams.filter { it.acronym == "LOG795" }
        assertEquals(
            listOf(
                ExamSchedulePdf.Exam("LOG795", "2026-12-09", "09:00", "12:00"),
                ExamSchedulePdf.Exam("LOG795", "2026-12-09", "13:30", "16:30"),
            ),
            sittings,
        )
    }

    @Test fun readsTheDraftToo() {
        val draft = ExamSchedulePdf.parse(
            javaClass.classLoader!!.getResourceAsStream("final-exams-a2026-draft.pdf")!!.readBytes(),
        )
        assertNotNull(draft)
        assertEquals("A2026", draft!!.session)
        assertEquals(false, draft.final)
        println("draft exams read: ${draft.exams.size}")
        assertEquals(ExamSchedulePdf.Exam("LOG100", "2026-12-08", "09:00", "12:00"), draft.exams.single { it.acronym == "LOG100" })
    }

    @Test fun readsOnlyBandLabels() {
        assertEquals("09:00" to "12:00", ExamSchedulePdf.slot("9h à midi"))
        assertEquals("13:30" to "16:30", ExamSchedulePdf.slot("13h30 à 16h30"))
        assertNull(ExamSchedulePdf.slot("REMARQUES : Durée 1 h 30 : COM120 - Durée 2 h : AER600"))
    }
}
