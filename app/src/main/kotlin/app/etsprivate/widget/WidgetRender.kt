package app.etsprivate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi::class)
internal object WidgetRender {
    private val views = GlanceRemoteViews()

    private val WIDTHS = (0 until 12).map { 100f + it * 40f }

    suspend fun push(context: Context, widgetId: Int, content: @Composable () -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        val options = runCatching { manager.getAppWidgetOptions(widgetId) }.getOrNull() ?: android.os.Bundle()

        val size = DpSize(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).coerceAtLeast(40).dp,
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).coerceAtLeast(40).dp,
        )

        val words = app.etsprivate.settings.LocaleHelper.wrap(context)

        val drawn = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val height = maxOf(size.height.value, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat())
            android.widget.RemoteViews(
                WIDTHS.associate { width ->
                    android.util.SizeF(width, 1f) to
                        views.compose(words, DpSize(width.dp, height.dp), appWidgetOptions = options, content = content).remoteViews
                },
            )
        } else {
            views.compose(context = words, size = size, appWidgetOptions = options, content = content).remoteViews
        }
        manager.updateAppWidget(widgetId, drawn)
    }

    fun placed(context: Context, provider: Class<out AppWidgetProvider>): IntArray =
        runCatching {
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))
        }.getOrNull() ?: IntArray(0)

    fun AppWidgetProvider.draw(context: Context, work: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                work()
            } finally {
                runCatching { pending.finish() }
            }
        }
    }
}
