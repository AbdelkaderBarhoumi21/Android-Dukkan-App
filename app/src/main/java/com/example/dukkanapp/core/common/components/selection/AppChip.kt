package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

/**
 * Figma `4.1 · Chip`:
 * [Outline] choice chip (selected = soft blue + blue border),
 * [Dark] filter trigger (opens the filter sheet),
 * [Brand] solid "All" pill at the start of a category row.
 */
enum class AppChipVariant { Outline, Dark, Brand }

@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    variant: AppChipVariant = AppChipVariant.Outline,
) {
    val colors = AppTheme.colors
    val fill: Color
    val stroke: Color?
    val content: Color
    when (variant) {
        AppChipVariant.Outline -> {
            fill = if (selected) colors.brandSoft else colors.card
            stroke = if (selected) colors.brand else colors.border
            content = if (selected) colors.brandSoftText else colors.textLabel
        }
        AppChipVariant.Dark -> {
            fill = colors.inverse
            stroke = null
            content = colors.onInverse
        }
        AppChipVariant.Brand -> {
            fill = colors.brand
            stroke = null
            content = colors.onBrand
        }
    }
    val click = if (variant == AppChipVariant.Dark) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
    }

    Row(
        modifier = modifier
            .heightIn(min = AppDimens.chip)
            .clip(AppPillShape)
            .background(fill)
            .then(if (stroke != null) Modifier.border(AppDimens.strokeThin, stroke, AppPillShape) else Modifier)
            .then(click)
            .padding(horizontal = AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        leadingIcon?.let {
            Icon(painter = it, contentDescription = null, tint = content, modifier = Modifier.size(AppDimens.iconXs))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
