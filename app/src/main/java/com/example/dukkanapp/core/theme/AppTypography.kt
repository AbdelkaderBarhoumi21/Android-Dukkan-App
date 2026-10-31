package com.example.dukkanapp.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.R


/** Plus Jakarta Sans, the Dukkan font for Latin scripts (English, French). */
internal val AppFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Cairo, the Dukkan font for Arabic. */
internal val AppArabicFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

/** Turns a Figma spec ("ExtraBold · 24/32") into a [TextStyle]. */
internal fun figmaStyle(
    fontFamily: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

/** Figma's Arabic rule: one weight step lighter than the Latin style. */
internal fun FontWeight.arabicWeight(): FontWeight = when (this) {
    FontWeight.ExtraBold -> FontWeight.Bold
    FontWeight.Bold -> FontWeight.SemiBold
    FontWeight.SemiBold -> FontWeight.Medium
    else -> FontWeight.Normal
}

internal fun scriptStyle(
    arabic: Boolean,
    latinWeight: FontWeight,
    size: TextUnit,
    latinLineHeight: TextUnit,
    arabicLineHeight: TextUnit,
    latinLetterSpacing: TextUnit = 0.sp
): TextStyle =
    if (arabic) {
        figmaStyle(AppArabicFontFamily, latinWeight.arabicWeight(), size, arabicLineHeight)
    } else {
        figmaStyle(AppFontFamily, latinWeight, size, latinLineHeight, latinLetterSpacing)
    }


internal fun appTypography(arabic: Boolean): Typography {

    fun s(
        weight: FontWeight,
        size: TextUnit,
        latinLineHeight: TextUnit,
        arabicLineHeight: TextUnit
    ) = scriptStyle(
        arabic = arabic,
        latinWeight = weight,
        size = size,
        latinLineHeight = latinLineHeight,
        arabicLineHeight = arabicLineHeight
    )

    return Typography(
        displayLarge = s(FontWeight.ExtraBold, 40.sp, 48.sp, 56.sp),
        displayMedium = s(FontWeight.ExtraBold, 32.sp, 40.sp, 46.sp),
        displaySmall = s(FontWeight.ExtraBold, 28.sp, 36.sp, 40.sp),
        headlineLarge = s(FontWeight.ExtraBold, 24.sp, 32.sp, 36.sp),
        headlineMedium = s(FontWeight.ExtraBold, 22.sp, 28.sp, 32.sp),
        headlineSmall = s(FontWeight.ExtraBold, 20.sp, 26.sp, 30.sp),
        titleLarge = s(FontWeight.Bold, 18.sp, 24.sp, 28.sp),
        titleMedium = s(FontWeight.Bold, 16.sp, 22.sp, 26.sp),
        titleSmall = s(FontWeight.Bold, 14.sp, 20.sp, 22.sp),
        bodyLarge = s(FontWeight.Medium, 15.sp, 22.sp, 24.sp),
        bodyMedium = s(FontWeight.Medium, 14.sp, 20.sp, 22.sp),
        bodySmall = s(FontWeight.Medium, 13.sp, 18.sp, 20.sp),
        labelLarge = s(FontWeight.Bold, 15.sp, 20.sp, 22.sp),
        labelMedium = s(FontWeight.Bold, 12.5.sp, 16.sp, 18.sp),
        labelSmall = s(FontWeight.Bold, 12.sp, 16.sp, 18.sp),
    )
}

/** Built once, not on every recomposition. AppTheme picks one (doc 08). */
internal val LatinTypography: Typography = appTypography(arabic = false)
internal val ArabicTypography: Typography = appTypography(arabic = true)