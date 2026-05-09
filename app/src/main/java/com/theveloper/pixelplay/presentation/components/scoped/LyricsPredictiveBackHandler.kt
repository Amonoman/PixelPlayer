package com.theveloper.pixelplay.presentation.components.scoped

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Predictive-back handler for the Lyrics page.
 *
 * Mirrors the pattern used by [PlayerSheetPredictiveBackHandler]: progress is
 * communicated to the caller via [onProgressChanged] so the caller can store it in
 * a plain `var backProgress by mutableFloatStateOf(...)`. That float is then read
 * inside `graphicsLayer` at draw-phase via `rememberUpdatedState` — zero recomposition
 * per gesture frame, identical to how SheetVisualState works in the player sheet.
 *
 * Visual contract (enforced by the caller in graphicsLayer):
 *   - 0f = fully visible
 *   - 1f = fully dismissed
 *   - Sheet slides **down** and fades — no horizontal shift.
 *
 * On Android < API 34 a plain [BackHandler] is registered as fallback.
 *
 * @param enabled           Whether this handler intercepts back events.
 * @param onProgressChanged Called on every gesture frame with progress in [0f, 1f].
 *                          Also called with 0f when a cancelled gesture snaps back,
 *                          and with 0f after a committed back (sheet will be gone).
 * @param animationDurationMs Duration for the snap-back animation on gesture cancel,
 *                            and for the commit-exit animation.
 * @param onBack            Called after the exit animation completes on commit.
 */
@Composable
fun LyricsPredictiveBackHandler(
    enabled: Boolean,
    onProgressChanged: (Float) -> Unit,
    animationDurationMs: Int = 350,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // Internal animatable to drive snap-back and commit-exit independently of the caller.
    val progressAnim = remember { Animatable(0f) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        PredictiveBackHandler(enabled = enabled) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    // Sync internal animatable so we can animate from current position.
                    progressAnim.snapTo(backEvent.progress)
                    // Notify caller — updates state, graphicsLayer reads at draw-phase.
                    onProgressChanged(backEvent.progress)
                }
                // Gesture committed: animate to fully dismissed, then navigate back.
                scope.launch {
                    progressAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(animationDurationMs / 2)
                    ) { onProgressChanged(value) }
                    onBack()
                    // Reset; sheet will be gone but keeps state clean for next open.
                    progressAnim.snapTo(0f)
                    onProgressChanged(0f)
                }
            } catch (_: CancellationException) {
                // Gesture cancelled — animate from current position back to fully visible.
                scope.launch {
                    progressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(animationDurationMs)
                    ) { onProgressChanged(value) }
                }
            }
        }
    } else {
        BackHandler(enabled = enabled) { onBack() }
    }
}
