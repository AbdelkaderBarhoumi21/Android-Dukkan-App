package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import java.util.Locale
import kotlin.math.abs

enum class AppPriceSize {
    Large, Medium, Small, Compact
}

/** 279000 → "279.000". Tunisian dinar has 3 decimals; always Latin digits. */
fun formatInd(millimes: Long): String =
    String.format(Locale.ROOT, "%d.%03d", millimes / 1000, abs(millimes % 1000))

@Composable
fun AppTextPrice(
    millimes: Long,
    currency: String,
    modifier: Modifier = Modifier,
    oldMillimes: Long? = null,
    size: AppPriceSize = AppPriceSize.Small,
    color: Color = AppTheme.colors.textPrimary,
) {
    val typography = MaterialTheme.typography
    val textStyles = AppTheme.textStyles
    val (amountStyle, currencyStyle) = when (size) {
        AppPriceSize.Large -> typography.headlineLarge to textStyles.titleCompact
        AppPriceSize.Medium -> typography.titleMedium to textStyles.overline
        AppPriceSize.Small -> textStyles.titleCompact to textStyles.overline
        AppPriceSize.Compact -> typography.titleSmall to textStyles.overline
    }
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
    ) {
        Text(
            text = buildAnnotatedString {
                append(formatInd(millimes))
                withStyle(currencyStyle.toSpanStyle()) {
                    append(" $currency")
                }
            },
            style = amountStyle,
            color = color,
            modifier = Modifier.alignByBaseline()
        )

        if (oldMillimes != null && oldMillimes > millimes) {
            Text(
                text = formatInd(oldMillimes),
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.LineThrough
                ),
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.alignByBaseline()
            )
        }

    }
}