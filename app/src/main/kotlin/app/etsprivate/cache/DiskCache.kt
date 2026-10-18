package app.etsprivate.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest

object DiskCache {
    private fun file(context: Context, key: String): File {
        val dir = File(context.cacheDir, "netcache").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    fun read(context: Context, key: String): String? =
        runCatching { file(context, key).takeIf { it.exists() }?.readText() }.getOrNull()

    fun write(context: Context, key: String, value: String) {
        runCatching { file(context, key).writeText(value) }
    }

    fun clear(context: Context) {
        runCatching { File(context.cacheDir, "netcache").listFiles()?.forEach { it.delete() } }
    }
}
