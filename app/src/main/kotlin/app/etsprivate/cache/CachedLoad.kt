package app.etsprivate.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

suspend fun <T> loadCached(
    context: Context,
    key: String,
    decode: (String) -> T,
    encode: (T) -> String,
    fetch: suspend () -> T,
    render: (T) -> Unit,

    keep: (T) -> Boolean = { true },
) {
    val cached = MemoryCache.get<T>(key)
        ?: withContext(Dispatchers.IO) { DiskCache.read(context, key) }?.let { runCatching { decode(it) }.getOrNull() }?.also { MemoryCache.put(key, it) }
    cached?.let(render)

    val fresh = try {
        fetch()
    } catch (e: Exception) {
        if (e is AuthRequired || cached == null) throw e else return
    }
    if (keep(fresh)) {
        MemoryCache.put(key, fresh)
        withContext(Dispatchers.IO) { DiskCache.write(context, key, encode(fresh)) }
    }
    if (fresh != cached) render(fresh)
}

fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }

fun JSONObject.putOrBlank(key: String, value: String?) {
    put(key, value ?: "")
}

fun JSONObject.getOrNullString(key: String): String? = getString(key).ifBlank { null }

fun JSONObject.putOrBlank(key: String, value: Double?) {
    put(key, value?.toString() ?: "")
}

fun JSONObject.getOrNullDouble(key: String): Double? = getString(key).toDoubleOrNull()

fun JSONObject.putOrBlank(key: String, value: Int?) {
    put(key, value?.toString() ?: "")
}

fun JSONObject.getOrNullInt(key: String): Int? = getString(key).toIntOrNull()
