package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

/** Figma `Rating`: "★ 4.8 (1.2k)" and, if given, "⚡ 25 min". */

@Composable
fun AppRating(
    value: String,
    count: String?,
    modifier: Modifier = Modifier,
    eta: String? = null
) {
    val c = AppTheme.colors
    val style = MaterialTheme.typography.labelSmall
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(AppIcons.StarFilled),
            null,
            tint = c.gold,
            modifier = Modifier.size(AppDimens.iconXxs)
        )
        Spacer(Modifier.width(AppDimens.space4))
        Text(
            text = if (count != null) "$value ($count)" else value,
            style = style,
            color = c.textLabel
        )

        if (eta != null) {
            Spacer(Modifier.width(AppDimens.space12))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
            ) {
                Icon(
                    painter = painterResource(
                        AppIcons.Zap
                    ),
                    contentDescription = null,
                    tint = c.brand,
                    modifier = Modifier.size(
                        AppDimens.iconXxs
                    )
                )
                Text(eta, style = style, color = c.brand)
            }

        }
    }
}