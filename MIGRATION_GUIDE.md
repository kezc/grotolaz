# Migration Guide: Monolithic to Component-Based Architecture

## Overview

The climbing wall application has been refactored from a monolithic structure into reusable, composable components. This guide helps you understand the changes and migrate any custom code.

## What Changed?

### Before (Monolithic)
```
ClimbingWallApp.kt (660+ lines)
├── ClimbingWallApp()
├── LoadingIndicator()
├── ErrorDisplay()
├── ClimbingWallContent()
├── ControlPanel()
├── InteractiveClimbingWall()
├── WallImage()
├── HoldOverlays()
├── ZoomControls()
├── DisplayParameters (data class)
├── calculateDisplayParameters()
├── findClickedHold()
├── isPointInHold()
├── isPointInBoundingBox()
├── isPointInPolygon()
├── drawHoldOverlay()
├── drawPolygonHold()
├── drawRectangleHold()
├── createHoldPath()
└── ... all logic in one file
```

### After (Component-Based)
```
com.wojtek.holds/
├── components/                      # Reusable UI components
│   ├── ClimbingWallView.kt             (Core component)
│   ├── ControlPanel.kt                 (UI controls)
│   └── ZoomControls.kt                 (Zoom UI)
├── state/                           # State management
│   └── HoldSelectionManager.kt         (Advanced state)
├── utils/                           # Utilities
│   └── ConfigurationLoader.kt          (Config loading)
├── examples/                        # Example apps
│   └── CustomClimbingApp.kt
└── ClimbingWallApp.kt              (Reference implementation)
```

## Key Improvements

1. **Modularity**: Components can be used independently or combined
2. **Reusability**: Build multiple apps from the same components
3. **Customization**: Easy to customize colors, behavior, and styling
4. **Testability**: Components can be tested in isolation
5. **Maintainability**: Smaller, focused files are easier to maintain
6. **Documentation**: Each component has clear API documentation

## Migration Examples

### Example 1: Basic App

**Before:**
```kotlin
@Composable
fun MyApp() {
    ClimbingWallApp()  // Everything bundled together
}
```

**After:**
```kotlin
@Composable
fun MyApp() {
    val configurationResult = rememberHoldConfiguration()
    var selectedHolds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    when (val result = configurationResult.value) {
        is ConfigurationLoadResult.Success -> {
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
        // Handle loading/error
    }
}
```

### Example 2: Custom Colors

**Before:**
```kotlin
// Had to modify ClimbingWallApp.kt source code
val color = if (isSelected) Color.Green else Color.Red
```

**After:**
```kotlin
ClimbingWallView(
    configuration = configuration,
    wallImagePainter = painterResource(Res.drawable.wall),
    selectedHoldIds = selectedHolds,
    onHoldClick = { /* ... */ },
    selectedColor = Color.Blue,      // Easy customization
    unselectedColor = Color.Gray
)
```

### Example 3: Custom Control Panel

**Before:**
```kotlin
// Had to copy and modify ControlPanel function from ClimbingWallApp.kt
```

**After:**
```kotlin
// Use built-in variant
MinimalControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size
)

// Or add custom actions
ControlPanel(
    selectedCount = selectedHolds.size,
    totalCount = configuration.holds.size,
    onClearClick = { /* ... */ },
    additionalActions = {
        Button(onClick = { /* custom action */ }) {
            Text("Custom")
        }
    }
)
```

### Example 4: Advanced State Management

**Before:**
```kotlin
// Manual state management with no undo/redo
var selectedHoldIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

// No easy way to add undo/redo
```

**After:**
```kotlin
// Use HoldSelectionManager for undo/redo and more
val selectionManager = rememberHoldSelectionManager(
    configuration = configuration,
    onSelectionChange = { selection ->
        println("Selected ${selection.size} holds")
    }
)

// Easy undo/redo
Button(
    onClick = { selectionManager.undo() },
    enabled = selectionManager.canUndo()
) {
    Text("Undo")
}
```

## Component API Changes

### ClimbingWallView

The main component now accepts many customization parameters:

```kotlin
ClimbingWallView(
    // Required
    configuration: HoldConfiguration,
    wallImagePainter: Painter,
    selectedHoldIds: Set<Int>,
    onHoldClick: (Int) -> Unit,

    // Optional customization
    modifier: Modifier = Modifier,
    showZoomControls: Boolean = true,
    selectedColor: Color = Color.Green,
    unselectedColor: Color = Color.Red,
    selectedAlpha: Float = 0.5f,
    unselectedAlpha: Float = 0.3f,
    minZoom: Float = 1f,
    maxZoom: Float = 5f,
    zoomStep: Float = 1.2f,
    zoomControlsContent: @Composable ((ZoomState, ZoomCallbacks) -> Unit)? = null
)
```

### ControlPanel

```kotlin
ControlPanel(
    // Required
    selectedCount: Int,
    totalCount: Int,
    onClearClick: () -> Unit,

    // Optional
    onSaveClick: () -> Unit = {},
    onLoadClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showSaveLoad: Boolean = true,
    additionalActions: @Composable (RowScope.() -> Unit)? = null
)
```

## Breaking Changes

### None for End Users
The default `ClimbingWallApp()` still works exactly as before. It's now implemented using the new components internally.

### For Custom Implementations
If you had custom code that imported internal functions from `ClimbingWallApp.kt`:

1. **Hold rendering logic** → Now in `ClimbingWallView` component
2. **Control panel UI** → Now in `ControlPanel` component
3. **Zoom controls** → Now in `ZoomControls` component
4. **Configuration loading** → Now in `ConfigurationLoader` utility
5. **State management** → Now available via `HoldSelectionManager`

## Next Steps

1. **Read [COMPONENTS.md](COMPONENTS.md)** - Complete component documentation
2. **Check examples** - See `examples/CustomClimbingApp.kt` for working examples
3. **Experiment** - Build your own custom climbing wall app!

## Backward Compatibility

The default `ClimbingWallApp()` remains unchanged and works exactly as before. No migration is required unless you want to take advantage of the new component-based architecture.

## Questions?

See the documentation:
- [COMPONENTS.md](COMPONENTS.md) - Component library guide
- [CLAUDE.md](CLAUDE.md) - Developer documentation
- [README.md](README.md) - User documentation
