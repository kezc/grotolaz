package com.wojtek.holds.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Default zoom controls component.
 *
 * Displays floating action buttons for zoom in, zoom out, and reset operations.
 * Positioned at the bottom-right corner by default.
 *
 * @param zoomState Current zoom state
 * @param zoomCallbacks Callbacks for zoom operations
 * @param modifier Optional modifier for the component
 */
@Composable
fun BoxScope.DefaultZoomControls(
    zoomState: ZoomState,
    zoomCallbacks: ZoomCallbacks,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom In button
        FloatingActionButton(
            onClick = zoomCallbacks.onZoomIn,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In"
            )
        }

        // Zoom Out button
        FloatingActionButton(
            onClick = zoomCallbacks.onZoomOut,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out"
            )
        }

        // Reset button (only show when zoomed)
        if (zoomState.scale > 1f) {
            FloatingActionButton(
                onClick = zoomCallbacks.onReset,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "1×",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
