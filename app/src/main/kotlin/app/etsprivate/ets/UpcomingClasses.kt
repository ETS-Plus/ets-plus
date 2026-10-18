package app.etsprivate.ets

import android.content.Context
import app.etsprivate.auth.TokenStore
import app.etsprivate.cache.DiskCache
import app.etsprivate.cache.MemoryCache
import app.etsprivate.cache.loadCached
import app.etsprivate.cache.mapObjects
import org.json.JSONArray
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val ETS_ACCOUNT = "ets"

const val UPCOMING_CLASSES = "ets/upcoming"

internal fun sessionActivitiesKey(session: SignetsSession): String =
    "ets/seances/${session.shortName}/${session.startDate.take(10)}/${session.endDate.take(10)}"

internal fun publishUpcoming(all: List<SignetsActivityOccurrence>): List<SignetsActivityOccurrence> {
    val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
    val upcoming = all.filter { it.start > now }.sortedBy { it.start }.take(12)
    MemoryCache.put(UPCOMING_CLASSES, upcoming)
    return upcoming
}

internal fun cachedEtsSession(context: Context): Pair<String, SignetsSession>? {
    val token = TokenStore.load(context, ETS_ACCOUNT)?.accessToken ?: return null
    val sessions = MemoryCache.get<List<SignetsSession>>(SESSIONS_KEY)
        ?: DiskCache.read(context, SESSIONS_KEY)
            ?.let { runCatching { JSONArray(it).mapObjects { o -> sessionFromJson(o) } }.getOrNull() }
            ?.also { MemoryCache.put(SESSIONS_KEY, it) }
        ?: return null
    return currentSessionOf(sessions)?.let { token to it }
}

private const val SESSIONS_KEY = "ets/sessions"
private val sessionRefresh = CoroutineScope(SupervisorJob() + Dispatchers.IO)
@Volatile private var refreshingSessions = false

internal suspend fun etsSession(context: Context): Pair<String, SignetsSession>? {
    cachedEtsSession(context)?.let { cached ->
        if (!refreshingSessions) {
            refreshingSessions = true
            sessionRefresh.launch {
                try {
                    loadCached(
                        context = context.applicationContext,
                        key = SESSIONS_KEY,
                        decode = { json -> JSONArray(json).mapObjects { sessionFromJson(it) } },
                        encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
                        fetch = { SignetsApi(cached.first).getSessions() },
                        render = {},
                        keep = { it.isNotEmpty() },
                    )
                } catch (_: Exception) {
                } finally {
                    refreshingSessions = false
                }
            }
        }
        return cached
    }

    val token = TokenStore.load(context, ETS_ACCOUNT)?.accessToken ?: return null
    var session: SignetsSession? = null
    runCatching {
        loadCached(
            context = context,
            key = SESSIONS_KEY,
            decode = { json -> JSONArray(json).mapObjects { sessionFromJson(it) } },
            encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
            fetch = { SignetsApi(token).getSessions() },
            render = { session = currentSessionOf(it) ?: session },
            keep = { it.isNotEmpty() },
        )
    }
    return session?.let { token to it }
}

suspend fun loadUpcomingClasses(context: Context, onLoaded: (List<SignetsActivityOccurrence>) -> Unit = {}) {
    val (token, current) = etsSession(context) ?: return
    val api = SignetsApi(token)
    runCatching {
        loadCached(
            context = context,
            key = sessionActivitiesKey(current),
            decode = { json -> JSONArray(json).mapObjects { activityFromJson(it) } },
            encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
            fetch = { api.getActivitiesBetween(current.shortName, current.startDate.take(10), current.endDate.take(10)) },
            render = { onLoaded(publishUpcoming(it)) },
        )
    }
}

suspend fun warmCourseTeachers(context: Context) {
    val prefs = context.getSharedPreferences("class_contacts", Context.MODE_PRIVATE)
    val since = System.currentTimeMillis() - prefs.getLong(WARMED, 0)

    val known = prefs.getStringSet(CURRENT, emptySet()).orEmpty().isNotEmpty()
    if (known && since < 12 * 60 * 60 * 1000L) return
    val (token, current) = etsSession(context) ?: return
    val courses = runCatching { SignetsApi(token).getCourses() }.getOrNull() ?: return

    prefs.edit().putStringSet(
        CURRENT,
        courses.filter { it.session == current.shortName }.map { it.acronym.uppercase() }.toSet(),
    ).apply()

    val read = prefs.getStringSet(READ, emptySet()).orEmpty().toMutableSet()
    courses.map { it.session to coursePrefix(it.acronym) }.distinct().forEach { (session, prefix) ->
        val key = "$session/$prefix"
        if (session != current.shortName && key in read) return@forEach
        loadCourseHoraire(context, prefix, session)
        read.add(key)
    }
    prefs.edit().putStringSet(READ, read).putLong(WARMED, System.currentTimeMillis()).apply()
}

fun currentCourseAcronyms(context: Context): Set<String> =
    context.getSharedPreferences("class_contacts", Context.MODE_PRIVATE)
        .getStringSet(CURRENT, emptySet()).orEmpty()

private const val CURRENT = "current"
private const val READ = "read"
private const val WARMED = "warmed"

private fun courseHoraireKey(sessionShortName: String, prefix: String) = "ets/horaire/$sessionShortName/$prefix"

internal fun coursePrefix(courseGroup: String): String = courseGroup.substringBefore('-').take(3).uppercase()

internal fun cachedCourseHoraire(context: Context, sessionShortName: String, prefix: String): List<SignetsCourseHoraire> {
    val key = courseHoraireKey(sessionShortName, prefix)

    return MemoryCache.get<List<SignetsCourseHoraire>>(key)
        ?: DiskCache.read(context, key)?.let { runCatching { JSONArray(it).mapObjects { o -> courseHoraireFromJson(o) } }.getOrNull() }
        ?: emptyList()
}

suspend fun loadCourseHoraire(
    context: Context,
    prefix: String,

    term: String? = null,
    onLoaded: (List<SignetsCourseHoraire>) -> Unit = {},
) {
    val (token, current) = etsSession(context) ?: return
    val session = term ?: current.shortName
    runCatching {
        loadCached(
            context = context,
            key = courseHoraireKey(session, prefix),
            decode = { json -> JSONArray(json).mapObjects { courseHoraireFromJson(it) } },
            encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
            fetch = { SignetsApi(token).getCourseHoraire(session, prefix) },
            render = { horaire ->

                app.etsprivate.mail.ClassContacts.remember(
                    context,
                    horaire.flatMap { course ->
                        course.teachers
                            .filter { it.email.isNotBlank() }
                            .map { Triple(it.name, it.email, course.acronym) }
                    },
                )
                onLoaded(horaire)
            },
        )
    }
}

private fun activityKind(name: String): String {
    val lower = name.lowercase()
    val letter = Regex("\\b(?:groupe\\s*)?([ab])\\b").find(lower)?.groupValues?.get(1).orEmpty()
    return when {
        lower.contains("labo") -> "labo$letter"
        lower.contains("travaux") || lower.startsWith("tp") -> "tp$letter"
        else -> "cours$letter"
    }
}

internal fun teachersFor(
    horaire: List<SignetsCourseHoraire>,
    activity: SignetsActivityOccurrence,
): List<SignetsTeacher> {
    val acronym = activity.courseGroup.substringBefore('-').trim()
    val group = activity.courseGroup.substringAfter('-', "").trim()
    val named = horaire.filter {
        it.acronym.equals(acronym, ignoreCase = true) && it.group == group && it.teachers.isNotEmpty()
    }
    val startsAt = activity.start.substringAfter('T', "").take(5)
    val kind = activityKind(activity.activityName)
    val match = named.firstOrNull { it.startTime.take(5) == startsAt }
        ?: named.firstOrNull { activityKind(it.activityName) == kind }
        ?: named.firstOrNull { activityKind(it.activityName).take(4) == kind.take(4) }
        ?: named.firstOrNull()
    return match?.teachers.orEmpty()
}
