package com.example.dukkanapp.core.common.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val track by animateColorAsState(
        if (checked) colors.brand else colors.border,
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        if (checked) AppDimens.switchThumbOffset else Dp.Hairline,
        label = "switchThumbOffset"
    )

    val toggle = if (onCheckChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckChange,
            role = Role.Switch,
            enabled = enabled
        )
    } else {
        Modifier // the parent row is toggleable (list row with a switch)
    }

    Box(
        modifier = modifier
            .then(toggle)
            .size(
                width = AppDimens.switchWidth,
                height = AppDimens.switchHeight
            )
            .background(track, AppPillShape)
            .padding(AppDimens.switchPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(AppDimens.switchThumb)
                .appShadow(
                    AppTheme.shadows.card,
                    CircleShape
                )
                .background(colors.card, CircleShape)
        )
    }
}