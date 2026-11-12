package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                if (selected) AppDimens.pagerIndicatorSelectedWidth else AppDimens.pagerIndicatorSize,
                label = "dotWidth"
            )

            Box(
                modifier = modifier
                    .size(width = width, height = AppDimens.pagerIndicatorSize)
                    .background(
                        if (selected) AppTheme.colors.brand else AppTheme.colors.border,
                        AppPillShape
                    ),

                )
        }
    }
}