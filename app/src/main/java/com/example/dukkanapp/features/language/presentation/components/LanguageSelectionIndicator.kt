package com.example.dukkanapp.features.language.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun LanguageSelectionIndicator(
    isSelected: Boolean
) {
    if (isSelected) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimens.iconSm)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.Transparent,
            modifier = Modifier
                .size(AppDimens.iconSm)
                .border(
                    width = AppDimens.BorderWidth,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}