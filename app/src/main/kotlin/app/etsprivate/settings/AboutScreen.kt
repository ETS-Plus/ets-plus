package app.etsprivate.settings

import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import app.etsprivate.ui.Trailing
import app.etsprivate.ui.SettingsItem
import app.etsprivate.ui.SettingsGroup
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.etsprivate.R
import app.etsprivate.ets.SectionLabel
import app.etsprivate.plugin.CreditSection
import app.etsprivate.plugin.PluginCredit

private data class Credit(
    val name: String,
    val noteRes: Int,
    val licence: String,
    val url: String,
)

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    fun open(url: String) {
        if (url.isBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    val own = PRIOR_WORK + LIBRARIES + SERVICES
    fun alreadyCredited(name: String, url: String): Boolean = own.any { mine ->
        (url.isNotBlank() && url == mine.url) ||
            name.equals(mine.name, ignoreCase = true) ||
            (name.contains("ARTM", ignoreCase = true) && mine.name.contains("ARTM", ignoreCase = true))
    }

    StudioHeader()
    SettingsGroup(footer = stringResource(R.string.about_not_affiliated)) {
        row {
            SettingsItem(
                it,
                title = stringResource(R.string.about_website),
                summary = "synocti.ca",
                icon = Icons.Outlined.Language,
                trailing = Trailing.Custom {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { open(STUDIO_SITE) },
            )
        }
        row {
            SettingsItem(
                it,
                title = stringResource(R.string.about_releases),
                summary = "github.com/ETS-Plus/ets-plus",
                icon = Icons.Outlined.Code,
                trailing = Trailing.Custom {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { open(RELEASES) },
            )
        }
        row {
            SettingsItem(
                it,
                title = stringResource(R.string.about_write),
                summary = STUDIO_MAIL,
                icon = Icons.Outlined.MailOutline,
                onClick = { open("mailto:$STUDIO_MAIL") },
            )
        }
    }
    val added = app.etsprivate.plugin.Plugins.modules().flatMap {
        runCatching { it.credits() }.getOrDefault(emptyList())
    }.filterNot { alreadyCredited(it.name, it.url) }.distinctBy { it.name.lowercase() }
    listOf(
        Triple(R.string.about_prior_work, PRIOR_WORK, CreditSection.PRIOR_WORK),
        Triple(R.string.about_libraries, LIBRARIES, CreditSection.LIBRARIES),
        Triple(R.string.about_services, SERVICES, CreditSection.SERVICES),
    ).forEach { (header, own, section) ->
        val notes = own.map { stringResource(it.noteRes) }
        SettingsGroup(header = stringResource(header)) {
            own.forEachIndexed { index, credit ->
                row { CreditItem(it, credit.name, notes[index], credit.licence, credit.url, ::open) }
            }
            added.filter { it.section == section }.forEach { credit ->
                row { CreditItem(it, credit.name, credit.note, credit.licence, credit.url, ::open) }
            }
        }
    }
}

private const val STUDIO_SITE = "https://synocti.ca"
private const val STUDIO_MAIL = "ets-plus@synocti.ca"
private const val RELEASES = "https://github.com/ETS-Plus/ets-plus/releases"

@Composable
private fun StudioHeader() {
    val context = LocalContext.current
    val icon = androidx.compose.runtime.remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        icon?.let {
            androidx.compose.foundation.Image(
                it,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
            )
        }
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.about_by_studio),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            stringResource(R.string.about_version, app.etsprivate.BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CreditItem(shape: androidx.compose.ui.graphics.Shape, name: String, note: String, licence: String, url: String, onOpen: (String) -> Unit) {
    SettingsItem(
        shape,
        title = name,
        summary = licence.ifBlank { null },
        trailing = if (url.isBlank()) Trailing.None else Trailing.Custom {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = if (url.isBlank()) null else ({ onOpen(url) }),
    )
}

private val PRIOR_WORK = listOf(
    Credit(
        "ÉTSMobile / Notre-Dame (ApplETS)",
        R.string.about_credit_ets_mobile,
        "Apache-2.0",
        "https://github.com/ApplETS/Notre-Dame",
    ),
    Credit("Organic Maps", R.string.about_credit_organic_maps, "Apache-2.0", "https://github.com/organicmaps/organicmaps"),
    Credit("SubTrack (IUPUI)", R.string.about_credit_subtrack, "", "https://scholarworks.indianapolis.iu.edu/items/67f905ad-ded7-4e2c-8f9f-5b618ba82975"),
)

private val LIBRARIES = listOf(
    Credit("Kotlin, AndroidX, Jetpack Compose", R.string.about_credit_androidx, "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    Credit("OkHttp", R.string.about_credit_okhttp, "Apache-2.0", "https://square.github.io/okhttp/"),
    Credit("Jakarta Mail (android-mail)", R.string.about_credit_mail, "EPL-2.0 / GPL-2.0 with Classpath Exception", "https://eclipse-ee4j.github.io/mail/"),
    Credit("MapLibre Native", R.string.about_credit_maplibre, "BSD-2-Clause", "https://maplibre.org/"),
    Credit("Material Components for Android", R.string.about_credit_material, "Apache-2.0", "https://github.com/material-components/material-components-android"),
)

private val SERVICES = listOf(
    Credit("Signets (ÉTS)", R.string.about_service_signets, "", ""),
    Credit("etsmtl.ca", R.string.about_service_ets_site, "", "https://www.etsmtl.ca/etudier-a-lets/horaire-des-examens-finaux"),
    Credit("Microsoft 365 (IMAP, SMTP, OAuth)", R.string.about_service_microsoft, "", ""),
    Credit("ARTM (Chrono)", R.string.about_service_artm, "", "https://www.artm.quebec/chrono/"),
    Credit("OpenStreetMap", R.string.about_service_osm, "ODbL 1.0", "https://www.openstreetmap.org/copyright"),
    Credit("OpenFreeMap", R.string.about_service_openfreemap, "", "https://openfreemap.org/"),
)
