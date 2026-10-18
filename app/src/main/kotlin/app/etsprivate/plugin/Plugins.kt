package app.etsprivate.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import app.etsprivate.NavDestination
import app.etsprivate.settings.Settings
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

sealed interface InstallFailure {
    data object Unreadable : InstallFailure

    data object NotAModule : InstallFailure

    data class WrongVersion(val built: Int, val runs: Int) : InstallFailure

    data object Broken : InstallFailure
}

class StaleModule(
    val file: File,
    val packageName: String,
    val version: String,
    val address: String?,
)

class InstalledModule(
    val module: EtsModule,

    val file: File,
    val packageName: String,
    val version: String,

    val updateUrl: String?,

    val newVersion: String? = null,
)

object Plugins {
    private const val META_CLASS = "ets.plugin"
    private const val META_API = "ets.plugin.api"
    private const val META_UPDATE = "ets.plugin.update"

    private val client = OkHttpClient()

    var installingFrom by mutableStateOf<String?>(null)
        private set

    var installProgress by mutableStateOf<Float?>(null)
        private set

    private val loaded: SnapshotStateList<InstalledModule> = mutableStateListOf()
    private val stale: SnapshotStateList<StaleModule> = mutableStateListOf()
    private var scanned = false

    val builtInModule: EtsModule = BuiltInModule

    val installed: List<InstalledModule> get() = loaded

    val outdated: List<StaleModule> get() = stale

    private const val SEEN_PREFS = "plugin_updates_seen"
    private val seenVersions: SnapshotStateMap<String, String> = mutableStateMapOf()
    private var seenLoaded = false

    private fun loadSeenVersions(context: Context) {
        if (seenLoaded) return
        seenLoaded = true
        val prefs = context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) -> (value as? String)?.let { seenVersions[key] = it } }
    }

    fun hasUnseenUpdate(context: Context): Boolean {
        loadSeenVersions(context)
        return loaded.any { it.newVersion != null && seenVersions[it.packageName] != it.newVersion }
    }

    fun markUpdatesSeen(context: Context) {
        loadSeenVersions(context)
        val prefs = context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        loaded.forEach { entry ->
            entry.newVersion?.let { version ->
                seenVersions[entry.packageName] = version
                edit.putString(entry.packageName, version)
            }
        }
        edit.apply()
    }

    private val switchedOff: SnapshotStateList<String> = mutableStateListOf()

    fun isOn(entry: InstalledModule): Boolean = entry.packageName !in switchedOff

    fun setOn(context: Context, entry: InstalledModule, on: Boolean) {
        if (on) switchedOff.remove(entry.packageName) else if (entry.packageName !in switchedOff) switchedOff.add(entry.packageName)
        context.getSharedPreferences(OFF_PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(OFF_KEY, switchedOff.toSet()).apply()
    }

    private const val OFF_PREFS = "modules"
    private const val OFF_KEY = "switched_off"

    private val builtInModules: List<EtsModule> get() = listOf(builtInModule, app.etsprivate.transit.TransportModule)

    private fun shadowed(entry: InstalledModule): Boolean = builtInModules.any { it.id == entry.module.id }

    fun modules(): List<EtsModule> = builtInModules + loaded.filter { isOn(it) && !shadowed(it) }.map { it.module }

    val builtIn: List<EtsPlugin> get() = BuiltInModule.pages()

    fun all(): List<EtsPlugin> = modules().flatMap { it.pages() }

    fun byId(id: String): EtsPlugin? = all().firstOrNull { it.id == id }

    fun moduleById(id: String): EtsModule? = modules().firstOrNull { it.id == id }

    fun chosen(context: Context): List<EtsPlugin> {
        val saved = Settings.getNavDestinations(context)?.mapNotNull(::byId)?.takeIf { it.isNotEmpty() }
        if (saved != null) return saved
        val defaults = NavDestination.DEFAULT.mapNotNull { destination -> byId(destination.id) }
        return defaults.ifEmpty { all().take(NavDestination.MAX) }
    }

    fun settingsPages(): List<Pair<EtsModule, PluginSettings>> =
        modules().mapNotNull { module -> module.settings()?.let { module to it } }

    private fun directory(context: Context) = File(context.filesDir, "plugins").apply { mkdirs() }

    fun load(context: Context) {
        if (scanned || app.etsprivate.BuildConfig.STORE_BUILD) return
        scanned = true
        switchedOff.clear()
        switchedOff.addAll(context.getSharedPreferences(OFF_PREFS, Context.MODE_PRIVATE).getStringSet(OFF_KEY, emptySet()).orEmpty())
        reload(context)
    }

    fun reload(context: Context) {
        loaded.clear()
        stale.clear()
        directory(context).listFiles { file -> file.extension == "apk" }?.sortedBy { it.name }?.forEach { file ->
            read(context, file)?.let(loaded::add)
        }
    }

    suspend fun install(context: Context, address: String): InstallFailure? {
        if (app.etsprivate.BuildConfig.STORE_BUILD) return InstallFailure.NotAModule
        val url = address.trim().trimEnd('/')
        if (!url.startsWith("http")) return InstallFailure.NotAModule
        installingFrom = url
        installProgress = null
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val apk = if (url.endsWith(".apk")) {
                    url
                } else {
                    val manifest = client.newCall(Request.Builder().url("$url/latest.json").build()).execute().use {
                        if (!it.isSuccessful) return@runCatching null
                        JSONObject(it.body?.string().orEmpty())
                    }
                    val name = manifest.optString("apk")
                    if (name.isBlank()) return@runCatching null
                    "$url/$name"
                }
                client.newCall(Request.Builder().url(apk).build()).execute().use { response ->
                    val body = response.body ?: return@runCatching null
                    if (!response.isSuccessful) return@runCatching null
                    val staged = File(context.cacheDir, "module-download.apk")
                    body.byteStream().use { input ->
                        staged.outputStream().use { output -> copyWatched(input, output, body.contentLength()) }
                    }
                    staged
                }
            }.getOrNull()
        }
        if (file == null) {
            installingFrom = null
            installProgress = null
            return InstallFailure.Unreadable
        }
        val failure = keep(context, file)
        installingFrom = null
        installProgress = null
        if (failure == null) {
            loaded.lastOrNull()?.let { entry ->
                if (entry.updateUrl == null && !url.endsWith(".apk")) remember(context, entry.packageName, url)
            }
        }
        return failure
    }

    private fun addresses(context: Context) = context.getSharedPreferences("modules", Context.MODE_PRIVATE)

    private fun remember(context: Context, packageName: String, url: String) {
        addresses(context).edit().putString(packageName, url).apply()
    }

    fun addressOf(context: Context, entry: InstalledModule): String? =
        entry.updateUrl ?: addresses(context).getString(entry.packageName, null)

    fun startAll(context: Context) {
        modules().forEach { module -> runCatching { module.onAppStart(context) } }
    }

    fun install(context: Context, uri: Uri): InstallFailure? {
        if (app.etsprivate.BuildConfig.STORE_BUILD) return InstallFailure.NotAModule
        val staged = File(context.cacheDir, "plugin-staged.apk")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                staged.outputStream().use(input::copyTo)
            } ?: return InstallFailure.Unreadable
        }.onFailure { return InstallFailure.Unreadable }
        return keep(context, staged)
    }

    private fun keep(context: Context, staged: File): InstallFailure? {
        val info = context.packageManager.getPackageArchiveInfo(staged.path, PackageManager.GET_META_DATA)
            ?: run { staged.delete(); return InstallFailure.NotAModule }
        val meta = info.applicationInfo?.metaData
        val declared = meta?.getString(META_CLASS)
        if (declared.isNullOrBlank()) {
            staged.delete()
            return InstallFailure.NotAModule
        }
        val api = meta.getInt(META_API, 0)
        if (api != PLUGIN_API) {
            staged.delete()
            return InstallFailure.WrongVersion(api, PLUGIN_API)
        }

        val file = File(directory(context), "${info.packageName}.apk")
        loaded.removeAll { it.packageName == info.packageName }
        file.setWritable(true)
        staged.copyTo(file, overwrite = true)
        staged.delete()
        sealed(file)

        val module = read(context, file) ?: run {
            file.delete()
            return InstallFailure.Broken
        }
        loaded.add(module)

        stale.removeAll { it.file.path == file.path }
        runCatching { module.module.onAppStart(context) }
        return null
    }

    fun forget(file: File) {
        file.setWritable(true)
        file.delete()
        stale.removeAll { it.file == file }
    }

    fun remove(context: Context, moduleId: String) {
        val entry = loaded.firstOrNull { it.module.id == moduleId } ?: return
        entry.file.setWritable(true)
        entry.file.delete()
        loaded.remove(entry)
    }

    private fun sealed(file: File) {
        file.setReadOnly()
    }

    private fun read(context: Context, file: File): InstalledModule? = runCatching {
        val manager = context.packageManager
        val info = manager.getPackageArchiveInfo(file.path, PackageManager.GET_META_DATA) ?: return null
        val application = info.applicationInfo ?: return null
        val meta = application.metaData ?: return null
        if (meta.getInt(META_API, 0) != PLUGIN_API) {
            stale.add(
                StaleModule(
                    file = file,
                    packageName = info.packageName,
                    version = info.versionName.orEmpty(),
                    address = meta.getString(META_UPDATE)?.trimEnd('/')
                        ?: addresses(context).getString(info.packageName, null),
                ),
            )
            return null
        }
        val className = meta.getString(META_CLASS) ?: return null

        application.sourceDir = file.path
        application.publicSourceDir = file.path
        val resources = manager.getResourcesForApplication(application)
        val updateUrl = meta.getString(META_UPDATE)?.trimEnd('/')

        sealed(file)

        val classes = ModuleClassLoader(
            file.path,
            File(context.codeCacheDir, "plugins").apply { mkdirs() }.path,
            unpackNativeLibraries(context, file),
            Plugins::class.java.classLoader,
        )
        val type = classes.loadClass(className)

        val instance = runCatching { type.getDeclaredField("INSTANCE").get(null) }
            .getOrElse { type.getDeclaredConstructor().newInstance() }
        val module = instance as EtsModule
        module.attach(PluginEnvironment(context, resources))
        InstalledModule(
            module = module,
            file = file,
            packageName = info.packageName,
            version = info.versionName ?: "",
            updateUrl = updateUrl,
        )
    }.getOrNull()

    suspend fun checkUpdates(context: Context) = withContext(Dispatchers.IO) {
        if (app.etsprivate.BuildConfig.STORE_BUILD) return@withContext
        loaded.toList().forEach { entry ->
            val url = addressOf(context, entry) ?: return@forEach
            val newest = runCatching {
                val request = Request.Builder().url("$url/latest.json").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    JSONObject(response.body?.string().orEmpty())
                }
            }.getOrNull() ?: return@forEach
            val version = newest.optString("version")

            if (version.isBlank() || !isNewer(version, than = entry.version)) return@forEach
            val index = loaded.indexOfFirst { it.packageName == entry.packageName }
            if (index >= 0) {
                withContext(Dispatchers.Main) {
                    loaded[index] = InstalledModule(
                        entry.module, entry.file, entry.packageName, entry.version, entry.updateUrl, version,
                    )
                }
            }
        }
    }

    internal fun isNewer(version: String, than: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val a = parts(version)
        val b = parts(than)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun update(context: Context, packageName: String): Boolean {
        val entry = loaded.firstOrNull { it.packageName == packageName } ?: return false
        val url = addressOf(context, entry) ?: return false
        installingFrom = url
        installProgress = null
        val done = withContext(Dispatchers.IO) {
            runCatching {
                val manifest = client.newCall(Request.Builder().url("$url/latest.json").build()).execute().use {
                    JSONObject(it.body?.string().orEmpty())
                }
                val file = manifest.optString("apk")
                if (file.isBlank()) return@withContext false
                client.newCall(Request.Builder().url("$url/$file").build()).execute().use { response ->
                    val body = response.body ?: return@runCatching false
                    if (!response.isSuccessful) return@runCatching false
                    val staged = File(context.cacheDir, "module-staged.apk")
                    body.byteStream().use { input ->
                        staged.outputStream().use { output -> copyWatched(input, output, body.contentLength()) }
                    }
                    entry.file.setWritable(true)
                    staged.copyTo(entry.file, overwrite = true)
                    staged.delete()
                    sealed(entry.file)
                }
                true
            }.getOrDefault(false)
        }
        installingFrom = null
        installProgress = null
        return done
    }

    private fun copyWatched(input: java.io.InputStream, output: java.io.OutputStream, total: Long) {
        val buffer = ByteArray(64 * 1024)
        var read = 0L
        while (true) {
            val count = input.read(buffer)
            if (count <= 0) break
            output.write(buffer, 0, count)
            read += count
            if (total > 0) installProgress = (read.toFloat() / total).coerceIn(0f, 1f)
        }
    }

    private fun unpackNativeLibraries(context: Context, file: File): String? = runCatching {
        val target = File(File(context.filesDir, "plugin-lib"), file.nameWithoutExtension)
        val wanted = android.os.Build.SUPPORTED_ABIS.toList()
        ZipFile(file).use { zip ->
            val libraries = zip.entries().asSequence()
                .filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
                .groupBy { it.name.split("/")[1] }
            val abi = wanted.firstOrNull { libraries.containsKey(it) } ?: return null
            target.mkdirs()
            libraries.getValue(abi).forEach { entry ->
                val out = File(target, entry.name.substringAfterLast('/'))

                if (out.exists() && out.length() == entry.size) return@forEach
                out.setWritable(true)
                zip.getInputStream(entry).use { input -> out.outputStream().use(input::copyTo) }
                out.setReadOnly()
            }
        }
        target.path
    }.getOrNull()
}

private class ModuleClassLoader(
    path: String,
    optimized: String,
    native: String?,
    parent: ClassLoader,
) : DexClassLoader(path, optimized, native, parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (OWN_RESOURCES.none { name.startsWith(it) }) return super.loadClass(name, resolve)
        synchronized(this) {
            findLoadedClass(name)?.let { return it }
            return try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                super.loadClass(name, resolve)
            }
        }
    }

    private companion object {
        val OWN_RESOURCES = listOf(
            "com.google.android.material.",
            "androidx.appcompat.",
            "androidx.cardview.",
            "androidx.constraintlayout.",
            "androidx.coordinatorlayout.",
            "androidx.recyclerview.",
        )
    }
}
