package app.etsprivate.cache

object MemoryCache {
    private val store = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = store[key] as? T

    fun put(key: String, value: Any?) {
        store[key] = value
    }

    fun clear() {
        store.clear()
    }
}
