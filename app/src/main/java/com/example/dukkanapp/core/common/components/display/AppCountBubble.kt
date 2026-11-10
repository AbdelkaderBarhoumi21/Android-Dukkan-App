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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppCountBubble(count: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(30.dp)
            .background(c.brand, CircleShape)
            .border(2.5.dp, c.card, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = AppTheme.textStyles.labelStrong,
            color = c.onBrand,
        )
    }
}