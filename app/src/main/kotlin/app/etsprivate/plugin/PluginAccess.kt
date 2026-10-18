package app.etsprivate.plugin

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

object PluginAccess {
    enum class Area(val packagePrefix: String) {
        SCHOOL("app/etsprivate/ets/"),
        MAIL("app/etsprivate/mail/"),
        ACCOUNTS("app/etsprivate/auth/"),
        STORED("app/etsprivate/cache/"),
        SETTINGS("app/etsprivate/settings/"),

        TRANSIT("app/etsprivate/transit/"),
    }

    class Reach(val areas: Set<Area>, val fromModules: Set<String>)

    private const val PREFS = "plugin_access"

    fun of(context: Context, file: File, others: List<InstalledModule>): Reach {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "${file.name}:${file.length()}:${file.lastModified()}"
        val cached = prefs.getString(key, null)
        val referenced = if (cached != null) {
            cached.split(',').filter { it.isNotEmpty() }.toSet()
        } else {
            val found = runCatching { borrowed(file) }.getOrDefault(emptySet())
            prefs.edit().clear().putString(key, found.joinToString(",")).apply()
            found
        }

        val areas = Area.entries.filter { area -> referenced.any { it.startsWith(area.packagePrefix) } }.toSet()

        val modules = others.mapNotNull { other ->
            val theirs = runCatching { defined(other.file) }.getOrDefault(emptySet())
            other.module.id.takeIf { theirs.any(referenced::contains) }
        }.toSet()
        return Reach(areas, modules)
    }

    private fun borrowed(file: File): Set<String> {
        val used = mutableSetOf<String>()
        val own = mutableSetOf<String>()
        forEachDex(file) { dex ->
            val types = types(dex)
            used += types.values
            own += defines(dex, types)
        }
        return (used - own).filter { it.startsWith("app/etsprivate/") }.toSet()
    }

    private fun defined(file: File): Set<String> {
        val own = mutableSetOf<String>()
        forEachDex(file) { dex -> own += defines(dex, types(dex)) }
        return own
    }

    private fun forEachDex(file: File, read: (ByteArray) -> Unit) {
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }
                .forEach { entry -> zip.getInputStream(entry).use { read(it.readBytes()) } }
        }
    }

    private fun int(dex: ByteArray, at: Int): Int =
        (dex[at].toInt() and 0xff) or
            ((dex[at + 1].toInt() and 0xff) shl 8) or
            ((dex[at + 2].toInt() and 0xff) shl 16) or
            ((dex[at + 3].toInt() and 0xff) shl 24)

    private fun types(dex: ByteArray): Map<Int, String> {
        val stringCount = int(dex, 56)
        val stringsAt = int(dex, 60)
        val typeCount = int(dex, 64)
        val typesAt = int(dex, 68)
        val names = HashMap<Int, String>(typeCount)
        for (index in 0 until typeCount) {
            val stringIndex = int(dex, typesAt + index * 4)
            if (stringIndex !in 0 until stringCount) continue
            val at = int(dex, stringsAt + stringIndex * 4)
            names[index] = descriptor(dex, at)
        }
        return names
    }

    private fun descriptor(dex: ByteArray, at: Int): String {
        var cursor = at

        while (cursor < dex.size && (dex[cursor].toInt() and 0x80) != 0) cursor++
        cursor++
        val start = cursor
        while (cursor < dex.size && dex[cursor].toInt() != 0) cursor++
        val raw = String(dex, start, cursor - start, Charsets.UTF_8)

        return raw.removePrefix("L").removeSuffix(";")
    }

    private fun defines(dex: ByteArray, types: Map<Int, String>): Set<String> {
        val count = int(dex, 96)
        val at = int(dex, 100)
        val own = mutableSetOf<String>()
        for (index in 0 until count) {
            val typeIndex = int(dex, at + index * 32)
            types[typeIndex]?.let(own::add)
        }
        return own
    }
}
