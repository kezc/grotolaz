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

/**
 * Compact zoom controls with smaller buttons.
 *
 * @param zoomState Current zoom state
 * @param zoomCallbacks Callbacks for zoom operations
 * @param modifier Optional modifier for the component
 */
@Composable
fun BoxScope.CompactZoomControls(
    zoomState: ZoomState,
    zoomCallbacks: ZoomCallbacks,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom Out button
        SmallFloatingActionButton(
            onClick = zoomCallbacks.onZoomOut,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out"
            )
        }

        // Reset button (only show when zoomed)
        if (zoomState.scale > 1f) {
            SmallFloatingActionButton(
                onClick = zoomCallbacks.onReset,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "1×",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Zoom In button
        SmallFloatingActionButton(
            onClick = zoomCallbacks.onZoomIn,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In"
            )
        }
    }
}

/**
 * Zoom controls with percentage display.
 *
 * @param zoomState Current zoom state
 * @param zoomCallbacks Callbacks for zoom operations
 * @param modifier Optional modifier for the component
 */
@Composable
fun BoxScope.ZoomControlsWithPercentage(
    zoomState: ZoomState,
    zoomCallbacks: ZoomCallbacks,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Show zoom percentage
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "${(zoomState.scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        // Reset button (only show when zoomed)
        if (zoomState.scale > 1f) {
            FloatingActionButton(
                onClick = zoomCallbacks.onReset,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
