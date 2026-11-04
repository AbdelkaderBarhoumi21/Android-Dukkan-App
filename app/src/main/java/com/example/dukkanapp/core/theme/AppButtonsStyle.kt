package com.example.dukkanapp.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

enum class AppButtonVariant { Primary, Deal, Dark, Tonal, Outline, Ghost, Danger }
enum class AppButtonSize(
    val height: Dp,
    val horizontalPadding: Dp
) {
    Large(AppDimens.buttonLg, AppDimens.space24),
    Compact(AppDimens.buttonCompact, AppDimens.space24),
    Medium(AppDimens.buttonMd, AppDimens.space20),
    Small(AppDimens.buttonSm, AppDimens.space16),
    ExtraSmall(AppDimens.buttonXs, AppDimens.space12),
}

/** Everything a pill needs to draw itself. Built by [appButtonStyle], never by screens. */
@Immutable
internal class AppButtonStyle(
    val background: Brush,
    val content: Color,
    val border: BorderStroke?,
    val shadow: AppShadow?,
)

/**
 * Maps a variant + state to colors (Figma `4.1 · Atoms` + `4.2 · Atom states`).
 * [pressed]: finger down. Pressed buttons have no shadow ("no lift").
 */
@Composable
internal fun appButtonStyle(
    variant: AppButtonVariant, enabled: Boolean, pressed: Boolean = false
): AppButtonStyle {
    val colors = AppTheme.colors
    val gradients = AppTheme.gradients
    val shadows = AppTheme.shadows

    if (!enabled) {
        val fill = if (variant == AppButtonVariant.Ghost) Color.Transparent else colors.field
        return AppButtonStyle(
            background = SolidColor(fill),
            content = colors.textTertiary,
            border = null,
            shadow = null,
        )
    }


    return when (variant) {
        AppButtonVariant.Primary -> AppButtonStyle(
            background = if (pressed) SolidColor(colors.brandPressed) else gradients.button,
            content = colors.onBrand,
            border = null,
            shadow = if (pressed) null else shadows.button,
        )

        AppButtonVariant.Deal -> AppButtonStyle(
            background = gradients.deal,
            content = colors.onBrand,
            border = null,
            shadow = if (pressed) null else shadows.deal,
        )

        AppButtonVariant.Dark -> AppButtonStyle(
            background = SolidColor(colors.inverse),
            content = colors.onInverse,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Tonal -> AppButtonStyle(
            background = SolidColor(if (pressed) colors.brandRing else colors.brandSoft),
            content = colors.brandSoftText,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Outline -> AppButtonStyle(
            background = SolidColor(if (pressed) colors.border else colors.card),
            content = colors.textPrimary,
            border = BorderStroke(AppDimens.strokeThin, colors.border),
            shadow = null,
        )

        AppButtonVariant.Ghost -> AppButtonStyle(
            background = SolidColor(Color.Transparent),
            content = colors.brand,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Danger -> AppButtonStyle(
            background = SolidColor(colors.errorSoft),
            content = colors.error,
            border = BorderStroke(AppDimens.strokeThin, colors.errorBorder),
            shadow = null,
        )
    }
}

/** Label style per size: real Figma text styles, read from the atoms. */
@Composable
internal fun appButtonTextStyle(size: AppButtonSize): TextStyle = when (size) {
    AppButtonSize.Large -> MaterialTheme.typography.labelLarge
    AppButtonSize.Compact, AppButtonSize.Medium -> AppTheme.textStyles.bodyStrong
    AppButtonSize.Small, AppButtonSize.ExtraSmall -> MaterialTheme.typography.labelMedium
}
