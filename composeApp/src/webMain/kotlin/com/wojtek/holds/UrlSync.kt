package com.wojtek.holds

import kotlinx.browser.window

/**
 * Utilities for syncing selected holds state with URL parameters.
 *
 * Selected holds are encoded as a comma-separated list of IDs in the URL hash.
 * Example: http://localhost:8080/#holds=1,5,12,23
 */
object UrlSync {
    private const val HOLDS_PARAM = "holds"

    /**
     * Encodes selected hold IDs into the URL hash.
     *
     * @param holdIds Set of hold IDs to encode
     */
    fun encodeToUrl(holdIds: Set<Int>) {
        val hash = if (holdIds.isEmpty()) {
            ""
        } else {
            "#$HOLDS_PARAM=${holdIds.sorted().joinToString(",")}"
        }
        window.location.hash = hash
    }

    /**
     * Decodes selected hold IDs from the URL hash.
     *
     * @return Set of hold IDs, or empty set if no valid data in URL
     */
    fun decodeFromUrl(): Set<Int> {
        val hash = window.location.hash.removePrefix("#")
        if (hash.isBlank()) return emptySet()

        val params = hash.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = parts.getOrNull(0) ?: ""
            val value = parts.getOrNull(1) ?: ""
            key to value
        }

        val holdsParam = params[HOLDS_PARAM] ?: return emptySet()

        return holdsParam.split(",")
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }
}
