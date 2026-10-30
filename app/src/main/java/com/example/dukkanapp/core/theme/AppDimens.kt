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
    val radiusScreen = 44.dp // device frame, Figma only
    val radiusFull = 999.dp

    // Control heights: Button L / input, Button M / icon button, Button S
    val controlLg = 52.dp
    val controlMd = 44.dp
    val controlSm = 36.dp

    // Component sizes: Figma size/* (01c · Size tokens)
    val buttonLg = 52.dp       // primary & outline buttons
    val buttonCompact = 48.dp  // danger / compact buttons
    val buttonMd = 44.dp       // secondary buttons
    val buttonSm = 36.dp       // small buttons
    val buttonXs = 26.dp       // inline "Apply" on coupons
    val buttonSlide = 58.dp    // slide to order / accept
    val input = 52.dp          // text fields
    val inputSearch = 44.dp    // map & header search pill
    val iconButton = 44.dp     // back, close, actions
    val iconButtonSm = 36.dp   // sheet close
    val chip = 34.dp           // filter & choice chips
    val badge = 20.dp          // status & deal badges
    val optionLg = 66.dp       // option row with subtitle
    val optionMd = 52.dp       // single-line option row
    val optionSm = 48.dp       // language rows
    val stepper = 32.dp        // qty stepper
    val addButton = 32.dp      // + on product card
    val wishlist = 28.dp       // heart on product card
    val checkbox = 20.dp       // checkbox
    val switchHeight = 28.dp   // switch track
    val switchWidth = 46.dp    // switch track
    val toast = 58.dp          // toast
    val screenWidth = 393.dp   // design frame (previews only)
    val screenHeight = 852.dp  // design frame (previews only)

    // Icons (Lucide, 24 grid): Figma icon/sm, md, lg, xl
    val iconSm = 16.dp
    val iconMd = 18.dp
    val icon = 20.dp   // Figma icon/lg
    val iconLg = 24.dp // Figma icon/xl

    // Layout
    val navBar = 66.dp      // customer bottom nav
    val teamNavBar = 64.dp  // staff bottom nav
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
