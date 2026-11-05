package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.dukkanapp.core.theme.AppTheme

/** Validation look of a field. The ViewModel decides it; the field only draws it. */
enum class AppFieldStatus { Default, Error, Success }

@Immutable
internal data class AppFieldColors(
    val fill: Color,
    val border: Color?,
    val showRing: Boolean,
    val label: Color,
    val text: Color,
    val leadingIcon: Color,
    val helper: Color,
)

@Composable
internal fun appFieldColors(
    status: AppFieldStatus,
    isFocused: Boolean,
    enabled: Boolean = true,
): AppFieldColors {
    val colors = AppTheme.colors
    return when {
        // Figma 4.2: disabled is drawn with tokens, not 50 % opacity, so it also works on dark.
        !enabled -> AppFieldColors(
            fill = colors.field,
            border = null,
            showRing = false,
            label = colors.textTertiary,
            text = colors.textTertiary,
            leadingIcon = colors.borderStrong, // Figma: slate-300
            helper = colors.textTertiary,
        )

        status == AppFieldStatus.Error -> AppFieldColors(
            fill = colors.errorSoft,
            border = colors.error,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.error,
            helper = colors.error,
        )

        status == AppFieldStatus.Success -> AppFieldColors(
            fill = colors.card,
            border = colors.success,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.textTertiary,
            helper = colors.success,
        )

        isFocused -> AppFieldColors(
            fill = colors.card,
            border = colors.brand,
            showRing = true,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.brand,
            helper = colors.textSecondary,
        )

        else -> AppFieldColors(
            fill = colors.field,
            border = null,
            showRing = false,
            label = colors.textPrimary,
            text = colors.textPrimary,
            leadingIcon = colors.textTertiary,
            helper = colors.textSecondary,
        )
    }
}