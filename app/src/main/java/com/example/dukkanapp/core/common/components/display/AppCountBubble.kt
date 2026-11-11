package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Figma `Count bubble`: 30 brand circle with a 2.5 white ring. */
@Composable
fun AppCountBubble(count: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppDimens.countBubble)
            .background(c.brand, CircleShape)
            .border(AppDimens.strokeBubble, c.card, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = AppTheme.textStyles.labelStrong,
            color = c.onBrand,
        )
    }
}