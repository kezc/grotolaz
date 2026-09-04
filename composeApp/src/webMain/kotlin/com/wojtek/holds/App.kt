@file:OptIn(ExperimentalBrowserHistoryApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.wojtek.holds

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import web.storage.sessionStorage
import web.window.window
import coil3.compose.setSingletonImageLoaderFactory
import com.wojtek.holds.utils.newImageLoader
import kotlin.js.unsafeCast
import kotlin.reflect.typeOf

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }

    val coroutineScope = rememberCoroutineScope()
    val savedVersion = remember { sessionStorage.getItem("selected_version") ?: DEFAULT_VERSION }
    val savedHolds = remember { sessionStorage.getItem("selected_holds") ?: "" }
    val savedHoldsSet = remember(savedHolds) {
        savedHolds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
    }

    val climbingWallState = remember {
        ClimbingWallState(coroutineScope).apply {
            selectedHoldIds = savedHoldsSet
        }
    }

    App(climbingWallState) { navController ->
        val initRoute = window.location.hash.substringAfter('#', "")
        when {
            initRoute.startsWith("problems") -> {
                navController.navigate(ProblemsListRoute)
            }

            initRoute.startsWith("save") || initRoute.startsWith(SaveDialog.serializer().descriptor.serialName) -> {
                navController.navigate(WallRoute(savedVersion, savedHolds))
            }

            // 1. Intercept the new Wall Test route
            initRoute.startsWith("v=") -> {
                println("Navigating to Wall screen from legacy URL")

                // Reusing your old parsing logic
                val params = initRoute.split("&").associate { param ->
                    val parts = param.split("=", limit = 2)
                    val key = parts.getOrNull(0) ?: ""
                    val value = parts.getOrNull(1) ?: ""
                    key to value
                }

                val version = params["v"] ?: DEFAULT_VERSION
                val holds = params["holds"] ?: ""
                
                val holdsSet = holds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
                climbingWallState.selectedHoldIds = holdsSet

                if (version != savedVersion || holds != savedHolds) {
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
                    navController.navigate(WallRoute()) {
                        launchSingleTop = true
                    }
                }
                manualRoute.startsWith("v=") -> {
                    val params = manualRoute.split("&").associate { param ->
                        val parts = param.split("=", limit = 2)
                        val key = parts.getOrNull(0) ?: ""
                        val value = parts.getOrNull(1) ?: ""
                        key to value
                    }
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
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val problemsDatabase = remember { ProblemRepository(coroutineScope) }

    val savedVersion = remember { sessionStorage.getItem("selected_version") ?: DEFAULT_VERSION }
    val savedHolds = remember { sessionStorage.getItem("selected_holds") ?: "" }

    LaunchedEffect(climbingWallState.selectedHoldIds, climbingWallState.configuration) {
        val version = climbingWallState.configuration?.version
        if (version != null) {
            sessionStorage.setItem("selected_version", version)
        }
        val holdsString = climbingWallState.selectedHoldIds.sorted().joinToString(",")
        sessionStorage.setItem("selected_holds", holdsString)
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
        startDestination = WallRoute(savedVersion, savedHolds)
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
                loadProblem = {
                    climbingWallState.selectedHoldIds = it.holdsIds.toSet()
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
