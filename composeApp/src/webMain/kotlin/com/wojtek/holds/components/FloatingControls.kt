package com.wojtek.holds.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Floating controls for mobile-friendly navigation.
 *
 * Displays zoom controls, lock button, and a menu button in the bottom-right corner.
 * The menu button opens a popup with additional options.
 *
 * @param zoomState Current zoom state
 * @param zoomCallbacks Callbacks for zoom operations
 * @param isLocked Whether selection is locked
 * @param onToggleLock Callback when lock toggle is clicked
 * @param showEmptyWall Whether to show empty wall mode
 * @param onToggleEmptyWall Callback when empty wall toggle is clicked
 * @param darkenNonSelected Whether to darken non-selected holds
 * @param onToggleDarkenNonSelected Callback when darken toggle is clicked
 * @param showBorders Whether to show borders on selected holds
 * @param onToggleBorders Callback when border toggle is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun BoxScope.FloatingControls(
    zoomState: ZoomState,
    zoomCallbacks: ZoomCallbacks,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    showEmptyWall: Boolean,
    onToggleEmptyWall: () -> Unit,
    darkenNonSelected: Boolean,
    onToggleDarkenNonSelected: () -> Unit,
    showBorders: Boolean,
    onToggleBorders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Zoom In button
        FloatingActionButton(
            onClick = zoomCallbacks.onZoomIn,
            modifier = Modifier.size(48.dp),
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
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out"
            )
        }

        // Lock button
        FloatingActionButton(
            onClick = onToggleLock,
            modifier = Modifier.size(48.dp),
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.secondary
            }
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (isLocked) "Locked" else "Unlocked"
            )
        }

        // More options button with popup menu
        Box {
            FloatingActionButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = DpOffset((-16).dp, 0.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = darkenNonSelected,
                                onCheckedChange = null,
                                enabled = !showEmptyWall
                            )
                            Text(
                                text = "Darken Non-Selected",
                                color = if (showEmptyWall) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    },
                    onClick = {
                        if (!showEmptyWall) {
                            onToggleDarkenNonSelected()
                        }
                    },
                    enabled = !showEmptyWall
                )

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = showEmptyWall,
                                onCheckedChange = null
                            )
                            Text("Show Selected Only")
                        }
                    },
                    onClick = {
                        onToggleEmptyWall()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = showBorders,
                                onCheckedChange = null,
                                enabled = darkenNonSelected || showEmptyWall
                            )
                            Text(
                                text = "Show Borders",
                                color = if (darkenNonSelected || showEmptyWall) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    },
                    onClick = {
                        if (darkenNonSelected || showEmptyWall) {
                            onToggleBorders()
                        }
                    },
                    enabled = darkenNonSelected || showEmptyWall
                )
            }
        }
    }
}
