package app.etsprivate.mail

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MailContact(
    val name: String,
    val address: String,
    val lastUsed: Long,
    val times: Int,
    val from: String? = null,
) {
    val display: String get() = if (name.isBlank()) address else "$name <$address>"
}

object MailContacts {
    private const val PREFS = "mail_contacts"
    private const val KEY = "contacts"

    private const val LIMIT = 300

    fun all(context: Context): List<MailContact> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            val address = o.optString("address").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MailContact(o.optString("name"), address, o.optLong("lastUsed"), o.optInt("times", 1))
        }
    }

    fun remember(context: Context, addresses: List<String>) {
        if (addresses.isEmpty()) return
        val now = System.currentTimeMillis()
        val known = all(context).associateBy { it.address.lowercase() }.toMutableMap()
        addresses.forEach { raw ->
            val (name, address) = split(raw)
            if (address.isBlank()) return@forEach
            val existing = known[address.lowercase()]
            known[address.lowercase()] = MailContact(
                name = name.ifBlank { existing?.name.orEmpty() },
                address = address,
                lastUsed = now,
                times = (existing?.times ?: 0) + 1,
            )
        }
        val kept = known.values.sortedWith(compareByDescending<MailContact> { it.lastUsed }.thenByDescending { it.times })
            .take(LIMIT)
        val array = JSONArray()
        kept.forEach {
            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("address", it.address)
                    .put("lastUsed", it.lastUsed)
                    .put("times", it.times),
            )
        }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    fun matching(context: Context, typed: String, limit: Int = 4): List<MailContact> {
        val needle = typed.trim().lowercase()
        if (needle.length < 2) return emptyList()
        fun matches(contact: MailContact) =
            contact.address.lowercase().contains(needle) || contact.name.lowercase().contains(needle)

        val everyCourse = app.etsprivate.settings.Settings.getTeachersFromEveryCourse(context)
        val thisTerm = app.etsprivate.ets.currentCourseAcronyms(context)

        val taught = ClassContacts.all(context).filter { teacher ->
            everyCourse || thisTerm.isEmpty() || teacher.from.orEmpty().uppercase() in thisTerm
        }
        val byAddress = taught.associateBy { it.address.lowercase() }
        val written = all(context).filter(::matches).map { contact ->
            val teacher = byAddress[contact.address.lowercase()] ?: return@map contact
            contact.copy(name = contact.name.ifBlank { teacher.name }, from = teacher.from)
        }

        val remaining = taught
            .filter(::matches)
            .filterNot { teacher -> written.any { it.address.equals(teacher.address, ignoreCase = true) } }
        return (
            written.sortedWith(compareByDescending<MailContact> { it.times }.thenByDescending { it.lastUsed }) +
                remaining.sortedBy { it.name }
            ).take(limit)
    }

    private fun split(raw: String): Pair<String, String> {
        val trimmed = raw.trim()
        val open = trimmed.lastIndexOf('<')
        val close = trimmed.lastIndexOf('>')
        return if (open in 0 until close) {
            trimmed.substring(0, open).trim().trim('"') to trimmed.substring(open + 1, close).trim()
        } else {
            "" to trimmed
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

object ClassContacts {
    private const val PREFS = "class_contacts"
    private const val KEY = "teachers"

    fun all(context: Context): List<MailContact> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            val address = o.optString("address").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MailContact(
                name = o.optString("name"),
                address = address,
                lastUsed = 0,
                times = 0,
                from = o.optString("from").takeIf { it.isNotBlank() },
            )
        }
    }

    fun remember(context: Context, teachers: List<Triple<String, String, String>>) {
        if (teachers.isEmpty()) return
        val known = all(context).associateBy { it.address.lowercase() }.toMutableMap()
        teachers.forEach { (name, address, course) ->
            if (address.isBlank()) return@forEach
            known[address.lowercase()] = MailContact(name = name, address = address, lastUsed = 0, times = 0, from = course)
        }
        val array = JSONArray()
        known.values.forEach {
            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("address", it.address)
                    .put("from", it.from.orEmpty()),
            )
        }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
