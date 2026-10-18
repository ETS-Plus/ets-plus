package app.etsprivate.ui.theme

import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import app.etsprivate.settings.AppThemeMode

private val EtsRed = Color(0xFFE4022C)
private val EtsDarkRed = Color(0xFFBF311A)

private val LightColors = lightColorScheme(
    primary = EtsRed,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = Color(0xFF410006),
    secondary = EtsDarkRed,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8E9E9),
    onSecondaryContainer = Color(0xFF410006),

    error = Color(0xFF8C1D18),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFF3F1F2),
    onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFF3F1F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1B),
    onSurfaceVariant = Color(0xFF534342),
    surfaceContainer = Color(0xFFF3F3F3),
    surfaceContainerHigh = Color(0xFFEEEBEB),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2C0),
)

private val DarkColors = darkColorScheme(
    primary = EtsRed,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7F2126),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFFF8A8E),
    onSecondary = Color(0xFF5F1216),
    secondaryContainer = Color(0xFF3A2223),
    onSecondaryContainer = Color(0xFFFFDAD8),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE5E2E3),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE5E2E3),
    onSurfaceVariant = Color(0xFFC5C6CC),
    surfaceContainer = Color(0xFF232325),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    outline = Color(0xFF9A8C8B),
    outlineVariant = Color(0xFF534342),
)

private fun TextStyle.scaled() = copy(letterSpacing = 0.sp)
private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.scaled(),
        displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(),
        headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(),
        headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(),
        titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(),
        bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(),
        bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(),
        labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
    )
}

private val OledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
)

@Composable
fun EtsPrivateTheme(
    mode: AppThemeMode = AppThemeMode.APP_DEFAULT,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val isDark = when (mode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.OLED -> true
        AppThemeMode.MATERIAL_YOU, AppThemeMode.APP_DEFAULT -> systemDark
    }

    val colorScheme: ColorScheme = when (mode) {
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.DARK -> DarkColors
        AppThemeMode.OLED -> OledColors
        AppThemeMode.MATERIAL_YOU ->
            if (dynamicSupported) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) DarkColors else LightColors
            }
        AppThemeMode.APP_DEFAULT -> if (systemDark) DarkColors else LightColors
    }

    val semanticColors = if (isDark) DarkSemanticColors else LightSemanticColors

    val view = androidx.compose.ui.platform.LocalView.current

    val lightStatusBar = app.etsprivate.ui.Immersive.lightStatusBar ?: !isDark
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightStatusBar
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
