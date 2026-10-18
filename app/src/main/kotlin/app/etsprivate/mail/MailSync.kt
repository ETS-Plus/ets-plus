package app.etsprivate.mail

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.etsprivate.MainActivity
import app.etsprivate.R
import app.etsprivate.auth.MicrosoftOAuth
import app.etsprivate.auth.TokenStore
import app.etsprivate.cache.DiskCache
import app.etsprivate.settings.MailPollInterval
import app.etsprivate.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object MailSync {
    private const val WORK_NAME = "mail-sync"
    private const val ETS_WORK_NAME = "ets-sync"
    private const val CHANNEL_ID = "new-mail-high"
    private const val PREFS = "mail_sync"
    private const val KEY_LAST_SEEN_UID = "last_seen_uid"
    private const val KEY_LAST_SYNC = "last_sync"

    fun schedule(context: Context) {
        schedulePeriodic<MailSyncWorker>(context, WORK_NAME, Settings.getMailPollInterval(context))
        scheduleEts(context)
    }

    fun scheduleEts(context: Context) {
        schedulePeriodic<EtsSyncWorker>(context, ETS_WORK_NAME, Settings.getEtsPollInterval(context))
    }

    private inline fun <reified W : androidx.work.ListenableWorker> schedulePeriodic(
        context: Context,
        name: String,
        interval: MailPollInterval,
    ) {
        val work = WorkManager.getInstance(context.applicationContext)
        if (interval == MailPollInterval.MANUAL) {
            work.cancelUniqueWork(name)
            return
        }
        val minutes = interval.minutes.toLong().coerceAtLeast(MIN_PERIODIC_MINUTES)
        val request = PeriodicWorkRequestBuilder<W>(minutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        work.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun clearNotification(context: Context, uid: Long) {
        runCatching {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(uid.toInt())
            manager.cancel(NOTIFICATION_ID)
        }
    }

    fun lastSync(context: Context): Long = prefs(context).getLong(KEY_LAST_SYNC, 0L)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.mail_notification_channel),

                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    suspend fun syncOnce(context: Context): Boolean = withContext(Dispatchers.IO) {
        val tokens = TokenStore.load(context, MAIL_ACCOUNT) ?: return@withContext true
        val email = tokens.email ?: return@withContext true
        val refresh = tokens.refreshToken

        val messages = try {
            OutlookImap.fetchFolder(email, tokens.accessToken, INBOX, limit = SYNC_FETCH_LIMIT, withPreviews = false)
        } catch (e: OutlookImap.AuthException) {
            if (refresh == null) return@withContext true
            val refreshed = try {
                MicrosoftOAuth.refresh(MicrosoftOAuth.MAIL, refresh)
            } catch (refreshError: Exception) {
                return@withContext false
            }
            TokenStore.save(context, MAIL_ACCOUNT, refreshed.copy(email = refreshed.email ?: email))
            try {
                OutlookImap.fetchFolder(email, refreshed.accessToken, INBOX, limit = SYNC_FETCH_LIMIT, withPreviews = false)
            } catch (retryError: Exception) {
                return@withContext false
            }
        } catch (e: Exception) {
            return@withContext false
        }

        val store = prefs(context)
        store.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()

        runCatching {
            val key = "mail/messages/$email/$INBOX"
            val oldest = messages.minOfOrNull { it.date } ?: Long.MAX_VALUE
            val kept = DiskCache.read(context, key)?.let { runCatching { decodeMailMessages(it) }.getOrNull() }.orEmpty()
            val fetched = messages.map { it.uid }.toSet()
            val older = kept.filter { it.uid !in fetched && it.date < oldest }

            val previews = kept.associate { it.uid to it.preview }
            val withPreviews = messages.map { if (it.preview.isEmpty()) it.copy(preview = previews[it.uid].orEmpty()) else it }
            val list = (withPreviews + older).take(app.etsprivate.settings.Settings.getMailKeep(context))
            DiskCache.write(context, key, encodeMailMessages(list))
            app.etsprivate.cache.MemoryCache.put(key, list)
        }

        val lastSeen = store.getLong(KEY_LAST_SEEN_UID, 0L)
        val highest = messages.maxOfOrNull { it.uid } ?: 0L
        if (highest > lastSeen) store.edit().putLong(KEY_LAST_SEEN_UID, highest).apply()

        if (lastSeen == 0L) return@withContext true

        val fresh = messages.filter { it.uid > lastSeen && !it.isRead }
        if (fresh.isNotEmpty()) notifyNewMail(context, fresh)
        true
    }

    private fun notifyNewMail(context: Context, messages: List<MailMessage>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val target = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(app.etsprivate.NotificationTarget.EXTRA_TAB, app.etsprivate.NotificationTarget.TAB_MAIL)
        if (messages.size == 1) {
            target.putExtra(app.etsprivate.NotificationTarget.EXTRA_FOLDER, INBOX)
                .putExtra(app.etsprivate.NotificationTarget.EXTRA_UID, messages.first().uid)
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val newest = messages.maxByOrNull { it.date } ?: messages.first()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(newest.from.ifBlank { context.getString(R.string.mail_notification_channel) })
            .setContentText(newest.subject)
            .setAutoCancel(true)
            .setContentIntent(openApp)
        if (messages.size > 1) {
            builder.setContentTitle(
                context.resources.getQuantityString(R.plurals.mail_notification_new, messages.size, messages.size),
            )
            val inbox = NotificationCompat.InboxStyle()
            messages.take(5).forEach { inbox.addLine("${it.from}: ${it.subject}") }
            builder.setStyle(inbox)
        }

        val id = if (messages.size == 1) messages.first().uid.toInt() else NOTIFICATION_ID
        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val MIN_PERIODIC_MINUTES = 15L
    private const val SYNC_FETCH_LIMIT = 25
    private const val NOTIFICATION_ID = 4201
    private const val INBOX = "INBOX"
    private const val MAIL_ACCOUNT = "mail"
}

class MailSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val done = MailSync.syncOnce(applicationContext)

        runCatching { app.etsprivate.ets.ScheduleShares.receive(applicationContext) }
        return if (done) Result.success() else Result.retry()
    }
}

class EtsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val done = app.etsprivate.ets.GradesSync.syncOnce(applicationContext)

        runCatching { app.etsprivate.ets.FinalExams.refresh(applicationContext) }

        runCatching { app.etsprivate.ets.ScheduleShares.resendIfChanged(applicationContext) }

        app.etsprivate.widget.Widgets.refresh(applicationContext)
        return if (done) Result.success() else Result.retry()
    }
}
