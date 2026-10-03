package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import com.example.dukkanapp.core.utils.constants.AppDimens

object AppTextButtonDefaults {
    // By default -> text button has padding 8.dp vertical and 12.dp horizontal
    val contentPadding = PaddingValues(horizontal = AppDimens.size4Xs, vertical = AppDimens.size5Xs)
    val textStyle: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val color: Color @Composable get() = MaterialTheme.colorScheme.onSurface
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = AppTextButtonDefaults.textStyle,
    color: Color = AppTextButtonDefaults.color
) {
    Text(
        text = text,
        style = textStyle,
        color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = AppDimens.spaceSm)
    )
}