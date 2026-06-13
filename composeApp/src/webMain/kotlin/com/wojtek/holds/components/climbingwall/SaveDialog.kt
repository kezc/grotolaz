package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wojtek.holds.components.AppButton
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import kotlinx.coroutines.launch

@Composable
fun SaveDialog(onDismissRequest: () -> Unit, problemRepository: ProblemRepository, problem: Problem) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
        ) {
            var name by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Text("Save boulder")
            Text("Enter name")
            TextField(value = name, onValueChange = { name = it })
            AppButton("Save") {
                coroutineScope.launch {
                    isLoading = true
                    problemRepository.save(problem.copy(name = name))
                    onDismissRequest()
                }
            }
        }
    }
}