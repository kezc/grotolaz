package com.wojtek.holds

import kotlinx.browser.window

/**
 * Utilities for syncing selected holds state with URL parameters.
 *
 * Selected holds are encoded as a comma-separated list of IDs in the URL hash,
 * along with a version identifier for the image set.
 * Example: http://localhost:8080/#v=v1&holds=1,5,12,23
 */
object UrlSync {
    private const val VERSION_PARAM = "v"
    private const val HOLDS_PARAM = "holds"

    /**
     * Encodes selected hold IDs and version into the URL hash.
     *
     * @param holdIds Set of hold IDs to encode
     * @param version Version identifier for the image set
     */
    fun encodeToUrl(holdIds: Set<Int>, version: String) {
        val hash = if (holdIds.isEmpty()) {
            ""
        } else {
            "#$VERSION_PARAM=$version&$HOLDS_PARAM=${holdIds.sorted().joinToString(",")}"
        }
        window.location.hash = hash
    }

    /**
     * Decodes selected hold IDs and version from the URL hash.
     *
     * @return Pair of version string and set of hold IDs, or null version and empty set if no valid data in URL
     */
    fun decodeFromUrl(): Pair<String?, Set<Int>> {
        val hash = window.location.hash.removePrefix("#")
        if (hash.isBlank()) return Pair(null, emptySet())

        val params = hash.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = parts.getOrNull(0) ?: ""
            val value = parts.getOrNull(1) ?: ""
            key to value
        }

        val version = params[VERSION_PARAM]
        val holdsParam = params[HOLDS_PARAM] ?: return Pair(version, emptySet())

        val holdIds = holdsParam.split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()

        return Pair(version, holdIds)
    }
}
