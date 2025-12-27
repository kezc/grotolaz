# Climbing Wall Components

This directory contains reusable UI components for building interactive climbing wall applications.

## Components

### ClimbingWallView
The core interactive climbing wall component that displays a wall image with hold overlays, handles click detection, and supports zoom/pan operations.

**Key Features:**
- Polygon-based hold rendering
- Click detection with point-in-polygon algorithm
- Zoom and pan functionality
- Customizable colors and styling
- Flexible zoom controls

### ControlPanel
A control panel component for managing hold selections with counter and action buttons.

**Variants:**
- `ControlPanel` - Full-featured with Clear/Save/Load buttons
- `MinimalControlPanel` - Simple counter display only

### ZoomControls
Zoom control UI components with multiple styles.

**Styles:**
- `DefaultZoomControls` - Vertical floating action buttons
- `CompactZoomControls` - Horizontal compact buttons
- `ZoomControlsWithPercentage` - Controls with zoom percentage display

## Usage

See [COMPONENTS.md](../../../../../../../../../COMPONENTS.md) in the project root for detailed usage examples and API documentation.

## Examples

Complete working examples are available in `com.wojtek.holds.examples.CustomClimbingApp.kt`:
- Minimal app
- Advanced app with undo/redo
- Side-by-side comparison app
