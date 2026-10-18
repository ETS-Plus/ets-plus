package app.etsprivate.mail

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore

object AttachmentSaver {
    fun saveToDownloads(context: Context, filename: String, mimeType: String, bytes: ByteArray): Boolean =
        save(context, filename, mimeType, bytes) != null

    fun save(context: Context, filename: String, mimeType: String, bytes: ByteArray): android.net.Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrNull()
    }

    fun open(context: Context, uri: android.net.Uri, mimeType: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType.ifBlank { "*/*" })
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(android.content.Intent.createChooser(intent, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
