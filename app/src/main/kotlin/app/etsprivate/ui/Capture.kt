package app.etsprivate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CaptureOffscreen(content: @Composable () -> Unit, onCaptured: suspend (android.graphics.Bitmap?) -> Unit) {
    class Shot { var bitmap: android.graphics.Bitmap? = null; var frames = 0 }
    val shot = remember { Shot() }
    val done = rememberUpdatedState(onCaptured)
    val width = LocalConfiguration.current.screenWidthDp.dp - GroupMargin * 2

    Box(
        modifier = Modifier.layout { measurable, _ ->
            val placeable = measurable.measure(androidx.compose.ui.unit.Constraints())
            layout(1, 1) { placeable.place(0, 0) }
        },
    ) {
        Box(
            modifier = Modifier
                .requiredWidth(width)
                .drawWithContent {
                    val w = size.width.toInt()
                    val h = size.height.toInt()
                    if (w <= 0 || h <= 0) return@drawWithContent

                    val image = ImageBitmap(w, h)
                    draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(image), size) {
                        this@drawWithContent.drawContent()
                    }
                    shot.bitmap = image.asAndroidBitmap()
                    shot.frames++
                },
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        }
    }
    LaunchedEffect(shot) {
        var waited = 0
        while (shot.frames < 4 && waited < 60) {
            delay(50)
            waited++
        }
        android.util.Log.i("Capture", "frames=${shot.frames} bitmap=${shot.bitmap?.width}x${shot.bitmap?.height}")
        done.value(shot.bitmap)
    }
}

@Composable
fun AppMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(groupColor)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.res.colorResource(app.etsprivate.R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(app.etsprivate.R.drawable.ic_launcher_foreground),
                contentDescription = null,

                modifier = Modifier.requiredSize(36.dp),
            )
        }
        androidx.compose.material3.Text(
            androidx.compose.ui.res.stringResource(app.etsprivate.R.string.shared_from_app),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
