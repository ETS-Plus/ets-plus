package app.etsprivate.settings

import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class SettingsEntry(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,

    val badge: Boolean = false,
)

@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> String,
    rowHeight: Dp,

    onOrder: (List<T>) -> Unit,

    scroll: ScrollState? = null,

    grip: Boolean = false,
    row: @Composable (T) -> Unit,
) {
    val height = with(LocalDensity.current) { (rowHeight + app.etsprivate.ui.GroupGap).toPx() }
    val edge = with(LocalDensity.current) { 96.dp.toPx() }
    var held by remember { mutableStateOf<String?>(null) }
    var travelled by remember { mutableFloatStateOf(0f) }

    var finger by remember { mutableFloatStateOf(0f) }
    var listCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val rowCoords = remember { mutableMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>() }

    fun drop(id: String) {
        val start = items.indexOfFirst { key(it) == id }
        val target = (start + (travelled / height).roundToInt()).coerceIn(0, items.lastIndex)
        held = null
        travelled = 0f
        if (start >= 0 && target != start) {
            onOrder(items.toMutableList().also { it.add(target, it.removeAt(start)) })
        }
    }

    val from = items.indexOfFirst { key(it) == held }
    val to = if (from < 0) -1 else (from + (travelled / height).roundToInt()).coerceIn(0, items.lastIndex)

    LaunchedEffect(held, scroll) {
        if (scroll == null) return@LaunchedEffect
        while (held != null) {
            val list = listCoords?.takeIf { it.isAttached } ?: break
            val seen = list.boundsInWindow()
            val top = list.positionInWindow().y
            val bottom = top + list.size.height
            val by = when {
                seen.top > top + 1 && finger < seen.top + edge && scroll.value > 0 -> -16f
                seen.bottom < bottom - 1 && finger > seen.bottom - edge && scroll.value < scroll.maxValue -> 16f
                else -> 0f
            }
            if (by != 0f) {
                travelled += scroll.scrollBy(by)
            }
            delay(16)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = app.etsprivate.ui.GroupMargin)
            .onGloballyPositioned { listCoords = it },
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(app.etsprivate.ui.GroupGap),
    ) {
        items.forEachIndexed { index, item ->
            val id = key(item)
            val isHeld = id == held
            val shift = when {
                from < 0 || isHeld -> 0f
                from < to && index in (from + 1)..to -> -height
                from > to && index in to until from -> height
                else -> 0f
            }
            val sliding by animateFloatAsState(shift, label = "reorder-shift")

            val slide = if (held == null) 0f else sliding
            Surface(
                color = app.etsprivate.ui.groupColor,
                shape = app.etsprivate.ui.groupShape(index, items.size),
                shadowElevation = if (isHeld) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isHeld) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isHeld) travelled else slide
                        scaleX = if (isHeld) 1.02f else 1f
                        scaleY = if (isHeld) 1.02f else 1f
                    }
                    .onGloballyPositioned { rowCoords[id] = it }

                    .pointerInput(id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { at ->
                                rowCoords[id]?.takeIf { it.isAttached }?.let { finger = it.localToWindow(at).y }
                                held = id
                                travelled = 0f
                            },
                            onDragEnd = { drop(id) },
                            onDragCancel = {
                                held = null
                                travelled = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                travelled += amount.y
                                rowCoords[id]?.takeIf { it.isAttached }?.let { finger = it.localToWindow(change.position).y }
                            },
                        )
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) { row(item) }

                    if (grip) Icon(
                        Icons.Outlined.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDragStart = {
                                        rowCoords[id]?.takeIf { c -> c.isAttached }?.let { c -> finger = c.positionInWindow().y + c.size.height / 2f }
                                        held = id
                                        travelled = 0f
                                    },
                                    onDragEnd = { drop(id) },
                                    onDragCancel = {
                                        held = null
                                        travelled = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        travelled += amount.y
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsList(
    entries: List<SettingsEntry>,

    scroll: ScrollState,
    row: @Composable (SettingsEntry) -> Unit,
) {
    val context = LocalContext.current

    var order by remember(entries.map { it.id }) {
        val saved = Settings.getSettingsOrder(context).orEmpty()
        val ids = saved.filter { id -> entries.any { it.id == id } } +
            entries.map { it.id }.filterNot { it in saved }
        mutableStateOf(ids)
    }
    val ordered = order.mapNotNull { id -> entries.firstOrNull { it.id == id } }

    ReorderableColumn(
        items = ordered,
        key = { it.id },
        rowHeight = SettingsRowHeight,
        scroll = scroll,

        grip = false,
        onOrder = { next ->
            order = next.map { it.id }
            Settings.setSettingsOrder(context, order)
        },
        row = row,
    )
}
