# Project: Holds - Climbing Wall Hold Tracker

## Overview
This is a Kotlin Multiplatform web application for tracking climbing holds on a wall. The project consists of two main parts:
1. **Preprocessor** - A CLI tool that detects holds from images and generates configuration
2. **Web Application** - An interactive web app built with Compose Multiplatform (JS/Wasm)

## Project Structure

```
holds/
├── preprocessor/            # CLI tool for hold detection
│   ├── src/main/kotlin/com/wojtek/holds/preprocessor/
│   │   ├── HoldDetector.kt          # Image processing & contour tracing
│   │   ├── Main.kt                  # CLI entry point
│   │   └── model/
│   │       └── HoldConfiguration.kt # Data models
│   └── build.gradle.kts
├── composeApp/              # Web application module
│   ├── src/
│   │   ├── webMain/         # Web-specific code (shared between JS and Wasm)
│   │   │   ├── kotlin/com/wojtek/holds/
│   │   │   │   ├── App.kt                # Main entry point
│   │   │   │   ├── ClimbingWallApp.kt    # Default app implementation
│   │   │   │   ├── LocalStorage.kt       # Save/load functionality
│   │   │   │   ├── UrlSync.kt            # URL-based state sharing
│   │   │   │   ├── components/           # Reusable UI components
│   │   │   │   │   ├── ClimbingWallView.kt   # Core interactive wall component
│   │   │   │   │   ├── ControlPanel.kt       # Control panel component
│   │   │   │   │   └── ZoomControls.kt       # Zoom control components
│   │   │   │   ├── state/                # State management
│   │   │   │   │   └── HoldSelectionManager.kt  # Selection state manager
│   │   │   │   ├── utils/                # Utility functions
│   │   │   │   │   └── ConfigurationLoader.kt   # Config loading utilities
│   │   │   │   ├── examples/             # Example implementations
│   │   │   │   │   └── CustomClimbingApp.kt     # Sample custom apps
│   │   │   │   └── model/
│   │   │   │       └── HoldConfiguration.kt  # Data models
│   │   │   └── composeResources/
│   │   │       ├── drawable/
│   │   │       │   ├── map.png           # Binary hold map
│   │   │       │   └── wall.png          # Wall photo
│   │   │       └── files/
│   │   │           └── holds.json        # Generated hold config
│   │   ├── jsMain/          # JavaScript-specific code
│   │   ├── wasmJsMain/      # WebAssembly-specific code
│   │   └── webTest/         # Web tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml   # Dependency versions
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
├── README.md                # User documentation
├── CLAUDE.md                # This file - Developer documentation
└── COMPONENTS.md            # Component library documentation
```

## Technology Stack

### Core Technologies
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.9.3
- **Compose Compiler**: 2.3.0
- **kotlinx.serialization**: 1.8.0
- **Targets**: JavaScript (browser) + WebAssembly (browser)

### Preprocessor Dependencies
- Java AWT (BufferedImage) for image processing
- kotlinx.serialization for JSON generation

### Web App Dependencies
- Compose runtime, foundation, material3, UI
- `androidx.lifecycle:lifecycle-viewmodel-compose`: 2.9.6
- `androidx.lifecycle:lifecycle-runtime-compose`: 2.9.6
- kotlinx.serialization for JSON parsing
- Browser localStorage API for persistence

### Build System
- Gradle with Kotlin DSL
- Version catalog (libs.versions.toml)
- Multiplatform plugin configuration

## How to Run

### Step 1: Generate Hold Configuration (Preprocessor)

The preprocessor analyzes a binary map image and generates polygon contours for each hold.

**Command:**
```bash
# macOS/Linux
./gradlew :preprocessor:run --args="<map-image> <wall-image> <output-json> [version-id]"

# Windows
.\gradlew.bat :preprocessor:run --args="<map-image> <wall-image> <output-json> [version-id]"
```

**Example:**
```bash
./gradlew :preprocessor:run --args="/Users/wojtek/IdeaProjects/holds/composeApp/src/webMain/composeResources/drawable/map.png /Users/wojtek/IdeaProjects/holds/composeApp/src/webMain/composeResources/drawable/wall.png /Users/wojtek/IdeaProjects/holds/composeApp/src/webMain/composeResources/files/holds.json v1"
```

**Parameters:**
- `map-image`: Path to the binary map image (white holds on black background)
- `wall-image`: Path to the actual wall photo
- `output-json`: Path where the holds.json file will be saved
- `version-id` (optional): Version identifier for the image set (default: "v1")

**What it does:**
- Loads the binary map image (white holds on black background)
- Checks if wall image dimensions match map image dimensions
- **Automatically resizes wall image if dimensions don't match** (overwrites the wall image file)
- Uses flood-fill to detect hold regions
- Traces polygon contours using Moore neighborhood algorithm
- Simplifies polygons using Douglas-Peucker algorithm (epsilon=8.0)
- Generates `holds.json` with hold positions and polygon data

**Important Notes:**
- If the wall image has different dimensions than the map image, it will be **automatically resized and overwritten**
- The preprocessor uses high-quality bicubic interpolation for resizing
- Keep a backup of your original wall image if you want to preserve it at its original size

**Output format:**
```json
{
  "wallImage": "wall.png",
  "imageWidth": 2432,
  "imageHeight": 1760,
  "version": "v1",
  "holds": [
    {
      "id": 0,
      "x": 275,
      "y": 325,
      "width": 483,
      "height": 232,
      "polygon": [
        {"x": 275, "y": 325},
        {"x": 280, "y": 330},
        ...
      ]
    }
  ]
}
```

### Step 2: Run the Web Application

After generating `holds.json`, run the web app:

#### WebAssembly (Wasm) Target - Recommended
Faster performance, modern browsers only.

```bash
# macOS/Linux
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Windows
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

#### JavaScript (JS) Target
Slower but supports older browsers.

```bash
# macOS/Linux
./gradlew :composeApp:jsBrowserDevelopmentRun

# Windows
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

The app runs on `http://localhost:8080` by default.

### Continuous Development Mode

Both commands support `--continuous` flag for automatic reload on file changes:
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
```

## Application Features

### Current Implementation
- ✅ **Modular component architecture**: Reusable components for building custom apps
- ✅ **Polygon-based overlays**: Holds are rendered as polygons matching their exact shape
- ✅ **Interactive selection**: Click to select/deselect holds (green when selected, red when not)
- ✅ **Visual feedback**: Semi-transparent overlays with colored borders
- ✅ **Zoom and pan**: Zoom in/out and pan across the wall
- ✅ **Advanced state management**: HoldSelectionManager with undo/redo support
- ✅ **Persistent storage**: Save/load selections using browser localStorage
- ✅ **URL-based sharing**: Share selections via URL with version tracking (e.g., `#v=v1&holds=1,5,12,23`)
- ✅ **Multi-version support**: Maintain multiple wall versions simultaneously with automatic loading
- ✅ **Responsive design**: Automatically adjusts to window resizing
- ✅ **Accurate hit detection**: Point-in-polygon using ray casting algorithm
- ✅ **Hold counter**: Shows "Selected: X / Y"
- ✅ **Customizable styling**: Colors, transparency, zoom levels

### User Controls
- **Click hold**: Toggle selection (red ↔ green)
- **Drag**: Pan across the wall (when zoomed in)
- **Zoom buttons**: Zoom in/out with floating action buttons
- **Clear button**: Deselect all holds
- **Save button**: Store selection to localStorage
- **Load button**: Restore saved selection
- **Share URL**: Copy the browser URL to share your current selection with others (includes version ID)

## Component-Based Architecture

The application is now built using reusable components that can be composed to create custom climbing wall applications. See [COMPONENTS.md](COMPONENTS.md) for detailed documentation.

### Core Components

1. **ClimbingWallView** - The main interactive climbing wall component
   - Displays wall image with polygon-based hold overlays
   - Handles click detection, zoom, and pan
   - Fully customizable colors, transparency, and zoom levels

2. **ControlPanel** - Control panel with selection counter and action buttons
   - Configurable buttons (Clear, Save, Load)
   - Support for custom additional actions
   - Minimal variant available (MinimalControlPanel)

3. **ZoomControls** - Zoom control UI components
   - Multiple styles available (Default, Compact, WithPercentage)
   - Custom zoom controls can be provided

4. **HoldSelectionManager** - Advanced state management
   - Undo/redo support with history
   - Bulk operations (select/deselect multiple, invert)
   - Validation and callbacks

5. **ConfigurationLoader** - Utilities for loading hold configuration
   - Composable functions for loading configuration
   - Error handling and loading states

### Example Custom Apps

See `composeApp/src/webMain/kotlin/com/wojtek/holds/examples/CustomClimbingApp.kt` for complete working examples:

- **MinimalClimbingApp** - Minimal UI with just counter and compact controls
- **AdvancedClimbingApp** - Undo/redo, invert selection, state callbacks
- **ComparisonClimbingApp** - Side-by-side comparison of two routes

### Building Custom Apps

```kotlin
@Composable
fun MyCustomApp() {
    val configurationResult = rememberHoldConfiguration()
    var selectedHolds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    when (val result = configurationResult.value) {
        is ConfigurationLoadResult.Success -> {
            ClimbingWallView(
                configuration = result.configuration,
                wallImagePainter = painterResource(Res.drawable.wall),
                selectedHoldIds = selectedHolds,
                onHoldClick = { holdId -> /* handle click */ },
                selectedColor = Color.Blue,  // Custom colors
                maxZoom = 8f                 // Custom zoom limit
            )
        }
        // ... handle loading/error states
    }
}
```

## Technical Implementation Details

### Hold Detection Algorithm (Preprocessor)
1. **Flood Fill**: Identifies connected white pixels as hold regions
2. **Moore Neighborhood Tracing**: Traces the boundary of each hold
3. **Douglas-Peucker Simplification**: Reduces polygon complexity while preserving shape
4. **Bounding Box Calculation**: Computes min/max coordinates for each hold

### Rendering (Web App)
1. **Aspect Ratio Calculation**: Determines how the image fits in the container
2. **Scale Factor Calculation**: Computes scale for X and Y axes independently
3. **Offset Calculation**: Centers image when container aspect ratio differs
4. **Polygon Rendering**: Uses Compose Canvas with Path API to draw filled polygons with borders

### Click Detection (Web App)
1. **Coordinate Adjustment**: Applies offset to click coordinates
2. **Ray Casting**: Determines if click point is inside polygon
3. **Fallback**: Uses bounding box if polygon data is unavailable

### Responsive Behavior
- `onSizeChanged` modifier triggers recomposition on resize
- Scale factors and offsets recalculate automatically
- `pointerInput(scaleX, scaleY, offsetX, offsetY)` restarts gesture detection with updated values

## Package Structure

### Preprocessor
- **Root Package**: `com.wojtek.holds.preprocessor`
- **Main Components**:
  - `HoldDetector.kt` - Image processing and contour tracing
  - `Main.kt` - CLI entry point and argument parsing
  - `model/HoldConfiguration.kt` - Data models for serialization

### Web Application
- **Root Package**: `com.wojtek.holds`
- **Main Components**:
  - `App.kt` - Application entry point
  - `ClimbingWallApp.kt` - Default application implementation
  - `LocalStorage.kt` - Browser storage integration (localStorage)
  - `UrlSync.kt` - URL-based state sharing utilities
  - `model/HoldConfiguration.kt` - Data models matching preprocessor output

#### Components Package (`com.wojtek.holds.components`)
Reusable UI components for building custom climbing wall applications:
  - `ClimbingWallView.kt` - Core interactive wall component with zoom/pan
  - `ControlPanel.kt` - Control panel with counter and action buttons
  - `ZoomControls.kt` - Zoom control UI components (multiple styles)

#### State Package (`com.wojtek.holds.state`)
State management utilities:
  - `HoldSelectionManager.kt` - Advanced selection state manager with undo/redo

#### Utils Package (`com.wojtek.holds.utils`)
Utility functions and helpers:
  - `ConfigurationLoader.kt` - Configuration loading utilities with error handling

#### Examples Package (`com.wojtek.holds.examples`)
Example implementations demonstrating component usage:
  - `CustomClimbingApp.kt` - Sample custom apps (Minimal, Advanced, Comparison)

## Data Models

### Hold Configuration
```kotlin
@Serializable
data class HoldConfiguration(
    val wallImage: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val holds: List<Hold>,
    val version: String = "v1" // Version ID for the image trio (map, wall, empty)
)

@Serializable
data class Hold(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val polygon: List<Point> = emptyList()
)

@Serializable
data class Point(
    val x: Int,
    val y: Int
)
```

### Version Management

The application supports multiple wall versions simultaneously through versioned resource loading:

1. **Version ID**: Each configuration has a `version` field (e.g., "v1", "v2", "v3")
2. **URL Format**: URLs encode both version and holds: `#v=v1&holds=1,5,12`
3. **Resource Structure**: Images and configurations are organized by version:
   ```
   composeResources/
     files/
       v1/
         holds.json
         wall.png
         empty.png
       v2/
         holds.json
         wall.png
         empty.png
   ```
4. **Dynamic Loading**: When a URL is opened:
   - The app extracts the version from the URL (defaults to "v1" if not specified)
   - Loads the corresponding `holds.json` from `files/{version}/holds.json`
   - Loads images from `files/{version}/wall.png` and `files/{version}/empty.png`
   - No warnings needed - the correct version is always displayed
5. **Updating Walls**: When regenerating holds.json with new images:
   - Create a new version directory (e.g., `files/v2/`)
   - Generate holds.json with the new version ID
   - Place wall.png and empty.png in the version directory
   - Both v1 and v2 URLs work simultaneously
6. **Default Version**: The default version (used when no version is in the URL) is defined in `Constants.DEFAULT_VERSION`:
   - Web app: `composeApp/src/webMain/kotlin/com/wojtek/holds/Constants.kt`
   - Preprocessor: `preprocessor/src/main/kotlin/com/wojtek/holds/preprocessor/Constants.kt`
   - Change this value in both places to switch the default version

## Development Notes

### When Making Changes
1. **Preprocessor changes**: Regenerate `holds.json` after modifying hold detection
2. **Data model changes**: Update BOTH preprocessor and web app models (they're separate)
3. **Image changes**: Place in `composeApp/src/webMain/composeResources/drawable/`
4. **Component changes**: Update components in `com.wojtek.holds.components` package
5. **Custom apps**: Add new examples to `com.wojtek.holds.examples` package
6. **Dependencies**: Ensure all are installed before finalizing changes
7. **Continuous build**: Use `--continuous` flag for automatic reload during development

### Important Files
- `.gitignore` - Excludes `kotlin-js-store/`, `node_modules/`, build outputs
- `holds.json` - Generated by preprocessor, loaded by web app
- `LocalStorage.kt` - Web-only file (uses kotlinx.browser APIs)
- `COMPONENTS.md` - Component library documentation for developers

### Building Custom Apps
To create a custom climbing wall application:
1. Use `ClimbingWallView` as the core component
2. Add your own UI components (control panels, toolbars, etc.)
3. Choose between simple state management (`remember`) or advanced (`HoldSelectionManager`)
4. Customize colors, zoom levels, and behaviors via component parameters
5. See examples in `com.wojtek.holds.examples.CustomClimbingApp.kt`

### Platform-Specific Code
- **webMain**: Code shared between JS and Wasm targets
- **jsMain**: JavaScript-specific implementations (currently unused)
- **wasmJsMain**: Wasm-specific implementations (currently unused)

### Known Limitations
- Preprocessor requires absolute file paths
- Preprocessor will overwrite the wall image file if it needs to be resized (keep backups!)
- Web app requires modern browser for Wasm target
- localStorage is browser-specific (not synced)

## Testing
- Tests are located in `composeApp/src/webTest/`
- Uses `kotlin-test` library
- Currently minimal test coverage

## Resources
- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/)
- [Kotlin/Wasm](https://kotl.in/wasm/)
- Community: [#compose-web Slack channel](https://slack-chats.kotlinlang.org/c/compose-web)
- Issues: [YouTrack CMP Project](https://youtrack.jetbrains.com/newIssue?project=CMP)

## Future Enhancements (Potential)

### Application Features
- Multiple hold selection modes (routes, problems, training circuits)
- Color-coded difficulty levels or route grades
- Export/import routes as files (JSON, CSV)
- Multi-wall support (tabs or wall picker)
- Route naming, descriptions, and metadata
- Cloud sync for selections (Firebase, Supabase)
- Mobile app version (Android/iOS with Compose Multiplatform)
- 3D wall visualization

### Component Enhancements
- Animation for hold selection/deselection
- Touch gesture support for mobile (pinch-to-zoom)
- Hold highlighting on hover
- Custom hold shapes (not just polygons)
- Multiple selection modes (box select, lasso select)
- Hold filtering (by size, type, color)
- Accessibility improvements (keyboard navigation, screen reader support)

---
*Last Updated: 2025-12-27*
