package app.etsprivate.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.etsprivate.R
import app.etsprivate.settings.Settings as AppSettings
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.theme.EtsPrivateTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetSettingsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        enableEdgeToEdge()
        setContent {
            EtsPrivateTheme(mode = AppSettings.getTheme(this)) {
                Scaffold { insets ->
                    Column(modifier = Modifier.fillMaxSize().padding(insets)) {
                        AppTopBar(
                            title = stringResource(R.string.settings_widgets),
                            onBack = { finish() },
                        )
                        var clear by remember { mutableStateOf(AppSettings.isWidgetClear(this@WidgetSettingsActivity, widgetId)) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clear = !clear; set(widgetId, clear) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Text(
                                stringResource(R.string.settings_widget_clear),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = clear, onCheckedChange = { clear = it; set(widgetId, it) })
                        }

                        moduleSettings(widgetId)?.invoke()
                    }
                }
            }
        }
    }

    @Composable
    private fun moduleSettings(widgetId: Int): (@Composable () -> Unit)? {
        val provider = runCatching {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className
        }.getOrNull()
        if (provider != ModuleWidgetReceiver::class.java.name) return null
        app.etsprivate.plugin.Plugins.load(this)
        val widget = app.etsprivate.plugin.Plugins.modules()
            .flatMap { runCatching { it.widgets() }.getOrDefault(emptyList()) }
            .firstOrNull() ?: return null
        val settings = widget.settings ?: return null
        return settings
    }

    private fun set(widgetId: Int, clear: Boolean) {
        AppSettings.setWidgetClear(this, widgetId, clear)
        redraw()
    }

    override fun onPause() {
        super.onPause()
        redraw()
    }

    private fun redraw() {
        val app = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { Widgets.refresh(app) }
    }
}
