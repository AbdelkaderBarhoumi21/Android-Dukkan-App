package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AuthButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = null,
    iconColor: Color = Color.Unspecified, // don't apply any tint the icon is drawed with the original color(Google logo)

) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.buttonHeight),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = borderColor?.let { color -> BorderStroke(AppDimens.borderWidth, color) }
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.iconSm),
            tint = iconColor
        )
        Spacer(Modifier.width(AppDimens.spaceSm))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}