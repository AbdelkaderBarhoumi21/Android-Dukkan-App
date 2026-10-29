package com.example.dukkanapp.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dukkan semantic colors. Field names follow the Figma token names.
 *
 * Read them with `AppTheme.colors.brand` inside a composable.
 */
@Immutable
data class AppColors(
    // Surfaces
    val background: Color,
    val card: Color,
    val field: Color,
    val sheet: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textLabel: Color,

    // Lines
    val border: Color,
    val borderStrong: Color,

    // Brand
    val brand: Color,
    val brandPressed: Color,
    val brandSoft: Color,
    val brandSoftText: Color,
    val brandRing: Color,
    val onBrand: Color,

    // Status
    val success: Color,
    val successText: Color,
    val successSoft: Color,
    val warning: Color,
    val warningText: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val errorBorder: Color,

    // Commerce accents
    val deal: Color,
    val dealSoft: Color,
    val gold: Color,

    // Inverse and overlays
    val inverse: Color,
    val onInverse: Color,
    val scrim: Color,
    val shadow: Color,
)

internal val LightAppColors = AppColors(
    background = AppPalette.Slate50,
    card = AppPalette.White,
    field = AppPalette.Slate100,
    sheet = AppPalette.White,

    textPrimary = AppPalette.Slate900,
    textSecondary = AppPalette.Slate500,
    textTertiary = AppPalette.Slate400,
    textLabel = AppPalette.Slate700,

    border = AppPalette.Slate200,
    borderStrong = AppPalette.Slate300,

    brand = AppPalette.Blue600,
    brandPressed = AppPalette.Blue700,
    brandSoft = AppPalette.Blue50,
    brandSoftText = AppPalette.Blue700,
    brandRing = AppPalette.Blue100,
    onBrand = AppPalette.White,

    success = AppPalette.Green600,
    successText = AppPalette.Green700,
    successSoft = AppPalette.Green100,
    warning = AppPalette.Amber600,
    warningText = AppPalette.Amber700,
    warningSoft = AppPalette.Amber100,
    error = AppPalette.Red600,
    errorSoft = AppPalette.Red50,
    errorBorder = AppPalette.Red200,

    deal = AppPalette.Orange500,
    dealSoft = AppPalette.Orange100,
    gold = AppPalette.Amber500,

    inverse = AppPalette.Slate900,
    onInverse = AppPalette.White,
    scrim = AppPalette.Slate900,
    shadow = AppPalette.Slate900,
)

/** Official Figma `01b · Dark mode` values. Fields marked "derived" are ours (see the table above). */
internal val DarkAppColors = AppColors(
    background = AppPalette.Night950,
    card = AppPalette.Night900,
    field = AppPalette.Night800,
    sheet = AppPalette.Night850,

    textPrimary = AppPalette.Slate100,
    textSecondary = AppPalette.Slate400,
    textTertiary = AppPalette.Slate500,
    textLabel = AppPalette.Slate300,

    border = AppPalette.Night700,
    borderStrong = AppPalette.Slate700, // derived

    brand = AppPalette.Blue400,
    brandPressed = AppPalette.Blue500,
    brandSoft = AppPalette.Blue950,
    brandSoftText = AppPalette.Blue300,
    brandRing = AppPalette.Blue900,
    onBrand = AppPalette.White,

    success = AppPalette.Green500,
    successText = AppPalette.Green500,
    successSoft = AppPalette.Green950,
    warning = AppPalette.Amber500,
    warningText = AppPalette.Amber300,
    warningSoft = AppPalette.Amber950,
    error = AppPalette.Red400,
    errorSoft = AppPalette.Red950,
    errorBorder = AppPalette.Red400.copy(alpha = 0.5f),

    deal = AppPalette.Orange400,
    dealSoft = AppPalette.Orange950, // derived
    gold = AppPalette.Amber300,

    inverse = AppPalette.Slate100, // derived
    onInverse = AppPalette.Slate900, // derived
    scrim = AppPalette.Black,
    shadow = AppPalette.Black,
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors not provided wrap content in AppTheme")
}