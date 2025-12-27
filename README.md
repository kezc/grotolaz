# Holds - Climbing Wall Hold Tracker

A Kotlin Multiplatform web application for tracking climbing holds on a wall. Click on holds to select them, and save/load your selections.

## Project Structure

This project consists of two main components:

### 1. Preprocessor (CLI Tool)
Located in `/preprocessor` - A command-line tool that analyzes a binary map image (white holds on black background) and generates a JSON configuration file with polygon contours for each hold.

### 2. Web Application
Located in `/composeApp` - An interactive web app that displays the climbing wall with clickable hold overlays.

## Getting Started

### Prerequisites
- JDK 11 or higher
- Node.js (for web development)

### Step 1: Generate Hold Configuration (Preprocessor)

First, you need to process your climbing wall images to detect holds:

1. Prepare two images:
   - **Map image**: A binary image with white holds on a black background (`map.png`)
   - **Wall photo**: The actual photo of your climbing wall (`wall.png`)

2. Run the preprocessor:

**macOS/Linux:**
```bash
./gradlew :preprocessor:run --args="<path-to-map.png> <path-to-wall.png> <output-path>/holds.json"
```

**Windows:**
```bash
.\gradlew.bat :preprocessor:run --args="<path-to-map.png> <path-to-wall.png> <output-path>\holds.json"
```

**Example:**
```bash
./gradlew :preprocessor:run --args="/path/to/map.png /path/to/wall.png composeApp/src/webMain/composeResources/files/holds.json"
```

The preprocessor will:
- Detect all holds in the map image
- Trace polygon contours for each hold
- Simplify polygons using Douglas-Peucker algorithm
- Generate a `holds.json` file with hold positions and polygon data

### Step 2: Run the Web Application

After generating the `holds.json` file, run the web application:

**For Wasm target (recommended - faster, modern browsers):**

macOS/Linux:
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Windows:
```bash
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

**For JS target (broader browser support):**

macOS/Linux:
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

Windows:
```bash
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

The application will start a development server (usually on `http://localhost:8080`).

## Using the Application

Once the web app is running:

1. **Select holds**: Click on any hold to select it (turns green)
2. **Deselect holds**: Click a selected hold again to deselect it (turns red)
3. **Clear all**: Click the "Clear" button to deselect all holds
4. **Save selection**: Click "Save" to store your selection in browser localStorage
5. **Load selection**: Click "Load" to restore a previously saved selection

The overlays are rendered as polygons that match the exact shape of each hold, and they automatically adjust when you resize the browser window.

## Features

- ✅ Polygon-based hold detection and rendering
- ✅ Interactive hold selection with visual feedback
- ✅ Persistent storage (localStorage)
- ✅ Responsive design (works on any screen size)
- ✅ Accurate click detection using ray casting algorithm
- ✅ Auto-adjusting overlays on browser resize

## Technical Details

- **Frontend**: Compose Multiplatform for Web (Wasm/JS)
- **Image Processing**: Java AWT (BufferedImage)
- **Contour Tracing**: Moore neighborhood boundary tracing algorithm
- **Polygon Simplification**: Douglas-Peucker algorithm
- **Point-in-Polygon**: Ray casting algorithm
- **Data Format**: JSON with kotlinx.serialization

## Project Files

- `composeApp/src/webMain/composeResources/drawable/` - Wall and map images
- `composeApp/src/webMain/composeResources/files/holds.json` - Generated hold configuration
- `composeApp/src/webMain/kotlin/com/wojtek/holds/` - Application source code
- `preprocessor/src/main/kotlin/` - Preprocessor source code

---

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)

For feedback on Compose/Web and Kotlin/Wasm, visit [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web) on Slack.
For issues, report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
