package com.wojtek.holds.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wojtek.holds.components.ClimbingWallView
import com.wojtek.holds.components.CompactZoomControls
import com.wojtek.holds.components.MinimalControlPanel
import com.wojtek.holds.state.rememberHoldSelectionManager
import com.wojtek.holds.utils.ConfigurationLoadResult
import com.wojtek.holds.utils.rememberHoldConfiguration
import holds.composeapp.generated.resources.Res
import holds.composeapp.generated.resources.wall
import org.jetbrains.compose.resources.painterResource

/**
 * Example: Custom climbing wall app with minimal UI.
 *
 * This demonstrates how to build a custom app using the reusable components.
 * Features:
 * - Minimal control panel (counter only)
 * - Compact zoom controls
 * - Custom color scheme
 * - No save/load functionality
 */
@Composable
fun MinimalClimbingApp() {
    val configurationResult = rememberHoldConfiguration()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = configurationResult.value) {
                is ConfigurationLoadResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConfigurationLoadResult.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ConfigurationLoadResult.Success -> {
                    var selectedHoldIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        MinimalControlPanel(
                            selectedCount = selectedHoldIds.size,
                            totalCount = result.configuration.holds.size
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .clipToBounds()
                        ) {
                            ClimbingWallView(
                                configuration = result.configuration,
                                wallImagePainter = painterResource(Res.drawable.wall),
                                selectedHoldIds = selectedHoldIds,
                                onHoldClick = { holdId ->
                                    selectedHoldIds = if (holdId in selectedHoldIds) {
                                        selectedHoldIds - holdId
                                    } else {
                                        selectedHoldIds + holdId
                                    }
                                },
                                selectedColor = Color.Blue,
                                unselectedColor = Color.Gray,
                                zoomControlsContent = { zoomState, zoomCallbacks ->
                                    CompactZoomControls(zoomState, zoomCallbacks)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Example: Advanced climbing wall app with state manager.
 *
 * This demonstrates advanced features:
 * - HoldSelectionManager with undo/redo
 * - Custom control panel with extra buttons
 * - State change callbacks
 */
@Composable
fun AdvancedClimbingApp() {
    val configurationResult = rememberHoldConfiguration()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = configurationResult.value) {
                is ConfigurationLoadResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConfigurationLoadResult.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ConfigurationLoadResult.Success -> {
                    val selectionManager = rememberHoldSelectionManager(
                        configuration = result.configuration,
                        onSelectionChange = { selection ->
                            println("Selection changed: ${selection.size} holds selected")
                        }
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        AdvancedControlPanel(
                            selectionManager = selectionManager,
                            totalCount = result.configuration.holds.size
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .clipToBounds()
                        ) {
                            ClimbingWallView(
                                configuration = result.configuration,
                                wallImagePainter = painterResource(Res.drawable.wall),
                                selectedHoldIds = selectionManager.selectedHoldIds.value,
                                onHoldClick = { holdId -> selectionManager.toggleHold(holdId) },
                                selectedColor = Color(0xFF4CAF50), // Material Green
                                unselectedColor = Color(0xFFFF5722) // Material Deep Orange
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom control panel with undo/redo and invert selection.
 */
@Composable
private fun AdvancedControlPanel(
    selectionManager: com.wojtek.holds.state.HoldSelectionManager,
    totalCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected: ${selectionManager.getSelectedCount()} / $totalCount",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectionManager.clearSelection() }
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = { selectionManager.invertSelection() }
                ) {
                    Text("Invert")
                }

                Button(
                    onClick = { selectionManager.undo() },
                    enabled = selectionManager.canUndo()
                ) {
                    Text("Undo")
                }

                Button(
                    onClick = { selectionManager.redo() },
                    enabled = selectionManager.canRedo()
                ) {
                    Text("Redo")
                }
            }
        }
    }
}

/**
 * Example: Side-by-side comparison app.
 *
 * This demonstrates how to use multiple ClimbingWallView components
 * to compare different route selections.
 */
@Composable
fun ComparisonClimbingApp() {
    val configurationResult = rememberHoldConfiguration()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = configurationResult.value) {
                is ConfigurationLoadResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConfigurationLoadResult.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ConfigurationLoadResult.Success -> {
                    var route1 by remember { mutableStateOf<Set<Int>>(emptySet()) }
                    var route2 by remember { mutableStateOf<Set<Int>>(emptySet()) }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Route 1
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Route 1: ${route1.size} holds",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            ) {
                                ClimbingWallView(
                                    configuration = result.configuration,
                                    wallImagePainter = painterResource(Res.drawable.wall),
                                    selectedHoldIds = route1,
                                    onHoldClick = { holdId ->
                                        route1 = if (holdId in route1) route1 - holdId else route1 + holdId
                                    },
                                    selectedColor = Color.Blue,
                                    maxZoom = 3f
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                        )

                        // Route 2
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Route 2: ${route2.size} holds",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                            ) {
                                ClimbingWallView(
                                    configuration = result.configuration,
                                    wallImagePainter = painterResource(Res.drawable.wall),
                                    selectedHoldIds = route2,
                                    onHoldClick = { holdId ->
                                        route2 = if (holdId in route2) route2 - holdId else route2 + holdId
                                    },
                                    selectedColor = Color.Magenta,
                                    maxZoom = 3f
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
