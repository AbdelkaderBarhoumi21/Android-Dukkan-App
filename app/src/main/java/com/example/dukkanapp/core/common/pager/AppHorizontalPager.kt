package com.example.dukkanapp.core.common.pager

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppHorizontalPager(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true, // It controls whether the user can swipe with their finger.
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = userScrollEnabled,
        verticalAlignment = Alignment.CenterVertically,
        pageContent = pageContent
    )
}