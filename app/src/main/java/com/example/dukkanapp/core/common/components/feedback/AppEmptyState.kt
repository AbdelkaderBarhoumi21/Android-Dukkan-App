package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

data class AppEmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
    val variant: AppButtonVariant = AppButtonVariant.Primary
)

@Composable
fun AppEmptyState(
    icon: Painter,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: AppEmptyStateAction? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimens.space32
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(
                    AppDimens.emptyStateTile
                )
                .background(
                    AppTheme.colors.brandSoft,
                    RoundedCornerShape(AppDimens.radiusCard)
                ),
            contentAlignment = Alignment.Center

        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = AppTheme.colors.brand,
                modifier = Modifier.size(AppDimens.emptyStateIcon)
            )
        }
        Spacer(
            Modifier.height(AppDimens.space20)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(
            Modifier.height(AppDimens.space20)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(Modifier.height(AppDimens.space24))
            AppButton(
                action.label,
                onClick = action.onClick,
                variant = action.variant
            )
        }
    }
}