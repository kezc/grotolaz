// ============================================================================
// TEMPLATE — polling a data endpoint with a conditional GET, cached in the
// app-owned Store. Goes in the shared UI module's commonMain.
// Replace {{PACKAGE}}, {{ENDPOINT_URL}}, and rename Payload/PayloadStore to
// something meaningful for the app.
//
// Requires ktor-client-core in commonMain and a ktor engine per target
// (ktor-client-js for wasmJs, ktor-client-cio for JVM).
//
// The point of this file is the SHAPE, not the specific payload: read the
// cached entry, send its stored validator back up, and only store a new copy
// when the server says the content actually changed.
// ============================================================================
package {{PACKAGE}}

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A header we set ourselves, so the UI can say how stale a cached copy is. */
private const val FETCHED_AT = "X-Fetched-At"

/** What the UI renders. A plain view of what came out of the Store. */
data class Payload(
    val text: String,
    val lastModified: String?, // the server's Last-Modified, as stored
    val fetchedAt: String,     // when this copy was written
)

object PayloadStore {
    private const val KEY = "payload"

    suspend fun load(): Payload? = Store.get(KEY)?.let { entry ->
        Payload(
            text = entry.text,
            lastModified = entry[HttpHeaders.LastModified],
            fetchedAt = entry[FETCHED_AT] ?: "?",
        )
    }

    suspend fun save(text: String, lastModified: String?): Payload {
        val fetchedAt = nowIso()
        val headers = buildMap {
            lastModified?.let { put(HttpHeaders.LastModified, it) }
            put(FETCHED_AT, fetchedAt)
        }
        Store.put(KEY, StoredEntry(text, headers))
        return Payload(text, lastModified, fetchedAt)
    }
}

object PayloadService {

    private const val URL = "{{ENDPOINT_URL}}"

    // No explicit engine: Ktor auto-selects the one on the classpath per
    // platform. Default config does not throw on non-2xx, so a 304 comes back
    // as a normal response you can inspect.
    private val client = HttpClient()

    /**
     * Returns the up-to-date [Payload]: a freshly stored one on `200`, the
     * existing cached value on `304` (or any unexpected status).
     *
     * Throws on network failure — that is deliberate. Callers should treat it
     * as an ordinary outcome and record it (see the offline indicator asset),
     * not swallow it: a silently caught failure makes a permanently broken
     * endpoint look exactly like a healthy app serving a cached copy.
     */
    suspend fun refresh(): Payload? {
        val cached = PayloadStore.load()

        val response = client.get(URL) {
            // The stored header, sent straight back up. No hand-rolled metadata.
            cached?.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
        }

        return when (response.status) {
            HttpStatusCode.NotModified -> cached
            HttpStatusCode.OK -> PayloadStore.save(
                text = response.bodyAsText().trim(),
                lastModified = response.headers[HttpHeaders.LastModified],
            )
            else -> cached
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun nowIso(): String = Clock.System.now().toString()

// ---- Wiring it into a composable -------------------------------------------
//
//     var payload by remember { mutableStateOf<Payload?>(null) }
//     var offline by remember { mutableStateOf(false) }
//     LaunchedEffect(Unit) {
//         runCatching { payload = PayloadStore.load() }   // show cache first
//         while (isActive) {
//             runCatching { PayloadService.refresh() }
//                 .onSuccess { payload = it; offline = false }
//                 .onFailure { offline = true }           // ordinary outcome
//             delay(POLL_INTERVAL_MS)
//         }
//     }
//
// The loop IS the recovery: no connectivity listener and no retry policy are
// needed, because the next tick simply tries again.
//
// CORS: `If-Modified-Since` is not a CORS-safelisted request header, so
// cross-origin it triggers a preflight OPTIONS that a plain static host will not
// answer. Serve the data same-origin with the app, or be prepared to configure
// CORS on the data host.
