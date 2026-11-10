package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Drawing only. Put `Modifier.selectable(selected, role = Role.RadioButton)` on the parent row. */
@Composable
fun AppRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    // The slot is always 22 so the row doesn't move when the radio turns on.
    Box(modifier = modifier.size(AppDimens.radio), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(AppDimens.radio)
                    .background(colors.brand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(AppDimens.radioDot)
                        .background(colors.card, CircleShape)
                )
            }
        } else {
            Box(
                Modifier
                    .size(AppDimens.radioOff)
                    .background(colors.card, CircleShape)
                    .border(AppDimens.strokeControl, colors.borderControl, CircleShape)
            )
        }
    }
}
