package com.wojtek.holds.components.climbingwall

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.wojtek.holds.model.HoldConfiguration
import kotlin.math.max

/**
 * Generates an off-screen full-resolution ImageBitmap of the climbing wall
 * with all selected holds and masks applied.
 */
fun generateRouteImageBitmap(
    configuration: HoldConfiguration,
    wallImagePainter: Painter,
    selectedHoldIds: Set<Int>,
    density: Density,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    emptyWallImagePainter: Painter? = null,
    showEmptyWall: Boolean = false,
    darkenNonSelected: Boolean = false,
    backgroundDarkeningAlpha: Float = 0.5f,
    showBorders: Boolean = true,
    borderColor: Color = Color.Green,
    maxDimension: Int = 384
): ImageBitmap {
    val originalWidth = configuration.imageWidth.toFloat()
    val originalHeight = configuration.imageHeight.toFloat()

    // 1. Calculate the scale required to fit within maxDimension
    val maxOriginalDimension = max(originalWidth, originalHeight)
    val scaleFactor = if (maxOriginalDimension > maxDimension) {
        maxDimension / maxOriginalDimension
    } else {
        1f
    }

    // 2. Determine target dimensions
    val targetWidth = (originalWidth * scaleFactor).toInt()
    val targetHeight = (originalHeight * scaleFactor).toInt()
    val targetSize = Size(targetWidth.toFloat(), targetHeight.toFloat())

    // 3. Create a smaller canvas directly
    val imageBitmap = ImageBitmap(targetWidth, targetHeight)
    val canvas = Canvas(imageBitmap)

    // 4. Inject the scale into your DisplayParameters
    val displayParams = DisplayParameters(
        scaleX = scaleFactor,
        scaleY = scaleFactor,
        offsetX = 0f,
        offsetY = 0f
    )

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = layoutDirection,
        canvas = canvas,
        size = targetSize
    ) {
        if (showEmptyWall && emptyWallImagePainter != null) {
            with(emptyWallImagePainter) {
                draw(targetSize)
            }

            if (backgroundDarkeningAlpha > 0f) {
                drawRect(
                    color = Color.Black.copy(alpha = backgroundDarkeningAlpha),
                    size = targetSize
                )
            }

            configuration.holds.forEach { hold ->
                if (hold.id in selectedHoldIds) {
                    drawHoldImage(
                        hold = hold,
                        wallImagePainter = wallImagePainter,
                        displayParams = displayParams
                    )
                }
            }
        } else {
            with(wallImagePainter) {
                draw(targetSize)
            }

            // Notice we pass targetWidth and targetHeight here now
            val backgroundMaskPath = createDarkMaskPath(
                holds = configuration.holds,
                displayParams = displayParams,
                canvasWidth = targetWidth.toFloat(),
                canvasHeight = targetHeight.toFloat()
            )

            // Apply the adjustable darkening alpha
            drawPath(
                path = backgroundMaskPath,
                color = Color.Black.copy(alpha = backgroundDarkeningAlpha),
                style = Fill
            )

            // 3. Darken unselected holds if toggled
            if (darkenNonSelected) {
                configuration.holds.forEach { hold ->
                    if (hold.id !in selectedHoldIds) {
                        drawHoldDarkOverlay(
                            hold = hold,
                            displayParams = displayParams
                        )
                    }
                }
            }
        }

        // Draw borders on selected holds
        val shouldShowBorders = if (showEmptyWall) {
            showBorders
        } else {
            showBorders || !darkenNonSelected
        }

        if (shouldShowBorders) {
            configuration.holds.forEach { hold ->
                if (hold.id in selectedHoldIds) {
                    drawHoldBorder(
                        hold = hold,
                        displayParams = displayParams,
                        borderColor = borderColor,
                        strokeWidth = 1f
                    )
                }
            }
        }
    }

    return imageBitmap
}