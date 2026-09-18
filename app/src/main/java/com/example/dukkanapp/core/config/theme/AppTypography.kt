package com.example.dukkanapp.core.config.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import com.example.dukkanapp.R

private val AppFontFamily = FontFamily(
    Font(R.font.konnect_hairline, FontWeight.Thin),
    Font(R.font.konnect_hairline_italic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.konnect_thin, FontWeight.ExtraLight),
    Font(R.font.konnect_thin_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.konnect_light, FontWeight.Light),
    Font(R.font.konnect_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.konnect_regular, FontWeight.Normal),
    Font(R.font.konnect_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.konnect_medium, FontWeight.Medium),
    Font(R.font.konnect_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.konnect_semi_bold, FontWeight.SemiBold),
    Font(R.font.konnect_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.konnect_bold, FontWeight.Bold),
    Font(R.font.konnect_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.konnect_extra_bold, FontWeight.ExtraBold),
    Font(R.font.konnect_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.konnect_black, FontWeight.Black),
    Font(R.font.konnect_black_italic, FontWeight.Black, FontStyle.Italic)
)
internal val AppTypography=  Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)