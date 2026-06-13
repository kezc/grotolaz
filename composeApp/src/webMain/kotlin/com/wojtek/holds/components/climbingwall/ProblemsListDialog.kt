package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository

@Composable
fun ProblemsListDialog(problemRepository: ProblemRepository, onDialogDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDialogDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val problems by getProblemsList(problemRepository)
            LazyColumn {
                items(problems, key = { it.id }) {
                    Text(it.name)
                }
            }
        }
    }
}

@Composable
fun getProblemsList(problemRepository: ProblemRepository): State<List<Problem>> = produceState(emptyList()) {
    value = problemRepository.getAll()
}