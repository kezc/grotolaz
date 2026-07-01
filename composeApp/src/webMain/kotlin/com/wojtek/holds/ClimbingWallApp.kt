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
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.savedstate.write
import com.wojtek.holds.components.climbingwall.ClimbingWallView
import com.wojtek.holds.components.climbingwall.ProblemsListDialog
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.model.HoldConfiguration
import com.wojtek.holds.utils.ConfigurationLoadResult

import com.wojtek.holds.components.climbingwall.ClimbingWallState

/**
 * Main application composable for the climbing wall hold tracker.
 *
 * Loads hold configuration from resources and manages hold selection state.
 * This is a reference implementation using all the reusable components.
 */
@Composable
fun ClimbingWallApp(
    problemsRepository: ProblemRepository,
    climbingWallState: ClimbingWallState,
    initialHolds: Set<Int>,
    version: String,
    navController: NavController,
    backStackEntry: NavBackStackEntry,
) {
    // Load configuration and painters inside climbingWallState when version changes
    LaunchedEffect(version) {
        climbingWallState.loadVersion(version)
    }

    // When show selected only is turned on, automatically turn off darken non-selected
    LaunchedEffect(climbingWallState.showEmptyWall) {
        if (climbingWallState.showEmptyWall) {
            climbingWallState.darkenNonSelected = false
        }
    }

    // Sync initialHolds when it changes in the navigation/URL
    LaunchedEffect(initialHolds) {
        if (initialHolds != climbingWallState.selectedHoldIds) {
            climbingWallState.selectedHoldIds = initialHolds
        }
    }

    // Sync URL and NavBackStackEntry when selected holds or configuration changes
    LaunchedEffect(climbingWallState.selectedHoldIds, climbingWallState.configuration) {
        val config = climbingWallState.configuration
        if (config != null) {
            SilentUrlUpdater.updateHoldsInUrl(climbingWallState.selectedHoldIds, config.version)
            val holdsString = climbingWallState.selectedHoldIds.sorted().joinToString(",")
            backStackEntry.arguments?.write {
                putString("holds", holdsString)
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val result = climbingWallState.loadResult) {
                is ConfigurationLoadResult.Loading -> LoadingIndicator()
                is ConfigurationLoadResult.Error -> ErrorDisplay(result.message)
                is ConfigurationLoadResult.Success -> {
                    val wallPainter = climbingWallState.wallPainter
                    val emptyPainter = climbingWallState.emptyPainter

                    if (wallPainter != null && emptyPainter != null) {
                        ClimbingWallContent(
                            navController = navController,
                            configuration = result.configuration,
                            wallPainter = wallPainter,
                            emptyPainter = emptyPainter,
                            state = climbingWallState,
                            problemsRepository = problemsRepository
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
    navController: NavController,
    configuration: HoldConfiguration,
    wallPainter: Painter,
    emptyPainter: Painter,
    state: ClimbingWallState,
    problemsRepository: ProblemRepository
) {
    var showProblemsDialog by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        ClimbingWallView(
            state = state,
            configuration = configuration,
            wallImagePainter = wallPainter,
            emptyWallImagePainter = emptyPainter,
            modifier = Modifier.fillMaxSize(),
            problemsRepository = problemsRepository,
            showSaveDialog = { problem -> navController.navigate(SaveDialog(problem)) },
            showProblemsDialog = { showProblemsDialog = true }
        )

        SelectionCounter(state.selectedHoldIds)

        if (showProblemsDialog) {
            ProblemsListDialog(
                problemRepository = problemsRepository,
                onDialogDismiss = { showProblemsDialog = false },
                loadProblem = { state.selectedHoldIds = it.holdsIds.toSet() }
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

