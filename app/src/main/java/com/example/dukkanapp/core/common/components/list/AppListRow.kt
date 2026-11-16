package com.example.dukkanapp.core.common.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppListRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colors
    val textStyles = AppTheme.textStyles
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.listRow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppDimens.listRowPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Box(
            Modifier
                .size(AppDimens.listTileIcon)
                .background(c.field, RoundedCornerShape(AppDimens.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.textPrimary, modifier = Modifier.size(AppDimens.icon))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = textStyles.bodyStrong, color = c.textPrimary)
            subtitle?.let { Text(it, style = textStyles.supporting, color = c.textSecondary) }
        }
        value?.let { Text(it, style = textStyles.helper, color = c.brand) }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                painterResource(AppIcons.ChevronRight),
                null,
                tint = c.textTertiary,
                modifier = Modifier.size(AppDimens.iconXs)
            )
        }
    }
}