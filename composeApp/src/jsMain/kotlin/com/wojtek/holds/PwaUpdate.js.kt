package com.wojtek.holds

// No service worker on this target, so there is never a pending web update.
actual object PwaUpdate {
    actual fun isUpdateAvailable(): Boolean = false
    actual fun applyUpdate() {}
}
