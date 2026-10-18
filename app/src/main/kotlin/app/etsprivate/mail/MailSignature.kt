package app.etsprivate.mail

import android.content.Context
import app.etsprivate.R
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.StudentName

enum class SignatureMode { DEFAULT, CUSTOM, NONE }

object MailSignature {
    const val APP_LINK = "https://github.com/ETS-Plus/ets-plus/releases/latest"

    private const val PREFS = "mail_signature"
    private const val KEY_MODE = "mode"
    private const val KEY_CUSTOM = "custom"

    fun mode(context: Context): SignatureMode =
        runCatching { SignatureMode.valueOf(prefs(context).getString(KEY_MODE, null) ?: "") }.getOrDefault(SignatureMode.DEFAULT)

    fun setMode(context: Context, mode: SignatureMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun custom(context: Context): String = prefs(context).getString(KEY_CUSTOM, "").orEmpty()

    fun setCustom(context: Context, text: String) {
        prefs(context).edit().putString(KEY_CUSTOM, text.trimEnd()).apply()
    }

    fun text(context: Context): String? = when (mode(context)) {
        SignatureMode.NONE -> null
        SignatureMode.CUSTOM -> custom(context).takeIf { it.isNotBlank() }
        SignatureMode.DEFAULT -> listOfNotNull(StudentName.outgoing(context), sentWith(context)).joinToString("\n")
    }

    private fun sentWith(context: Context): String = words(context).getString(R.string.mail_signature_sent_with, "ÉTS+")

    fun linked(html: String): String? {
        val lines = listOf("Sent with ÉTS+", "Envoyé avec ÉTS+")
        val line = lines.firstOrNull { html.contains(escapeHtml(it)) } ?: return null
        val escaped = escapeHtml(line)
        return html.replace(escaped, escaped.replace("ÉTS+", "<a href=\"$APP_LINK\">ÉTS+</a>"))
    }

    private fun words(context: Context): Context = LocaleHelper.wrap(context)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
