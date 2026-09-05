@file:OptIn(ExperimentalBrowserHistoryApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.wojtek.holds

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.read
import com.wojtek.holds.Constants.DEFAULT_VERSION
import com.wojtek.holds.components.climbingwall.ClimbingWallState
import com.wojtek.holds.components.climbingwall.SaveDialog
import com.wojtek.holds.components.climbingwall.ProblemsListDialog
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.utils.ProblemNavType
import kotlinx.serialization.Serializable
import web.events.*
import web.storage.localStorage
import web.window.window
import coil3.compose.setSingletonImageLoaderFactory
import com.wojtek.holds.utils.newImageLoader
import kotlin.js.unsafeCast
import kotlin.reflect.typeOf

private fun parseUrlHashParams(hash: String): Map<String, String> {
    if (!hash.startsWith("v=")) return emptyMap()
    return hash.split("&").associate { param ->
        val parts = param.split("=", limit = 2)
        val key = parts.getOrNull(0) ?: ""
        val value = parts.getOrNull(1) ?: ""
        key to value
    }
}

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    val coroutineScope = rememberCoroutineScope()
    val initRoute = remember { window.location.hash.substringAfter('#', "") }
    val urlParams = remember(initRoute) { parseUrlHashParams(initRoute) }

    val initialVersion = remember {
        urlParams["v"] ?: localStorage.getItem("selected_version") ?: DEFAULT_VERSION
    }
    val initialHolds = remember {
        urlParams["holds"] ?: localStorage.getItem("selected_holds") ?: ""
    }
    val initialHoldsSet = remember(initialHolds) {
        initialHolds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
    }

    val climbingWallState = remember {
        ClimbingWallState(coroutineScope).apply {
            selectedHoldIds = initialHoldsSet
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        App(
            climbingWallState = climbingWallState,
            initialVersion = initialVersion,
            initialHolds = initialHolds
        ) { navController ->
            when {
                initRoute.startsWith("problems") -> {
                    navController.navigate(ProblemsListRoute)
                }

                initRoute.startsWith("save") || initRoute.startsWith(SaveDialog.serializer().descriptor.serialName) -> {
                    navController.navigate(WallRoute(initialVersion, initialHolds))
                }

                // 1. Intercept the new Wall Test route if different from initial
                initRoute.startsWith("v=") -> {
                    val version = urlParams["v"] ?: DEFAULT_VERSION
                    val holds = urlParams["holds"] ?: ""
                    if (version != initialVersion || holds != initialHolds) {
                        navController.navigate(WallRoute(version, holds))
                    }
                }
            }

            val hashChangeListener = EventHandler {
                // Read the new manually typed hash
                val manualRoute = window.location.hash.substringAfter('#', "")

                when {
                    manualRoute.startsWith("problems") -> {
                        navController.navigate(ProblemsListRoute) {
                            launchSingleTop = true
                        }
                    }
                    manualRoute.startsWith("save") -> {
                        navController.navigate(WallRoute(initialVersion, initialHolds)) {
                            launchSingleTop = true
                        }
                    }
                    manualRoute.startsWith("v=") -> {
                        val params = parseUrlHashParams(manualRoute)
                        val version = params["v"] ?: DEFAULT_VERSION
                        val holds = params["holds"] ?: ""

                        val holdsSet = holds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
                        if (climbingWallState.selectedHoldIds != holdsSet) {
                            climbingWallState.selectedHoldIds = holdsSet
                        }

                        // Navigate to the newly typed URL state
                        navController.navigate(WallRoute(version, holds)) {
                            // Use singleTop so we don't blow up the backstack
                            launchSingleTop = true
                        }
                    }
                }
            }

            // Using 'hashchange' is often more reliable than 'popstate'
            // when the user is ONLY modifying the fragment (#) in the address bar
            val eventType = EventType<Event>("hashchange")

            try {
                window.addEventListener(eventType, hashChangeListener)

                navController.bindToBrowserNavigation { entry ->
                    val route = entry.destination.route.orEmpty()
                    when {
                        route.startsWith(ProblemsListRoute.serializer().descriptor.serialName) -> "#problems"
                        route.startsWith(SaveDialog.serializer().descriptor.serialName) -> "#save"

                        // 2. Bind the hash string to the browser URL
                        route.startsWith(WallRoute.serializer().descriptor.serialName) -> {
                            val version = entry.toRoute<WallRoute>().version
                            val holds = climbingWallState.selectedHoldIds.sorted().joinToString(",")
                            if (holds.isEmpty()) {
                                "#v=$version"
                            } else {
                                "#v=$version&holds=$holds"
                            }
                        }

                        else -> ""
                    }
                }
            } finally {
                window.removeEventListener(eventType, hashChangeListener)
            }
        }
        UpdateAffordance()
    }
}

@Serializable
data object ProblemsListRoute

@Serializable
data class SaveDialog(val problem: Problem)

@Serializable
data class WallRoute(val version: String = DEFAULT_VERSION, val holds: String = "")

external interface SafeBeforeUnloadEvent : kotlin.js.JsAny {
    var returnValue: String
}

@Composable
internal fun App(
    climbingWallState: ClimbingWallState,
    initialVersion: String = DEFAULT_VERSION,
    initialHolds: String = "",
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val problemsDatabase = remember { ProblemRepository(coroutineScope) }

    LaunchedEffect(climbingWallState.selectedHoldIds, climbingWallState.configuration) {
        val version = climbingWallState.configuration?.version
        if (version != null) {
            localStorage.setItem("selected_version", version)
        }
        val holdsString = climbingWallState.selectedHoldIds.sorted().joinToString(",")
        localStorage.setItem("selected_holds", holdsString)
    }

    val hasUnsavedChanges = climbingWallState.selectedHoldIds.isNotEmpty()
    DisposableEffect(hasUnsavedChanges) {
        if (hasUnsavedChanges) {
            val beforeUnloadType = EventType<Event>("beforeunload")
            val listener = { event: Event ->
                event.preventDefault()
                val unloadEvent = event.unsafeCast<SafeBeforeUnloadEvent>()
                unloadEvent.returnValue = ""
            }
            window.addEventListener(beforeUnloadType, listener)
            onDispose {
                window.removeEventListener(beforeUnloadType, listener)
            }
        } else {
            onDispose {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = WallRoute(initialVersion, initialHolds)
    ) {
        composable<SaveDialog>(typeMap = mapOf(typeOf<Problem>() to ProblemNavType)) { backStackEntry ->
            val args = backStackEntry.toRoute<SaveDialog>()
            SaveDialog(
                onDismissRequest = { navController.popBackStack() },
                problemRepository = problemsDatabase,
                problem = args.problem,
                climbingWallState = climbingWallState
            )
        }
        composable<ProblemsListRoute> {
            ProblemsListDialog(
                problemRepository = problemsDatabase,
                onDialogDismiss = { navController.popBackStack() },
                loadProblem = { problem ->
                    if (climbingWallState.configuration?.version != problem.version) {
                        climbingWallState.loadVersion(problem.version)
                    }
                    climbingWallState.selectedHoldIds = problem.holdsIds.toSet()
                }
            )
        }

        // 3. The test composable
        composable<WallRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<WallRoute>()
            val versionArg = backStackEntry.arguments?.read { getString("version") } ?: args.version
            val holdsArg = if (climbingWallState.configuration?.version == versionArg) {
                climbingWallState.selectedHoldIds.sorted().joinToString(",")
            } else {
                backStackEntry.arguments?.read { getString("holds") } ?: args.holds
            }
            ClimbingWallApp(
                problemsRepository = problemsDatabase,
                climbingWallState = climbingWallState,
                initialHolds = holdsArg.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet(),
                version = versionArg,
                navController = navController,
                backStackEntry = backStackEntry,
            )
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}

@Composable
fun BoxScope.UpdateAffordance() {
    // Only shown when a new version was detected WHILE the app was running.
    val updateAvailable = rememberUpdateAvailable()
    if (!updateAvailable) return

    var showUpdateDialog by remember { mutableStateOf(false) }

    // Pulsate the button to nudge the user into updating sooner rather than later.
    val pulse = rememberInfiniteTransition(label = "newSoftwarePulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "newSoftwareScale"
    )

    ElevatedButton(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        onClick = { showUpdateDialog = true }
    ) {
        Text("New Software")
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("New software available") },
            text = { Text("A new version of the app is ready. Update now to restart with the latest version.") },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    PwaUpdate.applyUpdate()
                }) { Text("Update") }
            }
        )
    }
}

