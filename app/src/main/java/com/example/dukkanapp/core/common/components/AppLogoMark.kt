package com.example.dukkanapp.core.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** The Dukkan mark alone. Use it at 48 dp and below (Figma rule). */
@Composable
fun AppLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = AppDimens.logoMark,
) {
    Image(
        painter = painterResource(R.drawable.ic_dukkan_mark),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(size),
    )
}

/** Mark + "dukkan" wordmark (or "دكّان" in Arabic, from strings.xml). */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        AppLogoMark(size = AppDimens.controlSm)
        Text(
            text = stringResource(R.string.logo_wordmark),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.035).em,
            ),
            color = AppTheme.colors.textPrimary,
        )
    }
}