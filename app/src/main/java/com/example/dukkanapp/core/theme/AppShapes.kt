package com.example.dukkanapp.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Material shapes, same as Figma "06 · Compose tokens → Shape.kt" (one scale).
 * Material widgets read these (cards → medium/large, sheets → extraLarge, menus → extraSmall).
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.radiusSm),     // 10: checkbox, swatch, small thumb
    small = RoundedCornerShape(AppDimens.radiusMd),          // 14: icon tile, info row, coupon
    medium = RoundedCornerShape(AppDimens.radiusLg),         // 16: photo, tile, OTP box
    large = RoundedCornerShape(AppDimens.radiusCard),        // 20: card, product card
    extraLarge = RoundedCornerShape(AppDimens.radiusSheet),  // 28: bottom sheet, dialog
)

/** radius/full: buttons, inputs, options, chips, badges, bottom nav. */
val AppPillShape = RoundedCornerShape(percent = 50)