package app.etsprivate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LightSemanticColors = SemanticColors(
    success = Color(0xFF194E2C),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFF4F9F4),
    onSuccessContainer = Color(0xFF194E2C),
    warning = Color(0xFF713F12),
    onWarning = Color(0xFFFEFAE8),
    warningContainer = Color(0xFFFEFAE8),
    onWarningContainer = Color(0xFF713F12),
)

val DarkSemanticColors = SemanticColors(
    success = Color(0xFF8EE7AA),
    onSuccess = Color(0xFF082B16),
    successContainer = Color(0xFF082B16),
    onSuccessContainer = Color(0xFF8EE7AA),
    warning = Color(0xFFFEE78A),
    onWarning = Color(0xFF411107),
    warningContainer = Color(0xFF423606),
    onWarningContainer = Color(0xFFFEE78A),
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object Spacing {
    val quarter = 4.dp
    val half = 8.dp
    val default = 12.dp
    val oneHalf = 16.dp
    val double = 24.dp
    val triple = 32.dp
    val quadruple = 40.dp
}

object AppShapes {
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
}

val MaterialTheme.semantic: SemanticColors
    @Composable get() = LocalSemanticColors.current
