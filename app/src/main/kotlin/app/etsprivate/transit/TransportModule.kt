package app.etsprivate.transit

import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Map
import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.etsprivate.PAGE_TRANSIT
import app.etsprivate.R
import app.etsprivate.cache.MemoryCache
import app.etsprivate.plugin.EtsModule
import app.etsprivate.plugin.EtsPlugin
import app.etsprivate.plugin.PluginHost
import app.etsprivate.plugin.PluginService
import app.etsprivate.plugin.PluginSettings
import app.etsprivate.plugin.PluginWidget
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings as AppSettings

object TransportModule : EtsModule {
    override val id = "ets.transport"

    override fun pages(): List<EtsPlugin> = PAGES

    override fun onAppStart(context: Context) {
        Plug.attach(context)

        TripFollowWork.ensureChannels(context)
        RideLog.ensureChannel(context)
    }

    override fun settings(): PluginSettings = PluginSettings(
        name = { _ -> Plug.string(R.string.tab_transit) },
        icon = Icons.Outlined.DirectionsBus,
        content = { TransportSettingsPage() },
    )

    override fun widgets(): List<PluginWidget> = listOf(TransportWidget.widget)

    override fun service(): PluginService = TransportWork

    private val PAGES: List<EtsPlugin> = listOf(
        TransportPage("transit", null, Icons.Outlined.DirectionsBus) { Plug.string(R.string.tab_transit) },
        TransportPage("map", "MAP", Icons.Outlined.Map) { Plug.string(R.string.transit_map) },
    )
}

private class TransportPage(
    override val id: String,
    private val page: String?,
    override val icon: ImageVector,
    private val label: () -> String,
) : EtsPlugin {
    override fun name(context: Context): String = label()

    override val surface: String get() = "ets.transport"

    override fun open(): Boolean {
        if (page == null) return false
        val already = MemoryCache.get<String>(PAGE_TRANSIT) == page
        MemoryCache.put(PAGE_TRANSIT, page)
        return !already
    }

    override fun reset() {
        MemoryCache.put(PAGE_TRANSIT, page)
    }

    fun atRoot(): Boolean {
        val current = MemoryCache.get<String>(PAGE_TRANSIT)
        return if (page == null) current == null || current == "HOME" else current == page
    }

    @Composable
    override fun Content(host: PluginHost) {
        TransitScreen(rootPage = page, visible = host.visible)
    }
}

internal fun isTransportAtRoot(plugin: EtsPlugin): Boolean = (plugin as? TransportPage)?.atRoot() == true

object Plug {
    private var app: Context? = null

    fun attach(context: Context) {
        app = context.applicationContext
    }

    private var cached: Pair<String, Resources>? = null

    fun resources(context: Context? = null): Resources {
        val from = context ?: app ?: error("transport was never attached")
        val language = AppSettings.getLanguage(from).tag ?: from.resources.configuration.locales[0].toLanguageTag()
        cached?.let { (tag, resources) -> if (tag == language) return resources }
        return LocaleHelper.wrap(from).resources.also { cached = language to it }
    }

    fun context(context: Context): Context =
        android.view.ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar)

    fun string(@StringRes id: Int): String = resources().getString(id)

    fun string(@StringRes id: Int, vararg arguments: Any): String = resources().getString(id, *arguments)
}

@Composable
fun pluginString(@StringRes id: Int): String = stringResource(id)

@Composable
fun pluginString(@StringRes id: Int, vararg arguments: Any): String = stringResource(id, *arguments)
