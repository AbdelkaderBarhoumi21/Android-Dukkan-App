package com.example.dukkanapp.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Accessor for Dukkan tokens inside composables:
 * `AppTheme.colors.brand`, `AppTheme.gradients.button`, `AppTheme.shadows.card`,
 * `AppTheme.textStyles.priceSmall`.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val gradients: AppGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalAppGradients.current

    val shadows: AppShadows
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShadows.current

    val textStyles: AppTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTextStyles.current
}

/**
 * Dukkan theme. Wrap every screen and every preview in it.
 *
 * @param darkTheme light or dark tokens (Figma 01 / 01b).
 * @param isArabic Cairo type scale instead of Plus Jakarta Sans (Figma "Typography · Cairo").
 *   Defaults to the current app language.
 */

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = LocalConfiguration.current.locales[0].language == "ar",
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val gradients = if (darkTheme) DarkAppGradients else LightAppGradients
    val shadows = if (darkTheme) DarkAppShadows else LightAppShadows
    val colorScheme =
        remember(darkTheme) { colors.toColorScheme(isDark = darkTheme) }

    val typography = if (isArabic) ArabicTypography else LatinTypography
    val textStyles = if (isArabic) ArabicTextStyles else LatinTextStyles
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppGradients provides gradients,
        LocalAppShadows provides shadows,
        LocalAppTextStyles provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes,
            content = content
        )
    }
}