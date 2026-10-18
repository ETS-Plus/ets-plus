package app.etsprivate.settings

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf

enum class AppThemeMode { APP_DEFAULT, LIGHT, DARK, OLED, MATERIAL_YOU }

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    FRENCH("fr"),
}

enum class MailSwipeAction { NONE, TOGGLE_READ, ARCHIVE, DELETE }

enum class MailPollInterval(val minutes: Int) {
    MANUAL(0),
    FIFTEEN_MIN(15),
    THIRTY_MIN(30),
    HOUR(60),
    TWO_HOURS(120),
    THREE_HOURS(180),
    SIX_HOURS(360),
    TWELVE_HOURS(720),
    DAY(1440),
}

object Settings {
    private const val PREFS = "settings"
    private const val KEY_THEME = "theme"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_MAIL_SWIPE_ENABLED = "mail_swipe_enabled"
    private const val KEY_MAIL_SWIPE_LEFT = "mail_swipe_left"
    private const val KEY_MAIL_SWIPE_RIGHT = "mail_swipe_right"
    private const val KEY_MAIL_POLL_INTERVAL = "mail_poll_interval"
    private const val KEY_MAIL_KEEP = "mail_keep"
    private const val KEY_ETS_POLL_INTERVAL = "ets_poll_interval"
    private const val KEY_ASKED_UNRESTRICTED = "asked_unrestricted"
    private const val KEY_MAIL_FOLDER_ORDER = "mail_folder_order"
    private const val KEY_TRANSIT_TRIPS = "transit_show_trips"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getTheme(context: Context): AppThemeMode =
        runCatching { AppThemeMode.valueOf(prefs(context).getString(KEY_THEME, null) ?: "") }
            .getOrDefault(AppThemeMode.APP_DEFAULT)

    fun setTheme(context: Context, mode: AppThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun getLanguage(context: Context): AppLanguage =
        runCatching { AppLanguage.valueOf(prefs(context).getString(KEY_LANGUAGE, null) ?: "") }
            .getOrDefault(AppLanguage.SYSTEM)

    fun setLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun getTransitShowTrips(context: Context): Boolean = prefs(context).getBoolean(KEY_TRANSIT_TRIPS, false)

    fun setTransitShowTrips(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_TRANSIT_TRIPS, show).apply()
    }

    fun getMailSwipeEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_MAIL_SWIPE_ENABLED, true)

    fun setMailSwipeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MAIL_SWIPE_ENABLED, enabled).apply()
    }

    fun getMailSwipeLeft(context: Context): MailSwipeAction =
        runCatching { MailSwipeAction.valueOf(prefs(context).getString(KEY_MAIL_SWIPE_LEFT, null) ?: "") }
            .getOrDefault(MailSwipeAction.ARCHIVE)

    fun setMailSwipeLeft(context: Context, action: MailSwipeAction) {
        prefs(context).edit().putString(KEY_MAIL_SWIPE_LEFT, action.name).apply()
    }

    fun getMailSwipeRight(context: Context): MailSwipeAction =
        runCatching { MailSwipeAction.valueOf(prefs(context).getString(KEY_MAIL_SWIPE_RIGHT, null) ?: "") }
            .getOrDefault(MailSwipeAction.DELETE)

    fun setMailSwipeRight(context: Context, action: MailSwipeAction) {
        prefs(context).edit().putString(KEY_MAIL_SWIPE_RIGHT, action.name).apply()
    }

    fun hasAskedUnrestricted(context: Context): Boolean = prefs(context).getBoolean(KEY_ASKED_UNRESTRICTED, false)

    fun markAskedUnrestricted(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED_UNRESTRICTED, true).apply()
    }

    fun getEtsPollInterval(context: Context): MailPollInterval =
        runCatching { MailPollInterval.valueOf(prefs(context).getString(KEY_ETS_POLL_INTERVAL, null) ?: "") }
            .getOrDefault(MailPollInterval.THREE_HOURS)

    fun setEtsPollInterval(context: Context, interval: MailPollInterval) {
        prefs(context).edit().putString(KEY_ETS_POLL_INTERVAL, interval.name).apply()
    }

    fun getMailPollInterval(context: Context): MailPollInterval =
        runCatching { MailPollInterval.valueOf(prefs(context).getString(KEY_MAIL_POLL_INTERVAL, null) ?: "") }
            .getOrDefault(MailPollInterval.FIFTEEN_MIN)

    fun getMailKeep(context: Context): Int = prefs(context).getInt(KEY_MAIL_KEEP, 100)

    fun setMailKeep(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_MAIL_KEEP, count).apply()
    }

    val MAIL_KEEP_STEPS = listOf(25, 50, 100, 250, 500, 1000, MAIL_KEEP_ALL)
    const val MAIL_KEEP_ALL = Int.MAX_VALUE

    fun setMailPollInterval(context: Context, interval: MailPollInterval) {
        prefs(context).edit().putString(KEY_MAIL_POLL_INTERVAL, interval.name).apply()
    }

    fun getMailFolderOrder(context: Context): List<String>? =
        prefs(context).getString(KEY_MAIL_FOLDER_ORDER, null)?.split(FOLDER_SEPARATOR)?.filter { it.isNotEmpty() }

    fun setMailFolderOrder(context: Context, folders: List<String>) {
        prefs(context).edit().putString(KEY_MAIL_FOLDER_ORDER, folders.joinToString(FOLDER_SEPARATOR)).apply()
    }

    private const val FOLDER_SEPARATOR = ""

    fun getNavDestinations(context: Context): List<String>? =
        prefs(context).getString(KEY_NAV_DESTINATIONS, null)?.split(',')?.filter { it.isNotEmpty() }

    fun setNavDestinations(context: Context, ids: List<String>) {
        prefs(context).edit().putString(KEY_NAV_DESTINATIONS, ids.joinToString(",")).apply()
    }

    private const val KEY_NAV_DESTINATIONS = "nav_destinations"

    fun getStartDestination(context: Context): String? =
        prefs(context).getString(KEY_START_DESTINATION, null)?.takeIf { it.isNotEmpty() }

    fun setStartDestination(context: Context, id: String) {
        prefs(context).edit().putString(KEY_START_DESTINATION, id).apply()
    }

    private const val KEY_START_DESTINATION = "start_destination"

    fun isWidgetClear(context: Context, widgetId: Int): Boolean =
        prefs(context).getBoolean("widget_clear_$widgetId", false)

    fun setWidgetClear(context: Context, widgetId: Int, clear: Boolean) {
        prefs(context).edit().putBoolean("widget_clear_$widgetId", clear).apply()
    }

    fun getSettingsOrder(context: Context): List<String>? =
        prefs(context).getString(KEY_SETTINGS_ORDER, null)?.split(',')?.filter { it.isNotEmpty() }

    fun setSettingsOrder(context: Context, ids: List<String>) {
        prefs(context).edit().putString(KEY_SETTINGS_ORDER, ids.joinToString(",")).apply()
    }

    private const val KEY_SETTINGS_ORDER = "settings_order"

    fun getTeachersFromEveryCourse(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TEACHERS_EVERY_COURSE, true)

    fun setTeachersFromEveryCourse(context: Context, every: Boolean) {
        prefs(context).edit().putBoolean(KEY_TEACHERS_EVERY_COURSE, every).apply()
    }

    private const val KEY_TEACHERS_EVERY_COURSE = "mail_teachers_every_course"

    fun hiddenEtsSections(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ETS_HIDDEN, emptySet()).orEmpty()

    fun setHiddenEtsSections(context: Context, hidden: Set<String>) {
        prefs(context).edit().putStringSet(KEY_ETS_HIDDEN, hidden).apply()
    }

    private const val KEY_ETS_HIDDEN = "ets_hidden_sections"

    fun etsSectionOrder(context: Context): List<String>? =
        prefs(context).getString(KEY_ETS_ORDER, null)?.split(',')?.filter { it.isNotEmpty() }

    fun setEtsSectionOrder(context: Context, ids: List<String>) {
        prefs(context).edit().putString(KEY_ETS_ORDER, ids.joinToString(",")).apply()
    }

    private const val KEY_ETS_ORDER = "ets_section_order"

    private const val KEY_ETS_GRADES_PERCENT = "ets_grades_percent"
    private val gradesScale = mutableIntStateOf(0)

    fun gradesAsPercent(context: Context): Boolean {
        gradesScale.intValue
        return prefs(context).getBoolean(KEY_ETS_GRADES_PERCENT, true)
    }

    fun setGradesAsPercent(context: Context, asPercent: Boolean) {
        prefs(context).edit().putBoolean(KEY_ETS_GRADES_PERCENT, asPercent).apply()
        gradesScale.intValue++
    }

    fun getRememberPages(context: Context): Boolean = prefs(context).getBoolean(KEY_REMEMBER_PAGES, false)

    fun setRememberPages(context: Context, remember: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMEMBER_PAGES, remember).apply()
    }

    fun getVibrations(context: Context): Boolean = prefs(context).getBoolean(KEY_VIBRATIONS, true)

    fun setVibrations(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIBRATIONS, on).apply()
        app.etsprivate.ui.Haptics.enabled = on
    }

    fun getRememberMail(context: Context): Boolean = prefs(context).getBoolean(KEY_REMEMBER_MAIL, true)

    fun setRememberMail(context: Context, remember: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMEMBER_MAIL, remember).apply()
    }

    private const val KEY_REMEMBER_PAGES = "remember_pages"
    private const val KEY_VIBRATIONS = "vibrations"
    private const val KEY_REMEMBER_MAIL = "remember_mail"

    fun getBusAlerts(context: Context): Boolean = prefs(context).getBoolean(KEY_ALERT_BUS, false)

    fun setBusAlerts(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALERT_BUS, enabled).apply()
    }

    fun getMetroAlerts(context: Context): Boolean = prefs(context).getBoolean(KEY_ALERT_METRO, false)

    fun setMetroAlerts(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALERT_METRO, enabled).apply()
    }

    private const val KEY_ALERT_BUS = "transit_alert_bus"
    private const val KEY_ALERT_METRO = "transit_alert_metro"

    fun getMapRegionFetched(context: Context, regionId: String): Long =
        prefs(context).getLong("$KEY_MAP_FETCHED$regionId", 0L)

    fun setMapRegionFetched(context: Context, regionId: String, at: Long) {
        prefs(context).edit().putLong("$KEY_MAP_FETCHED$regionId", at).apply()
    }

    private const val KEY_MAP_THEME = "map_theme"
    private const val KEY_MAP_FETCHED = "map_fetched_"

    fun getMapOnLockScreen(context: Context): Boolean = prefs(context).getBoolean(KEY_MAP_LOCK_SCREEN, true)

    fun setMapOnLockScreen(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MAP_LOCK_SCREEN, enabled).apply()
    }

    private const val KEY_MAP_LOCK_SCREEN = "map_lock_screen"

    fun getFavoriteLinks(context: Context): Set<String> {
        val store = prefs(context)
        if (!store.contains(KEY_FAVORITE_LINKS)) return DEFAULT_FAVORITE_LINKS
        return store.getStringSet(KEY_FAVORITE_LINKS, emptySet()).orEmpty()
    }

    val DEFAULT_FAVORITE_LINKS = setOf("Preuve étudiante STM", "Remboursements")

    fun setFavoriteLinks(context: Context, labels: Set<String>) {
        prefs(context).edit().putStringSet(KEY_FAVORITE_LINKS, HashSet(labels)).apply()
    }

    private const val KEY_FAVORITE_LINKS = "favorite_links"

    fun getPrepareTripsOnWifi(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PREPARE_ON_WIFI, true)

    fun setPrepareTripsOnWifi(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREPARE_ON_WIFI, enabled).apply()
    }

    private const val KEY_PREPARE_ON_WIFI = "prepare_trips_wifi"

    fun getTrackDelays(context: Context): Boolean = prefs(context).getBoolean(KEY_TRACK_DELAYS, false)

    fun setTrackDelays(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TRACK_DELAYS, enabled).apply()
    }

    private const val KEY_TRACK_DELAYS = "track_delays"
}
