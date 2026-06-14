package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.utils.toImageBitmap

@Composable
fun ProblemsListDialog(
    problemRepository: ProblemRepository,
    onDialogDismiss: () -> Unit,
    loadProblem: (Problem) -> Unit
) {
    Dialog(
        onDismissRequest = onDialogDismiss,
        properties = DialogProperties(dismissOnBackPress = true)
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
                    Row(Modifier.clickable { loadProblem(it); onDialogDismiss() }) {
                        Text(it.name)
                        val bitmap = remember { it.imageBase64!!.toImageBitmap() }
                        Image(bitmap, null, Modifier.size(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun getProblemsList(problemRepository: ProblemRepository): State<List<Problem>> = produceState(emptyList()) {
    value = problemRepository.getAll()
}