package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme


@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null
) {
    val colors = AppTheme.colors
    val fill = if (selected) colors.brandSoft else colors.card
    val borderColor = if (selected) colors.brand else colors.border
    val content = if (selected) colors.brandSoftText else colors.textLabel
    Row(
        modifier = modifier
            .heightIn(AppDimens.chip)
            .clip(AppPillShape)
            .background(fill)
            .border(AppDimens.strokeThin, borderColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(
                horizontal = AppDimens.space12
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
    ) {
        leadingIcon?.let {
            Icon(
                painter = it, contentDescription = null, tint = content, modifier = Modifier.size(
                    AppDimens.iconSm
                )
            )
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}