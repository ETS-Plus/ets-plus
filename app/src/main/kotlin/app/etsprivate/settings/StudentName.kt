package app.etsprivate.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object StudentName {
    private const val PREFS = "student_name"
    private const val KEY_CHOSEN = "chosen"
    private const val KEY_OUTSIDE = "outside"
    private const val KEY_REAL = "real"

    var revision by mutableIntStateOf(0)
        private set

    fun real(context: Context): String? = prefs(context).getString(KEY_REAL, null)

    fun rememberReal(context: Context, firstName: String, lastName: String) {
        val name = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")
        if (name.isBlank() || name == real(context)) return
        prefs(context).edit().putString(KEY_REAL, name).apply()
        revision++
    }

    fun chosen(context: Context): String? = prefs(context).getString(KEY_CHOSEN, null)?.takeIf { it.isNotBlank() }

    fun setChosen(context: Context, name: String?) {
        val clean = name?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() && it != real(context) }
        prefs(context).edit().putString(KEY_CHOSEN, clean).apply()
        revision++
    }

    fun usedOutside(context: Context): Boolean = prefs(context).getBoolean(KEY_OUTSIDE, false)

    fun setUsedOutside(context: Context, used: Boolean) {
        prefs(context).edit().putBoolean(KEY_OUTSIDE, used).apply()
        revision++
    }

    fun shown(context: Context, real: String? = real(context)): String? = chosen(context) ?: real

    fun outgoing(context: Context, real: String? = real(context)): String? =
        if (usedOutside(context)) chosen(context) ?: real else real

    fun initials(name: String): String {
        val words = name.split(' ').filter { it.isNotBlank() }
        return listOfNotNull(words.firstOrNull()?.first(), words.drop(1).lastOrNull()?.first())
            .joinToString("").uppercase()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
