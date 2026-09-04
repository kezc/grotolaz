@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package {{PACKAGE}}

// Bridges to the service-worker glue in registerServiceWorker.js, which exposes
// `window.pwaUpdateAvailable` (a flag) and `window.pwaApplyUpdate()` (activate +
// reload). Both globals are always defined by that script, so these calls are
// safe even when the browser has no service-worker support.
actual object PwaUpdate {
    actual fun isUpdateAvailable(): Boolean = pwaUpdateAvailable()
    actual fun applyUpdate() = pwaApplyUpdate()
}

// Kotlin/Wasm requires js("…") to be the sole expression of a TOP-LEVEL function
// (not a member function), so these helpers live outside the object.
private fun pwaUpdateAvailable(): Boolean = js("(window.pwaUpdateAvailable === true)")

private fun pwaApplyUpdate(): Unit =
    js("(window.pwaApplyUpdate ? window.pwaApplyUpdate() : undefined)")
