package app.etsprivate.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

@Composable
internal fun WidgetHeader(text: String, end: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Medium, fontSize = 15.sp),
        maxLines = 1,
        modifier = GlanceModifier.padding(start = 4.dp, end = end, bottom = 8.dp),
    )
}

internal fun textWidth(context: android.content.Context, text: String, sp: Float, medium: Boolean = false): Float {
    val metrics = context.resources.displayMetrics
    val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, sp, metrics)
        typeface = android.graphics.Typeface.create(if (medium) "sans-serif-medium" else "sans-serif", android.graphics.Typeface.NORMAL)
    }
    return paint.measureText(text) / metrics.density
}

internal fun courseWash(color: Color): ColorProvider =
    androidx.glance.color.ColorProvider(day = color.copy(alpha = 0.18f), night = color.copy(alpha = 0.32f))

internal fun solid(color: Color): ColorProvider = androidx.glance.color.ColorProvider(day = color, night = color)

@Composable
internal fun WidgetGroup(rows: List<@Composable () -> Unit>) {
    Column(modifier = GlanceModifier.fillMaxWidth().cornerRadius(20.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Spacer(GlanceModifier.height(2.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.surface)) { row() }
        }
    }
}

@Composable
internal fun CourseIcon(vector: ImageVector, color: Color, size: Int = 24) {
    Image(
        provider = ImageProvider(vectorBitmap(vector)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(solid(color)),
        modifier = GlanceModifier.size(size.dp),
    )
}

@Composable
internal fun CourseDisc(vector: ImageVector, color: Color) {
    Box(
        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).background(courseWash(color)),
        contentAlignment = Alignment.Center,
    ) {
        CourseIcon(vector, color, size = 20)
    }
}

private val bitmaps = mutableMapOf<ImageVector, Bitmap>()

internal fun vectorBitmap(vector: ImageVector, px: Int = 72): Bitmap = synchronized(bitmaps) {
    bitmaps.getOrPut(vector) {
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.concat(Matrix().apply { setScale(px / vector.viewportWidth, px / vector.viewportHeight) })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.White.toArgb() }
        fun draw(node: VectorNode) {
            when (node) {
                is VectorGroup -> node.forEach { draw(it) }
                is VectorPath -> {
                    val path = node.pathData.toPath()
                    path.fillType = node.pathFillType
                    paint.alpha = (node.fillAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawPath(path.asAndroidPath(), paint)
                }
            }
        }
        draw(vector.root)
        bitmap
    }
}
