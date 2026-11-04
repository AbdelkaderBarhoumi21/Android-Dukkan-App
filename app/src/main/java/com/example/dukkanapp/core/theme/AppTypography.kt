package com.example.dukkanapp.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
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
    // Figma centers text in its line box, with no extra font padding above or below.
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/** Figma's Arabic rule: one weight step lighter than the Latin style. */
internal fun FontWeight.arabicWeight(): FontWeight = when (this) {
    FontWeight.ExtraBold -> FontWeight.Bold
    FontWeight.Bold -> FontWeight.SemiBold
    FontWeight.SemiBold -> FontWeight.Medium
    else -> FontWeight.Normal
}

/**
 * One Figma style in both scripts. [latinWeight] is the Plus Jakarta weight.
 * Cairo defaults to one step lighter, unless Figma gives its own [cairoWeight] or [cairoSize].
 */
internal fun scriptStyle(
    arabic: Boolean,
    latinWeight: FontWeight,
    size: TextUnit,
    latinLineHeight: TextUnit,
    arabicLineHeight: TextUnit,
    latinLetterSpacing: TextUnit = 0.sp,
    cairoWeight: FontWeight = latinWeight.arabicWeight(),
    cairoSize: TextUnit = size,
): TextStyle =
    if (arabic) {
        figmaStyle(AppArabicFontFamily, cairoWeight, cairoSize, arabicLineHeight)
    } else {
        figmaStyle(AppFontFamily, latinWeight, size, latinLineHeight, latinLetterSpacing)
    }

/** Builds the 15 Material slots. Columns: weight, size, Latin line, Arabic line, then optional tracking. */
internal fun appTypography(arabic: Boolean): Typography {
    fun s(
        weight: FontWeight,
        size: TextUnit,
        latinLine: TextUnit,
        arabicLine: TextUnit,
        tracking: TextUnit = 0.sp,
        cairoWeight: FontWeight = weight.arabicWeight(),
        cairoSize: TextUnit = size,
    ) = scriptStyle(arabic, weight, size, latinLine, arabicLine, tracking, cairoWeight, cairoSize)

    return Typography(
        displayLarge = s(FontWeight.ExtraBold, 40.sp, 48.sp, 60.sp, tracking = (-0.02).em),
        displayMedium = s(FontWeight.ExtraBold, 32.sp, 40.sp, 48.sp, tracking = (-0.02).em),
        displaySmall = s(FontWeight.ExtraBold, 28.sp, 36.sp, 42.sp, tracking = (-0.02).em),
        headlineLarge = s(FontWeight.ExtraBold, 24.sp, 32.sp, 36.sp, tracking = (-0.02).em),
        headlineMedium = s(FontWeight.ExtraBold, 22.sp, 28.sp, 34.sp, tracking = (-0.015).em),
        headlineSmall = s(FontWeight.ExtraBold, 20.sp, 26.sp, 30.sp, tracking = (-0.01).em),
        titleLarge = s(FontWeight.ExtraBold, 17.sp, 24.sp, 26.sp, tracking = (-0.01).em, cairoWeight = FontWeight.SemiBold),
        titleMedium = s(FontWeight.ExtraBold, 16.sp, 22.sp, 26.sp, cairoWeight = FontWeight.SemiBold),
        titleSmall = s(FontWeight.ExtraBold, 14.sp, 20.sp, 22.sp, cairoWeight = FontWeight.SemiBold),
        bodyLarge = s(FontWeight.Medium, 15.sp, 22.sp, 24.sp),
        bodyMedium = s(FontWeight.Medium, 14.sp, 20.sp, 22.sp),
        bodySmall = s(FontWeight.Medium, 13.sp, 18.sp, 20.sp),
        labelLarge = s(FontWeight.Bold, 15.sp, 20.sp, 22.sp),
        labelMedium = s(FontWeight.Bold, 12.5.sp, 16.sp, 20.sp, cairoSize = 13.sp),
        labelSmall = s(FontWeight.Bold, 10.5.sp, 14.sp, 16.sp),
    )
}

/** Built once, not on every recomposition. AppTheme picks one (doc 08). */
internal val LatinTypography: Typography = appTypography(arabic = false)
internal val ArabicTypography: Typography = appTypography(arabic = true)