package app.etsprivate.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import app.etsprivate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object AppUpdate {
    private const val PREFS = "update"
    private const val KEY_DISMISSED = "dismissed_version"
    private const val KEY_SEEN = "seen_version"

    data class Release(val version: String, val notes: String, val assetUrl: String, val bytes: Long)

    var latest by mutableStateOf<Release?>(null)
        private set

    private var dismissedVersion by mutableStateOf<String?>(null)

    private var seenVersion by mutableStateOf<String?>(null)

    var checking by mutableStateOf(false)
        private set

    var failed by mutableStateOf(false)
        private set

    var progress by mutableStateOf<Float?>(null)
        private set

    val pending: Release? get() = latest?.takeIf { it.version != dismissedVersion }

    val badge: Boolean get() = pending != null && latest?.version != seenVersion

    private const val RELEASES = "https://api.github.com/repos/ETS-Plus/ets-plus/releases/latest"

    val configured: Boolean get() = true

    val installedVersion: String get() = BuildConfig.VERSION_NAME

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    fun markSeen(context: Context) {
        val version = latest?.version ?: return
        seenVersion = version
        prefs(context).edit().putString(KEY_SEEN, version).apply()
    }

    fun dismiss(context: Context) {
        val version = latest?.version ?: return
        dismissedVersion = version
        prefs(context).edit().putString(KEY_DISMISSED, version).apply()
    }

    suspend fun check(context: Context) {
        if (!configured || checking || app.etsprivate.BuildConfig.STORE_BUILD) return
        dismissedVersion = prefs(context).getString(KEY_DISMISSED, null)
        seenVersion = prefs(context).getString(KEY_SEEN, null)
        checking = true
        failed = false
        try {
            val release = withContext(Dispatchers.IO) { newestRelease(context) }

            latest = release?.takeIf { isNewer(it.version, installedVersion) }
        } catch (e: Exception) {
            failed = true
        } finally {
            checking = false
        }
    }

    private fun newestRelease(context: Context): Release? {
        val request = Request.Builder()
            .url(RELEASES)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string().orEmpty())
            val assets = json.optJSONArray("assets") ?: return null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (!asset.optString("name").endsWith(".apk")) continue
                return Release(
                    version = json.optString("tag_name").removePrefix("v"),
                    notes = json.optString("body").trim(),
                    assetUrl = asset.optString("browser_download_url"),
                    bytes = asset.optLong("size"),
                )
            }
            return null
        }
    }

    var downloadFailed by mutableStateOf(false)
        private set

    var needsPermission by mutableStateOf(false)
        private set

    private var ready: Pair<String, File>? = null

    suspend fun update(context: Context): Boolean {
        val release = latest ?: return false
        if (progress != null) return false
        downloadFailed = false
        val file = ready?.takeIf { (version, file) -> version == release.version && file.exists() }?.second ?: run {
            progress = 0f
            failed = false
            val downloaded = try {
                withContext(Dispatchers.IO) { download(context, release) }
            } catch (e: Exception) {
                android.util.Log.i("AppUpdate", "download failed: $e")
                null
            }
            progress = null
            downloaded
        }
        if (file == null) {
            failed = true
            downloadFailed = true
            return false
        }
        ready = release.version to file
        return install(context, file)
    }

    private fun download(context: Context, release: Release): File {
        val request = Request.Builder().url(release.assetUrl).build()
        client.newCall(request).execute().use { response ->
            val body = response.body ?: throw IllegalStateException("no body")
            if (!response.isSuccessful) throw IllegalStateException("http ${response.code}")
            val directory = File(context.cacheDir, "update").apply { mkdirs() }
            val file = File(directory, "update.apk")
            val total = if (release.bytes > 0) release.bytes else body.contentLength()
            var read = 0L
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) progress = (read.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
            return file
        }
    }

    private fun install(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            needsPermission = true
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        needsPermission = false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    suspend fun shareApp(context: Context): Boolean {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val source = File(context.applicationInfo.sourceDir)
                val directory = File(context.cacheDir, "shared").apply { mkdirs() }
                val copy = File(directory, "ets-$installedVersion.apk")
                source.copyTo(copy, overwrite = true)
                copy
            }.getOrNull()
        } ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                null,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    private fun isNewer(candidate: String, installed: String): Boolean {
        val left = parts(candidate)
        val right = parts(installed)
        for (i in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(i) { 0 }
            val b = right.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
}
