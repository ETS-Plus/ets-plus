package app.etsprivate.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.etsprivate.R

val GroupGap: Dp = 2.dp

private val OuterCorner = 24.dp
private val InnerCorner = 4.dp

val GroupMargin: Dp = 16.dp

fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) OuterCorner else InnerCorner
    val bottom = if (index == count - 1) OuterCorner else InnerCorner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

private val isDark: Boolean @Composable get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

val pageColor: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val groupColor: Color
    @Composable get() = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

@Composable
fun ExpressivePage(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    scroll: androidx.compose.foundation.ScrollState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    val section = true

    val titleHeight = with(density) { SectionTitleHeight.roundToPx() }
    val state = scroll ?: rememberScrollState(initial = if (section) titleHeight else 0)

    fun progress() = (state.value.toFloat() / titleHeight).coerceIn(0f, 1f)

    if (section) {
        androidx.compose.runtime.LaunchedEffect(state) {
            androidx.compose.runtime.snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
                val at = state.value
                if (!scrolling && at in 1 until titleHeight) {
                    state.animateScrollTo(if (at < titleHeight / 2) 0 else titleHeight)
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(pageColor)) {
        if (section) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) RoundBackButton(onBack)
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,

                    modifier = Modifier.weight(1f).padding(start = if (onBack != null) 8.dp else 12.dp).graphicsLayer {
                        val shown = ((progress() - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        alpha = shown
                        translationY = (1f - shown) * 12.dp.toPx()
                    },
                )
                actions()
            }
        }
        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val least = if (section) maxHeight + with(density) { titleHeight.toDp() } else 0.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state)
                    .heightIn(min = least)
                    .padding(bottom = 24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (section) Modifier.height(SectionTitleHeight) else Modifier)
                        .padding(start = GroupMargin + 12.dp, end = GroupMargin, top = if (section) 8.dp else 48.dp, bottom = if (section) 0.dp else 28.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        title,
                        style = if (section) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,

                        modifier = Modifier.graphicsLayer {
                            val p = progress()
                            val scale = 1f - 0.3f * p
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            alpha = (1f - p / 0.7f).coerceIn(0f, 1f)
                        },
                    )
                }
                content()
            }
        }
    }
}

val SectionTitleHeight: Dp = 96.dp

@Composable
fun rememberSectionScroll(): androidx.compose.foundation.ScrollState {
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { SectionTitleHeight.roundToPx() }
    return rememberScrollState(initial = px)
}

class GroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable (Shape) -> Unit>()

    fun row(content: @Composable (Shape) -> Unit) {
        rows += content
    }
}

@Composable
fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    build: GroupScope.() -> Unit,
) {
    val scope = GroupScope().apply(build)
    if (header != null) GroupHeader(header)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GroupMargin),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
    ) {
        scope.rows.forEachIndexed { index, row -> row(groupShape(index, scope.rows.size)) }
    }
    if (footer != null) GroupFooter(footer)
    Box(modifier = Modifier.padding(bottom = if (footer == null) 20.dp else 12.dp))
}

@Composable
fun RoundedGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GroupMargin)
            .clip(RoundedCornerShape(OuterCorner)),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
        content = content,
    )
}

@Composable
fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = GroupMargin + 16.dp, end = GroupMargin + 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun GroupFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = GroupMargin + 16.dp, end = GroupMargin + 16.dp, top = 8.dp),
    )
}

sealed interface Trailing {
    data object Chevron : Trailing
    data class Toggle(val checked: Boolean, val onChange: (Boolean) -> Unit) : Trailing
    data class Radio(val selected: Boolean) : Trailing
    data class Custom(val content: @Composable () -> Unit) : Trailing
    data object None : Trailing
}

@Composable
fun SettingsItem(
    shape: Shape,
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,

    iconColor: Color? = null,
    trailing: Trailing = Trailing.None,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    badge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tap: (() -> Unit)? = when {
        !enabled -> null
        trailing is Trailing.Toggle -> ({
            haptic.performHapticFeedback(
                if (!trailing.checked) androidx.compose.ui.hapticfeedback.HapticFeedbackType.ToggleOn
                else androidx.compose.ui.hapticfeedback.HapticFeedbackType.ToggleOff,
            )
            trailing.onChange(!trailing.checked)
        })
        else -> onClick
    }
    val content = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(groupColor)
            .pointedAt(title)
            .then(if (tap != null) Modifier.clickable(onClick = tap) else Modifier)
            .heightIn(min = if (summary != null) 72.dp else 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val glyph: @Composable () -> Unit = {
                if (iconColor != null) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                } else {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            if (!badge) {
                glyph()
            } else if (iconColor != null) {
                Box {
                    glyph()
                    NoticeDot(
                        Modifier
                            .offset(x = 28.dp, y = 0.dp)
                            .border(2.dp, groupColor, CircleShape)
                            .padding(2.dp),
                    )
                }
            } else {
                WithNoticeDot(true) { glyph() }
            }
            Box(modifier = Modifier.size(16.dp))
        }
        if (trailing is Trailing.Radio) {
            RadioButton(selected = trailing.selected, onClick = tap, enabled = enabled)
            Box(modifier = Modifier.size(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = content)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (trailing) {
            Trailing.Chevron -> Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is Trailing.Toggle -> {
                Box(modifier = Modifier.size(12.dp))
                ExpressiveSwitch(trailing.checked, trailing.onChange, enabled)
            }
            is Trailing.Custom -> trailing.content()
            else -> Unit
        }
    }
}

@Composable
fun ExpressiveSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = { on ->
            haptic.performHapticFeedback(
                if (on) androidx.compose.ui.hapticfeedback.HapticFeedbackType.ToggleOn
                else androidx.compose.ui.hapticfeedback.HapticFeedbackType.ToggleOff,
            )
            onChange(on)
        },
        enabled = enabled,
        thumbContent = {
            Icon(
                if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

val IconColors = listOf(
    Color(0xFF4F7BD9), Color(0xFF2E9C6A), Color(0xFFD9822B), Color(0xFF8E5BD0),
    Color(0xFFD14B6E), Color(0xFF2B95B8), Color(0xFF7A8B2E), Color(0xFFB0654A),
)

object PointedSetting {
    var title by androidx.compose.runtime.mutableStateOf<String?>(null)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.pointedAt(title: String): Modifier {
    val pointed = PointedSetting.title == title
    val requester = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var lit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pointed) {
        if (!pointed) return@LaunchedEffect

        kotlinx.coroutines.delay(250)
        requester.bringIntoView()
        lit = true
        kotlinx.coroutines.delay(1_600)
        lit = false
    }
    val wash by androidx.compose.animation.animateColorAsState(
        if (lit) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "pointed-setting",
    )
    return this
        .bringIntoViewRequester(requester)
        .drawBehind { drawRect(wash) }
}

object Immersive {
    var hideBottomBar by androidx.compose.runtime.mutableStateOf(false)

    var lightStatusBar by androidx.compose.runtime.mutableStateOf<Boolean?>(null)
        private set

    private var statusBarOwner: Any? = null

    fun claimStatusBar(owner: Any, light: Boolean) {
        statusBarOwner = owner
        lightStatusBar = light
    }

    fun releaseStatusBar(owner: Any) {
        if (statusBarOwner !== owner) return
        statusBarOwner = null
        lightStatusBar = null
    }

    var snackbarLift by androidx.compose.runtime.mutableStateOf(0.dp)
}

@Composable
fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val light = !isDark
        androidx.compose.runtime.SideEffect {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
        RespectHaptics { content() }

        SnackbarsOver(above = false)
    }
}

@Composable
fun NoticeDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
}

@Composable
fun WithNoticeDot(shown: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
        if (shown) NoticeDot(Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = 2.dp))
    }
}

object Haptics {
    var enabled by androidx.compose.runtime.mutableStateOf(true)

    internal val silent = object : androidx.compose.ui.hapticfeedback.HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) = Unit
    }
}

@Composable
fun RespectHaptics(content: @Composable () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    val enabled = Haptics.enabled
    androidx.compose.runtime.SideEffect { view.rootView.isHapticFeedbackEnabled = enabled }
    val own = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalHapticFeedback provides if (enabled) own else Haptics.silent,
        content = content,
    )
}
