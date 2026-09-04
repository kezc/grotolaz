// ============================================================================
// UPDATE UI SNIPPET — merge this into your ROOT composable (the one mounted by
// ComposeViewport, e.g. `App`). It adds a pulsating top-right "New Software"
// button and a confirmation dialog. It intentionally uses plain string literals
// and no icons so it has no extra dependencies; swap in stringResource(...) and
// an Icon(...) if your app already uses compose-resources.
//
// Two changes to your root composable:
//   1. Wrap your existing content in a Box so the button can float top-end.
//   2. Paste the `UpdateAffordance()` call inside that Box, and the composable
//      function below somewhere in the file.
// ============================================================================

// ---- Required imports (add the ones you don't already have) ----------------
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ---- 1. Wrap your root content ---------------------------------------------
// Before:
//     @Composable fun App() { /* your content */ }
// After:
//     @Composable fun App() {
//         Box(modifier = Modifier.fillMaxSize()) {
//             /* your content */
//             UpdateAffordance()      // <-- add this, last, so it draws on top
//         }
//     }

// ---- 2. The affordance ------------------------------------------------------
@Composable
fun androidx.compose.foundation.layout.BoxScope.UpdateAffordance() {
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
