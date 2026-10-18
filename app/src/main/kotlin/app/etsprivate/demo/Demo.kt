package app.etsprivate.demo

import android.content.Context
import app.etsprivate.ets.SignetsActivityOccurrence
import app.etsprivate.ets.SignetsCourse
import app.etsprivate.ets.SignetsCourseHoraire
import app.etsprivate.ets.SignetsCourseSummary
import app.etsprivate.ets.SignetsProfile
import app.etsprivate.ets.SignetsProgram
import app.etsprivate.ets.SignetsScheduleActivity
import app.etsprivate.ets.SignetsSession
import app.etsprivate.mail.MailBody
import app.etsprivate.mail.MailFolderInfo
import app.etsprivate.mail.MailMessage

interface DemoSource {
    val email: String

    fun seed(context: Context)

    fun sessions(): List<SignetsSession>
    fun profile(): SignetsProfile
    fun courses(): List<SignetsCourse>
    fun programs(): List<SignetsProgram>
    fun schedule(): List<SignetsScheduleActivity>
    fun activities(startDate: String, endDate: String): List<SignetsActivityOccurrence>
    fun summary(acronym: String): SignetsCourseSummary?
    fun horaire(prefix: String): List<SignetsCourseHoraire>

    fun folders(): List<MailFolderInfo>
    fun messages(folder: String): List<MailMessage>
    fun body(uid: Long): MailBody
}

object Demo {
    private const val PREFS = "demo"
    private const val KEY_ON = "on"
    private const val KEY_SHARES_BEFORE = "shares_before"

    @Volatile private var on = false

    private val data: DemoSource? by lazy {
        if (!app.etsprivate.BuildConfig.STORE_BUILD) return@lazy null
        runCatching { Class.forName("app.etsprivate.demo.DemoData").getDeclaredField("INSTANCE").get(null) as DemoSource }.getOrNull()
    }

    val source: DemoSource? get() = if (on) data else null

    val available: Boolean get() = data != null

    fun restore(context: Context) {
        on = available && prefs(context).getBoolean(KEY_ON, false)
    }

    fun start(context: Context) {
        val demo = data ?: return

        val shares = context.getSharedPreferences("schedule_shares", Context.MODE_PRIVATE)
        prefs(context).edit()
            .putBoolean(KEY_ON, true)
            .putString(KEY_SHARES_BEFORE, shares.getString("people", null))
            .apply()
        on = true
        demo.seed(context)
        app.etsprivate.auth.TokenStore.changed()
    }

    fun stop(context: Context) {
        if (!on) return
        on = false
        val before = prefs(context).getString(KEY_SHARES_BEFORE, null)
        context.getSharedPreferences("schedule_shares", Context.MODE_PRIVATE).edit().putString("people", before ?: "[]").apply()
        prefs(context).edit().clear().apply()
        app.etsprivate.cache.MemoryCache.clear()
        app.etsprivate.cache.DiskCache.clear(context)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
