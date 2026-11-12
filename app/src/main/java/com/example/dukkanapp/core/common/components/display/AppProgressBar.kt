package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        progress.coerceIn(0f, 1f), label = "progress"
    )
    Box(
        modifier = modifier
            .height(AppDimens.progressTrack)
            .background(AppTheme.colors.border, AppPillShape)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    animated, 0f..1f
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(AppTheme.colors.brand, AppPillShape)
        )

    }
}