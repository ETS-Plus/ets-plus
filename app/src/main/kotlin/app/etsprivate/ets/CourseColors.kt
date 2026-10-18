package app.etsprivate.ets

import androidx.compose.ui.graphics.Color

object CourseColors {
    private val palette = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726),
        Color(0xFFAB47BC), Color(0xFF26A69A), Color(0xFFEC407A), Color(0xFF7E57C2),
        Color(0xFF8D6E63), Color(0xFF29B6F6),
    )

    private val assigned = mutableMapOf<String, Color>()

    fun forCourse(acronym: String): Color = assigned.getOrPut(acronym) { palette[assigned.size % palette.size] }
}
