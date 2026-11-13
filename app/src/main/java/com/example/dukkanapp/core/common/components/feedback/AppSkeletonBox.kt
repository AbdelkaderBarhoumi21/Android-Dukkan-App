package com.example.dukkanapp.core.common.components.feedback

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.theme.AppTheme

private const val SHIMMER_DURATION_MS = 1_200

/** True when the user turned animations off (Settings → Accessibility → Remove animations). */
@Composable
internal fun animationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}


/**
 * Paints a soft highlight that sweeps across the content every 1.2 s (Figma 14).
 * Put it AFTER clip + background so the highlight stays inside the shape.
 */
fun Modifier.shimmer(): Modifier = composed {
    if (animationsDisabled()) return@composed this

    val highlight = AppTheme.colors.skeletonHighlight // Figma `Skeleton/Shimmer` middle stop
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    drawWithContent {
        drawContent()
        val band = size.width * 0.6f
        // Travels from fully off one edge to fully off the other.
        val travel = -band + (size.width + band * 2) * progress
        val x = if (isRtl) size.width - travel else travel
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x, 0f),
            ),
        )
    }
}


/** One grey placeholder block. Give it a size and the shape of the content it stands for. */
@Composable
fun AppSkeletonBox(shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppTheme.colors.skeleton)
            .shimmer()
    )
}