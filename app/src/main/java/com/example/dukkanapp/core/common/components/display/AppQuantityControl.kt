package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppStepperStyle { Compact, Large }

@Composable
fun AppQuantityControl(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppStepperStyle = AppStepperStyle.Compact,
    canIncrease: Boolean = true
) {
    AnimatedContent(
        targetState = quantity == 0, label = "addToStepper", modifier = modifier
    ) { isEmpty ->
        if (isEmpty) {
            AppAddButton(
                onClick = onIncrease,
                enabled = canIncrease
            )

        } else {

        }
    }
}

@Composable
fun AppAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppDimens.addButton)
            .appShadow(
                AppTheme.shadows.card, CircleShape
            )
            .clip(
                CircleShape
            )
            .background(if (filled) c.brand else c.card)
            .clickable(
                enabled = enabled, onClick = onClick, role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(AppIcons.Plus),
            contentDescription = stringResource(R.string.cd_add_to_cart),
            tint = if (filled) c.onBrand else c.brand,
            modifier = Modifier.size(AppDimens.iconSm)
        )
    }
}


@Composable
fun AppStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    style: AppStepperStyle,
    canIncrease: Boolean
) {
    val c = AppTheme.colors
    val isLarge = style == AppStepperStyle.Large
    val isRemove = quantity == 1
    // Every number comes from AppDimens: the two styles only pick different tokens.
    val height = if (isLarge) AppDimens.cartStepper else AppDimens.stepper
    val minWidth = if (isLarge) AppDimens.cartStepperWidth else AppDimens.stepperWidth
    val buttonSize = if (isLarge) AppDimens.cartStepperButton else AppDimens.stepperButton
    val iconSize = if (isLarge) AppDimens.iconSm else AppDimens.iconXs
    val countStyle =
        if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall

    Row(
        modifier = Modifier
            .height(height)
            .widthIn(min = minWidth)
            .background(
                if (isLarge) c.field else c.brand, AppPillShape
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            icon = if (isRemove) AppIcons.Trash2 else AppIcons.Minus,
            contentDescription = stringResource(if (isRemove) R.string.cd_remove_from_cart else R.string.cd_decrease),
            onClick = if (isRemove) onRemove else onDecrease,
            circleSize = buttonSize,
            iconSize = iconSize,
            circle = if (isLarge) c.card else Color.Transparent,
            tint = if (isLarge) c.textPrimary else c.onBrand
        )
        Text(
            text = quantity.toString(),
            style = countStyle,
            color = if (isLarge) c.textPrimary else c.onBrand,
            modifier = Modifier.weight(1f, fill = false)
        )
        StepperButton(
            icon = AppIcons.Plus,
            contentDescription = stringResource(R.string.cd_increase),
            onClick = onIncrease,
            enabled = canIncrease,
            circleSize = buttonSize,
            iconSize = iconSize,
            circle = if (isLarge) c.brand else c.card.copy(alpha = 0.27f),
            tint = c.onBrand
        )
    }


}


/**
 * The touch area is a square as tall as the pill (32 or 52), so each end is easy to hit.
 * The visible circle inside it is the Figma 24 / 32 slot.
 */
@Composable
private fun StepperButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    circleSize: Dp,
    iconSize: Dp,
    circle: Color,
    tint: Color,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(circleSize)
                .background(circle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

