package com.example.dukkanapp.core.common.components.banner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppMessageBanner(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.radiusMd),
        color = if (isError) scheme.errorContainer else scheme.surfaceVariant,
        contentColor = if (isError) scheme.onErrorContainer else scheme.onSurfaceVariant
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(AppDimens.spaceMd)
        )
    }
}