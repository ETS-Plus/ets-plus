package app.etsprivate.plugin

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.etsprivate.settings.LocaleHelper

class PluginForegroundService : Service() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    private var work: PluginService? = null
    private var moduleId: String? = null

    private val shell = object : ServiceShell {
        override val context: Context get() = this@PluginForegroundService

        override fun foreground(id: Int, notification: Notification) {
            startForeground(id, notification)
        }

        override fun notify(id: Int, notification: Notification) {
            if (NotificationManagerCompat.from(this@PluginForegroundService).areNotificationsEnabled()) {
                NotificationManagerCompat.from(this@PluginForegroundService).notify(id, notification)
            }
        }

        override fun cancel(id: Int) {
            NotificationManagerCompat.from(this@PluginForegroundService).cancel(id)
        }

        override fun stop() {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_MODULE) ?: moduleId
        if (id == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (work == null) {
            moduleId = id
            work = Plugins.moduleById(id)?.service()
            if (work == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            work?.onStart(shell)
        }
        intent?.action?.let { action -> work?.onCommand(shell, action) }
        return START_STICKY
    }

    override fun onDestroy() {
        work?.onStop()
        work = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODULE = "module"

        fun start(context: Context, moduleId: String, action: String? = null) {
            val intent = Intent(context, PluginForegroundService::class.java)
                .putExtra(EXTRA_MODULE, moduleId)
                .setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PluginForegroundService::class.java))
        }
    }
}

class PluginActivity : Activity() {
    private var screen: PluginScreen? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val moduleId = intent.getStringExtra(EXTRA_MODULE)
        val screenId = intent.getStringExtra(EXTRA_SCREEN)
        val found = moduleId?.let { Plugins.moduleById(it) }?.screen(screenId ?: "")
        if (found == null) {
            finish()
            return
        }
        screen = found
        found.onCreate(this, intent.extras ?: Bundle())
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (screen?.onBack(this) == true) return
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_MODULE = "module"
        const val EXTRA_SCREEN = "screen"

        fun intent(context: Context, moduleId: String, screenId: String): Intent =
            Intent(context, PluginActivity::class.java)
                .putExtra(EXTRA_MODULE, moduleId)
                .putExtra(EXTRA_SCREEN, screenId)
    }
}
