package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
    problemsRepository: ProblemRepository,
    version: String,
    selectedHoldsId: Set<Int>,
    showSaveDialog: (Problem) -> Unit,
    modifier: Modifier = Modifier,
    showProblemsDialog: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Zoom In button
        FloatingIconButton(
            onClick = zoomCallbacks.onZoomIn,
            imageVector = Icons.Default.Add,
            contentDescription = "Zoom In",
            containerColor = MaterialTheme.colorScheme.primary
        )

        // Zoom Out button
        FloatingIconButton(
            onClick = zoomCallbacks.onZoomOut,
            imageVector = Icons.Default.Remove,
            contentDescription = "Zoom Out",
            containerColor = MaterialTheme.colorScheme.primary
        )

        // Lock button
        FloatingIconButton(
            onClick = onToggleLock,
            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.secondary
            },
            contentDescription = if (isLocked) "Locked" else "Unlocked"
        )

        // More options button with popup menu
        Box {
            FloatingIconButton(
                onClick = { showMenu = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options"
            )

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

                HorizontalDivider()

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                            Text("Share URL")
                        }
                    },
                    onClick = {
                        showMenu = false
                        // Copy current URL to clipboard
                        scope.launch {
                            try {
                                window.navigator.clipboard.writeText(window.location.href)
                                snackbarHostState.showSnackbar(
                                    message = "URL copied to clipboard",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                println("Failed to copy URL to clipboard: ${e.message}")
                            }
                        }
                    }
                )


                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save"
                            )
                            Text("Save boulder")
                        }
                    },
                    onClick = {
                        showMenu = false
                        scope.launch {
                            showSaveDialog(
                                Problem(
                                    name = Clock.System.now().epochSeconds.toString(),
                                    id = Clock.System.now().epochSeconds.toInt(),
                                    createdAt = Clock.System.now().epochSeconds,
                                    updatedAt = Clock.System.now().epochSeconds,
                                    version = version,
                                    holdsIds = selectedHoldsId.toList(),
                                )
                            )
                        }
                    }
                )

                DropdownMenuItem(
                    text =  {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null
                            )
                            Text("Show boulders")
                        }
                    },
                    onClick = {
                        showProblemsDialog()
                    }
                )

            }
        }
    }

    // Snackbar host for showing copy confirmation
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 80.dp)
    )
}

@Composable
fun FloatingIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    containerColor: Color
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        containerColor = containerColor
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription
        )
    }
}