package app.etsprivate.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    fun languageOverride(context: Context): Configuration? {
        val tag = Settings.getLanguage(context).tag ?: return null
        return Configuration().apply { setLocale(Locale(tag)) }
    }

    fun wrap(context: Context): Context {
        val tag = Settings.getLanguage(context).tag ?: return context
        val locale = Locale(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
