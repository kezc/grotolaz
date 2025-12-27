package com.wojtek.holds.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Control panel component for managing hold selections.
 *
 * Displays selection counter and action buttons for clearing, saving, and loading selections.
 *
 * @param selectedCount Number of currently selected holds
 * @param totalCount Total number of holds available
 * @param onClearClick Callback when Clear button is clicked
 * @param onSaveClick Callback when Save button is clicked
 * @param onLoadClick Callback when Load button is clicked
 * @param modifier Optional modifier for the component
 * @param showSaveLoad Whether to show Save and Load buttons (default: true)
 * @param additionalActions Optional additional action buttons composable
 */
@Composable
fun ControlPanel(
    selectedCount: Int,
    totalCount: Int,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit = {},
    onLoadClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showSaveLoad: Boolean = true,
    additionalActions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected: $selectedCount / $totalCount",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClearClick) { Text("Clear") }

                if (showSaveLoad) {
                    Button(onClick = onSaveClick) { Text("Save") }
                    Button(onClick = onLoadClick) { Text("Load") }
                }

                additionalActions?.invoke(this)
            }
        }
    }
}

/**
 * Minimal control panel component with just a counter.
 *
 * @param selectedCount Number of currently selected holds
 * @param totalCount Total number of holds available
 * @param modifier Optional modifier for the component
 */
@Composable
fun MinimalControlPanel(
    selectedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Text(
            text = "Selected: $selectedCount / $totalCount",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
