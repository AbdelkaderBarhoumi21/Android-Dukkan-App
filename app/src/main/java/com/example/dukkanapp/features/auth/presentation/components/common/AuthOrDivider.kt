package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AuthOrDivider(
    text: String,
    modifier: Modifier = Modifier,
    showLines: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            AppDimens.space12,
            Alignment.CenterHorizontally
        )
    ) {
        if (showLines) HorizontalDivider(
            Modifier.weight(1f),
            thickness = AppDimens.strokeThin,
            color = AppTheme.colors.border
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textTertiary
            )
        )
        if (showLines) HorizontalDivider(
            Modifier.weight(1f),
            thickness = AppDimens.strokeThin,
            color = AppTheme.colors.border
        )
    }
}