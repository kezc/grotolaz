// ============================================================================
// DATA-STATUS INDICATOR — variants. Pick the one the user chose; delete the rest.
// Merge into the composable that shows the polled data. Replace {{PACKAGE}}.
//
// What this reports is the health of the DATA poll — not software freshness.
// Those are separate mechanisms and deserve separate signals; collapsing them
// into one "offline" lamp blurs the software/data line the whole setup exists
// to keep sharp.
// ============================================================================

// ---- Required imports (variants A and B) -----------------------------------
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// =============================================================================
// VARIANT A — pulsing dot (matches the pulsating update button's rhythm)
// =============================================================================

/**
 * Pulse factor for the status dot, using the same 600ms/FastOutSlowInEasing
 * rhythm as the "New Software" button so everything that wants attention speaks
 * one visual language. The amplitude is larger because the dot is small — the
 * button's 1.18x is imperceptible at 8dp.
 */
@Composable
private fun rememberStatusPulse(): Float {
    val pulse = rememberInfiniteTransition(label = "statusPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusPulseScale"
    )
    return scale
}

@Composable
fun DataStatusPulsing(offline: Boolean, hasCachedCopy: Boolean) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusColor =
            if (offline) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary

        // Only animate while offline: an infinite transition repaints every
        // frame for as long as it is composed, which is a permanent cost in an
        // app meant to sit open for hours. Conditional composable calls are
        // fine — Compose handles the group structure.
        val dotScale = if (offline) rememberStatusPulse() else 1f

        // Fixed slot: graphicsLayer does not clip, so a scaled dot would
        // otherwise overdraw its bounds and shove the label around.
        Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                    .background(statusColor, CircleShape)
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = when {
                !offline -> "live"
                hasCachedCopy -> "offline — showing cached copy"
                else -> "offline — no cached copy yet"
            },
            color = statusColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// =============================================================================
// VARIANT B — static dot (same layout, no animation)
// Use rememberStatusPulse() nowhere; drop the graphicsLayer line and the
// fixed-size Box wrapper, leaving just the 8dp dot + label.
// =============================================================================

// =============================================================================
// VARIANT C — text only, shown just when offline
// =============================================================================

@Composable
fun DataStatusTextOnly(offline: Boolean, hasCachedCopy: Boolean) {
    if (!offline) return
    Text(
        text = if (hasCachedCopy) "offline — showing cached copy"
               else "offline — no cached copy yet",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// =============================================================================
// VARIANT D — none
// Keep the `offline` state in the composable anyway. Recording the outcome
// costs nothing and leaves the app one line away from surfacing it; what you
// must not do is discard the failure, because then a dead endpoint is
// indistinguishable from a healthy app showing a cached copy.
// =============================================================================
