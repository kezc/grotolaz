// ============================================================================
// Store — the APP-OWNED data cache. Goes in the shared UI module's commonMain.
// Replace {{PACKAGE}} with the module's package.
//
// This is the counterpart to the service worker's software cache. The SW owns
// the static software; this owns the data. Keeping them apart is what makes a
// deploy able to swap the software without disturbing a byte of cached data.
// ============================================================================
package {{PACKAGE}}

/**
 * Name of the app-owned data cache.
 *
 * Deliberately NOT prefixed with `workbox`: the service worker's precache is
 * `workbox-precache-*`, and `cleanupOutdatedCaches: true` only ever deletes
 * caches matching Workbox's own naming scheme. Disjoint names are what let a
 * software update swap the precache while this cache survives untouched.
 */
const val DATA_CACHE_NAME = "app-data-v1"

/**
 * One entry: the payload plus the HTTP response headers it arrived with.
 *
 * Storing the headers alongside the body is the whole point. `Last-Modified`
 * (or `ETag`) comes back out of the cache exactly as the server sent it, so
 * revalidating with a conditional GET needs no bookkeeping of your own — the
 * revalidation metadata simply *is* the cache entry. A JSON blob in
 * `localStorage` would force you to invent, serialize and hand-maintain that
 * same metadata.
 */
class StoredEntry(
    val text: String,
    val headers: Map<String, String>,
) {
    /** Case-insensitive lookup, as HTTP requires. */
    operator fun get(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/**
 * Backed by the browser Cache API on wasmJs, and by files on JVM desktop.
 *
 * Every operation is `suspend`: the Cache API is promise-based, and file I/O
 * should not run on the UI thread either.
 *
 * Treat a miss as an ordinary outcome, never an error. Cache Storage is subject
 * to eviction under storage pressure, so an entry you wrote can legitimately
 * disappear — which is exactly how a cache is supposed to behave.
 */
expect object Store {
    suspend fun get(key: String): StoredEntry?
    suspend fun put(key: String, entry: StoredEntry)
    suspend fun remove(key: String)
}
