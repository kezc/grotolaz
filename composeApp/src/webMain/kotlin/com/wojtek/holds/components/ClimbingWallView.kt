package com.wojtek.holds.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.IntSize
import com.wojtek.holds.model.Hold
import com.wojtek.holds.model.HoldConfiguration
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
 * @param zoomControlsContent Optional custom zoom controls composable
 * @param emptyWallImagePainter Optional painter for empty wall (without holds)
 * @param showEmptyWall Whether to show empty wall with only selected holds (default: false)
 */
@Composable
fun ClimbingWallView(
    configuration: HoldConfiguration,
    wallImagePainter: Painter,
    selectedHoldIds: Set<Int>,
    onHoldClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showZoomControls: Boolean = true,
    selectedColor: Color = Color.Green,
    unselectedColor: Color = Color.Red,
    selectedAlpha: Float = 0.5f,
    unselectedAlpha: Float = 0.3f,
    minZoom: Float = 1f,
    maxZoom: Float = 5f,
    zoomStep: Float = 1.2f,
    zoomControlsContent: @Composable ((ZoomState, ZoomCallbacks) -> Unit)? = null,
    emptyWallImagePainter: Painter? = null,
    showEmptyWall: Boolean = false
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
        modifier = modifier
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
            // Background empty wall image when in empty wall mode
            if (showEmptyWall && emptyWallImagePainter != null) {
                Image(
                    painter = emptyWallImagePainter,
                    contentDescription = "Empty climbing wall",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Full wall image in normal mode
            if (!showEmptyWall) {
                Image(
                    painter = wallImagePainter,
                    contentDescription = "Climbing wall",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Clipped hold images in empty wall mode
            if (showEmptyWall && displayParams.isValid) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    configuration.holds.forEach { hold ->
                        if (hold.id in selectedHoldIds) {
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
                        .pointerInput(displayParams, scale) {
                            detectTapGestures { tapOffset ->
                                findClickedHold(
                                    tapOffset = tapOffset,
                                    holds = configuration.holds,
                                    displayParams = displayParams
                                )?.let { onHoldClick(it.id) }
                            }
                        }
                ) {
                    configuration.holds.forEach { hold ->
                        // In empty wall mode, only show overlays for selected holds
                        val shouldDraw = if (showEmptyWall) {
                            hold.id in selectedHoldIds
                        } else {
                            true
                        }

                        if (shouldDraw) {
                            drawHoldOverlay(
                                hold = hold,
                                isSelected = hold.id in selectedHoldIds,
                                displayParams = displayParams,
                                selectedColor = selectedColor,
                                unselectedColor = unselectedColor,
                                selectedAlpha = selectedAlpha,
                                unselectedAlpha = unselectedAlpha
                            )
                        }
                    }
                }
            }
        }

        // Zoom controls
        if (showZoomControls) {
            val zoomState = ZoomState(scale = scale)
            val zoomCallbacks = ZoomCallbacks(
                onZoomIn = {
                    scale = min(scale * zoomStep, maxZoom)
                },
                onZoomOut = {
                    scale = max(scale / zoomStep, minZoom)
                    // Reset pan when zooming out to minimum
                    if (scale <= minZoom) {
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

            if (zoomControlsContent != null) {
                zoomControlsContent(zoomState, zoomCallbacks)
            } else {
                DefaultZoomControls(zoomState, zoomCallbacks)
            }
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
internal fun DrawScope.drawHoldOverlay(
    hold: Hold,
    isSelected: Boolean,
    displayParams: DisplayParameters,
    selectedColor: Color,
    unselectedColor: Color,
    selectedAlpha: Float,
    unselectedAlpha: Float
) {
    val color = if (isSelected) selectedColor else unselectedColor
    val alpha = if (isSelected) selectedAlpha else unselectedAlpha

    if (hold.polygon.isNotEmpty()) {
        drawPolygonHold(hold, color, alpha, displayParams)
    } else {
        drawRectangleHold(hold, color, alpha, displayParams)
    }
}

/**
 * Draws a polygon-based hold overlay.
 */
internal fun DrawScope.drawPolygonHold(
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
internal fun DrawScope.drawRectangleHold(
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
internal fun createHoldPath(
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
        clipPath(path) {
            // Draw the full wall image positioned correctly
            // The image needs to be drawn at its display size and position
            translate(displayParams.offsetX, displayParams.offsetY) {
                with(wallImagePainter) {
                    // Calculate the size the image should be drawn at based on intrinsic size and scale
                    val drawWidth = intrinsicSize.width * displayParams.scaleX
                    val drawHeight = intrinsicSize.height * displayParams.scaleY
                    draw(Size(drawWidth, drawHeight))
                }
            }
        }
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
}
