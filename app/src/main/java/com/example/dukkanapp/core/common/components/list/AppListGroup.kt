package com.example.dukkanapp.core.common.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow

/** The white card that holds a group of rows. Put [AppListDivider] between rows. */
@Composable
fun AppListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appShadow(AppTheme.shadows.card, shape)
            .clip(shape)
            .background(AppTheme.colors.card),
        content = content,
    )
}
