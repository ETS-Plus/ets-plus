package app.etsprivate.cache

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

object ImageCache {
    private const val DIRECTORY = "image-cache"

    suspend fun load(context: Context, url: String): ImageBitmap? {
        MemoryCache.get<ImageBitmap>(key(url))?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = file(context, url)
            val bytes = if (file.exists()) {
                runCatching { file.readBytes() }.getOrNull()
            } else {
                runCatching { URL(url).openStream().use { it.readBytes() } }
                    ?.getOrNull()
                    ?.also { downloaded ->

                        runCatching {
                            file.parentFile?.mkdirs()
                            file.writeBytes(downloaded)
                        }
                    }
            } ?: return@withContext null

            val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            if (bitmap == null) {
                runCatching { file.delete() }
            } else {
                MemoryCache.put(key(url), bitmap)
            }
            bitmap
        }
    }

    private fun key(url: String) = "image/$url"

    private fun file(context: Context, url: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, DIRECTORY), hash)
    }
}
