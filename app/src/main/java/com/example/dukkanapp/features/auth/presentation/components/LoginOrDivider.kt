package com.example.dukkanapp.features.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun LoginOrDivider(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.or_divider)
) {
    val lineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FadingLine(
            modifier = Modifier.weight(1f),
            colors = listOf(
                Color.Transparent, lineColor
            )

        )
        Text(
            text = text,
            modifier = Modifier.padding(AppDimens.size3Xs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline
        )

        FadingLine(
            modifier = Modifier.weight(1f),
            colors = listOf(
                lineColor, Color.Transparent
            )
        )

    }
}

@Composable
private fun FadingLine(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(AppDimens.dividerHeight)
            .background(Brush.horizontalGradient(colors))
    ) { }
}