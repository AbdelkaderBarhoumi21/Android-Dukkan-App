package com.example.dukkanapp.core.common.components.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppListDivider(

) {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = AppDimens.listRowPaddingH + AppDimens.listTileIcon + AppDimens.space12,
        ),
        thickness = AppDimens.strokeHairline,
        color = AppTheme.colors.borderSubtle
    )
}