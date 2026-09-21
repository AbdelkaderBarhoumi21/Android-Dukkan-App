package com.example.dukkanapp.features.language.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.language.presentation.model.LanguageUiModel

@Composable
fun LanguageItemCard(
    language: LanguageUiModel,
    isSelected: Boolean,
    isHighlightedStyle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(if (isHighlightedStyle) RoundedCornerShape(percent = 50) else MaterialTheme.shapes.small)
            .then(
                if (isHighlightedStyle) {
                    Modifier.border(
                        width = AppDimens.BorderWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(percent = 50)
                    )
                } else if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.SpaceXs, vertical = AppDimens.SpaceXs)

    ) {
        Image(
            painter = painterResource(id = language.flagRes),
            contentDescription = null,
            modifier = Modifier
                .size(AppDimens.flagSize)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(AppDimens.SpaceSm))
        Text(
            text = language.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        LanguageSelectionIndicator(isSelected = isSelected)
    }
}