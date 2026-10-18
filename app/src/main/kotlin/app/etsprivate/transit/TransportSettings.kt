package app.etsprivate.transit

import android.content.Context

object TransportSettings {
    private const val PREFS = "settings"
    private const val KEY_MAP_THEME = "map_theme"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mapTheme(context: Context): MapTheme =
        prefs(context).getString(KEY_MAP_THEME, null)
            ?.let { runCatching { MapTheme.valueOf(it) }.getOrNull() }
            ?: MapTheme.APP

    fun setMapTheme(context: Context, theme: MapTheme) {
        prefs(context).edit().putString(KEY_MAP_THEME, theme.name).apply()
        mapThemeChanges.intValue++
    }

    val mapThemeChanges = androidx.compose.runtime.mutableIntStateOf(0)
}
