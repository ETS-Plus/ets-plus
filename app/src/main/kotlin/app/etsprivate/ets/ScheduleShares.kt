package app.etsprivate.ets

import android.content.Context
import app.etsprivate.R
import app.etsprivate.auth.MicrosoftOAuth
import app.etsprivate.auth.TokenStore
import app.etsprivate.mail.OutgoingAttachment
import app.etsprivate.mail.OutlookImap
import app.etsprivate.mail.OutlookSmtp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.format.TextStyle

data class LinkedPerson(
    val email: String,
    val name: String,

    val schedule: SharedSchedule?,

    val sharing: Boolean,

    val sentFingerprint: String?,
)

object ScheduleShares {
    private const val PREFS = "schedule_shares"
    private const val KEY_PEOPLE = "people"
    private const val KEY_SCANNED = "scanned_uid"
    private const val MAIL_ACCOUNT = "mail"

    fun people(context: Context): List<LinkedPerson> =
        runCatching {
            val array = JSONArray(prefs(context).getString(KEY_PEOPLE, "[]"))
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                LinkedPerson(
                    email = o.getString("email"),
                    name = o.getString("name"),
                    schedule = o.optString("schedule").takeIf { it.isNotBlank() }?.let(::sharedScheduleFromJson),
                    sharing = o.optBoolean("sharing"),
                    sentFingerprint = o.optString("sent").ifBlank { null },
                )
            }
        }.getOrDefault(emptyList())

    var revision by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    private fun save(context: Context, people: List<LinkedPerson>) {
        val array = JSONArray()
        people.forEach { p ->
            array.put(
                JSONObject()
                    .put("email", p.email)
                    .put("name", p.name)
                    .put("schedule", p.schedule?.toJson() ?: "")
                    .put("sharing", p.sharing)
                    .put("sent", p.sentFingerprint ?: ""),
            )
        }
        prefs(context).edit().putString(KEY_PEOPLE, array.toString()).apply()
        revision++
    }

    fun mailReady(context: Context): Boolean = TokenStore.load(context, MAIL_ACCOUNT)?.email != null

    suspend fun receive(context: Context): List<LinkedPerson> = withContext(Dispatchers.IO) {
        val scanned = prefs(context).getLong(KEY_SCANNED, 0L)
        val (highest, files) = withMail(context) { email, token ->
            OutlookImap.findFiles(email, token, SHARED_SCHEDULE_FILE, scanned)
        } ?: return@withContext emptyList()
        prefs(context).edit().putLong(KEY_SCANNED, highest).apply()
        val mine = TokenStore.load(context, MAIL_ACCOUNT)?.email?.lowercase()
        val people = people(context).toMutableList()
        val changed = mutableListOf<LinkedPerson>()
        files.sortedBy { it.uid }.forEach { file ->
            val schedule = sharedScheduleFromJson(file.text) ?: return@forEach

            if (schedule.email != file.from || schedule.email == mine) {
                android.util.Log.i("ScheduleShare", "ignored a schedule naming ${schedule.email} from ${file.from}")
                return@forEach
            }
            val index = people.indexOfFirst { it.email == schedule.email }
            val existing = people.getOrNull(index)
            if (existing?.schedule != null && existing.schedule.sentAt >= schedule.sentAt) return@forEach
            val updated = (existing ?: LinkedPerson(schedule.email, schedule.name, null, false, null))
                .copy(name = schedule.name, schedule = schedule)
            if (index >= 0) people[index] = updated else people.add(updated)
            changed.add(updated)
        }
        if (changed.isNotEmpty()) save(context, people)
        android.util.Log.i("ScheduleShare", "looked after uid $scanned, up to $highest: ${files.size} files, ${changed.size} new")
        changed
    }

    suspend fun shareWith(context: Context, address: String): Boolean = withContext(Dispatchers.IO) {
        val email = address.trim().lowercase()
        val week = myTerm(context) ?: return@withContext false
        if (!send(context, email, week)) return@withContext false
        val people = people(context).toMutableList()
        val index = people.indexOfFirst { it.email == email }
        val existing = people.getOrNull(index)
        val updated = (existing ?: LinkedPerson(email, email.substringBefore('@'), null, true, null))
            .copy(sharing = true, sentFingerprint = fingerprint(week))
        if (index >= 0) people[index] = updated else people.add(updated)
        save(context, people)
        true
    }

    suspend fun resendIfChanged(context: Context) = withContext(Dispatchers.IO) {
        val people = people(context)
        if (people.none { it.sharing } || !mailReady(context)) return@withContext
        val week = myTerm(context) ?: return@withContext
        val now = fingerprint(week)
        val updated = people.map { person ->
            if (!person.sharing || person.sentFingerprint == now) return@map person
            if (send(context, person.email, week)) person.copy(sentFingerprint = now) else person
        }
        if (updated != people) save(context, updated)
    }

    fun remove(context: Context, email: String) {
        save(context, people(context).filterNot { it.email == email })
    }

    suspend fun myTerm(context: Context): SharedSchedule? {
        val (token, session) = etsSession(context) ?: return null
        val api = SignetsApi(token)
        val key = sessionActivitiesKey(session)
        val occurrences = runCatching {
            api.getActivitiesBetween(session.shortName, session.startDate.take(10), session.endDate.take(10))
                .also { if (it.isNotEmpty()) app.etsprivate.cache.DiskCache.write(context, key, JSONArray(it.map { o -> o.toJson() }).toString()) }
        }.getOrNull()
            ?: app.etsprivate.cache.DiskCache.read(context, key)
                ?.let { json -> runCatching { JSONArray(json).let { a -> (0 until a.length()).map { activityFromJson(a.getJSONObject(it)) } } }.getOrNull() }
            ?: return null
        val classes = sharedClassesOf(occurrences)
        if (classes.isEmpty()) return null
        val profile = runCatching { api.getProfile() }.getOrNull()

        val email = TokenStore.load(context, MAIL_ACCOUNT)?.email?.lowercase().orEmpty()
        profile?.let { app.etsprivate.settings.StudentName.rememberReal(context, it.firstName, it.lastName) }

        val name = app.etsprivate.settings.StudentName.outgoing(context)
            ?: email.substringBefore('@').ifBlank { "?" }
        android.util.Log.i(
            "ScheduleShare",
            "my term ${session.shortName}: ${classes.size} classes, ${classes.first().date} to ${classes.last().date}, " +
                "a week: " + typicalWeek(classes).values.flatten().joinToString { "${it.weekday} ${it.start}-${it.end} ${it.acronym}-${it.group} ${it.kind}" },
        )
        return SharedSchedule(name, email, session.name, System.currentTimeMillis(), classes, datedEvaluations(context, session.shortName))
    }

    private fun fingerprint(week: SharedSchedule) =
        (week.session + week.name + week.classes.joinToString() + week.exams.joinToString()).hashCode().toString()

    private suspend fun send(context: Context, to: String, term: SharedSchedule): Boolean {
        val from = TokenStore.load(context, MAIL_ACCOUNT)?.email?.lowercase() ?: return false
        val week = term.copy(email = from)

        val french = inLanguage(context, java.util.Locale.CANADA_FRENCH)
        val english = inLanguage(context, java.util.Locale.CANADA)
        val subject = french.getString(R.string.compare_mail_subject, week.name, week.session) +
            " / " + english.getString(R.string.compare_mail_subject, week.name, week.session)
        val body = listOf(french, english).joinToString("\n\n— — —\n\n") { language ->
            buildString {
                append(language.getString(R.string.compare_mail_intro, week.name, week.session))
                append("\n\n")
                val locale = language.resources.configuration.locales[0]

                typicalWeek(week.classes).forEach { (day, classes) ->
                    append(dayName(day, locale)).append('\n')
                    classes.forEach { c ->
                        append("  ${c.start}–${c.end}  ${c.acronym}-${c.group}  ${c.kind}\n")
                    }
                }
                append('\n').append(language.getString(R.string.compare_mail_outro))
            }
        }
        val sent = withMail(context) { email, token ->
            OutlookSmtp.send(
                fromEmail = email,
                fromName = app.etsprivate.settings.StudentName.outgoing(context),
                accessToken = token,
                to = listOf(to),
                cc = emptyList(),
                bcc = emptyList(),
                subject = subject,
                body = body,
                attachments = listOf(OutgoingAttachment(SHARED_SCHEDULE_FILE, "application/json", week.toJson().toByteArray())),
            )
            true
        }
        android.util.Log.i("ScheduleShare", "sent to $to: ${sent == true}")
        return sent == true
    }

    private suspend fun <T> withMail(context: Context, block: (String, String) -> T): T? {
        val tokens = TokenStore.load(context, MAIL_ACCOUNT) ?: return null
        val email = tokens.email ?: return null
        return try {
            block(email, tokens.accessToken)
        } catch (e: Exception) {
            val refresh = tokens.refreshToken ?: return null
            val renewed = runCatching { MicrosoftOAuth.refresh(MicrosoftOAuth.MAIL, refresh) }.getOrNull() ?: run {
                android.util.Log.i("ScheduleShare", "mail failed and could not be renewed: $e")
                return null
            }
            TokenStore.save(context, MAIL_ACCOUNT, renewed.copy(email = renewed.email ?: email))
            runCatching { block(email, renewed.accessToken) }
                .onFailure { android.util.Log.i("ScheduleShare", "mail failed after renewing: $it") }
                .getOrNull()
        }
    }

    private fun inLanguage(context: Context, locale: java.util.Locale): Context =
        context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply { setLocale(locale) })

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

internal fun dayName(day: Int, locale: java.util.Locale): String =
    runCatching { DayOfWeek.of(day).getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) } }
        .getOrDefault(day.toString())
