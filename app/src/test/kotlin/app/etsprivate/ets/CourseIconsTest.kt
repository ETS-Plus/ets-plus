package app.etsprivate.ets

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseIconsTest {
    @Test fun tableWinsForKnownCourses() {
        assertEquals("software", CourseIcons.category("LOG121-03"))
        assertEquals("embedded", CourseIcons.category("IND105"))
        assertEquals("communication", CourseIcons.category("COM120"))
    }

    @Test fun titleGuessesUnknownCourses() {
        assertEquals("math", CourseIcons.category("MAT145", "Calcul différentiel et intégral"))
        assertEquals("ai", CourseIcons.category("XYZ900", "Introduction à l'apprentissage profond"))
        assertEquals("safety", CourseIcons.category("ZZZ100", "Santé et sécurité au travail"))
    }

    @Test fun prefixWhenTheTitleSaysNothing() {
        assertEquals("software", CourseIcons.category("LOG999", "Sujets spéciaux"))
        assertEquals("math", CourseIcons.category("MAT999"))
    }
}
