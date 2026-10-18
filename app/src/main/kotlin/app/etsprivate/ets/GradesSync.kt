package app.etsprivate.ets

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
import app.etsprivate.MainActivity
import app.etsprivate.R
import app.etsprivate.auth.MicrosoftOAuth
import app.etsprivate.auth.OAuthException
import app.etsprivate.auth.TokenStore
import app.etsprivate.NotificationTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GradesSync {
    private const val PREFS = "grades_sync"
    private const val KEY_SNAPSHOT = "snapshot"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val CHANNEL_ID = "new-grades-high"
    private const val NOTIFICATION_ID = 4202
    private const val ACCOUNT = "ets"

    fun lastSync(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SYNC, 0L)

    suspend fun syncOnce(context: Context): Boolean = withContext(Dispatchers.IO) {
        val tokens = TokenStore.load(context, ACCOUNT) ?: return@withContext true

        val marks = try {
            collectMarks(tokens.accessToken)
        } catch (e: SignetsAuthException) {
            val refresh = tokens.refreshToken ?: return@withContext true
            val refreshed = try {
                MicrosoftOAuth.refresh(MicrosoftOAuth.ETS, refresh)
            } catch (refreshError: Exception) {
                return@withContext (refreshError as? OAuthException)?.requiresReauth == true
            }
            TokenStore.save(context, ACCOUNT, refreshed)
            try {
                collectMarks(refreshed.accessToken)
            } catch (retryError: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext false

        if (marks.isEmpty()) return@withContext true

        val store = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        store.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        val previous = readSnapshot(store.getString(KEY_SNAPSHOT, null))
        store.edit().putString(KEY_SNAPSHOT, writeSnapshot(marks)).apply()

        if (previous == null) return@withContext true

        val fresh = marks.filter { (key, value) -> previous[key] != value }
        if (fresh.isNotEmpty()) {
            notifyNewGrades(
                context,
                fresh.keys.map { it.substringBefore('|') }.distinct(),

                fresh.keys.mapNotNull { it.substringAfter('|', "").takeIf { title -> title != "__final" } }
                    .singleOrNull(),
            )
        }
        true
    }

    private suspend fun collectMarks(accessToken: String): Map<String, String> {
        val api = SignetsApi(accessToken)
        val sessions = api.getSessions()
        val courses = api.getCourses()
        val current = currentSessionOf(sessions) ?: return emptyMap()

        val active = courses.filter { it.session == current.shortName }

        val marks = LinkedHashMap<String, String>()
        for (course in active) {
            val summary = runCatching { api.getCourseSummary(course.session, course.acronym, course.group) }.getOrNull()
            summary?.evaluations.orEmpty()

                .filter { it.mark != null }
                .forEach { evaluation ->
                    marks["${course.acronym}|${evaluation.title}"] = "${evaluation.mark}/${evaluation.outOf}"
                }
            course.grade?.takeIf { it.isNotBlank() }?.let { marks["${course.acronym}|__final"] = it }
        }
        return marks
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.ets_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    fun clearNotification(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    private fun notifyNewGrades(context: Context, courseAcronyms: List<String>, evaluationTitle: String?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val target = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(NotificationTarget.EXTRA_TAB, NotificationTarget.TAB_ETS)
        if (courseAcronyms.size == 1) {
            target.putExtra(NotificationTarget.EXTRA_COURSE, courseAcronyms.first())
            evaluationTitle?.let { target.putExtra(NotificationTarget.EXTRA_EVALUATION, it) }
        }
        val openGrades = PendingIntent.getActivity(
            context,
            1,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.ets_notification_new_grades,
                    courseAcronyms.size,
                    courseAcronyms.size,
                ),
            )
            .setContentText(courseAcronyms.joinToString(", "))
            .setAutoCancel(true)
            .setContentIntent(openGrades)
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build()) }
    }

    private fun readSnapshot(raw: String?): Map<String, String>? {
        if (raw == null) return null
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optString(it) }
        }.getOrDefault(emptyMap())
    }

    private fun writeSnapshot(marks: Map<String, String>): String =
        JSONObject().apply { marks.forEach { (k, v) -> put(k, v) } }.toString()
}
