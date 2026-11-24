package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.common.components.AppLogoMark
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

sealed interface AuthHeaderLogo {
    /** The Dukkan awning mark, no background. */
    data class Mark(val size: Dp) : AuthHeaderLogo

    /** 48 dp gradient tile holding a white Lucide icon. */
    data class Tile(val icon: Int) : AuthHeaderLogo
}

@Composable
fun AppAuthHeader(
    title: String,
    subtitle: String,
    logo: AuthHeaderLogo,
    modifier: Modifier = Modifier,
    centered: Boolean = logo is AuthHeaderLogo.Mark
) {
    val colors = AppTheme.colors
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        when (logo) {
            is AuthHeaderLogo.Mark -> AppLogoMark(
                size = logo.size
            )

            is AuthHeaderLogo.Tile -> Box(
                modifier = modifier
                    .size(AppDimens.logoMark)
                    .background(
                        AppTheme.gradients.brand, RoundedCornerShape(
                            AppDimens.radiusMd
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        logo.icon
                    ),
                    contentDescription = null,
                    tint = colors.onBrand,
                    modifier = Modifier.size(
                        AppDimens.iconLg
                    )
                )
            }
        }
        Spacer(
            modifier = Modifier.height(AppDimens.space8)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            textAlign = textAlign,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(AppDimens.space8))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = textAlign,
        )
    }

}