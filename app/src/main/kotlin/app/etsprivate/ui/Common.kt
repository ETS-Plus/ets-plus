package app.etsprivate.ui

import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.layout.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.etsprivate.ui.theme.Spacing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import app.etsprivate.ui.theme.AppShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import app.etsprivate.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignInPrompt(
    message: String,
    buttonLabel: String,
    onSignIn: () -> Unit,
    status: String? = null,
    loading: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val demo = if (!app.etsprivate.demo.Demo.available) Modifier else Modifier.pointerInput(Unit) {
        detectTapGestures(onLongPress = { app.etsprivate.demo.Demo.start(context) })
    }
    Column(
        modifier = Modifier.fillMaxSize().then(demo).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = onSignIn, enabled = !loading) { Text(buttonLabel) }
        if (loading) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        status?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
fun CenteredMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier, onLightBackground: Boolean = false) {
    val content = if (onLightBackground) Color(0xFF444444) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = content)
        Text(
            stringResource(R.string.loading_generic),
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun TopLoadingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(modifier = modifier.fillMaxWidth())
}

@Composable
fun rememberDelayedLoading(loading: Boolean, delayMillis: Long = 300): Boolean {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(delayMillis)
            show = true
        } else {
            show = false
        }
    }
    return show
}

object AppFeedback {
    val hostState = SnackbarHostState()
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var currentJob: Job? = null

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        hostState.currentSnackbarData?.dismiss()
    }

    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        currentJob?.cancel()
        currentJob = scope.launch {
            hostState.currentSnackbarData?.dismiss()

            val result = hostState.showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSnackbarHost(modifier: Modifier = Modifier) {
    SnackbarHost(hostState = AppFeedback.hostState, modifier = modifier) { data ->
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) data.dismiss()
                true
            },
        )
        SwipeToDismissBox(state = dismissState, backgroundContent = {}) {
            Snackbar(snackbarData = data)
        }
    }
}

object SnackbarLayer {
    internal val owners = androidx.compose.runtime.mutableStateListOf<Any>()

    val covered: Boolean get() = owners.isNotEmpty()
}

@Composable
fun SnackbarsOver(
    above: Boolean = true,

    reachAbove: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val token = remember { Any() }
    androidx.compose.runtime.DisposableEffect(token) {
        SnackbarLayer.owners.add(token)
        onDispose { SnackbarLayer.owners.remove(token) }
    }

    if (SnackbarLayer.owners.lastOrNull() !== token) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    val margin = with(density) { 8.dp.roundToPx() }
    val reach = with(density) { reachAbove.roundToPx() }
    val navigation = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(density)
    val position = remember(above, margin, navigation, reach) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize,
            ): androidx.compose.ui.unit.IntOffset {
                val x = (windowSize.width - popupContentSize.width) / 2
                val atFoot = windowSize.height - navigation - popupContentSize.height - margin
                val overTop = anchorBounds.top - reach - popupContentSize.height - margin
                val y = when {
                    !above -> atFoot

                    overTop >= windowSize.height / 5 -> overTop

                    else -> minOf(anchorBounds.bottom - popupContentSize.height - margin, atFoot)
                }
                return androidx.compose.ui.unit.IntOffset(x, y)
            }
        }
    }

    androidx.compose.runtime.key(AppFeedback.hostState.currentSnackbarData) {
        androidx.compose.ui.window.Popup(
            popupPositionProvider = position,
            properties = androidx.compose.ui.window.PopupProperties(focusable = false, clippingEnabled = false),
        ) {
            AppSnackbarHost(Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        }
    }
}

@Composable
fun ZoomableBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()

                        fun bounded(o: Offset, s: Float) = Offset(
                            o.x.coerceIn(-(s - 1) * size.width / 2, (s - 1) * size.width / 2),
                            o.y.coerceIn(-(s - 1) * size.height / 2, (s - 1) * size.height / 2),
                        )
                        if (event.changes.size >= 2) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)

                            val centre = Offset(size.width / 2f, size.height / 2f)
                            val fingers = event.calculateCentroid(useCurrent = true) - centre
                            val grown = fingers - (fingers - offset) * (newScale / scale) + event.calculatePan()
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else bounded(grown, newScale)
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            offset = bounded(offset + event.calculatePan(), scale)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        content = content,
    )
}

val AppTopBarHeight = 64.dp

@Composable
fun RoundBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
    }
}

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(app.etsprivate.ui.pageColor)

            .requiredHeight(AppTopBarHeight)

            .padding(start = 4.dp, end = Spacing.half),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            onBack != null -> RoundBackButton(onBack)

            leading != null -> Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
                content = { leading() },
            )

            else -> Spacer(Modifier.width(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = Spacing.half),
        )
        actions?.invoke(this)
    }
}

@Composable
fun PreparingOverlay(text: String) {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Surface(shape = AppShapes.large, tonalElevation = 6.dp) {
            Row(
                modifier = Modifier.padding(Spacing.double),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = Spacing.default))
            }
        }
    }
}

@Composable
fun CopyableValue(
    value: String,
    modifier: Modifier = Modifier,
    label: String = value,
    style: androidx.compose.ui.text.TextStyle? = null,
    color: Color = Color.Unspecified,
) {
    val context = LocalContext.current
    Text(
        value,
        style = style ?: MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier
            .clip(AppShapes.small)
            .clickable {
                context.getSystemService(android.content.ClipboardManager::class.java)
                    ?.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
            }
            .padding(horizontal = Spacing.half, vertical = Spacing.quarter),
    )
}

@Deprecated("Kept for modules compiled before followsKeyboard", level = DeprecationLevel.HIDDEN)
@Composable
fun AppBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    dismissible: Boolean = true,
    titleAction: (@Composable () -> Unit)? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit,
) = AppBottomSheet(
    title = title,
    onDismissRequest = onDismissRequest,
    confirmLabel = confirmLabel,
    onConfirm = onConfirm,
    dismissLabel = dismissLabel,
    dismissible = dismissible,
    titleAction = titleAction,
    confirmEnabled = confirmEnabled,
    followsKeyboard = true,
    content = content,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,

    dismissible: Boolean = true,

    titleAction: (@Composable () -> Unit)? = null,

    confirmEnabled: Boolean = true,

    followsKeyboard: Boolean = true,

    content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit,
) {
    val keyboard = androidx.compose.runtime.rememberUpdatedState(androidx.compose.ui.platform.LocalSoftwareKeyboardController.current)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            val allowed = dismissible || target != SheetValue.Hidden
            if (allowed && target == SheetValue.Hidden) keyboard.value?.hide()
            allowed
        },
    )
    val scope = rememberCoroutineScope()

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) then()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = {
            if (followsKeyboard) androidx.compose.material3.BottomSheetDefaults.windowInsets
            else androidx.compose.foundation.layout.WindowInsets.systemBars
                .only(androidx.compose.foundation.layout.WindowInsetsSides.Vertical)
        },
    ) {
        RespectHaptics {
        val sheetLayout = androidx.compose.ui.platform.LocalView.current.parent as? android.view.View
        val shield = remember(sheetLayout) { sheetLayout?.let { KeyboardShield(it) } }
        androidx.compose.runtime.SideEffect { shield?.shielding = !followsKeyboard }
        Column(
            modifier = Modifier
                .fillMaxWidth()

                .padding(start = Spacing.double, end = Spacing.double, bottom = Spacing.double),
        ) {
            if (title.isNotBlank() || titleAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.default),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    titleAction?.invoke()
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            ) {
                content(::close)
            }

            val confirms = onConfirm != null && confirmLabel != null
            if (confirms) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.double),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.default, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissLabel != null) {
                        TextButton(
                            onClick = { close(onDismissRequest) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(dismissLabel, style = MaterialTheme.typography.titleSmall) }
                    }
                    if (confirms) {
                        Button(
                            onClick = { close(onConfirm!!) },
                            enabled = confirmEnabled,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(confirmLabel!!, style = MaterialTheme.typography.titleSmall) }
                    }
                }
            }

            SnackbarsOver(above = true, reachAbove = 48.dp)
        }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                RespectHaptics { content() }
                SnackbarsOver(above = false)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = dismissLabel?.let { { TextButton(onClick = onDismissRequest) { Text(it) } } },
    )
}

@Composable
fun ImageSlot(
    bitmap: ImageBitmap?,
    contentDescription: String?,
    height: Dp,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(height).background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

fun Modifier.bleed(horizontal: androidx.compose.ui.unit.Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val extra = horizontal.roundToPx()
        val wide = constraints.copy(
            minWidth = constraints.minWidth + extra * 2,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth,
        )
        val placeable = measurable.measure(wide)
        layout(placeable.width - extra * 2, placeable.height) { placeable.place(-extra, 0) }
    },
)

private class KeyboardShield(private val layout: android.view.View) {
    private var installed = false

    var shielding: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value && !installed) install()
            androidx.core.view.ViewCompat.requestApplyInsets(layout)
        }

    private fun install() {
        installed = true
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(layout) { _, insets ->
            val keyboardUp = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            if (!shielding && !keyboardUp) {
                uninstall()
                insets
            } else {
                androidx.core.view.WindowInsetsCompat.Builder(insets)
                    .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                    .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), false)
                    .build()
            }
        }
        androidx.core.view.ViewCompat.setWindowInsetsAnimationCallback(
            layout,
            object : androidx.core.view.WindowInsetsAnimationCompat.Callback(
                androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
            ) {
                override fun onProgress(
                    insets: androidx.core.view.WindowInsetsCompat,
                    running: MutableList<androidx.core.view.WindowInsetsAnimationCompat>,
                ) = insets
            },
        )
    }

    private fun uninstall() {
        installed = false
        layout.post {
            if (installed) return@post
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(layout, null)
            androidx.core.view.ViewCompat.setWindowInsetsAnimationCallback(layout, null)
            androidx.core.view.ViewCompat.requestApplyInsets(layout)
        }
    }
}
