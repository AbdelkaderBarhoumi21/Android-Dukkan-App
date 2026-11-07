package com.example.dukkanapp.features.language.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun LanguageSelectionIndicator(
    isSelected: Boolean
) {
    if (isSelected) {
        Icon(
            painter = painterResource(AppIcons.CircleCheck),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimens.iconSm)
        )
    } else {
        Icon(
            painter = painterResource(AppIcons.Check),
            contentDescription = null,
            tint = Color.Transparent,
            modifier = Modifier
                .size(AppDimens.iconSm)
                .border(
                    width = AppDimens.borderWidth,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}