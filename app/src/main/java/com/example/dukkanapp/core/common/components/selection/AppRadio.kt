package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Drawing only. Put `Modifier.selectable(selected, role = Role.RadioButton)` on the parent row. */
@Composable
fun AppRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(colors.brand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier
                    .size(AppDimens.space8)
                    .background(colors.onBrand, CircleShape))
            }
        } else {
            Box(
                Modifier
                    .size(AppDimens.icon)
                    .background(colors.card, CircleShape)
                    .border(AppDimens.strokeFocus, colors.borderStrong, CircleShape)
            )
        }
    }
}