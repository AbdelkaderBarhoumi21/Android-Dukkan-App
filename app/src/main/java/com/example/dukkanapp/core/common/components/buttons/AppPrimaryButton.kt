package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppPrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(
                AppDimens.ButtonHeight
            ),
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}