package com.wojtek.holds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.wojtek.holds.Constants.DEFAULT_VERSION
import com.wojtek.holds.components.climbingwall.ClimbingWallView
import com.wojtek.holds.components.climbingwall.SaveDialog
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.model.HoldConfiguration
import com.wojtek.holds.utils.ConfigurationLoadResult
import com.wojtek.holds.utils.rememberHoldConfiguration
import com.wojtek.holds.utils.rememberVersionedImage

/**
 * Main application composable for the climbing wall hold tracker.
 *
 * Loads hold configuration from resources and manages hold selection state.
 * This is a reference implementation using all the reusable components.
 */
@Composable
fun ClimbingWallApp(
    problemsRepository: ProblemRepository
) {
    var selectedHoldIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showEmptyWall by remember { mutableStateOf(false) }
    var darkenNonSelected by remember { mutableStateOf(false) }
    var showBorders by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Get version from URL, default to DEFAULT_VERSION if not present
    val (urlVersion, urlHolds) = remember { UrlSync.decodeFromUrl() }
    val version = urlVersion ?: DEFAULT_VERSION

    // Load configuration for the requested version
    val configurationResult = rememberHoldConfiguration(version = version)

    // Load versioned images
    val wallImageState = rememberVersionedImage(version, "wall.png")
    val emptyImageState = rememberVersionedImage(version, "empty.png")

    // When show selected only is turned on, automatically turn off darken non-selected
    LaunchedEffect(showEmptyWall) {
        if (showEmptyWall) {
            darkenNonSelected = false
        }
    }

    // Load selected holds from URL after config is loaded
    LaunchedEffect(configurationResult.value) {
        if (configurationResult.value is ConfigurationLoadResult.Success && urlHolds.isNotEmpty()) {
            selectedHoldIds = urlHolds
        }
    }

    // Sync URL when selected holds change
    LaunchedEffect(selectedHoldIds) {
        if (configurationResult.value is ConfigurationLoadResult.Success) {
            val config = (configurationResult.value as ConfigurationLoadResult.Success).configuration
            UrlSync.encodeToUrl(selectedHoldIds, config.version)
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = configurationResult.value) {
                is ConfigurationLoadResult.Loading -> LoadingIndicator()
                is ConfigurationLoadResult.Error -> ErrorDisplay(result.message)
                is ConfigurationLoadResult.Success -> {
                    val wallPainter = wallImageState.value
                    val emptyPainter = emptyImageState.value

                    if (wallPainter != null && emptyPainter != null) {
                        ClimbingWallContent(
                            configuration = result.configuration,
                            wallPainter = wallPainter,
                            emptyPainter = emptyPainter,
                            selectedHoldIds = selectedHoldIds,
                            showEmptyWall = showEmptyWall,
                            darkenNonSelected = darkenNonSelected,
                            showBorders = showBorders,
                            isLocked = isLocked,
                            onToggleEmptyWall = { showEmptyWall = !showEmptyWall },
                            onToggleDarkenNonSelected = { darkenNonSelected = !darkenNonSelected },
                            onToggleBorders = { showBorders = !showBorders },
                            onToggleLock = { isLocked = !isLocked },
                            onHoldClick = { holdId ->
                                if (!isLocked) {
                                    selectedHoldIds = if (holdId in selectedHoldIds) {
                                        selectedHoldIds - holdId
                                    } else {
                                        selectedHoldIds + holdId
                                    }
                                }
                            },
                            problemsRepository = problemsRepository,
                        )
                    } else {
                        LoadingIndicator()
                    }
                }
            }
        }
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
 * Main content showing the climbing wall with floating controls.
 */
@Composable
private fun ClimbingWallContent(
    configuration: HoldConfiguration,
    wallPainter: Painter,
    emptyPainter: Painter,
    selectedHoldIds: Set<Int>,
    showEmptyWall: Boolean,
    darkenNonSelected: Boolean,
    showBorders: Boolean,
    isLocked: Boolean,
    onToggleEmptyWall: () -> Unit,
    onToggleDarkenNonSelected: () -> Unit,
    onToggleBorders: () -> Unit,
    onToggleLock: () -> Unit,
    onHoldClick: (Int) -> Unit,
    problemsRepository: ProblemRepository
) {
    var saveDialogData by remember { mutableStateOf<Problem?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        ClimbingWallView(
            configuration = configuration,
            wallImagePainter = wallPainter,
            selectedHoldIds = selectedHoldIds,
            onHoldClick = onHoldClick,
            emptyWallImagePainter = emptyPainter,
            showEmptyWall = showEmptyWall,
            darkenNonSelected = darkenNonSelected,
            showBorders = showBorders,
            isLocked = isLocked,
            onToggleLock = onToggleLock,
            onToggleEmptyWall = onToggleEmptyWall,
            onToggleDarkenNonSelected = onToggleDarkenNonSelected,
            onToggleBorders = onToggleBorders,
            modifier = Modifier.fillMaxSize(),
            problemsRepository = problemsRepository,
            showSaveDialog = { problem -> saveDialogData = problem }
        )

        SelectionCounter(selectedHoldIds)

        saveDialogData?.let {
            SaveDialog(
                onDismissRequest = { saveDialogData = null },
                problemRepository = problemsRepository,
                problem = it
            )
        }
    }
}

@Composable
private fun BoxScope.SelectionCounter(selectedHoldIds: Set<Int>) {
    Text(
        text = "${selectedHoldIds.size} chwytów", // TODO string resources
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .align(Alignment.TopStart)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
