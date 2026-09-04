@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import {{PACKAGE}}.App
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

// The app is not mounted immediately. registerServiceWorker.js exposes
// `window.pwaStartupGate`, a Promise that resolves only once the running code is
// confirmed to be the latest version (i.e. no update-triggered reload is
// pending). Until then the HTML splash stays up. This avoids launching the old
// version and interrupting the user with a reload a few seconds later.
fun main() {
    val gate = pwaStartupGate()
    if (gate == null) {
        startApp()
    } else {
        gate.then { startApp(); null }
    }
}

private fun startApp() {
    removeSplash()
    ComposeViewport { App() }   // replace App() with your root composable
}

// Kotlin/Wasm requires js("…") to be the sole expression of a top-level function.
private fun pwaStartupGate(): Promise<JsAny?>? = js("(window.pwaStartupGate || null)")

private fun removeSplash(): Unit =
    js("(function(){var e=document.getElementById('pwa-splash'); if(e){e.remove();}})()")
