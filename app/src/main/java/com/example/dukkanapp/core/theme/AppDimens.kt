package com.example.dukkanapp.core.theme

import androidx.compose.ui.unit.dp

/** Dukkan spacing, radius and size tokens. Figma: "Dukkan / 4". */
object AppDimens {

    // Spacing: Figma space/*
    val space2 = 2.dp
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp
    val space64 = 64.dp

    // Radius: Figma "one scale" 10 / 14 / 16 / 20 / 28 / full
    val radiusSm = 10.dp
    val radiusMd = 14.dp
    val radiusLg = 16.dp
    val radiusCard = 20.dp
    val radiusSheet = 28.dp
    val radiusFull = 999.dp

    // Control heights: Button L / input, Button M / icon button, Button S / chip
    val controlLg = 52.dp
    val controlMd = 44.dp
    val controlSm = 36.dp

    // Icons (Lucide, 24 grid)
    val iconSm = 16.dp
    val icon = 20.dp
    val iconLg = 24.dp

    // Layout
    val navBar = 64.dp
    val screenPadding = 20.dp

    // Strokes: not part of the 4 dp grid on purpose
    val strokeThin = 1.2.dp
    val strokeFocus = 1.5.dp
    val focusRing = 3.dp

    // Accessibility
    val touchTargetMin = 48.dp

    // Feature sizes: used by one feature only; move them into the feature when you touch it
    val onboardingIllustrationHeight = 280.dp
    val pagerIndicatorSize = 6.dp
    val pagerIndicatorSelectedWidth = 22.dp
    val flagSize = 37.dp
    val productImageHeight = 180.dp
    val logoMark = 48.dp
}