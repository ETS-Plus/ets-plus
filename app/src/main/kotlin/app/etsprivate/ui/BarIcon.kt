package app.etsprivate.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private val faces: Map<String, Pair<ImageVector, ImageVector>> by lazy {
    listOf(
        Icons.Outlined.School to Icons.Filled.School,
        Icons.Outlined.CalendarMonth to Icons.Filled.CalendarMonth,
        Icons.Outlined.Grade to Icons.Filled.Grade,
        Icons.Outlined.Person to Icons.Filled.Person,
        Icons.Outlined.OpenInBrowser to Icons.Filled.OpenInBrowser,
        Icons.Outlined.Mail to Icons.Filled.Mail,
        Icons.Outlined.Settings to Icons.Filled.Settings,
        Icons.Outlined.DirectionsBus to Icons.Filled.DirectionsBus,
        Icons.Outlined.CreditCard to Icons.Filled.CreditCard,
        Icons.Outlined.Map to Icons.Filled.Map,
        Icons.Outlined.Extension to Icons.Filled.Extension,
    ).associateBy { it.first.name.substringAfterLast('.') }
}

@Composable
fun BarIcon(icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier) {
    val (outlined, filled) = remember(icon) { faces[icon.name.substringAfterLast('.')] ?: (icon to icon) }
    Crossfade(targetState = selected, animationSpec = tween(180), label = "bar-icon") { on ->
        Icon(if (on) filled else outlined, contentDescription = null, modifier = modifier)
    }
}
