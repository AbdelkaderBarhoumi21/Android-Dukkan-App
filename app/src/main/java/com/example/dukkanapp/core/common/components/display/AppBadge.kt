package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class AppBadgeVariant { Discount, Hot, New, Eta, Bestseller, LowStock, FreeDelivery, Rating }

@Composable
fun AppBadge(
    text: String,
    variant: AppBadgeVariant,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
) {
    val c = AppTheme.colors
    val (fill: Brush, content: Color) = when (variant) {
        AppBadgeVariant.Discount -> AppTheme.gradients.deal to c.onBrand
        AppBadgeVariant.Hot -> SolidColor(c.deal) to c.onBrand
        AppBadgeVariant.New -> SolidColor(c.inverse) to c.onInverse
        AppBadgeVariant.Eta, AppBadgeVariant.Bestseller -> SolidColor(c.brandSoft) to c.brandSoftText
        AppBadgeVariant.LowStock, AppBadgeVariant.Rating -> SolidColor(c.warningSoft) to c.warningText
        AppBadgeVariant.FreeDelivery -> SolidColor(c.successSoft) to c.successText
    }
    // Figma "Badge/*": M3/labelSmall 10.5/14 Bold
    val style = MaterialTheme.typography.labelSmall

    Row(
        modifier = modifier
            .height(
                AppDimens.badge
            )
            .background(fill, AppPillShape)
            .padding(horizontal = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space2)
    ) {
        leadingIcon?.let {
            Icon(
                it,
                null,
                tint = content,
                modifier = Modifier.size(AppDimens.space12)
            )
        }
        Text(text, style = style, color = content, maxLines = 1)
    }
}