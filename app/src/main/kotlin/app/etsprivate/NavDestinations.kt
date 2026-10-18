package app.etsprivate

import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import app.etsprivate.cache.MemoryCache

enum class Tab(val labelRes: Int, val icon: ImageVector) {
    ETS(R.string.tab_ets, Icons.Outlined.School),
    MAIL(R.string.tab_mail, Icons.Outlined.Mail),
    SETTINGS(R.string.tab_settings, Icons.Outlined.Settings),
}

enum class NavDestination(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val tab: Tab,

    private val page: Pair<String, String>? = null,
) {
    ETS("ets", R.string.tab_ets, Icons.Outlined.School, Tab.ETS),
    SCHEDULE("schedule", R.string.ets_schedule, Icons.Outlined.CalendarMonth, Tab.ETS, PAGE_ETS to "SCHEDULE"),
    GRADES("grades", R.string.ets_notes, Icons.Outlined.Grade, Tab.ETS, PAGE_ETS to "NOTES"),
    PROFILE("profile", R.string.ets_profile, Icons.Outlined.Person, Tab.ETS, PAGE_ETS to "PROFILE"),
    EXTERNAL("external", R.string.ets_external, Icons.Outlined.OpenInBrowser, Tab.ETS, PAGE_ETS to "EXTERNAL"),
    MAIL("mail", R.string.tab_mail, Icons.Outlined.Mail, Tab.MAIL),
    SETTINGS("settings", R.string.tab_settings, Icons.Outlined.Settings, Tab.SETTINGS),
    ;

    val isTabRoot: Boolean get() = page == null

    val pageValue: String? get() = page?.second

    fun requestPage(): Boolean {
        val (key, value) = page ?: return false
        val already = MemoryCache.get<String>(key) == value
        MemoryCache.put(key, value)
        return !already
    }

    fun resetPage() {
        if (tab == Tab.SETTINGS) {
            MemoryCache.put(app.etsprivate.settings.PAGE_KEY, null)
            MemoryCache.put(app.etsprivate.settings.MAIL_PAGE_KEY, null)
            MemoryCache.put(app.etsprivate.settings.PLUGIN_PAGE_KEY, null)
            MemoryCache.put(app.etsprivate.settings.GENERAL_PAGE_KEY, null)
            MemoryCache.put(app.etsprivate.settings.TRANSPORT_PAGE_KEY, null)
            MemoryCache.put(app.etsprivate.settings.NOTIFICATIONS_PAGE_KEY, null)
            return
        }
        val key = page?.first ?: when (tab) {
            Tab.ETS -> PAGE_ETS
            else -> return
        }
        MemoryCache.put(key, page?.second)
    }

    companion object {
        val DEFAULT = listOf(ETS, SCHEDULE, MAIL, SETTINGS)

        const val MAX = 5

        fun byId(id: String): NavDestination? = entries.firstOrNull { it.id == id }
    }
}

const val PAGE_ETS = "ets/page"
const val PAGE_TRANSIT = "transit/page"
