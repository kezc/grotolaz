package com.wojtek.holds

import androidx.compose.runtime.*
import com.wojtek.holds.database.ProblemRepository

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val problemsDatabase = remember { ProblemRepository(coroutineScope) }
    ClimbingWallApp(problemsDatabase)
}
