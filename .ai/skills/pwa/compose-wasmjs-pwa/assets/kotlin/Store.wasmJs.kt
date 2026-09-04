// ============================================================================
// Store — wasmJs actual, backed by the browser Cache API. Goes in wasmJsMain.
// Replace {{PACKAGE}}.
//
// Requires kotlinx-serialization-json on the classpath (used only to marshal
// across the Wasm/JS boundary — see the note below).
// ============================================================================
package {{PACKAGE}}

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise

/**
 * Cache API entries are keyed by request URL, so app-owned data needs a
 * synthetic key. It is never fetched over the network — it exists purely as a
 * cache key — and the `/app-data/` prefix keeps it visibly distinct from the
 * software URLs in the Workbox precache when you inspect DevTools.
 */
private fun cacheUrl(key: String) = "/app-data/$key"

// --- Cache API bridge --------------------------------------------------------
// Kotlin/Wasm ships no bindings for CacheStorage, so reach it through small
// js() shims. Note what crosses the Wasm/JS boundary: a JSON *marshalling* of
// the entry, nothing more. What lands in Cache Storage is a real Response with
// real headers, exactly as if it had come off the network.
//
// js() must be the sole expression of a top-level function in Kotlin/Wasm —
// never a member function. That is why these are private top-level helpers.

private fun cacheMatch(cacheName: String, url: String): Promise<JsString?> = js(
    "caches.open(cacheName).then(c => c.match(url)).then(r => r ? r.text().then(t => JSON.stringify({ text: t, headers: Object.fromEntries(r.headers.entries()) })) : null)"
)

private fun cachePut(cacheName: String, url: String, body: String, headersJson: String): Promise<JsAny?> = js(
    "caches.open(cacheName).then(c => c.put(url, new Response(body, { headers: JSON.parse(headersJson) })))"
)

private fun cacheDelete(cacheName: String, url: String): Promise<JsAny?> = js(
    "caches.open(cacheName).then(c => c.delete(url))"
)

private val boundaryJson = Json { ignoreUnknownKeys = true }

actual object Store {

    actual suspend fun get(key: String): StoredEntry? {
        // A miss is normal: never written, or evicted under storage pressure.
        val raw = runCatching { cacheMatch(DATA_CACHE_NAME, cacheUrl(key)).await() }
            .getOrNull() ?: return null
        return runCatching {
            val obj = boundaryJson.parseToJsonElement(raw.toString()).jsonObject
            StoredEntry(
                text = obj.getValue("text").jsonPrimitive.content,
                headers = obj.getValue("headers").jsonObject
                    .mapValues { (_, value) -> value.jsonPrimitive.content },
            )
        }.getOrNull()
    }

    actual suspend fun put(key: String, entry: StoredEntry) {
        val headersJson = JsonObject(entry.headers.mapValues { (_, v) -> JsonPrimitive(v) }).toString()
        cachePut(DATA_CACHE_NAME, cacheUrl(key), entry.text, headersJson).await()
    }

    actual suspend fun remove(key: String) {
        cacheDelete(DATA_CACHE_NAME, cacheUrl(key)).await()
    }
}
