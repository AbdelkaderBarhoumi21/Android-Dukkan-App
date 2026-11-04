package com.example.dukkanapp.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Figma text styles that have no Material slot.
 * Read them with `AppTheme.textStyles.bodyStrong`.
 */
@Immutable
data class AppTextStyles(
    val caption: TextStyle,
    /** Write the text in UPPERCASE yourself (Compose has no text-transform). */
    val overline: TextStyle,
    val fieldText: TextStyle,
    val helper: TextStyle,
    val titleCompact: TextStyle,
    val labelStrong: TextStyle,
    val bodyStrong: TextStyle,
    val supporting: TextStyle,
    val micro: TextStyle,
)

/** Columns: weight, size, Latin line, Arabic line. */
internal fun appTextStyles(arabic: Boolean): AppTextStyles = AppTextStyles(
    caption = scriptStyle(arabic, FontWeight.SemiBold, 11.sp, 14.sp, 16.sp),
    overline = scriptStyle(
        arabic, FontWeight.ExtraBold, 10.5.sp, 12.sp, 16.sp,
        latinLetterSpacing = 0.086.em, // Latin only: tracking breaks Arabic letter joins
    ),
    fieldText = scriptStyle(arabic, FontWeight.SemiBold, 14.5.sp, 20.sp, 22.sp),
    helper = scriptStyle(arabic, FontWeight.SemiBold, 11.5.sp, 14.sp, 18.sp),
    titleCompact = scriptStyle(arabic, FontWeight.ExtraBold, 15.sp, 20.sp, 24.sp),
    labelStrong = scriptStyle(arabic, FontWeight.ExtraBold, 12.5.sp, 16.sp, 20.sp),
    bodyStrong = scriptStyle(arabic, FontWeight.Bold, 13.5.sp, 18.sp, 22.sp),
    supporting = scriptStyle(arabic, FontWeight.Medium, 11.5.sp, 16.sp, 18.sp),
    micro = scriptStyle(arabic, FontWeight.Medium, 10.5.sp, 14.sp, 16.sp),
)

internal val LatinTextStyles: AppTextStyles = appTextStyles(arabic = false)
internal val ArabicTextStyles: AppTextStyles = appTextStyles(arabic = true)
internal val LocalAppTextStyles = staticCompositionLocalOf<AppTextStyles> {
    error("AppTextStyles not provided: wrap content in AppTheme")
}