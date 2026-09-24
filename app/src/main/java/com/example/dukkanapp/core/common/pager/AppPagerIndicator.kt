package com.example.dukkanapp.core.common.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier
) {
    val description = stringResource(R.string.cd_pager_indicator, currentPage + 1, pageCount)

    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(AppDimens.pagerIndicatorSize)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }

    }
}