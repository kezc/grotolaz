package com.wojtek.holds

import kotlinx.browser.window

private const val VERSION_PARAM = "v"
private const val HOLDS_PARAM = "holds"

object SilentUrlUpdater {
    fun updateHoldsInUrl(holdIds: Set<Int>, version: String) {
        val hash = if (holdIds.isEmpty()) {
            ""
        } else {
            "#$VERSION_PARAM=$version&$HOLDS_PARAM=${holdIds.sorted().joinToString(",")}"
        }

        // replaceState(data, title, url)
        // We MUST pass window.history.state as the data.
        // This ensures bindToBrowserNavigation doesn't lose track of its backstack
        // if the user later clicks the browser's Back button.
        window.history.replaceState(
            data = window.history.state,
            title = "",
            url = hash
        )
    }
}