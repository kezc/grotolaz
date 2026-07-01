package com.wojtek.holds.components.climbingwall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wojtek.holds.components.AppButton
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.utils.getCompletedOrNull
import com.wojtek.holds.utils.toBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

@Composable
fun SaveDialog(
    onDismissRequest: () -> Unit,
    problemRepository: ProblemRepository,
    problem: Problem,
    climbingWallState: ClimbingWallState
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    fun getImage(): ImageBitmap {
        val configuration = climbingWallState.configuration
        val wallPainter = climbingWallState.wallPainter
        val emptyPainter = climbingWallState.emptyPainter
        val selectedHoldIds = climbingWallState.selectedHoldIds

        if (configuration == null || wallPainter == null) {
            return ImageBitmap(10, 10)
        }

        return generateRouteImageBitmap(
            configuration = configuration,
            wallImagePainter = wallPainter,
            selectedHoldIds = selectedHoldIds,
            density = density,
            layoutDirection = layoutDirection,
            darkenNonSelected = true,
            showEmptyWall = true,
            emptyWallImagePainter = emptyPainter
        )
    }


    Column(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        var name by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val imageDeferred = remember { coroutineScope.async { getImage() } }
        val image: ImageBitmap? by produceState(null) { value = imageDeferred.await() }
        image?.let {
            Image(it, null, modifier = Modifier.border(1.dp, Color.Red).size(200.dp))
        }

        Text("Save boulder")
        Text("Enter name")
        TextField(value = name, onValueChange = { name = it })
        AppButton("Save") {
            coroutineScope.launch {
                isLoading = true
                val imagePng = withContext(Dispatchers.Default) { imageDeferred.await().toBase64() }

                problemRepository.save(problem.copy(name = name, imageBase64 = imagePng))
                onDismissRequest()
            }
        }
    }
}