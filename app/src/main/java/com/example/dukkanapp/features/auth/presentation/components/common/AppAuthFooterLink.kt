package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppAuthFooterLink(
    promptText: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        // Ghost button = brand text, ripple, Role.Button and a real touch area
        AppButton(
            text = actionText,
            onClick = onActionClick,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
        )

    }
}