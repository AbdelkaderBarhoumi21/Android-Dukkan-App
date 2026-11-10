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

    // Component sizes: Figma size/* (1.4 · Sizes & spacing)
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
    val toast = 64.dp          // toast
    val screenWidth = 393.dp   // design frame (previews only)
    val screenHeight = 852.dp  // design frame (previews only)

    // Icons (Lucide, 24 grid): Figma icon/sm, md, lg, xl
    val iconXxs = 12.dp // inside atoms: badge, rating star, step, toast ×, card arrow
    val iconXs = 14.dp  // inside atoms: checkbox check, chip, stepper, helper, perks
    val iconSm = 16.dp
    val iconMd = 18.dp
    val icon = 20.dp   // Figma icon/lg
    val iconLg = 24.dp // Figma icon/xl

    // Layout
    val navBar = 66.dp      // customer bottom nav
    val teamNavBar = 64.dp  // staff bottom nav
    val screenPadding = 20.dp

    // Strokes: not part of the 4 dp grid on purpose
    val strokeHairline = 1.dp   // toast border, dark card outline, list divider, strike line
    val strokeThin = 1.2.dp
    val strokeFocus = 1.5.dp
    val strokeControl = 1.6.dp  // checkbox / radio off border, selected option border
    val strokeSpinner = 2.4.dp
    val strokeBubble = 2.5.dp   // white ring around count bubbles
    val focusRing = 3.dp

    // Accessibility
    val touchTargetMin = 48.dp

    // ── Component internals ────────────────────────────────────────────────
    // Sizes drawn inside one component. They live here, named <component><Part>,
    // so no composable writes a raw dp (doc 07).

    // Component parts: Figma variables (1.4 · Sizes & spacing → "Component parts")
    val stepperButton = 24.dp      // size/stepper-button: round − / + / trash slot in the qty stepper
    val radioDot = 9.dp            // size/radio-dot
    val radiusCheckbox = 6.dp      // radius/checkbox
    val listTileIcon = 38.dp       // size/list-tile-icon: tile in account list rows
    val optionTile = 40.dp         // size/option-tile: tile in payment option rows
    val countBubble = 30.dp        // size/count-bubble: "2" bubble on the cart icon
    val countBubbleSm = 20.dp      // size/count-bubble-sm: deal count on the bottom-nav cart
    val toastHalo = 44.dp          // size/toast-halo
    val toastRing = 34.dp          // size/toast-ring
    val toastCore = 24.dp          // size/toast-core
    val statusStepDone = 20.dp     // size/status-step-done: completed step circle
    val statusStep = 22.dp         // size/status-step: upcoming step circle
    val statusStepCurrent = 30.dp  // size/status-step-current: active step pill height

    // Selection controls (Figma 4.1 Controls, no variable)
    val radio = 22.dp           // radio on
    val radioOff = 20.dp        // radio off
    val switchThumb = 22.dp
    val switchPadding = 3.dp
    val switchThumbOffset = 18.dp // switchWidth − switchThumb − 2 × switchPadding

    // Quantity (Figma 4.1 Qty stepper · Add)
    val stepperWidth = 88.dp       // compact stepper on product cards
    val cartStepper = 52.dp        // cart-line stepper height
    val cartStepperWidth = 112.dp
    val cartStepperButton = 32.dp  // white − and brand + circles
    val cartStepperPadding = 10.dp

    // Display atoms
    val avatarSm = 28.dp
    val avatarMd = 36.dp
    val avatarHeader = 48.dp       // home header
    val avatarLg = 50.dp
    val progressTrack = 4.dp       // sign-up progress, password strength
    val pagerIndicatorSize = 6.dp
    val pagerIndicatorSelectedWidth = 22.dp
    val stepConnector = 2.dp
    val liveSegment = 5.dp         // live-order progress segment height
    val liveSegmentGap = 6.dp

    // Tiles (rounded squares / circles holding one icon)
    val infoTile = 36.dp           // auth "sent to" card, verify steps
    val liveOrderTile = 44.dp      // live order card
    val languageBadge = 32.dp      // "EN" / "FR" circle in language rows
    val stepNumber = 28.dp         // numbered circle in verify-email steps
    val emptyStateTile = 72.dp
    val emptyStateIcon = 32.dp

    // Rows
    val listRow = 58.dp            // 38 tile + 2 × 10
    val listRowPaddingH = 14.dp
    val sheetHandleWidth = 40.dp
    val sheetHandleHeight = 4.5.dp

    // Inputs
    val searchField = 48.dp        // flat search bar inside pages
    val otpBoxWidth = 52.dp
    val otpBoxHeight = 58.dp

    // Toast (Figma 4.4)
    val toastGlyph = 13.dp
    val toastSpinner = 20.dp
    val toastSpinnerStroke = 2.5.dp
    val toastTimer = 28.dp
    val toastTimerStroke = 2.dp
    val toastAction = 32.dp
    val toastActionPaddingH = 14.dp
    val toastPaddingStart = 10.dp  // icon side; close side uses space12
    val toastSlide = 16.dp         // enter animation distance
    val toastSwipeThreshold = 56.dp
    val toastCoreShadowY = 3.dp
    val toastCoreShadowBlur = 8.dp
    val toastActionShadowY = 4.dp
    val toastActionShadowBlur = 10.dp
    val toastActionShadowSpread = (-2).dp

    // Molecules (Figma 4.3, doc 16)
    val productCardWidth = 170.dp
    val productPhotoHeight = 124.dp
    val categoryTile = 64.dp
    val categoryCardPhoto = 92.dp
    val brandLogo = 62.dp
    val brandPhoto = 54.dp
    val promoBanner = 164.dp
    val promoPadding = 18.dp
    val promoPhotoWidth = 126.dp
    val promoPhotoHeight = 140.dp
    val promoCta = 28.dp
    val coupon = 96.dp
    val couponStub = 82.dp
    val couponNotch = 16.dp
    val couponDash = 3.dp
    val couponDashGap = 4.dp
    val cartBar = 64.dp
    val checkoutBar = 66.dp
    val cartThumb = 34.dp
    val navBarInset = 16.dp        // bar sits 16 from the screen edges
    val navActivePillWidth = 44.dp
    val navActivePillHeight = 28.dp
    val navCartButton = 58.dp
    val navCartRing = 70.dp
    val headerActionsWidth = 88.dp // heart + bell capsule in the home header

    // Aurora header (Figma 02.1 / 02.2 / 02.6, doc 13)
    val auroraCompact = 260.dp
    val auroraShort = 300.dp
    val auroraTall = 520.dp
    val auroraFade = 90.dp
    val auroraBlueY = 20.dp
    val auroraBlueRadius = 230.dp
    val auroraSkyY = 60.dp
    val auroraSkyRadius = 190.dp
    val auroraSunRadius = 150.dp
    val auroraSunYShort = 250.dp
    val auroraSunYTall = 380.dp
    val cardHighlightRadius = 90.dp

    // Feature sizes: used by one feature only; move them into the feature when you touch it
    val onboardingIllustrationHeight = 280.dp
    val flagSize = 37.dp
    val productImageHeight = 180.dp
    val logoMark = 48.dp
    val logoMarkLg = 56.dp         // welcome screen mark
}
