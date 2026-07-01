@file:OptIn(ExperimentalBrowserHistoryApi::class)

package com.wojtek.holds

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wojtek.holds.Constants.DEFAULT_VERSION
import com.wojtek.holds.components.climbingwall.SaveDialog
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.utils.ProblemNavType
import kotlinx.serialization.Serializable
import web.events.*
import web.window.window
import kotlin.reflect.typeOf

@Composable
fun App() {
    App { navController ->
        val initRoute = window.location.hash.substringAfter('#', "")
        when {
            initRoute.startsWith("start") -> {
                navController.navigate(StartScreen)
            }

            initRoute.startsWith(SaveDialog.serializer().descriptor.serialName) -> {
                navController.navigate(WallRoute())
            }

            initRoute.startsWith("patient") -> {
                val name = initRoute.substringAfter("patient_").substringBefore("_")
                val id = initRoute.substringAfter("patient_").substringAfter("_").toLong()
                navController.navigate(Patient(name, id))
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

                navController.navigate(WallRoute(version, holds))
            }
        }

        val hashChangeListener = EventHandler {
            // Read the new manually typed hash
            val manualRoute = window.location.hash.substringAfter('#', "")

            if (manualRoute.startsWith("v=")) {
                val params = manualRoute.split("&").associate { param ->
                    val parts = param.split("=", limit = 2)
                    val key = parts.getOrNull(0) ?: ""
                    val value = parts.getOrNull(1) ?: ""
                    key to value
                }
                val version = params["v"] ?: "v1"
                val holds = params["holds"] ?: ""

                // Navigate to the newly typed URL state
                navController.navigate(WallRoute(version, holds)) {
                    // Use singleTop so we don't blow up the backstack
                    launchSingleTop = true
                }
            }
            // (Add else-if blocks here for other routes like Id or Patient if needed)
        }

        // Using 'hashchange' is often more reliable than 'popstate'
        // when the user is ONLY modifying the fragment (#) in the address bar
        val eventType = EventType<Event>("hashchange")

        try {
            window.addEventListener(eventType, hashChangeListener)

            navController.bindToBrowserNavigation { entry ->
                val route = entry.destination.route.orEmpty()
                when {
                    route.startsWith(StartScreen.serializer().descriptor.serialName) -> "#start"
                    route.startsWith(SaveDialog.serializer().descriptor.serialName) -> {
                        SaveDialog.serializer().descriptor.serialName
                    }

                    route.startsWith(Patient.serializer().descriptor.serialName) -> {
                        val args = entry.toRoute<Patient>()
                        "#patient_${args.name}_${args.age}"
                    }
                    // 2. Bind the hash string to the browser URL
                    route.startsWith(WallRoute.serializer().descriptor.serialName) -> {
                        // This now outputs #v=v1&holds=74,77,91
                        entry.toRoute<WallRoute>().toBrowserHash()
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
data object StartScreen

@Serializable
data class SaveDialog(val problem: Problem)

@Serializable
data class Patient(val name: String, val age: Long)

@Serializable
data class WallRoute(val version: String = DEFAULT_VERSION, val holds: String = "") {
    fun toBrowserHash(): String {
        return if (holds.isEmpty()) {
            "#v=$version"
        } else {
            "#v=$version&holds=$holds"
        }
    }

}

@Composable
internal fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val problemsDatabase = remember { ProblemRepository(coroutineScope) }
    NavHost(
        navController = navController,
        startDestination = WallRoute(DEFAULT_VERSION, "")
    ) {
        composable<SaveDialog>(typeMap = mapOf(typeOf<Problem>() to ProblemNavType)) { backStackEntry ->
            val args = backStackEntry.toRoute<SaveDialog>()

            SaveDialog(
                onDismissRequest = { navController.popBackStack() },
                problemRepository = problemsDatabase,
                problem = args.problem
            )
        }
        composable<Patient> { Text("Patient screen") }

        // 3. The test composable
        composable<WallRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<WallRoute>()
            ClimbingWallApp(
                problemsRepository = problemsDatabase,
                initialHolds = args.holds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet(),
                version = args.version,
                navController = navController,
            )
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}
