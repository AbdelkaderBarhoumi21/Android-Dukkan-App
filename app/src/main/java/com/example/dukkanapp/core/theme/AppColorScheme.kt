package com.example.dukkanapp.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Builds Material's [ColorScheme] from Dukkan's semantic colors so Material widgets
 * and Dukkan components always agree.
 */
internal fun AppColors.toColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brandSoftText,
            inversePrimary = brandRing,
            secondary = deal,
            onSecondary = onBrand,
            secondaryContainer = dealSoft,
            onSecondaryContainer = textPrimary,
            tertiary = gold,
            onTertiary = AppPalette.Slate900,
            tertiaryContainer = warningSoft,
            onTertiaryContainer = warningText,
            background = background,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = field,
            onSurfaceVariant = textSecondary,
            surfaceTint = card,
            inverseSurface = inverse,
            inverseOnSurface = onInverse,
            error = error,
            onError = onBrand,
            errorContainer = errorSoft,
            onErrorContainer = error,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = card,
            surfaceDim = field,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = sheet,
            surfaceContainerHigh = sheet,
            surfaceContainerHighest = field,
        )
    } else {
        lightColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brandSoftText,
            inversePrimary = brandRing,
            secondary = deal,
            onSecondary = onBrand,
            secondaryContainer = dealSoft,
            onSecondaryContainer = textPrimary,
            tertiary = gold,
            onTertiary = AppPalette.Slate900,
            tertiaryContainer = warningSoft,
            onTertiaryContainer = warningText,
            background = background,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = field,
            onSurfaceVariant = textSecondary,
            surfaceTint = card,
            inverseSurface = inverse,
            inverseOnSurface = onInverse,
            error = error,
            onError = onBrand,
            errorContainer = errorSoft,
            onErrorContainer = error,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = card,
            surfaceDim = field,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = sheet,
            surfaceContainerHigh = sheet,
            surfaceContainerHighest = field,
        )
    }
