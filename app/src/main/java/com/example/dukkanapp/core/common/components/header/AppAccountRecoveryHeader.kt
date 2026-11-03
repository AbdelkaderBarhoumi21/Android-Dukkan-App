package com.example.dukkanapp.core.common.components.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.common.components.AppLogoMark
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppAccountRecoveryHeader(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        AppLogoMark(size = AppDimens.iconXxl)
        Spacer(Modifier.height(AppDimens.spaceMd))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(AppDimens.spaceSm))
        Text(
            text = subTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }


}