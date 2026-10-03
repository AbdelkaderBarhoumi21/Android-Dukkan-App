package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.dukkanapp.core.utils.constants.AppDimens

object AppTextButtonDefaults {
    // By default -> text button has padding 8.dp vertical and 12.dp horizontal
    val contentPadding = PaddingValues(horizontal = AppDimens.size4Xs, vertical = AppDimens.size5Xs)
    val textStyle: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val color: Color @Composable get() = MaterialTheme.colorScheme.onSurface
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = AppTextButtonDefaults.textStyle,
    color: Color = AppTextButtonDefaults.color,
    contentPadding: PaddingValues = AppTextButtonDefaults.contentPadding
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = textStyle
        )
    }
}