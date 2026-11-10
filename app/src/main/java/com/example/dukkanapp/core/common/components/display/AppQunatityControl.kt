package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppIconButton
import com.example.dukkanapp.core.common.components.buttons.AppIconButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppStepperStyle { Compact, Large }

@Composable
fun AppQuantityControl(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppStepperStyle = AppStepperStyle.Compact
) {
    if (quantity == 0) {
        AppIconButton(
            icon = painterResource(AppIcons.Plus),
            contentDescription = stringResource(R.string.cd_add_to_cart),
            onClick = onIncrease,
            variant = AppIconButtonVariant.White,
            size = AppDimens.space32,
            modifier = modifier
        )
    }

    val c = AppTheme.colors
    val isLarge = style == AppStepperStyle.Large
    val fill = if (isLarge) c.field else c.brand
    val content = if (isLarge) c.textPrimary else c.onBrand
    val countStyle =
        if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall

    Row(
        modifier = modifier
            .size(
                width = if (isLarge) 112.dp else 88.dp,
                height = if (isLarge) AppDimens.controlLg else 32.dp
            )
            .background(fill, AppPillShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDecrease
        ) {
            Icon(
                painter = painterResource(AppIcons.Minus),
                stringResource(R.string.cd_decrease),
                tint = content,
                modifier = Modifier.size(AppDimens.iconSm)
            )
        }
        Text(
            text = quantity.toString(),
            style = countStyle,
            color = content
        )
        IconButton(onClick = onIncrease) {
            Icon(
                painterResource(AppIcons.Plus),
                stringResource(R.string.cd_increase),
                tint = content,
                modifier = Modifier.size(AppDimens.iconSm)
            )
        }

    }

}