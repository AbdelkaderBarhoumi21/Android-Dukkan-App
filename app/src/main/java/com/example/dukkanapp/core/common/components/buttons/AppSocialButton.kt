package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.appButtonStyle

/**
 * "Continue with Google / Facebook". Same pill as [AppButton] Outline,
 * but the logo keeps its brand colors (Image, not a tinted Icon).
 */
@Composable
fun AppSocialButton(
    text: String,
    logo: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val style = appButtonStyle(AppButtonVariant.Outline, enabled, pressed = isPressed && !isLoading)

    AppButtonContainer(
        onClick = onClick,
        style = style,
        interactionSource = interaction,
        height = AppDimens.buttonLg,
        horizontalPadding = AppDimens.space16,
        enabled = enabled,
        isLoading = isLoading,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
        ) {
            Image(
                painter = logo,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.icon)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = style.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}