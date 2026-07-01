package com.wojtek.holds.components.climbingwall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import com.wojtek.holds.model.HoldConfiguration
import com.wojtek.holds.utils.ConfigurationLoadResult
import com.wojtek.holds.utils.loadHoldConfiguration
import com.wojtek.holds.utils.loadVersionedImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for the climbing wall view and controls.
 * Encapsulates zoom, pan, layout size, hold selection, and visual/interaction options.
 */
class ClimbingWallState(private val scope: CoroutineScope) {
    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var containerSize by mutableStateOf(IntSize.Zero)

    var selectedHoldIds by mutableStateOf<Set<Int>>(emptySet())
    var showEmptyWall by mutableStateOf(false)
    var darkenNonSelected by mutableStateOf(false)
    var showBorders by mutableStateOf(true)
    var isLocked by mutableStateOf(false)

    // Configuration and Painters for image generation
    var configuration by mutableStateOf<HoldConfiguration?>(null)
    var wallPainter by mutableStateOf<Painter?>(null)
    var emptyPainter by mutableStateOf<Painter?>(null)

    var loadResult by mutableStateOf<ConfigurationLoadResult>(ConfigurationLoadResult.Loading)
        private set

    fun loadVersion(version: String) {
        val currentVer = configuration?.version
        if (currentVer == version && wallPainter != null && emptyPainter != null) {
            return
        }

        loadResult = ConfigurationLoadResult.Loading
        resetZoomAndPan()

        scope.launch {
            val configResult = loadHoldConfiguration(version)
            if (configResult is ConfigurationLoadResult.Success) {
                val config = configResult.configuration
                val wall = loadVersionedImage(version, "wall.png")
                val empty = loadVersionedImage(version, "empty.png")
                if (wall != null && empty != null) {
                    configuration = config
                    wallPainter = wall
                    emptyPainter = empty
                    loadResult = configResult
                } else {
                    loadResult = ConfigurationLoadResult.Error("Failed to load wall images for version '$version'")
                }
            } else {
                loadResult = configResult
            }
        }
    }

    fun toggleHold(holdId: Int) {
        if (!isLocked) {
            selectedHoldIds = if (holdId in selectedHoldIds) {
                selectedHoldIds - holdId
            } else {
                selectedHoldIds + holdId
            }
        }
    }

    fun toggleLock() {
        isLocked = !isLocked
    }

    fun toggleEmptyWall() {
        showEmptyWall = !showEmptyWall
        if (showEmptyWall) {
            darkenNonSelected = false
        }
    }

    fun toggleDarkenNonSelected() {
        if (!showEmptyWall) {
            darkenNonSelected = !darkenNonSelected
        }
    }

    fun toggleBorders() {
        if (darkenNonSelected || showEmptyWall) {
            showBorders = !showBorders
        }
    }

    fun zoomIn(zoomStep: Float = 1.2f, maxZoom: Float = 5f) {
        scale = (scale * zoomStep).coerceAtMost(maxZoom)
    }

    fun zoomOut(zoomStep: Float = 1.2f, minZoom: Float = 1f) {
        scale = (scale / zoomStep).coerceAtLeast(minZoom)
        if (scale <= minZoom) {
            resetPan()
        }
    }

    fun resetZoomAndPan() {
        scale = 1f
        resetPan()
    }

    private fun resetPan() {
        offsetX = 0f
        offsetY = 0f
    }
}
