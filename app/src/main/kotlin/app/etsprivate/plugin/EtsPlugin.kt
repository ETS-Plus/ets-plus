package app.etsprivate.plugin

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface EtsModule {
    val id: String

    fun pages(): List<EtsPlugin>

    fun attach(environment: PluginEnvironment) {}

    fun onAppStart(context: Context) {}

    fun settings(): PluginSettings? = null

    fun service(): PluginService? = null

    fun credits(): List<PluginCredit> = emptyList()

    fun widgets(): List<PluginWidget> = emptyList()

    fun screen(id: String): PluginScreen? = null
}

interface EtsPlugin {
    val id: String

    fun name(context: Context): String

    val icon: ImageVector

    val surface: String get() = id

    @Composable
    fun Content(host: PluginHost)

    fun open(): Boolean = false

    fun reset() {}
}

class PluginHost(
    val context: Context,

    val visible: Boolean,

    val onBack: (() -> Unit)? = null,
)

class PluginEnvironment(
    val context: Context,

    val resources: Resources,
)

enum class CreditSection { PRIOR_WORK, LIBRARIES, SERVICES }

class PluginCredit(
    val name: String,
    val note: String,
    val licence: String = "",
    val url: String = "",
    val section: CreditSection = CreditSection.LIBRARIES,
)

class PluginWidget(
    val id: String,
    val name: (Context) -> String,

    val page: String? = null,

    val prepare: suspend (Context, Boolean) -> Unit = { _, _ -> },
    val content: @Composable () -> Unit,
) {
    var settings: (@Composable () -> Unit)? = null
}

class PluginSettings(
    val name: (Context) -> String,
    val icon: ImageVector,
    val content: @Composable () -> Unit,
)

interface PluginService {
    fun onStart(shell: ServiceShell)

    fun onCommand(shell: ServiceShell, action: String?) {}

    fun onStop()
}

interface ServiceShell {
    val context: Context

    fun foreground(id: Int, notification: Notification)

    fun notify(id: Int, notification: Notification)

    fun cancel(id: Int)

    fun stop()
}

interface PluginScreen {
    fun onCreate(activity: Activity, arguments: Bundle)

    fun onBack(activity: Activity): Boolean = false
}

const val PLUGIN_API = 3
