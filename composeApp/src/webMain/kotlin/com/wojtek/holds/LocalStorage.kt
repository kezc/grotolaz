package com.wojtek.holds

import kotlinx.browser.localStorage
import com.wojtek.holds.model.SelectedHolds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Save and load functions using localStorage (web-specific)
fun saveSelectedHolds(holdIds: List<Int>) {
    try {
        val json = Json.encodeToString(SelectedHolds(holdIds))
        localStorage.setItem("selectedHolds", json)
        println("Saved selected holds: $holdIds")
    } catch (e: Exception) {
        println("Error saving holds: ${e.message}")
    }
}

fun loadSelectedHolds(): List<Int>? {
    return try {
        val json = localStorage.getItem("selectedHolds")
        if (json != null) {
            val selectedHolds = Json.decodeFromString<SelectedHolds>(json)
            println("Loaded selected holds: ${selectedHolds.selectedIds}")
            selectedHolds.selectedIds
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error loading holds: ${e.message}")
        null
    }
}
