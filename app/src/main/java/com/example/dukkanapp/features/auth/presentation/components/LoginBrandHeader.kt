package com.example.dukkanapp.features.auth.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun LoginBrandHeader(
    logo: Painter,
    title: String,
    subTitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimens.sizeSm,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = logo,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.size4Xl)
        )
        Spacer(Modifier.height(AppDimens.spaceXl))
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