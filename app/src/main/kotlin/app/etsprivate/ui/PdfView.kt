package app.etsprivate.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.etsprivate.R
import app.etsprivate.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfView(bytes: ByteArray, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pages by remember(bytes) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var failed by remember(bytes) { mutableStateOf(false) }

    LaunchedEffect(bytes) {
        val rendered = withContext(Dispatchers.IO) {
            runCatching {
                val file = File.createTempFile("document", ".pdf", context.cacheDir)
                file.writeBytes(bytes)
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                val bitmaps = (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->

                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
                renderer.close()
                descriptor.close()
                file.delete()
                bitmaps
            }.getOrNull()
        }
        if (rendered == null) failed = true else pages = rendered
    }

    when {
        failed -> Text(
            stringResource(R.string.mail_attachment_save_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(Spacing.double),
        )
        pages.isEmpty() -> FullScreenLoading()
        else -> ZoomableBox(modifier = modifier.fillMaxSize().background(Color.Black)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                pages.forEach { page ->
                    Image(
                        page.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.half),
                        contentScale = ContentScale.FillWidth,
                    )
                }
                }
            }
        }
    }
}
