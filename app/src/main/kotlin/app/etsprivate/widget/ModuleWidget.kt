package app.etsprivate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.etsprivate.MainActivity
import app.etsprivate.NotificationTarget
import app.etsprivate.R
import app.etsprivate.plugin.Plugins
import app.etsprivate.settings.Settings
import app.etsprivate.widget.WidgetRender.draw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ModuleWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        draw(context) { ids.forEach { id -> drawModule(context, id, asked = false) } }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        draw(context) { drawModule(context, id, asked = false) }
    }
}

class ModuleReloadReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                drawModule(context, id, asked = false, turning = true)
                drawModule(context, id, asked = true)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }
}

internal suspend fun drawModule(context: Context, widgetId: Int, asked: Boolean, turning: Boolean = false) {
    Plugins.load(context)
    val widget = Plugins.modules().flatMap { runCatching { it.widgets() }.getOrDefault(emptyList()) }.firstOrNull()
    if (!turning) {
        widget?.let { runCatching { withTimeout(20_000) { it.prepare(context, asked) } } }
    }
    WidgetRender.push(context, widgetId) {
        GlanceTheme(colors = GlanceTheme.colors) {
            val clear = Settings.isWidgetClear(LocalContext.current, widgetId)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(if (clear) transparent() else GlanceTheme.colors.widgetBackground)
                    .appWidgetBackground()
                    .cornerRadius(24.dp)
                    .padding(12.dp),
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()

                        .let { column ->
                            widget?.page?.let { page ->
                                column.clickable(actionStartActivity(open(context, page)))
                            } ?: column
                        },
                ) {
                    if (widget == null) {
                        Text(
                            LocalContext.current.getString(R.string.widget_module_empty),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    } else {
                        widget.content()
                    }
                }
                if (widget != null) {
                    Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                        if (turning) {
                            AndroidRemoteViews(
                                remoteViews = android.widget.RemoteViews(
                                    context.packageName,
                                    R.layout.widget_reload_spinner,
                                ),
                                modifier = GlanceModifier.size(28.dp).padding(4.dp),
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.ic_refresh),
                                contentDescription = LocalContext.current.getString(R.string.widget_reload),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .padding(4.dp)

                                    .cornerRadius(14.dp)
                                    .clickable(
                                        actionSendBroadcast(
                                            Intent(context, ModuleReloadReceiver::class.java)
                                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun transparent() =
    androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.Transparent)

private fun open(context: Context, page: String): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .setAction("open/$page")
        .putExtra(NotificationTarget.EXTRA_TAB, NotificationTarget.TAB_ETS)
        .putExtra(NotificationTarget.EXTRA_PAGE, page)
