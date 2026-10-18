package app.etsprivate.ets

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.concurrent.TimeUnit

internal object FinalExams {
    private const val PAGE = "https://www.etsmtl.ca/etudier-a-lets/horaire-des-examens-finaux"
    private const val PREFS = "final_exams"
    private const val KEY = "json"

    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
    private val revisionState = mutableIntStateOf(0)

    val revision: Int get() = revisionState.intValue

    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        val page = get(PAGE)?.let { String(it, Charsets.UTF_8) } ?: return@withContext false
        val href = Regex("href=\"([^\"]*horaire-?examen[^\"]*\\.pdf[^\"]*)\"", RegexOption.IGNORE_CASE).find(page)
            ?.groupValues?.get(1)?.replace("&amp;", "&")

            ?: return@withContext true
        val url = if (href.startsWith("http")) href else "https://www.etsmtl.ca" + (if (href.startsWith("/")) "" else "/") + href
        val stored = load(context)
        if (stored?.url == url) return@withContext true
        val pdf = get(url) ?: return@withContext false

        val schedule = ExamSchedulePdf.parse(pdf) ?: return@withContext true
        save(context, Stored(url, schedule))
        revisionState.intValue++
        true
    }

    fun sittings(context: Context, session: String, acronym: String): List<ExamSchedulePdf.Exam> {
        revisionState.intValue
        val schedule = load(context)?.schedule?.takeIf { it.session == session } ?: return emptyList()
        return schedule.exams.filter { it.acronym.equals(acronym, ignoreCase = true) }
            .sortedWith(compareBy({ it.date }, { it.start }))
    }

    fun dateOf(context: Context, session: String, acronym: String, title: String): String? =
        if (isFinalExam(title)) sittings(context, session, acronym).firstOrNull()?.date else null

    fun isFinalExam(title: String): Boolean {
        val plain = Normalizer.normalize(title, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().trim()
        return "final" in plain && ("exam" in plain || plain == "final")
    }

    private fun get(url: String): ByteArray? = runCatching {
        client.newCall(Request.Builder().url(url).header("User-Agent", "ETS+ (Android)").build()).execute().use { response ->
            if (response.isSuccessful) response.body.bytes() else null
        }
    }.getOrNull()

    private class Stored(val url: String, val schedule: ExamSchedulePdf.Schedule)

    @Volatile private var cached: Stored? = null

    private fun load(context: Context): Stored? {
        cached?.let { return it }
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val exams = root.optJSONArray("exams") ?: return null
        val list = (0 until exams.length()).mapNotNull { exams.optJSONObject(it) }.map {
            ExamSchedulePdf.Exam(it.optString("acronym"), it.optString("date"), it.optString("start"), it.optString("end"))
        }
        return Stored(
            root.optString("url"),
            ExamSchedulePdf.Schedule(root.optString("session"), root.optBoolean("final"), list),
        ).also { cached = it }
    }

    private fun save(context: Context, stored: Stored) {
        val exams = JSONArray()
        stored.schedule.exams.forEach {
            exams.put(JSONObject().put("acronym", it.acronym).put("date", it.date).put("start", it.start).put("end", it.end))
        }
        val root = JSONObject()
            .put("url", stored.url)
            .put("session", stored.schedule.session)
            .put("final", stored.schedule.final)
            .put("exams", exams)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, root.toString()).apply()
        cached = stored
    }
}
