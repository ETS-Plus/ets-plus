package app.etsprivate.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

object ShareAsImage {
    private const val MAX_PIXELS = 26_000_000

    private const val GUTTER_X = 4
    private const val GUTTER_Y = 8

    private const val FIT_CONTENT =
        "<style>img,video,canvas,svg{max-width:100%!important;height:auto!important}" +
            "table,pre{max-width:100%!important}</style>"

    suspend fun render(
        context: Context,
        html: String,
        name: String,

        images: Map<String, Pair<String, String>> = emptyMap(),
    ): File? = withContext(Dispatchers.Main) {
        val view = object : WebView(context) {
            fun contentScrollRange(): Int = computeVerticalScrollRange()
        }.apply {
            settings.javaScriptEnabled = false

            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.blockNetworkImage = false
            settings.loadsImagesAutomatically = true
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val loaded = suspendCancellableCoroutine { continuation ->
            view.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    v: WebView?,
                    request: android.webkit.WebResourceRequest?,
                ): android.webkit.WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    if (!url.startsWith("cid:")) return null
                    val image = images[url.removePrefix("cid:")] ?: return null
                    val bytes = android.util.Base64.decode(image.second, android.util.Base64.NO_WRAP)
                    return android.webkit.WebResourceResponse(image.first, "utf-8", java.io.ByteArrayInputStream(bytes))
                }

                override fun onPageFinished(v: WebView?, url: String?) {
                    if (continuation.isActive) continuation.resume(true)
                }
            }
            view.loadDataWithBaseURL(null, html + FIT_CONTENT, "text/html", "UTF-8", null)
        }
        if (!loaded) return@withContext null

        val density = context.resources.displayMetrics.density
        val gutterX = (GUTTER_X * density).toInt()
        val gutterY = (GUTTER_Y * density).toInt()
        val width = (context.resources.displayMetrics.widthPixels - 2 * gutterX).coerceAtLeast(320)

        var height = 0
        var stable = 0
        repeat(20) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            view.layout(0, 0, width, view.measuredHeight.coerceAtLeast(1))
            delay(200)

            val measured = maxOf(view.measuredHeight, view.contentScrollRange())
            if (measured > 0 && measured == height) {
                stable++

                if (stable >= 2) return@repeat
            } else {
                stable = 0
            }
            height = maxOf(height, measured)
        }
        height = height.coerceAtLeast(200)
        view.layout(0, 0, width, height)

        var scale = 1f
        val pixels = width.toFloat() * height
        if (pixels > MAX_PIXELS) scale = kotlin.math.sqrt(MAX_PIXELS / pixels)
        val drawnWidth = (width * scale).toInt().coerceAtLeast(1)
        val drawnHeight = (height * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(drawnWidth + 2 * gutterX, drawnHeight + 2 * gutterY, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        view.draw(
            Canvas(bitmap).apply {
                translate(gutterX.toFloat(), gutterY.toFloat())
                scale(scale, scale)
            },
        )
        view.destroy()

        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = trimBlankEdges(bitmap)
                val directory = File(context.cacheDir, "shared").apply { mkdirs() }
                val file = File(directory, "$name.png")
                file.outputStream().use { trimmed.compress(Bitmap.CompressFormat.PNG, 100, it) }
                file
            }.getOrNull()
        }
    }

    private fun trimBlankEdges(bitmap: Bitmap): Bitmap {
        val row = IntArray(bitmap.width)
        var lastUsed = bitmap.height - 1
        while (lastUsed > 0) {
            bitmap.getPixels(row, 0, bitmap.width, 0, lastUsed, bitmap.width, 1)
            if (row.any { it != android.graphics.Color.WHITE && it != 0 }) break
            lastUsed--
        }
        val height = (lastUsed + 24).coerceAtMost(bitmap.height)
        return if (height >= bitmap.height) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, height)
    }

    suspend fun savePng(context: Context, bitmap: Bitmap, name: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val plain = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
            val directory = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(directory, "$name.png")
            file.outputStream().use { plain.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file
        }.getOrNull()
    }

    fun sharePng(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    suspend fun renderOrNull(
        context: Context,
        html: String,
        name: String,
        images: Map<String, Pair<String, String>> = emptyMap(),
    ): File? = withTimeoutOrNull(20_000) { render(context, html, name, images) }
}
