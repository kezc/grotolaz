package com.wojtek.holds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.wojtek.holds.components.ClimbingWallView
import com.wojtek.holds.components.ControlPanel
import com.wojtek.holds.utils.ConfigurationLoadResult
import com.wojtek.holds.utils.rememberHoldConfiguration
import holds.composeapp.generated.resources.Res
import holds.composeapp.generated.resources.empty
import holds.composeapp.generated.resources.wall
import kotlinx.coroutines.launch
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
    val configurationResult = rememberHoldConfiguration()
    val scope = rememberCoroutineScope()

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
                    onToggleEmptyWall = { showEmptyWall = !showEmptyWall },
                    onToggleDarkenNonSelected = { darkenNonSelected = !darkenNonSelected },
                    onToggleBorders = { showBorders = !showBorders },
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
    configuration: com.wojtek.holds.model.HoldConfiguration,
    selectedHoldIds: Set<Int>,
    showEmptyWall: Boolean,
    darkenNonSelected: Boolean,
    showBorders: Boolean,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit,
    onLoadClick: () -> Unit,
    onToggleEmptyWall: () -> Unit,
    onToggleDarkenNonSelected: () -> Unit,
    onToggleBorders: () -> Unit,
    onHoldClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ControlPanel(
            selectedCount = selectedHoldIds.size,
            totalCount = configuration.holds.size,
            onClearClick = onClearClick,
            onSaveClick = onSaveClick,
            onLoadClick = onLoadClick,
            showEmptyWall = showEmptyWall,
            onToggleEmptyWall = onToggleEmptyWall,
            darkenNonSelected = darkenNonSelected,
            onToggleDarkenNonSelected = onToggleDarkenNonSelected,
            showBorders = showBorders,
            onToggleBorders = onToggleBorders
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clipToBounds()
        ) {
            ClimbingWallView(
                configuration = configuration,
                wallImagePainter = painterResource(Res.drawable.wall),
                selectedHoldIds = selectedHoldIds,
                onHoldClick = onHoldClick,
                emptyWallImagePainter = painterResource(Res.drawable.empty),
                showEmptyWall = showEmptyWall,
                darkenNonSelected = darkenNonSelected,
                showBorders = showBorders
            )
        }
    }
}
