# Climbing Wall Components - Developer Guide

## Overview

The Holds project provides a set of reusable Compose Multiplatform components for building interactive climbing wall applications. This guide explains how to use these components to build custom climbing wall apps.

## Architecture

The application is now split into modular, reusable components:

```
com.wojtek.holds/
├── components/              # Reusable UI components
│   ├── ClimbingWallView.kt     # Core interactive wall component
│   ├── ControlPanel.kt          # Control panel with buttons
│   └── ZoomControls.kt          # Zoom UI controls
├── state/                   # State management
│   └── HoldSelectionManager.kt  # Hold selection state manager
├── utils/                   # Utility functions
│   └── ConfigurationLoader.kt   # Configuration loading utilities
├── examples/                # Example implementations
│   └── CustomClimbingApp.kt     # Sample custom apps
├── model/                   # Data models
│   └── HoldConfiguration.kt     # Data structures
├── ClimbingWallApp.kt      # Default implementation
├── LocalStorage.kt         # Browser storage integration
└── UrlSync.kt              # URL-based state sharing
```

## Core Components

### 1. ClimbingWallView

The main interactive component that displays the climbing wall with hold overlays.

**Features:**
- Polygon-based hold rendering
- Click detection for hold selection
- Zoom and pan functionality
- Customizable colors and styling
- Flexible zoom controls

**Basic Usage:**

```kotlin
@Composable
fun MyApp() {
    val configuration = // ... load configuration
    var selectedHolds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    ClimbingWallView(
        configuration = configuration,
        wallImagePainter = painterResource(Res.drawable.wall),
        selectedHoldIds = selectedHolds,
        onHoldClick = { holdId ->
            selectedHolds = if (holdId in selectedHolds) {
                selectedHolds - holdId
            } else {
                selectedHolds + holdId
            }
        }
    )
}
```

**Advanced Usage with Custom Styling:**

```kotlin
ClimbingWallView(
    configuration = configuration,
    wallImagePainter = painterResource(Res.drawable.wall),
    selectedHoldIds = selectedHolds,
    onHoldClick = { holdId -> /* handle click */ },
    selectedColor = Color.Blue,           // Custom selected color
    unselectedColor = Color.Gray,         // Custom unselected color
    selectedAlpha = 0.7f,                 // Custom transparency
    unselectedAlpha = 0.2f,
    minZoom = 1f,                         // Min zoom level
    maxZoom = 5f,                         // Max zoom level
    showZoomControls = true               // Show/hide zoom controls
)
```

**Custom Zoom Controls:**

```kotlin
ClimbingWallView(
    configuration = configuration,
    wallImagePainter = painterResource(Res.drawable.wall),
    selectedHoldIds = selectedHolds,
    onHoldClick = { holdId -> /* handle click */ },
    zoomControlsContent = { zoomState, zoomCallbacks ->
        // Use built-in compact controls
        CompactZoomControls(zoomState, zoomCallbacks)

        // Or create your own custom controls
        // MyCustomZoomButtons(zoomState, zoomCallbacks)
    }
)
```

### 2. ControlPanel

A control panel for managing hold selections with counter and action buttons.

**Basic Usage:**

```kotlin
ControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size,
    onClearClick = { selectedHolds = emptySet() },
    onSaveClick = { /* save to storage */ },
    onLoadClick = { /* load from storage */ }
)
```

**Without Save/Load:**

```kotlin
ControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size,
    onClearClick = { selectedHolds = emptySet() },
    showSaveLoad = false
)
```

**With Custom Actions:**

```kotlin
ControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size,
    onClearClick = { selectedHolds = emptySet() },
    additionalActions = {
        Button(onClick = { /* custom action */ }) {
            Text("Custom")
        }
    }
)
```

**Minimal Panel (Counter Only):**

```kotlin
MinimalControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size
)
```

### 3. ZoomControls

Zoom control UI components. Multiple styles available.

**Default Zoom Controls:**

```kotlin
Box {
    // ... content ...

    DefaultZoomControls(
        zoomState = ZoomState(scale = currentScale),
        zoomCallbacks = ZoomCallbacks(
            onZoomIn = { /* zoom in */ },
            onZoomOut = { /* zoom out */ },
            onReset = { /* reset zoom */ }
        )
    )
}
```

**Compact Zoom Controls:**

```kotlin
CompactZoomControls(zoomState, zoomCallbacks)
```

**Zoom Controls with Percentage:**

```kotlin
ZoomControlsWithPercentage(zoomState, zoomCallbacks)
```

## State Management

### HoldSelectionManager

Advanced state manager with undo/redo, validation, and callbacks.

**Basic Usage:**

```kotlin
@Composable
fun MyApp() {
    val configuration = // ... load configuration
    val selectionManager = rememberHoldSelectionManager(
        configuration = configuration,
        onSelectionChange = { selection ->
            println("Selection changed: $selection")
        }
    )

    ClimbingWallView(
        configuration = configuration,
        wallImagePainter = painterResource(Res.drawable.wall),
        selectedHoldIds = selectionManager.selectedHoldIds.value,
        onHoldClick = { holdId -> selectionManager.toggleHold(holdId) }
    )
}
```

**Available Operations:**

```kotlin
// Toggle hold selection
selectionManager.toggleHold(holdId)

// Select/deselect specific holds
selectionManager.selectHold(holdId)
selectionManager.deselectHold(holdId)

// Bulk operations
selectionManager.selectHolds(setOf(1, 2, 3))
selectionManager.deselectHolds(setOf(1, 2, 3))
selectionManager.clearSelection()
selectionManager.invertSelection()

// Undo/redo
if (selectionManager.canUndo()) {
    selectionManager.undo()
}
if (selectionManager.canRedo()) {
    selectionManager.redo()
}

// Query state
val isSelected = selectionManager.isSelected(holdId)
val count = selectionManager.getSelectedCount()
```

## Utility Functions

### Configuration Loading

**Using rememberHoldConfiguration:**

```kotlin
@Composable
fun MyApp() {
    val configResult = rememberHoldConfiguration(
        resourcePath = "files/holds.json",
        onSuccess = { config -> println("Loaded: ${config.holds.size} holds") },
        onError = { error -> println("Error: $error") }
    )

    when (val result = configResult.value) {
        is ConfigurationLoadResult.Loading -> LoadingScreen()
        is ConfigurationLoadResult.Error -> ErrorScreen(result.message)
        is ConfigurationLoadResult.Success -> MyContent(result.configuration)
    }
}
```

**Using rememberHoldConfigurationState:**

```kotlin
@Composable
fun MyApp() {
    val (configuration, isLoading, errorMessage) = rememberHoldConfigurationState()

    when {
        isLoading -> LoadingScreen()
        errorMessage != null -> ErrorScreen(errorMessage)
        configuration != null -> MyContent(configuration)
    }
}
```

## Complete Examples

### Example 1: Minimal App

A simple app with just the climbing wall and a counter.

```kotlin
@Composable
fun MinimalApp() {
    val configurationResult = rememberHoldConfiguration()
    var selectedHolds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    MaterialTheme {
        when (val result = configurationResult.value) {
            is ConfigurationLoadResult.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MinimalControlPanel(
                        selectedCount = selectedHolds.size,
                        totalCount = result.configuration.holds.size
                    )
                    ClimbingWallView(
                        configuration = result.configuration,
                        wallImagePainter = painterResource(Res.drawable.wall),
                        selectedHoldIds = selectedHolds,
                        onHoldClick = { holdId ->
                            selectedHolds = if (holdId in selectedHolds) {
                                selectedHolds - holdId
                            } else {
                                selectedHolds + holdId
                            }
                        }
                    )
                }
            }
            // ... handle loading/error states
        }
    }
}
```

### Example 2: Advanced App with State Manager

An app with undo/redo, invert selection, and state callbacks.

```kotlin
@Composable
fun AdvancedApp() {
    val configurationResult = rememberHoldConfiguration()

    MaterialTheme {
        when (val result = configurationResult.value) {
            is ConfigurationLoadResult.Success -> {
                val selectionManager = rememberHoldSelectionManager(
                    configuration = result.configuration,
                    onSelectionChange = { selection ->
                        // Handle selection changes
                        println("Selected ${selection.size} holds")
                    }
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom control panel
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { selectionManager.clearSelection() }) {
                                Text("Clear")
                            }
                            Button(onClick = { selectionManager.invertSelection() }) {
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

                    ClimbingWallView(
                        configuration = result.configuration,
                        wallImagePainter = painterResource(Res.drawable.wall),
                        selectedHoldIds = selectionManager.selectedHoldIds.value,
                        onHoldClick = { holdId -> selectionManager.toggleHold(holdId) }
                    )
                }
            }
            // ... handle loading/error states
        }
    }
}
```

### Example 3: Side-by-Side Comparison

Compare two different route selections side by side.

```kotlin
@Composable
fun ComparisonApp() {
    val configurationResult = rememberHoldConfiguration()
    var route1 by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var route2 by remember { mutableStateOf<Set<Int>>(emptySet()) }

    MaterialTheme {
        when (val result = configurationResult.value) {
            is ConfigurationLoadResult.Success -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Route 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Route 1: ${route1.size} holds")
                        ClimbingWallView(
                            configuration = result.configuration,
                            wallImagePainter = painterResource(Res.drawable.wall),
                            selectedHoldIds = route1,
                            onHoldClick = { holdId ->
                                route1 = if (holdId in route1) route1 - holdId else route1 + holdId
                            },
                            selectedColor = Color.Blue
                        )
                    }

                    // Route 2
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Route 2: ${route2.size} holds")
                        ClimbingWallView(
                            configuration = result.configuration,
                            wallImagePainter = painterResource(Res.drawable.wall),
                            selectedHoldIds = route2,
                            onHoldClick = { holdId ->
                                route2 = if (holdId in route2) route2 - holdId else route2 + holdId
                            },
                            selectedColor = Color.Magenta
                        )
                    }
                }
            }
            // ... handle loading/error states
        }
    }
}
```

### Example 4: Custom Colors and Styling

```kotlin
@Composable
fun StyledApp() {
    val configurationResult = rememberHoldConfiguration()
    var selectedHolds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    MaterialTheme(
        colorScheme = darkColorScheme() // Use dark theme
    ) {
        when (val result = configurationResult.value) {
            is ConfigurationLoadResult.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    ControlPanel(
                        selectedCount = selectedHolds.size,
                        totalCount = result.configuration.holds.size,
                        onClearClick = { selectedHolds = emptySet() }
                    )

                    ClimbingWallView(
                        configuration = result.configuration,
                        wallImagePainter = painterResource(Res.drawable.wall),
                        selectedHoldIds = selectedHolds,
                        onHoldClick = { holdId ->
                            selectedHolds = if (holdId in selectedHolds) {
                                selectedHolds - holdId
                            } else {
                                selectedHolds + holdId
                            }
                        },
                        // Custom Material Design colors
                        selectedColor = Color(0xFF4CAF50),  // Green 500
                        unselectedColor = Color(0xFFFF5722), // Deep Orange 500
                        selectedAlpha = 0.6f,
                        unselectedAlpha = 0.4f,
                        maxZoom = 8f // Allow higher zoom level
                    )
                }
            }
            // ... handle loading/error states
        }
    }
}
```

## Component Parameters Reference

### ClimbingWallView Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `configuration` | `HoldConfiguration` | Required | Hold configuration data |
| `wallImagePainter` | `Painter` | Required | Wall background image |
| `selectedHoldIds` | `Set<Int>` | Required | Currently selected hold IDs |
| `onHoldClick` | `(Int) -> Unit` | Required | Callback when hold is clicked |
| `modifier` | `Modifier` | `Modifier` | Optional modifier |
| `showZoomControls` | `Boolean` | `true` | Show zoom controls |
| `selectedColor` | `Color` | `Color.Green` | Color for selected holds |
| `unselectedColor` | `Color` | `Color.Red` | Color for unselected holds |
| `selectedAlpha` | `Float` | `0.5f` | Alpha for selected holds |
| `unselectedAlpha` | `Float` | `0.3f` | Alpha for unselected holds |
| `minZoom` | `Float` | `1f` | Minimum zoom level |
| `maxZoom` | `Float` | `5f` | Maximum zoom level |
| `zoomStep` | `Float` | `1.2f` | Zoom step multiplier |
| `zoomControlsContent` | `@Composable` | `null` | Custom zoom controls |

### ControlPanel Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `selectedCount` | `Int` | Required | Number of selected holds |
| `totalCount` | `Int` | Required | Total number of holds |
| `onClearClick` | `() -> Unit` | Required | Callback for Clear button |
| `onSaveClick` | `() -> Unit` | `{}` | Callback for Save button |
| `onLoadClick` | `() -> Unit` | `{}` | Callback for Load button |
| `modifier` | `Modifier` | `Modifier` | Optional modifier |
| `showSaveLoad` | `Boolean` | `true` | Show Save/Load buttons |
| `additionalActions` | `@Composable` | `null` | Additional action buttons |

## Tips and Best Practices

1. **Always handle loading states**: Use `ConfigurationLoadResult` to properly display loading and error states.

2. **Use HoldSelectionManager for complex state**: If you need undo/redo or validation, prefer `HoldSelectionManager` over raw state.

3. **Customize colors for different use cases**: Use different color schemes to distinguish between different route types or difficulty levels.

4. **Consider zoom limits**: Adjust `minZoom` and `maxZoom` based on your wall image resolution and user needs.

5. **Reuse components**: All components are designed to be reusable. Build custom layouts by combining them in different ways.

6. **State management**: Keep selection state at the appropriate level. Use `remember` for local state or `HoldSelectionManager` for advanced features.

## See Also

- [CLAUDE.md](CLAUDE.md) - Main project documentation
- [README.md](README.md) - User documentation
- [examples/CustomClimbingApp.kt](composeApp/src/webMain/kotlin/com/wojtek/holds/examples/CustomClimbingApp.kt) - Working examples
