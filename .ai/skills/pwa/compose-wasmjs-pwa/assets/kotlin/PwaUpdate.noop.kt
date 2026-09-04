package {{PACKAGE}}

// No service worker on this target, so there is never a pending web update.
// Copy this file into EACH non-wasmJs target source set the shared UI module
// compiles for (e.g. jvmMain, jsMain, androidMain, iosMain) so the `expect`
// object in commonMain has an `actual` everywhere.
actual object PwaUpdate {
    actual fun isUpdateAvailable(): Boolean = false
    actual fun applyUpdate() {}
}
