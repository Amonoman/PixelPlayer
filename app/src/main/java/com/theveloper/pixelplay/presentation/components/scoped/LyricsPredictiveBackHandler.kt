package com.theveloper.pixelplay.presentation.components.scoped

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Predictive-back handler for the Lyrics page.
 *
 * On Android 13+ (UPSIDE_DOWN_CAKE / API 34) the gesture drives [progress] live so the
 * caller can shrink / fade the Lyrics sheet while the user is swiping. When the gesture
 * is committed [onBack] is called; when it is cancelled the progress snaps back to 0.
 *
 * On older Android versions a plain [BackHandler] is registered instead.
 *
 * @param enabled         Whether the handler should intercept back events.
 * @param progress        [Animatable] in [0f, 1f] – 0 = fully visible, 1 = fully dismissed.
 *                        The caller applies this value to scale / alpha / translation.
 * @param onSwipeEdge     Receives [BackEventCompat.EDGE_LEFT] / [BackEventCompat.EDGE_RIGHT]
 *                        while the gesture is in progress, null when idle.
 * @param animationDurationMs Duration for the cancel-snap-back animation.
 * @param onBack          Called when the back gesture is committed (after the animation).
 */
@Composable
fun LyricsPredictiveBackHandler(
    enabled: Boolean,
    progress: Animatable<Float, *>,
    onSwipeEdge: (Int?) -> Unit = {},
    animationDurationMs: Int = 350,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        PredictiveBackHandler(enabled = enabled) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    onSwipeEdge(backEvent.swipeEdge)
                    // Drive the dismiss animation in real time with the gesture progress.
                    scope.launch { progress.snapTo(backEvent.progress) }
                }
                // Gesture committed – animate to fully dismissed, then call onBack.
                scope.launch {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(animationDurationMs / 2)
                    )
                    onSwipeEdge(null)
                    onBack()
                    // Reset for next open (will be invisible while closed anyway).
                    progress.snapTo(0f)
                }
            } catch (_: CancellationException) {
                // Gesture cancelled – snap back to fully visible.
                scope.launch {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(animationDurationMs)
                    )
                    onSwipeEdge(null)
                }
            }
        }
    } else {
        BackHandler(enabled = enabled) { onBack() }
    }
}
