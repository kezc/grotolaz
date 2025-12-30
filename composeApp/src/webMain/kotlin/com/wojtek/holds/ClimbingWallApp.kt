package com.wojtek.holds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wojtek.holds.components.ClimbingWallView
import com.wojtek.holds.utils.ConfigurationLoadResult
import com.wojtek.holds.utils.rememberHoldConfiguration
import holds.composeapp.generated.resources.Res
import holds.composeapp.generated.resources.empty
import holds.composeapp.generated.resources.wall
import org.jetbrains.compose.resources.painterResource

/**
 * Main application composable for the climbing wall hold tracker.
 *
 * Loads hold configuration from resources and manages hold selection state.
 * This is a reference implementation using all the reusable components.
 */
@Composable
fun ClimbingWallApp() {
    var selectedHoldIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showEmptyWall by remember { mutableStateOf(false) }
    var darkenNonSelected by remember { mutableStateOf(false) }
    var showBorders by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    val configurationResult = rememberHoldConfiguration()

    // Load selected holds from URL after config is loaded
    LaunchedEffect(configurationResult.value) {
        if (configurationResult.value is ConfigurationLoadResult.Success) {
            val holdsFromUrl = UrlSync.decodeFromUrl()
            if (holdsFromUrl.isNotEmpty()) {
                selectedHoldIds = holdsFromUrl
            }
        }
    }

    // Sync URL when selected holds change
    LaunchedEffect(selectedHoldIds) {
        if (configurationResult.value is ConfigurationLoadResult.Success) {
            UrlSync.encodeToUrl(selectedHoldIds)
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
                is ConfigurationLoadResult.Success -> ClimbingWallContent(
                    configuration = result.configuration,
                    selectedHoldIds = selectedHoldIds,
                    showEmptyWall = showEmptyWall,
                    darkenNonSelected = darkenNonSelected,
                    showBorders = showBorders,
                    isLocked = isLocked,
                    onClearClick = {
                        if (!isLocked) selectedHoldIds = emptySet()
                    },
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
                    }
                )
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
    configuration: com.wojtek.holds.model.HoldConfiguration,
    selectedHoldIds: Set<Int>,
    showEmptyWall: Boolean,
    darkenNonSelected: Boolean,
    showBorders: Boolean,
    isLocked: Boolean,
    onClearClick: () -> Unit,
    onToggleEmptyWall: () -> Unit,
    onToggleDarkenNonSelected: () -> Unit,
    onToggleBorders: () -> Unit,
    onToggleLock: () -> Unit,
    onHoldClick: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main climbing wall view
        ClimbingWallView(
            configuration = configuration,
            wallImagePainter = painterResource(Res.drawable.wall),
            selectedHoldIds = selectedHoldIds,
            onHoldClick = onHoldClick,
            emptyWallImagePainter = painterResource(Res.drawable.empty),
            showEmptyWall = showEmptyWall,
            darkenNonSelected = darkenNonSelected,
            showBorders = showBorders,
            isLocked = isLocked,
            useFloatingControls = true,
            onToggleLock = onToggleLock,
            onToggleEmptyWall = onToggleEmptyWall,
            onToggleDarkenNonSelected = onToggleDarkenNonSelected,
            onToggleBorders = onToggleBorders,
            modifier = Modifier.fillMaxSize()
        )

        // Selection counter overlay (top-left)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Selected: ${selectedHoldIds.size} / ${configuration.holds.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
