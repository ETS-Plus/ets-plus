package app.etsprivate.plugin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import app.etsprivate.NavDestination
import app.etsprivate.NotificationTarget
import app.etsprivate.Tab
import app.etsprivate.cache.MemoryCache
import app.etsprivate.ets.EtsScreen
import app.etsprivate.mail.MailScreen
import app.etsprivate.settings.AppThemeMode
import app.etsprivate.settings.SettingsScreen

object BuiltInModule : EtsModule {
    override val id = "ets.app"
    override fun pages(): List<EtsPlugin> = PAGES
    private val PAGES: List<EtsPlugin> = NavDestination.entries.map(::BuiltInPage)
}

class BuiltInPage(private val destination: NavDestination) : EtsPlugin {
    override val id: String get() = destination.id

    override fun name(context: Context): String = context.getString(destination.labelRes)

    override val icon: ImageVector get() = destination.icon

    override val surface: String get() = destination.tab.name

    override fun open(): Boolean = destination.requestPage()

    fun atStart(): Boolean = destination.tab == Tab.ETS && destination.pageValue == null &&
        MemoryCache.get<Boolean>(app.etsprivate.ets.ETS_AT_START) == true

    override fun reset() {
        destination.resetPage()

        if (destination.tab == Tab.MAIL) {
            MemoryCache.put(app.etsprivate.mail.FOLDER_KEY, null)
            MemoryCache.put(app.etsprivate.mail.OPEN_UID_KEY, null)
        }
    }

    @Composable
    override fun Content(host: PluginHost) {
        when (destination.tab) {
            Tab.ETS -> EtsScreen(
                rootPage = destination.pageValue,
                openCourseAcronym = AppShell.pendingCourse,
                openEvaluationTitle = AppShell.pendingEvaluation,
                onOpenHandled = { AppShell.pendingCourse = null; AppShell.pendingEvaluation = null },
                visible = host.visible,
            )
            Tab.MAIL -> MailScreen(
                openFolder = AppShell.pendingMail?.folder,
                openUid = AppShell.pendingMail?.uid,
                onOpenHandled = { AppShell.pendingMail = null },
                shared = AppShell.pendingShare,
                onSharedHandled = { AppShell.pendingShare = null },
                visible = host.visible,
            )
            Tab.SETTINGS -> SettingsScreen(
                currentTheme = AppShell.theme,
                onThemeChange = { AppShell.onThemeChange(it) },
                onNavDestinationsChange = { AppShell.onDestinationsChange(it) },
            )
        }
    }
}

object AppShell {
    var theme by mutableStateOf(AppThemeMode.APP_DEFAULT)
    var onThemeChange: (AppThemeMode) -> Unit = {}
    var onDestinationsChange: (List<EtsPlugin>) -> Unit = {}

    var openDestination: (String) -> Boolean = { false }

    var pendingMail by mutableStateOf<NotificationTarget?>(null)

    var pendingShare by mutableStateOf<app.etsprivate.mail.ComposeDraft?>(null)
    var pendingCourse by mutableStateOf<String?>(null)
    var pendingEvaluation by mutableStateOf<String?>(null)
}
