package com.wojtek.holds.state

import androidx.compose.runtime.*
import com.wojtek.holds.model.HoldConfiguration

/**
 * State manager for hold selections.
 *
 * Provides a centralized way to manage hold selection state with additional features
 * like selection modes, undo/redo, and event callbacks.
 *
 * @param initialSelection Initial set of selected hold IDs
 * @param configuration Hold configuration for validation
 * @param onSelectionChange Optional callback when selection changes
 */
class HoldSelectionManager(
    initialSelection: Set<Int> = emptySet(),
    private val configuration: HoldConfiguration? = null,
    private val onSelectionChange: ((Set<Int>) -> Unit)? = null
) {
    private var _selectedHoldIds = mutableStateOf(initialSelection)
    val selectedHoldIds: State<Set<Int>> = _selectedHoldIds

    private var _isLocked = mutableStateOf(false)
    val isLocked: State<Boolean> = _isLocked

    private val history = mutableListOf<Set<Int>>()
    private var historyIndex = -1

    init {
        if (initialSelection.isNotEmpty()) {
            saveToHistory(initialSelection)
        }
    }

    /**
     * Locks the selection to prevent changes.
     */
    fun lock() {
        _isLocked.value = true
    }

    /**
     * Unlocks the selection to allow changes.
     */
    fun unlock() {
        _isLocked.value = false
    }

    /**
     * Toggles the lock state.
     */
    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    /**
     * Toggles selection of a hold.
     */
    fun toggleHold(holdId: Int) {
        if (_isLocked.value) return
        val newSelection = if (holdId in _selectedHoldIds.value) {
            _selectedHoldIds.value - holdId
        } else {
            _selectedHoldIds.value + holdId
        }
        setSelection(newSelection)
    }

    /**
     * Selects a hold.
     */
    fun selectHold(holdId: Int) {
        if (_isLocked.value) return
        if (holdId !in _selectedHoldIds.value) {
            setSelection(_selectedHoldIds.value + holdId)
        }
    }

    /**
     * Deselects a hold.
     */
    fun deselectHold(holdId: Int) {
        if (_isLocked.value) return
        if (holdId in _selectedHoldIds.value) {
            setSelection(_selectedHoldIds.value - holdId)
        }
    }

    /**
     * Selects multiple holds.
     */
    fun selectHolds(holdIds: Set<Int>) {
        if (_isLocked.value) return
        setSelection(_selectedHoldIds.value + holdIds)
    }

    /**
     * Deselects multiple holds.
     */
    fun deselectHolds(holdIds: Set<Int>) {
        if (_isLocked.value) return
        setSelection(_selectedHoldIds.value - holdIds)
    }

    /**
     * Clears all selections.
     */
    fun clearSelection() {
        if (_isLocked.value) return
        setSelection(emptySet())
    }

    /**
     * Sets the selection to a specific set of hold IDs.
     */
    fun setSelection(holdIds: Set<Int>) {
        if (_isLocked.value) return
        val validated = if (configuration != null) {
            holdIds.filter { id -> configuration.holds.any { it.id == id } }.toSet()
        } else {
            holdIds
        }

        _selectedHoldIds.value = validated
        saveToHistory(validated)
        onSelectionChange?.invoke(validated)
    }

    /**
     * Checks if a hold is selected.
     */
    fun isSelected(holdId: Int): Boolean {
        return holdId in _selectedHoldIds.value
    }

    /**
     * Gets the count of selected holds.
     */
    fun getSelectedCount(): Int {
        return _selectedHoldIds.value.size
    }

    /**
     * Inverts the selection (selects unselected, deselects selected).
     */
    fun invertSelection() {
        if (_isLocked.value) return
        if (configuration != null) {
            val allIds = configuration.holds.map { it.id }.toSet()
            setSelection(allIds - _selectedHoldIds.value)
        }
    }

    /**
     * Saves current selection to history.
     */
    private fun saveToHistory(selection: Set<Int>) {
        // Remove any history after current index
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }

        // Add new state
        history.add(selection)
        historyIndex = history.size - 1

        // Limit history size
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
            historyIndex--
        }
    }

    /**
     * Checks if undo is available.
     */
    fun canUndo(): Boolean {
        return historyIndex > 0
    }

    /**
     * Checks if redo is available.
     */
    fun canRedo(): Boolean {
        return historyIndex < history.size - 1
    }

    /**
     * Undoes the last selection change.
     */
    fun undo() {
        if (canUndo()) {
            historyIndex--
            _selectedHoldIds.value = history[historyIndex]
            onSelectionChange?.invoke(_selectedHoldIds.value)
        }
    }

    /**
     * Redoes the last undone selection change.
     */
    fun redo() {
        if (canRedo()) {
            historyIndex++
            _selectedHoldIds.value = history[historyIndex]
            onSelectionChange?.invoke(_selectedHoldIds.value)
        }
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}

/**
 * Remembers a HoldSelectionManager instance.
 *
 * @param initialSelection Initial set of selected hold IDs
 * @param configuration Hold configuration for validation
 * @param onSelectionChange Optional callback when selection changes
 */
@Composable
fun rememberHoldSelectionManager(
    initialSelection: Set<Int> = emptySet(),
    configuration: HoldConfiguration? = null,
    onSelectionChange: ((Set<Int>) -> Unit)? = null
): HoldSelectionManager {
    return remember(configuration) {
        HoldSelectionManager(
            initialSelection = initialSelection,
            configuration = configuration,
            onSelectionChange = onSelectionChange
        )
    }
}
