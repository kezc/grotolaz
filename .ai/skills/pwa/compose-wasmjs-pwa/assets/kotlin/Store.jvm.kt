// ============================================================================
// Store — JVM desktop actual, backed by files. Goes in jvmMain.
// Replace {{PACKAGE}} and {{APP_DIR}} (e.g. ".myapp").
//
// Each entry is stored in HTTP message shape — header lines, a blank line, then
// the body — which is the same body-plus-headers contract the Cache API gives
// you on the web. That is what lets the calling code stay fully common.
// ============================================================================
package {{PACKAGE}}

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

actual object Store {

    private val dir: Path =
        Paths.get(System.getProperty("user.home"), "{{APP_DIR}}", DATA_CACHE_NAME)

    private fun fileOf(key: String): Path =
        dir.resolve(key.replace(Regex("[^A-Za-z0-9._-]"), "_"))

    actual suspend fun get(key: String): StoredEntry? = withContext(Dispatchers.IO) {
        val file = fileOf(key)
        if (!Files.isRegularFile(file)) return@withContext null
        runCatching {
            val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
            val separator = lines.indexOf("")
            require(separator >= 0) { "malformed cache entry: no header/body separator" }
            val headers = lines.take(separator).mapNotNull { line ->
                val colon = line.indexOf(':')
                if (colon <= 0) null
                else line.substring(0, colon).trim() to line.substring(colon + 1).trim()
            }.toMap()
            StoredEntry(lines.drop(separator + 1).joinToString("\n"), headers)
        }.getOrNull()
    }

    actual suspend fun put(key: String, entry: StoredEntry): Unit = withContext(Dispatchers.IO) {
        Files.createDirectories(dir)
        val content = buildString {
            entry.headers.forEach { (name, value) -> append(name).append(": ").append(value).append('\n') }
            append('\n')
            append(entry.text)
        }
        Files.write(fileOf(key), content.toByteArray(StandardCharsets.UTF_8))
        Unit
    }

    actual suspend fun remove(key: String): Unit = withContext(Dispatchers.IO) {
        Files.deleteIfExists(fileOf(key))
        Unit
    }
}
