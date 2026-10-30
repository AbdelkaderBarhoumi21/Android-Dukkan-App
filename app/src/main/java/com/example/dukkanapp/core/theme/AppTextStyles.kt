package com.example.dukkanapp.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Figma text styles that have no Material slot.
 * Read them with `AppTheme.textStyles.priceSmall`.
 */
@Immutable
data class AppTextStyles(
    val caption: TextStyle,
    /** Write the text in UPPERCASE yourself (Compose has no text-transform). */
    val overline: TextStyle,
    val priceLarge: TextStyle,
    val priceSmall: TextStyle,
    val fieldText: TextStyle,
    val fieldPlaceholder: TextStyle,
    val helper: TextStyle,
)

/** Columns: weight, size, Latin line, Arabic line. */
internal fun appTextStyles(arabic: Boolean): AppTextStyles = AppTextStyles(
    caption = scriptStyle(arabic, FontWeight.SemiBold, 11.sp, 14.sp, 16.sp),
    overline = scriptStyle(
        arabic, FontWeight.ExtraBold, 10.5.sp, 12.sp, 16.sp,
        latinLetterSpacing = 0.9.sp, // Latin only: tracking breaks Arabic letter joins
    ),
    priceLarge = scriptStyle(arabic, FontWeight.ExtraBold, 24.sp, 28.sp, 32.sp),
    priceSmall = scriptStyle(arabic, FontWeight.ExtraBold, 15.sp, 20.sp, 22.sp),
    fieldText = scriptStyle(arabic, FontWeight.SemiBold, 14.5.sp, 20.sp, 22.sp),
    fieldPlaceholder = scriptStyle(arabic, FontWeight.Medium, 14.5.sp, 20.sp, 22.sp),
    helper = scriptStyle(arabic, FontWeight.SemiBold, 11.5.sp, 14.sp, 16.sp),
)

internal val latinTextStyles: AppTextStyles = appTextStyles(arabic = false)
internal val arabicTextStyles: AppTextStyles = appTextStyles(arabic = true)
internal val LocalAppTextStyles = staticCompositionLocalOf<AppTextStyles> {
    error("AppTextStyles not provided: wrap content in AppTheme")
}