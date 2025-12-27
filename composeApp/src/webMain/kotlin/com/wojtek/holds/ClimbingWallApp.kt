package com.wojtek.holds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import com.wojtek.holds.model.Hold
import com.wojtek.holds.model.HoldConfiguration
import holds.composeapp.generated.resources.Res
import holds.composeapp.generated.resources.wall
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Main application composable for the climbing wall hold tracker.
 *
 * Loads hold configuration from resources and manages hold selection state.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ClimbingWallApp() {
    var holdConfiguration by remember { mutableStateOf<HoldConfiguration?>(null) }
    var selectedHoldIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load hold configuration on first composition
    LaunchedEffect(Unit) {
        loadHoldConfiguration(
            onSuccess = { config ->
                holdConfiguration = config
                isLoading = false
                // Load selected holds from URL after config is loaded
                val holdsFromUrl = UrlSync.decodeFromUrl()
                if (holdsFromUrl.isNotEmpty()) {
                    selectedHoldIds = holdsFromUrl
                }
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    // Sync URL when selected holds change
    LaunchedEffect(selectedHoldIds) {
        if (!isLoading && holdConfiguration != null) {
            UrlSync.encodeToUrl(selectedHoldIds)
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> LoadingIndicator()
                errorMessage != null -> ErrorDisplay(errorMessage!!)
                holdConfiguration != null -> ClimbingWallContent(
                    configuration = holdConfiguration!!,
                    selectedHoldIds = selectedHoldIds,
                    onClearClick = { selectedHoldIds = emptySet() },
                    onSaveClick = {
                        scope.launch {
                            saveSelectedHolds(selectedHoldIds.toList())
                        }
                    },
                    onLoadClick = {
                        scope.launch {
                            loadSelectedHolds()?.let { loaded ->
                                selectedHoldIds = loaded.toSet()
                            }
                        }
                    },
                    onHoldClick = { holdId ->
                        selectedHoldIds = if (holdId in selectedHoldIds) {
                            selectedHoldIds - holdId
                        } else {
                            selectedHoldIds + holdId
                        }
                    }
                )
            }
        }
    }
}

/**
 * Loads the hold configuration from resources.
 */
@OptIn(ExperimentalResourceApi::class)
private suspend fun loadHoldConfiguration(
    onSuccess: (HoldConfiguration) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val configText = Res.readBytes("files/holds.json").decodeToString()
        val config = Json.decodeFromString<HoldConfiguration>(configText)
        onSuccess(config)
    } catch (e: Exception) {
        onError("Failed to load holds configuration: ${e.message}")
    }
}

/**
 * Loading indicator composable.
 */
@Composable
private fun BoxScope.LoadingIndicator() {
    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
}

/**
 * Error display composable.
 */
@Composable
private fun BoxScope.ErrorDisplay(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.align(Alignment.Center)
    )
}

/**
 * Main content showing the control panel and climbing wall.
 */
@Composable
private fun ClimbingWallContent(
    configuration: HoldConfiguration,
    selectedHoldIds: Set<Int>,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit,
    onLoadClick: () -> Unit,
    onHoldClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ControlPanel(
            selectedCount = selectedHoldIds.size,
            totalCount = configuration.holds.size,
            onClearClick = onClearClick,
            onSaveClick = onSaveClick,
            onLoadClick = onLoadClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clipToBounds()
        ) {
            InteractiveClimbingWall(
                configuration = configuration,
                selectedHoldIds = selectedHoldIds,
                onHoldClick = onHoldClick
            )
        }
    }
}

/**
 * Control panel with selection counter and action buttons.
 */
@Composable
private fun ControlPanel(
    selectedCount: Int,
    totalCount: Int,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit,
    onLoadClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected: $selectedCount / $totalCount",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClearClick) { Text("Clear") }
                Button(onClick = onSaveClick) { Text("Save") }
                Button(onClick = onLoadClick) { Text("Load") }
            }
        }
    }
}

/**
 * Interactive climbing wall display with hold overlays.
 *
 * Displays the wall image with polygon overlays for each hold.
 * Handles click detection, zoom, pan, and responsive resizing.
 */
@Composable
private fun InteractiveClimbingWall(
    configuration: HoldConfiguration,
    selectedHoldIds: Set<Int>,
    onHoldClick: (Int) -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Calculate display parameters based on container size (without zoom/pan)
    val displayParams = remember(containerSize, configuration) {
        calculateDisplayParameters(containerSize, configuration)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> containerSize = size },
        contentAlignment = Alignment.Center
    ) {
        // Content box that contains both image and overlays with zoom and pan applied
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    clip = true
                )
                .pointerInput(scale) {
                    detectDragGestures { change, dragAmount ->
                        if (scale > 1f) {
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                }
        ) {
            // Background wall image
            WallImage()

            // Hold overlays
            if (displayParams.isValid) {
                HoldOverlays(
                    configuration = configuration,
                    selectedHoldIds = selectedHoldIds,
                    displayParams = displayParams,
                    scale = scale,
                    onHoldClick = onHoldClick
                )
            }
        }

        // Zoom controls
        ZoomControls(
            scale = scale,
            onZoomIn = {
                scale = min(scale * 1.2f, 5f)
            },
            onZoomOut = {
                scale = max(scale / 1.2f, 1f)
                // Reset pan when zooming out to minimum
                if (scale <= 1f) {
                    offsetX = 0f
                    offsetY = 0f
                }
            },
            onReset = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            }
        )
    }
}

/**
 * Wall image background.
 */
@Composable
private fun WallImage() {
    Image(
        painter = painterResource(Res.drawable.wall),
        contentDescription = "Climbing wall",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Parameters for displaying the scaled and positioned image.
 */
private data class DisplayParameters(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float
) {
    val isValid: Boolean
        get() = scaleX > 0 && scaleY > 0
}

/**
 * Calculates display parameters based on container size and image dimensions.
 */
private fun calculateDisplayParameters(
    containerSize: IntSize,
    configuration: HoldConfiguration
): DisplayParameters {
    if (containerSize.width == 0 || containerSize.height == 0) {
        return DisplayParameters(0f, 0f, 0f, 0f)
    }

    val imageAspectRatio = configuration.imageWidth.toFloat() / configuration.imageHeight
    val containerAspectRatio = containerSize.width.toFloat() / containerSize.height

    val (displayedWidth, displayedHeight, offsetX, offsetY) = when {
        containerAspectRatio > imageAspectRatio -> {
            // Container is wider - image is constrained by height
            val height = containerSize.height.toFloat()
            val width = height * imageAspectRatio
            val xOffset = (containerSize.width - width) / 2f
            listOf(width, height, xOffset, 0f)
        }
        else -> {
            // Container is taller - image is constrained by width
            val width = containerSize.width.toFloat()
            val height = width / imageAspectRatio
            val yOffset = (containerSize.height - height) / 2f
            listOf(width, height, 0f, yOffset)
        }
    }

    return DisplayParameters(
        scaleX = displayedWidth / configuration.imageWidth,
        scaleY = displayedHeight / configuration.imageHeight,
        offsetX = offsetX,
        offsetY = offsetY
    )
}

/**
 * Canvas with hold overlays and click detection.
 */
@Composable
private fun HoldOverlays(
    configuration: HoldConfiguration,
    selectedHoldIds: Set<Int>,
    displayParams: DisplayParameters,
    scale: Float,
    onHoldClick: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(displayParams, scale) {
                detectTapGestures { tapOffset ->
                    // Only process clicks when not significantly zoomed
                    // or adjust logic if needed
                    findClickedHold(
                        tapOffset = tapOffset,
                        holds = configuration.holds,
                        displayParams = displayParams
                    )?.let { onHoldClick(it.id) }
                }
            }
    ) {
        configuration.holds.forEach { hold ->
            drawHoldOverlay(
                hold = hold,
                isSelected = hold.id in selectedHoldIds,
                displayParams = displayParams
            )
        }
    }
}

/**
 * Finds which hold was clicked based on tap position.
 */
private fun findClickedHold(
    tapOffset: Offset,
    holds: List<Hold>,
    displayParams: DisplayParameters
): Hold? {
    val adjustedOffset = Offset(
        x = tapOffset.x - displayParams.offsetX,
        y = tapOffset.y - displayParams.offsetY
    )

    return holds.find { hold ->
        isPointInHold(adjustedOffset, hold, displayParams)
    }
}

/**
 * Checks if a point is inside a hold using polygon or bounding box.
 */
private fun isPointInHold(
    point: Offset,
    hold: Hold,
    displayParams: DisplayParameters
): Boolean {
    return if (hold.polygon.isNotEmpty()) {
        isPointInPolygon(point, hold.polygon, displayParams.scaleX, displayParams.scaleY)
    } else {
        isPointInBoundingBox(point, hold, displayParams.scaleX, displayParams.scaleY)
    }
}

/**
 * Checks if a point is inside a bounding box.
 */
private fun isPointInBoundingBox(
    point: Offset,
    hold: Hold,
    scaleX: Float,
    scaleY: Float
): Boolean {
    val rect = Rect(
        left = hold.x * scaleX,
        top = hold.y * scaleY,
        right = (hold.x + hold.width) * scaleX,
        bottom = (hold.y + hold.height) * scaleY
    )
    return rect.contains(point)
}

/**
 * Point-in-polygon test using ray casting algorithm.
 */
private fun isPointInPolygon(
    point: Offset,
    polygon: List<com.wojtek.holds.model.Point>,
    scaleX: Float,
    scaleY: Float
): Boolean {
    if (polygon.size < 3) return false

    var inside = false
    var j = polygon.size - 1

    for (i in polygon.indices) {
        val xi = polygon[i].x * scaleX
        val yi = polygon[i].y * scaleY
        val xj = polygon[j].x * scaleX
        val yj = polygon[j].y * scaleY

        val intersect = ((yi > point.y) != (yj > point.y)) &&
                (point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi)

        if (intersect) inside = !inside
        j = i
    }

    return inside
}

/**
 * Draws a single hold overlay (polygon or rectangle).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoldOverlay(
    hold: Hold,
    isSelected: Boolean,
    displayParams: DisplayParameters
) {
    val color = if (isSelected) Color.Green else Color.Red
    val alpha = if (isSelected) 0.5f else 0.3f

    if (hold.polygon.isNotEmpty()) {
        drawPolygonHold(hold, color, alpha, displayParams)
    } else {
        drawRectangleHold(hold, color, alpha, displayParams)
    }
}

/**
 * Draws a polygon-based hold overlay.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolygonHold(
    hold: Hold,
    color: Color,
    alpha: Float,
    displayParams: DisplayParameters
) {
    val path = createHoldPath(hold.polygon, displayParams)

    // Fill
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Fill
    )

    // Border
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f)
    )
}

/**
 * Draws a rectangle-based hold overlay (fallback).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectangleHold(
    hold: Hold,
    color: Color,
    alpha: Float,
    displayParams: DisplayParameters
) {
    val topLeft = Offset(
        x = hold.x * displayParams.scaleX + displayParams.offsetX,
        y = hold.y * displayParams.scaleY + displayParams.offsetY
    )
    val size = Size(
        width = hold.width * displayParams.scaleX,
        height = hold.height * displayParams.scaleY
    )

    // Fill
    drawRect(
        color = color.copy(alpha = alpha),
        topLeft = topLeft,
        size = size
    )

    // Border
    drawRect(
        color = color,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = 2f)
    )
}

/**
 * Creates a Path from polygon points with scaling and offset.
 */
private fun createHoldPath(
    polygon: List<com.wojtek.holds.model.Point>,
    displayParams: DisplayParameters
): Path {
    return Path().apply {
        if (polygon.isEmpty()) return@apply

        val firstPoint = polygon.first()
        moveTo(
            firstPoint.x * displayParams.scaleX + displayParams.offsetX,
            firstPoint.y * displayParams.scaleY + displayParams.offsetY
        )

        polygon.drop(1).forEach { point ->
            lineTo(
                point.x * displayParams.scaleX + displayParams.offsetX,
                point.y * displayParams.scaleY + displayParams.offsetY
            )
        }

        close()
    }
}

/**
 * Zoom controls overlay with buttons for zoom in, zoom out, and reset.
 */
@Composable
private fun BoxScope.ZoomControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom In button
        FloatingActionButton(
            onClick = onZoomIn,
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
            onClick = onZoomOut,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out"
            )
        }

        // Reset button (only show when zoomed)
        if (scale > 1f) {
            FloatingActionButton(
                onClick = onReset,
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

// Note: saveSelectedHolds and loadSelectedHolds are defined in LocalStorage.kt
