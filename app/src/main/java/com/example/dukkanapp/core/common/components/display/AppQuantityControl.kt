package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
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