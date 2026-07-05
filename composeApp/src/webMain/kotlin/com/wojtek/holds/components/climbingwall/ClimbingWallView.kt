package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.model.Hold
import com.wojtek.holds.model.HoldConfiguration
import com.wojtek.holds.model.Point
import kotlin.math.max
import kotlin.math.min

/**
 * Core interactive climbing wall component.
 *
 * This is the main reusable component that displays a climbing wall image with
 * interactive hold overlays. It handles zooming, panning, and hold selection.
 *
 * @param configuration The hold configuration containing image dimensions and hold data
 * @param wallImagePainter Painter for the wall background image
 * @param selectedHoldIds Set of currently selected hold IDs
 * @param onHoldClick Callback when a hold is clicked
 * @param modifier Optional modifier for the component
 * @param showZoomControls Whether to show zoom controls (default: true)
 * @param selectedColor Color for selected holds (default: Green)
 * @param unselectedColor Color for unselected holds (default: Red)
 * @param selectedAlpha Alpha for selected holds (default: 0.5f)
 * @param unselectedAlpha Alpha for unselected holds (default: 0.3f)
 * @param minZoom Minimum zoom level (default: 1f)
 * @param maxZoom Maximum zoom level (default: 5f)
 * @param zoomStep Zoom step multiplier (default: 1.2f)
 * @param emptyWallImagePainter Optional painter for empty wall (without holds)
 * @param showEmptyWall Whether to show empty wall with only selected holds (default: false)
 * @param darkenNonSelected Whether to darken non-selected holds (default: false)
 * @param showBorders Whether to show borders on selected holds (default: true)
 * @param isLocked Whether clicking holds is disabled (default: false)
 * @param onToggleLock Optional callback when lock toggle is clicked (for floating controls)
 * @param onToggleEmptyWall Optional callback when empty wall toggle is clicked (for floating controls)
 * @param onToggleDarkenNonSelected Optional callback when darken toggle is clicked (for floating controls)
 * @param onToggleBorders Optional callback when border toggle is clicked (for floating controls)
 */
@Composable
fun ClimbingWallView(
    state: ClimbingWallState,
    configuration: HoldConfiguration,
    wallImagePainter: Painter,
    modifier: Modifier = Modifier,
    showZoomControls: Boolean = true,
    selectedColor: Color = Color.Green,
    unselectedColor: Color = Color.Red,
    selectedAlpha: Float = 0.5f,
    unselectedAlpha: Float = 0.3f,
    minZoom: Float = 1f,
    maxZoom: Float = 7.5f,
    zoomStep: Float = 1.2f,
    emptyWallImagePainter: Painter? = null,
    problemsRepository: ProblemRepository,
    showSaveDialog: (Problem) -> Unit,
    showProblemsDialog: () -> Unit
) {
    // Calculate display parameters based on container size (without zoom/pan)
    val displayParams = remember(state.containerSize, configuration) {
        calculateDisplayParameters(state.containerSize, configuration)
    }

    // Pre-calculate and cache the dark mask path for background
    // Only exclude all holds from this mask (not dependent on selection)
    val backgroundMaskPath = remember(displayParams, configuration, state.containerSize) {
        if (displayParams.isValid && state.containerSize.width > 0 && state.containerSize.height > 0) {
            createDarkMaskPath(
                holds = configuration.holds,
                displayParams = displayParams,
                canvasWidth = state.containerSize.width.toFloat(),
                canvasHeight = state.containerSize.height.toFloat()
            )
        } else {
            null
        }
    }

    val currentDisplayParams by rememberUpdatedState(displayParams)
    val currentConfiguration by rememberUpdatedState(configuration)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size -> state.containerSize = size },
        contentAlignment = Alignment.Center
    ) {
        // Content box that contains both image and overlays with zoom and pan applied
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformAndTapGestures(
                        panZoomLock = false,
                        onTap = { tapOffset ->
                            if (!state.isLocked) {
                                // Convert tapOffset from unscaled/unpanned screen space to the local scaled/panned space of the holds
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val localTapOffset = Offset(
                                    x = (tapOffset.x - centerX) / state.scale + centerX - state.offsetX,
                                    y = (tapOffset.y - centerY) / state.scale + centerY - state.offsetY
                                )

                                val hold = findClickedHold(
                                    tapOffset = localTapOffset,
                                    holds = currentConfiguration.holds,
                                    displayParams = currentDisplayParams
                                )
                                hold?.let { state.toggleHold(it.id) }
                            }
                        },
                        onGesture = { centroid, pan, zoom, rotation ->
                            val oldScale = state.scale

                            // Apply zoom with constraints
                            state.scale = (state.scale * zoom).coerceIn(minZoom, maxZoom)

                            // Calculate zoom factor applied
                            val zoomFactor = state.scale / oldScale

                            // Handle zoom transformation
                            if (zoomFactor != 1f) {
                                // Calculate the difference between centroid and current center
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val deltaX = centroid.x - centerX
                                val deltaY = centroid.y - centerY

                                // Adjust offsets to zoom towards the gesture centroid (using unscaled screen-space math)
                                val localDeltaX = deltaX / oldScale
                                val localDeltaY = deltaY / oldScale
                                state.offsetX = state.offsetX - localDeltaX * (1f - 1f / zoomFactor)
                                state.offsetY = state.offsetY - localDeltaY * (1f - 1f / zoomFactor)
                            } else {
                                // Only apply pan when not zooming (pure drag gesture)
                                // We divide by state.scale because the offset modifier is placed after the graphicsLayer scale modifier
                                state.offsetX += pan.x / state.scale
                                state.offsetY += pan.y / state.scale
                            }

                            // Reset pan when at minimum zoom
                            if (state.scale <= minZoom) {
                                state.offsetX = 0f
                                state.offsetY = 0f
                            }
                        }
                    )
                }
                .graphicsLayer(
                    scaleX = state.scale,
                    scaleY = state.scale,
                    clip = false
                )
                .offset {
                    IntOffset(state.offsetX.toInt(), state.offsetY.toInt())
                }
        ) {
            // Background empty wall image when in empty wall mode
            if (state.showEmptyWall && emptyWallImagePainter != null) {
                Image(
                    painter = emptyWallImagePainter,
                    contentDescription = "Empty climbing wall",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Full wall image in normal mode
            if (!state.showEmptyWall) {
                Image(
                    painter = wallImagePainter,
                    contentDescription = "Climbing wall",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Clipped hold images in empty wall mode
            if (state.showEmptyWall && displayParams.isValid) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    configuration.holds.forEach { hold ->
                        if (hold.id in state.selectedHoldIds) {
                            drawHoldImage(
                                hold = hold,
                                wallImagePainter = wallImagePainter,
                                displayParams = displayParams
                            )
                        }
                    }
                }
            }

            // Hold overlays (shown in normal mode only, or as highlights in empty wall mode)
            if (displayParams.isValid) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (!state.showEmptyWall) {
                        // First, draw a dark mask over the background (excluding all holds)
                        if (backgroundMaskPath != null) {
                            drawPath(
                                path = backgroundMaskPath,
                                color = Color.Black.copy(alpha = 0.4f),
                                style = Fill
                            )
                        }

                        // If darkening non-selected holds, draw dark overlays on them
                        if (state.darkenNonSelected) {
                            configuration.holds.forEach { hold ->
                                if (hold.id !in state.selectedHoldIds) {
                                    drawHoldDarkOverlay(
                                        hold = hold,
                                        displayParams = displayParams
                                    )
                                }
                            }
                        }
                    }

                    // Draw borders on selected holds
                    // In normal mode: always show borders when darken non-selected is off
                    // In empty wall mode: respect the showBorders setting
                    val shouldShowBorders = if (state.showEmptyWall) {
                        state.showBorders
                    } else {
                        state.showBorders || !state.darkenNonSelected
                    }

                    if (shouldShowBorders) {
                        configuration.holds.forEach { hold ->
                            if (hold.id in state.selectedHoldIds) {
                                drawHoldBorder(
                                    hold = hold,
                                    displayParams = displayParams,
                                    borderColor = selectedColor,
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }
                }
            }
        }

        // Zoom controls
        if (showZoomControls) {
            val zoomState = ZoomState(scale = state.scale)
            val zoomCallbacks = ZoomCallbacks(
                onZoomIn = {
                    state.zoomIn(zoomStep, maxZoom)
                },
                onZoomOut = {
                    state.zoomOut(zoomStep, minZoom)
                },
                onReset = {
                    state.resetZoomAndPan()
                }
            )

            FloatingControls(
                zoomState = zoomState,
                zoomCallbacks = zoomCallbacks,
                isLocked = state.isLocked,
                onToggleLock = { state.toggleLock() },
                showEmptyWall = state.showEmptyWall,
                onToggleEmptyWall = { state.toggleEmptyWall() },
                darkenNonSelected = state.darkenNonSelected,
                onToggleDarkenNonSelected = { state.toggleDarkenNonSelected() },
                showBorders = state.showBorders,
                onToggleBorders = { state.toggleBorders() },
                problemsRepository = problemsRepository,
                version = configuration.version,
                selectedHoldsId = state.selectedHoldIds,
                showSaveDialog = showSaveDialog,
                showProblemsDialog = showProblemsDialog,
                onClearSelectedHolds = { state.clearSelectedHolds() }
            )
        }
    }
}

/**
 * State holder for zoom level.
 */
data class ZoomState(
    val scale: Float
)

/**
 * Callbacks for zoom operations.
 */
data class ZoomCallbacks(
    val onZoomIn: () -> Unit,
    val onZoomOut: () -> Unit,
    val onReset: () -> Unit
)

/**
 * Parameters for displaying the scaled and positioned image.
 */
internal data class DisplayParameters(
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
internal fun calculateDisplayParameters(
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
 * Finds which hold was clicked based on tap position.
 */
internal fun findClickedHold(
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
internal fun isPointInHold(
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
internal fun isPointInBoundingBox(
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
internal fun isPointInPolygon(
    point: Offset,
    polygon: List<Point>,
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
 * Creates a dark mask path that covers the entire canvas except where holds are located.
 * This is cached and only recalculated when display parameters or configuration changes.
 */
internal fun createDarkMaskPath(
    holds: List<Hold>,
    displayParams: DisplayParameters,
    canvasWidth: Float,
    canvasHeight: Float
): Path {
    // Create a path covering the entire canvas
    val fullCanvasPath = Path().apply {
        addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
    }

    // Create a combined path of all holds by appending their coordinates directly
    val holdsPaths = Path()
    holds.forEach { hold ->
        if (hold.polygon.isNotEmpty()) {
            val firstPoint = hold.polygon.first()
            holdsPaths.moveTo(
                firstPoint.x * displayParams.scaleX + displayParams.offsetX,
                firstPoint.y * displayParams.scaleY + displayParams.offsetY
            )
            hold.polygon.drop(1).forEach { point ->
                holdsPaths.lineTo(
                    point.x * displayParams.scaleX + displayParams.offsetX,
                    point.y * displayParams.scaleY + displayParams.offsetY
                )
            }
            holdsPaths.close()
        } else {
            holdsPaths.addRect(
                Rect(
                    left = hold.x * displayParams.scaleX + displayParams.offsetX,
                    top = hold.y * displayParams.scaleY + displayParams.offsetY,
                    right = (hold.x + hold.width) * displayParams.scaleX + displayParams.offsetX,
                    bottom = (hold.y + hold.height) * displayParams.scaleY + displayParams.offsetY
                )
            )
        }
    }

    // Subtract holds from the full canvas to get the mask area
    val maskPath = Path()
    maskPath.op(fullCanvasPath, holdsPaths, PathOperation.Difference)

    return maskPath
}

/**
 * Draws a dark overlay on a hold (for darkening non-selected holds).
 */
internal fun DrawScope.drawHoldDarkOverlay(
    hold: Hold,
    displayParams: DisplayParameters
) {
    if (hold.polygon.isNotEmpty()) {
        val path = createHoldPath(hold.polygon, displayParams)
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.4f),
            style = Fill
        )
    } else {
        val topLeft = Offset(
            x = hold.x * displayParams.scaleX + displayParams.offsetX,
            y = hold.y * displayParams.scaleY + displayParams.offsetY
        )
        val size = Size(
            width = hold.width * displayParams.scaleX,
            height = hold.height * displayParams.scaleY
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = topLeft,
            size = size,
            style = Fill
        )
    }
}

/**
 * Draws a subtle border on a hold.
 */
internal fun DrawScope.drawHoldBorder(
    hold: Hold,
    displayParams: DisplayParameters,
    borderColor: Color,
    strokeWidth: Float
) {
    if (hold.polygon.isNotEmpty()) {
        val path = createHoldPath(hold.polygon, displayParams)
        drawPath(
            path = path,
            color = borderColor,
            style = Stroke(width = strokeWidth)
        )
    } else {
        val topLeft = Offset(
            x = hold.x * displayParams.scaleX + displayParams.offsetX,
            y = hold.y * displayParams.scaleY + displayParams.offsetY
        )
        val size = Size(
            width = hold.width * displayParams.scaleX,
            height = hold.height * displayParams.scaleY
        )
        drawRect(
            color = borderColor,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Creates a Path from polygon points with scaling and offset.
 */
internal fun createHoldPath(
    polygon: List<Point>,
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
 * Draws a clipped portion of the wall image for a specific hold.
 * This is used in empty wall mode to show only the selected holds from the full wall image.
 */
internal fun DrawScope.drawHoldImage(
    hold: Hold,
    wallImagePainter: Painter,
    displayParams: DisplayParameters
) {
    if (hold.polygon.isNotEmpty()) {
        // Use polygon clipping for accurate hold shape
        val path = createHoldPath(hold.polygon, displayParams)
        drawWallImage(path, displayParams, wallImagePainter)
    } else {
        // Fallback to bounding box clipping
        val rect = Rect(
            left = hold.x * displayParams.scaleX + displayParams.offsetX,
            top = hold.y * displayParams.scaleY + displayParams.offsetY,
            right = (hold.x + hold.width) * displayParams.scaleX + displayParams.offsetX,
            bottom = (hold.y + hold.height) * displayParams.scaleY + displayParams.offsetY
        )
        val path = Path().apply {
            addRect(rect)
        }
        drawWallImage(path, displayParams, wallImagePainter)
    }
}

private fun DrawScope.drawWallImage(
    path: Path,
    displayParams: DisplayParameters,
    wallImagePainter: Painter
) {
    clipPath(path) {
        translate(displayParams.offsetX, displayParams.offsetY) {
            with(wallImagePainter) {
                val drawWidth = intrinsicSize.width * displayParams.scaleX
                val drawHeight = intrinsicSize.height * displayParams.scaleY
                draw(Size(drawWidth, drawHeight))
            }
        }
    }
}

private suspend fun PointerInputScope.detectTransformAndTapGestures(
    panZoomLock: Boolean = false,
    onTap: (Offset) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val slop = 5f // Tight 5px threshold to separate clicks from drags
        var lockedToPanOrZoom = false
        
        var isTapCandidate = true
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val firstDownPosition = firstDown.position

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                if (event.changes.size > 1) {
                    isTapCandidate = false
                }

                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                    val rotationMotion = kotlin.math.abs(rotation) * 100f 
                    val panMotion = pan.getDistance()

                    if (panMotion > slop) {
                        isTapCandidate = false
                    }

                    if (zoomMotion > slop ||
                        rotationMotion > slop ||
                        panMotion > slop
                    ) {
                        pastTouchSlop = true
                        lockedToPanOrZoom = panZoomLock && (zoomMotion > slop || rotationMotion > slop)
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanOrZoom) 0f else rotationChange
                    val effectiveZoom = if (lockedToPanOrZoom) 1f else zoomChange
                    val effectivePan = if (lockedToPanOrZoom) Offset.Zero else panChange

                    if (effectiveRotation != 0f ||
                        effectiveZoom != 1f ||
                        effectivePan != Offset.Zero
                    ) {
                        onGesture(centroid, effectivePan, effectiveZoom, effectiveRotation)
                    }
                    event.changes.forEach {
                        if (it.previousPosition != it.position) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (isTapCandidate && !pastTouchSlop) {
            onTap(firstDownPosition)
        }
    }
}
