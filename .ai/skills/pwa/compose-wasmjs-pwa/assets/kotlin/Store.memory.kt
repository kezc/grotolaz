// ============================================================================
// Store — in-memory actual for any remaining target (androidMain, iosMain, …).
// Copy once per target source set. Replace {{PACKAGE}}.
//
// Every `expect` needs an `actual` per target or the module will not compile.
// This is a deliberate placeholder: it satisfies the contract and behaves
// correctly within a session, but does not survive a restart. If the app really
// uses its data cache on that platform, replace this with a persistent
// implementation (files on Android/iOS, mirroring Store.jvm.kt).
// ============================================================================
package {{PACKAGE}}

actual object Store {
    private val entries = mutableMapOf<String, StoredEntry>()

    actual suspend fun get(key: String): StoredEntry? = entries[key]

    actual suspend fun put(key: String, entry: StoredEntry) {
        entries[key] = entry
    }

    actual suspend fun remove(key: String) {
        entries.remove(key)
    }
}
