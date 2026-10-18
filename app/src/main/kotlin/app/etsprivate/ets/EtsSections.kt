package app.etsprivate.ets

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.etsprivate.R
import app.etsprivate.settings.Settings

enum class EtsSection(val id: String, val labelRes: Int) {
    PROFILE("profile", R.string.ets_profile),
    PROGRESS("progress", R.string.ets_section_progress),
    SCHEDULE("schedule", R.string.ets_schedule),
    NOTES("notes", R.string.ets_notes),
    EXTERNAL("external", R.string.ets_external),
}

object EtsSections {
    private var hidden by mutableStateOf<Set<String>?>(null)
    private var order by mutableStateOf<List<EtsSection>?>(null)

    fun shown(context: Context, section: EtsSection): Boolean = section.id !in current(context)

    fun setShown(context: Context, section: EtsSection, shown: Boolean) {
        val next = current(context).toMutableSet().apply { if (shown) remove(section.id) else add(section.id) }
        hidden = next
        Settings.setHiddenEtsSections(context, next)
    }

    fun ordered(context: Context): List<EtsSection> = order ?: read(context).also { order = it }

    fun reorder(context: Context, sections: List<EtsSection>) {
        order = sections
        Settings.setEtsSectionOrder(context, sections.map { it.id })
    }

    private fun read(context: Context): List<EtsSection> {
        val saved = Settings.etsSectionOrder(context).orEmpty().mapNotNull { id ->
            EtsSection.entries.firstOrNull { it.id == id }
        }
        return saved + EtsSection.entries.filterNot { it in saved }
    }

    private fun current(context: Context): Set<String> =
        hidden ?: Settings.hiddenEtsSections(context).also { hidden = it }
}
