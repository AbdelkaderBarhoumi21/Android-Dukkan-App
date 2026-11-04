package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppShadow
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow

enum class AppIconButtonVariant { White, Glass, Soft, Field, Brand, Dark }

@Composable
fun AppIcon(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppIconButtonVariant = AppIconButtonVariant.White,
    size: Dp = AppDimens.iconButton,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val gradients = AppTheme.gradients
    val cardShadow = AppTheme.shadows.card
    val (fill: Brush, tint: Color, shadow: AppShadow?) = when (variant) {
        AppIconButtonVariant.White -> Triple(
            SolidColor(colors.card),
            colors.textPrimary,
            cardShadow
        )

        AppIconButtonVariant.Glass -> Triple(
            SolidColor(colors.card.copy(alpha = 0.72f)),
            colors.textPrimary,
            cardShadow
        )

        AppIconButtonVariant.Soft -> Triple(SolidColor(colors.brandSoft), colors.brand, null)
        AppIconButtonVariant.Field -> Triple(SolidColor(colors.field), colors.textPrimary, null)
        AppIconButtonVariant.Brand -> Triple(gradients.brand, colors.onBrand, cardShadow)
        AppIconButtonVariant.Dark -> Triple(
            SolidColor(colors.inverse),
            colors.onInverse,
            cardShadow
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (shadow != null) Modifier.appShadow(shadow, AppPillShape) else Modifier)
            .clip(
                CircleShape
            )
            .background(fill)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = ripple(color = tint),
                interactionSource = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textTertiary,
            modifier = Modifier.size(AppDimens.icon)
        )
    }
}