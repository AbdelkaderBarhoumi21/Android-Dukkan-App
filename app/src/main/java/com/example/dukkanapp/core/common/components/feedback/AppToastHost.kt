package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.dukkanapp.core.theme.AppDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DURATION_MS = 4_000L
private const val DURATION_WITH_ACTION_MS = 6_000L
private const val TIMER_TICK_MS = 50L

@Immutable
data class AppToastData(
    val title: String,
    val type: AppToastType,
    val subtitle: String? = null,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null,
    val showClose: Boolean = true,
    internal val id: Long = 0L,
) {
    /** null = stays until dismissed: Loading, and an Error with an action. */
    internal val durationMs: Long?
        get() = when {
            type == AppToastType.Loading -> null
            type == AppToastType.Error && actionText != null -> null
            actionText != null -> DURATION_WITH_ACTION_MS
            else -> DURATION_MS
        }
}

/** One toast at a time: [show] replaces the current one. */
@Stable
class AppToastState {
    var current by mutableStateOf<AppToastData?>(null)
        private set
    private var lastId = 0L

    fun show(toast: AppToastData) {
        lastId += 1
        current = toast.copy(id = lastId)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberAppToastState(): AppToastState = remember { AppToastState() }

/** Top by default. Use [Bottom] when the keyboard or a sheet is open. */
enum class AppToastPlacement { Top, Bottom }

/** Put it last in a full-screen Box, so it draws above the screen. `AppScaffold` does this (doc 13). */
@Composable
fun AppToastHost(
    state: AppToastState,
    modifier: Modifier = Modifier,
    placement: AppToastPlacement = AppToastPlacement.Top,
) {
    val slidePx = with(LocalDensity.current) { AppDimens.toastSlide.roundToPx() }
    val reducedMotion = animationsDisabled()

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.current,
            contentKey = { it?.id },
            transitionSpec = {
                val enter = fadeIn(tween(240))
                val exit = fadeOut(tween(160))
                if (reducedMotion) {
                    enter togetherWith exit
                } else {
                    (enter + slideInVertically(
                        tween(
                            240,
                            easing = EaseOutBack
                        )
                    ) { -slidePx }) togetherWith
                            (exit + scaleOut(tween(160), targetScale = 0.96f))
                }
            },
            modifier = Modifier
                .align(if (placement == AppToastPlacement.Top) Alignment.TopCenter else Alignment.BottomCenter)
                .then(
                    if (placement == AppToastPlacement.Top) {
                        Modifier
                            .statusBarsPadding()
                            .padding(top = AppDimens.space8)
                    } else {
                        Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(bottom = AppDimens.navBar + AppDimens.space12)
                    },
                )
                .padding(horizontal = AppDimens.screenPadding),
            label = "toast",
        ) { toast ->
            if (toast != null) ToastWithBehaviour(toast = toast, onDismiss = state::dismiss)
        }
    }
}

@Composable
private fun ToastWithBehaviour(toast: AppToastData, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipeThresholdPx = with(LocalDensity.current) { AppDimens.toastSwipeThreshold.toPx() }
    val swipe = remember(toast.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isPressed by remember(toast.id) { mutableStateOf(false) }
    var progress by remember(toast.id) { mutableFloatStateOf(1f) }

    LaunchedEffect(toast.id) {
        when (toast.type) {
            AppToastType.Success -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            AppToastType.Error -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            else -> Unit
        }
    }

    // A delay loop, not an animation: when the user removes animations an animation would
    // finish instantly and the toast would vanish, while a delay still takes the full time.
    LaunchedEffect(toast.id) {
        val total = toast.durationMs ?: return@LaunchedEffect
        var remaining = total
        snapshotFlow { isPressed }.collectLatest { pressed ->
            if (pressed) return@collectLatest
            while (remaining > 0) {
                delay(TIMER_TICK_MS)
                remaining -= TIMER_TICK_MS
                progress = remaining.toFloat() / total
            }
            onDismiss()
        }
    }

    AppToast(
        title = toast.title,
        type = toast.type,
        subtitle = toast.subtitle,
        actionText = toast.actionText,
        onAction = {
            toast.onAction?.invoke()
            onDismiss()
        },
        onClose = if (toast.showClose) onDismiss else null,
        timerProgress = if (toast.durationMs != null) ({ progress }) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = swipe.value.x
                translationY = swipe.value.y
                val distance = maxOf(abs(swipe.value.x), abs(swipe.value.y))
                alpha = 1f - (distance / (swipeThresholdPx * 2)).coerceIn(0f, 1f)
            }
            // Pause the timer while a finger is down, whatever the gesture turns out to be.
            .pointerInput(toast.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            // Swipe up or sideways to dismiss; downward drags are ignored.
            .pointerInput(toast.id) {
                detectDragGestures(
                    onDragEnd = {
                        val value = swipe.value
                        if (abs(value.x) > swipeThresholdPx || value.y < -swipeThresholdPx) {
                            onDismiss()
                        } else {
                            scope.launch { swipe.animateTo(Offset.Zero) }
                        }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(Offset.Zero) } },
                ) { change, drag ->
                    change.consume()
                    val next = swipe.value + drag
                    scope.launch { swipe.snapTo(Offset(next.x, minOf(next.y, 0f))) }
                }
            },
    )
}