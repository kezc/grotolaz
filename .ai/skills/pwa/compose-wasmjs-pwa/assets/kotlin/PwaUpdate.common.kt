package {{PACKAGE}}

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Bridge between the platform's software-update mechanism and the Compose UI.
 *
 * On wasmJs this talks to the service-worker glue in `registerServiceWorker.js`,
 * which does NOT reload the page the instant a new version is found while the app
 * is running. Instead it raises a flag; the UI surfaces a "New Software" button
 * and only [applyUpdate] actually activates the new worker and restarts.
 *
 * On every other target (JVM/desktop, plain JS, Android, iOS, …) there is no
 * service worker, so the implementations are no-ops: [isUpdateAvailable] is
 * always `false`. Provide one such no-op `actual` per target the module compiles
 * for (see PwaUpdate.noop.kt).
 */
expect object PwaUpdate {
    /** `true` when a new version was detected *while the app was running*. */
    fun isUpdateAvailable(): Boolean

    /** Activate the pending update and restart the app. No-op if none is pending. */
    fun applyUpdate()
}

/**
 * Observes [PwaUpdate.isUpdateAvailable] and recomposes when it flips.
 *
 * The service-worker glue only mutates a plain JS flag, so we poll it (cheaply,
 * once a second) rather than plumbing a JS→wasm callback across the boundary.
 */
@Composable
fun rememberUpdateAvailable(): Boolean {
    var available by remember { mutableStateOf(PwaUpdate.isUpdateAvailable()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            available = PwaUpdate.isUpdateAvailable()
            delay(1_000)
        }
    }
    return available
}
