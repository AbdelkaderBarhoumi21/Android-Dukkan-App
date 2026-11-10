# 07 — AppDimens and AppShapes

**Files:**
- `core/theme/AppDimens.kt` (new; the old `core/utils/constants/AppDimens.kt` stays until the screens move)
- `core/theme/AppShapes.kt` (new; the old `core/utils/constants/AppShapes.kt` stays until the screens move)

## Why

Figma says **"Dukkan / 4 — every padding, gap and corner uses these."**
Today's `AppDimens` uses T-shirt names (`spaceSm`, `sizeXl`, `size3Xs`), and the same number lives under several names: 12 dp is `spaceSm`, `size3Xs` and `icon2Xs`. When reading `AppDimens.size3Xs` you can't tell it's 12, or whether it's a gap or a size.

v4 names spacing **by value**, exactly like Figma: `space/16` → `space16`. Radii and control sizes keep role names (`radiusCard`, `controlLg`), because Figma names them by role.

## Figma spec

| Group | Kotlin | Figma | dp |
|---|---|---|---|
| Spacing | `space2` | — (kept, hairline gaps) | 2 |
| | `space4` | space/4 | 4 |
| | `space8` | space/8 | 8 |
| | `space12` | space/12 | 12 |
| | `space16` | space/16 | 16 |
| | `space20` | space/20 | 20 |
| | `space24` | space/24 | 24 |
| | `space32` | space/32 | 32 |
| | `space48` | space/48 | 48 |
| | `space64` | — (kept, large gaps) | 64 |
| Radius | `radiusSm` | radius/sm | 10 |
| | `radiusMd` | radius/md | 14 |
| | `radiusLg` | radius/lg | 16 |
| | `radiusCard` | radius/card | 20 |
| | `radiusSheet` | radius/sheet | 28 |
| | `radiusScreen` | radius/screen | 44 (device frame, **Figma only**, not used in the app) |
| | `radiusFull` | radius/full | 999 (pill) |
| Sizes | `controlLg` | Button L / input | 52 |
| | `controlMd` | Button M / icon button | 44 |
| | `controlSm` | Button S | 36 |
| Component sizes | `buttonLg` | size/button-lg (primary & outline buttons) | 52 |
| | `buttonCompact` | size/button-compact (danger / compact buttons) | 48 |
| | `buttonMd` | size/button-md (secondary buttons) | 44 |
| | `buttonSm` | size/button-sm (small buttons) | 36 |
| | `buttonXs` | size/button-xs (inline "Apply" on coupons) | 26 |
| | `buttonSlide` | size/button-slide (slide to order / accept) | 58 |
| | `input` | size/input (text fields) | 52 |
| | `inputSearch` | size/input-search (map & header search pill) | 44 |
| | `iconButton` | size/icon-button (back, close, actions) | 44 |
| | `iconButtonSm` | size/icon-button-sm (sheet close) | 36 |
| | `chip` | size/chip (filter & choice chips) | **34** |
| | `badge` | size/badge (status & deal badges) | 20 |
| | `optionLg` | size/option-lg (option row with subtitle) | 66 |
| | `optionMd` | size/option-md (single-line option row) | 52 |
| | `optionSm` | size/option-sm (language rows) | 48 |
| | `stepper` | size/stepper (qty stepper) | 32 |
| | `addButton` | size/add-button (+ on product card) | 32 |
| | `wishlist` | size/wishlist (heart on product card) | 28 |
| | `checkbox` | size/checkbox | 20 |
| | `switchHeight` | size/switch-height (switch track) | 28 |
| | `switchWidth` | size/switch-width (switch track) | 46 |
| | `toast` | size/toast | 64 |
| | `screenWidth` | size/screen-width (design frame, previews only) | 393 |
| | `screenHeight` | size/screen-height (design frame, previews only) | 852 |
| Icons | `iconSm` | icon/sm: small icons in badges / steppers | 16 |
| | `iconMd` | icon/md (**new**) | 18 |
| | `icon` | icon/lg: fields, buttons, rows | 20 |
| | `iconLg` | icon/xl: app bar, bottom nav | 24 |
| Layout | `navBar` | size/nav-bar (customer bottom nav) | **66** |
| | `teamNavBar` | size/team-nav-bar (staff bottom nav) | 64 |
| | `screenPadding` | screen padding | 20 |
| Strokes | `strokeThin` | outline / chip / social border | 1.2 |
| | `strokeFocus` | focused / error / success input border | 1.5 |
| | `strokeSpinner` | spinner inside a loading button (Figma `4.2`) | 2.4 |
| | `focusRing` | focus ring around a field | 3 |
| A11y | `touchTargetMin` | Material minimum | 48 |

### Figma `1.4 · Sizes & spacing`

Figma now has a **Size tokens** chapter: every radius, component size, spacing and icon size, stored as local variables (collection "Sizes"). Select a layer in Figma and its W / H / corner-radius field shows a token chip, for example `size/button-lg`.
In code, every token stays in the one `AppDimens` object. The name is the Figma name in camelCase: `size/button-lg` → `AppDimens.buttonLg`, `size/option-md` → `AppDimens.optionMd`, `radius/screen` → `AppDimens.radiusScreen`.

Two old names don't follow that rule, because renaming them would silently change every caller:

| Figma | Kotlin | dp |
|---|---|---|
| icon/lg | `icon` (not `iconLg`) | 20 |
| icon/xl | `iconLg` (not `iconXl`) | 24 |

**What changed in this Figma update:**

| Token | Before | Now |
|---|---|---|
| `navBar` | 64 | **66** (customer app). Team app keeps 64 as `teamNavBar` |
| chip height | `controlSm` 36 | **`chip` 34** |
| `iconMd` | — | **18** (new) |
| `radiusScreen` | — | 44 (Figma device frame only) |
| component sizes | 3 `control*` sizes | the 26 `size/*` tokens above |

`controlLg` / `controlMd` / `controlSm` stay: they have the same values as `buttonLg` / `buttonMd` / `buttonSm`. Prefer the Figma name in new code (`input` for a field, `iconButton` for an icon button), so each token follows its own Figma variable.

### Component parts and component internals

The `size/*` variables cover the **outside** of a component (a stepper is 32 tall). Since the latest Figma update, 13 **inside** sizes are variables too, in a new **"Component parts"** group on `1.4 · Sizes & spacing`:

| Figma variable | Kotlin | dp |
|---|---|---|
| size/stepper-button | `stepperButton` | 24 |
| size/radio-dot | `radioDot` | 9 |
| radius/checkbox | `radiusCheckbox` | 6 |
| size/list-tile-icon | `listTileIcon` | 38 |
| size/option-tile | `optionTile` | 40 |
| size/count-bubble · count-bubble-sm | `countBubble` · `countBubbleSm` | 30 · 20 |
| size/toast-halo · toast-ring · toast-core | `toastHalo` · `toastRing` · `toastCore` | 44 · 34 · 24 |
| size/status-step-done · status-step · status-step-current | `statusStepDone` · `statusStep` · `statusStepCurrent` | 20 · 22 · 30 |

They follow the usual rule (Figma name in camelCase, `radius/checkbox` → `radiusCheckbox`), so Dev Mode chips and code match. `status-step` (22) is the **upcoming** step.

The rest of the inside sizes (the 14 dp icon in the stepper, the 112 dp cart stepper, the aurora blobs) still have no variable, so Figma draws them with plain numbers. Earlier versions of docs 11–14 typed those numbers in the composable (`Modifier.size(22.dp)`). That's gone now: **every dp lives in `AppDimens`**, including one-component sizes.

**Naming rule:** `<component><Part>` in camelCase, after the Figma layer name. Examples: Figma `Qty stepper` → its round slot → `stepperButton`; `Count bubble` → `countBubble`; `Toast` → halo → `toastHalo`. Search `AppDimens.toast` and you see every toast size in one place.

| Group | Tokens | Figma source (measured in the file) |
|---|---|---|
| Icons inside atoms | `iconXxs` 12, `iconXs` 14 | badge / rating / step icons are 12; checkbox check, chip, stepper, helper icons are 14 |
| Strokes | `strokeHairline` 1, `strokeControl` 1.6, `strokeBubble` 2.5 | toast border; checkbox & radio off border, selected option border; count bubble ring |
| Selection (no variable) | `radio` 22, `radioOff` 20, `switchThumb` 22, `switchPadding` 3, `switchThumbOffset` 18 | `Radio/*`, `Switch/*` |
| Quantity | `stepperWidth` 88, `cartStepper` 52, `cartStepperWidth` 112, `cartStepperButton` 32, `cartStepperPadding` 10 | `Qty stepper`, `Cart stepper` |
| Display | `avatarSm/Md/Header/Lg` 28/36/48/50, `progressTrack` 4, `pagerIndicator*` 6/22, `stepConnector` 2, `liveSegment` 5 | `Avatar/*`, `Pager`, `Status steps`, `Live order` |
| Tiles | `infoTile` 36, `liveOrderTile` 44, `languageBadge` 32, `stepNumber` 28, `emptyStateTile` 72 / `emptyStateIcon` 32 | `Option/English`, `Live order`, screens 02.6 and 14.x |
| Rows | `listRow` 58, `listRowPaddingH` 14, `sheetHandleWidth` 40, `sheetHandleHeight` 4.5 | `List rows`, `Sheet header` |
| Inputs | `searchField` 48, `otpBoxWidth` 52, `otpBoxHeight` 58 | `Search/field`, `OTP` |
| Toast | `toastGlyph` 13, `toastAction` 32, `toastTimer` 28, spinner, shadows … | `4.4 · Toast` |
| Molecules | `productCardWidth` 170, `productPhotoHeight` 124, `promoBanner` 164, `coupon` 96, `cartBar` 64, `checkoutBar` 66, `navCartButton` 58 … | `4.3 · Molecules` (doc 16) |
| Aurora | `auroraCompact/Short/Tall` 260/300/520, blob positions and radii, `auroraFade` 90 | screens 02.1 / 02.2 / 02.6 (doc 13) |

> **Your code already has some of these** under other names: `radio`, `switchPadding` and `iconXs` keep their names. `thumbOffsetDistance` becomes **`switchThumbOffset`** (put the caret on it, Shift+F6), so every switch size starts with `switch`.

**Where may a raw `dp` still appear?** Only in three theme files: `AppDimens.kt` (sizes), `AppShadows.kt` (the Figma effect styles: offset / blur / spread, doc 05) and `AppTypography.kt` (`sp`). The leak grep in doc 15 checks this.

### One radius scale (Figma: "Radius · one scale")

Figma now has **one** scale, **10 / 14 / 16 / 20 / 28 / full**, in chapter `1.4` and in the `Sizes` variables. The old conflict (Foundations 14/18/22 vs components 16/20) is gone.

**Component → radius token** (Figma table):

| Component | Token | dp |
|---|---|---|
| checkbox, swatch, small thumb | `radiusSm` | 10 |
| icon tile, info row, coupon | `radiusMd` | 14 |
| product photo, category tile, OTP box | `radiusLg` | 16 |
| card, product card | `radiusCard` | 20 |
| bottom sheet, dialog | `radiusSheet` | 28 |
| **input / field**, button, option, chip, bottom nav | `radiusFull` → `AppPillShape` | pill |

> **Inputs are pills now.** In the first Figma version text fields had a 16 dp radius. They are now "fully rounded like buttons" (52 dp high → 26 dp corners). Doc 11 uses `AppPillShape`.

## Step 0 — Rename the conflicting old names first (important)

Some old names stay, but **with new values**. If you just replace the file, every caller silently changes size:

| Old name (old value) | New value with same name | Do this first |
|---|---|---|
| `iconSm` (20) | `iconSm` = 16 | **Rename** to `icon` |
| `iconLg` (32) | `iconLg` = 24 | Not used today; just let it change |
| `radiusSm` (8) | 10 | Accept (Figma) |
| `radiusMd` (12) | 14 | Accept (Figma) |
| `radiusLg` (16) | 16 | Same value, nothing to do |
| `radiusXl` (24) | removed → deprecated alias = `radiusCard` (20) | Alt+Enter on the warning |
| `radius2Xl` (32) | removed → deprecated alias = `radiusSheet` (28) | Alt+Enter on the warning |

In Android Studio, open the **old** `core/utils/constants/AppDimens.kt`, put the caret on `iconSm`, press **Shift+F6** (Rename) and type `icon`. Every caller updates. Then continue.

## Code — `AppDimens.kt`

```kotlin
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
    // Figma draws these inside one component and has no size/* variable for them.
    // They still live here, named <component><Part>, so no composable writes a raw dp.

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

    // Legacy names: kept so the app compiles during the migration. Alt+Enter on each warning.
    @Deprecated("Use space4", ReplaceWith("AppDimens.space4")) val space2Xs = 4.dp
    @Deprecated("Use space8", ReplaceWith("AppDimens.space8")) val spaceXs = 8.dp
    @Deprecated("Use space12", ReplaceWith("AppDimens.space12")) val spaceSm = 12.dp
    @Deprecated("Use space16", ReplaceWith("AppDimens.space16")) val spaceMd = 16.dp
    @Deprecated("Use space24", ReplaceWith("AppDimens.space24")) val spaceLg = 24.dp
    @Deprecated("Use space32", ReplaceWith("AppDimens.space32")) val spaceXl = 32.dp
    @Deprecated("Use space4", ReplaceWith("AppDimens.space4")) val size5Xs = 4.dp
    @Deprecated("Use space8", ReplaceWith("AppDimens.space8")) val size4Xs = 8.dp
    @Deprecated("Use space12", ReplaceWith("AppDimens.space12")) val size3Xs = 12.dp
    @Deprecated("Use space16", ReplaceWith("AppDimens.space16")) val size2Xs = 16.dp
    @Deprecated("Use space20 or icon", ReplaceWith("AppDimens.space20")) val sizeXs = 20.dp
    @Deprecated("Use space24", ReplaceWith("AppDimens.space24")) val sizeSm = 24.dp
    @Deprecated("Use space32", ReplaceWith("AppDimens.space32")) val sizeMd = 32.dp
    @Deprecated("Use controlMd for icon buttons") val sizeLg = 40.dp
    @Deprecated("Use space48", ReplaceWith("AppDimens.space48")) val sizeXl = 48.dp
    @Deprecated("Use space48 or space64") val size2Xl = 56.dp
    @Deprecated("Use logoMark or a feature size") val size4Xl = 80.dp
    @Deprecated("Use space64", ReplaceWith("AppDimens.space64")) val iconXxl = 64.dp
    @Deprecated("Use iconXxs", ReplaceWith("AppDimens.iconXxs")) val icon2Xs = 12.dp
    @Deprecated("Use switchThumbOffset", ReplaceWith("AppDimens.switchThumbOffset")) val thumbOffsetDistance = 18.dp
    @Deprecated("Use controlLg", ReplaceWith("AppDimens.controlLg")) val buttonHeight = 52.dp
    @Deprecated("Use strokeThin", ReplaceWith("AppDimens.strokeThin")) val borderWidth = 1.dp
    @Deprecated("Use strokeThin", ReplaceWith("AppDimens.strokeThin")) val borderSubtleHeight = 1.dp
    @Deprecated("Use screenPadding", ReplaceWith("AppDimens.screenPadding")) val screenHorizontalPadding = 20.dp
    @Deprecated("Use radiusCheckbox", ReplaceWith("AppDimens.radiusCheckbox")) val radiusXs = 6.dp
    @Deprecated("Not in the Figma scale; use radiusSm") val radius3Xs = 2.dp
    @Deprecated("Not in the Figma scale; use radiusSm") val radius2Xs = 4.dp
    @Deprecated("Use radiusCard", ReplaceWith("AppDimens.radiusCard")) val radiusXl = 20.dp
    @Deprecated("Use radiusSheet", ReplaceWith("AppDimens.radiusSheet")) val radius2Xl = 28.dp
}
```

## Code — `AppShapes.kt`

```kotlin
package com.example.dukkanapp.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Material shapes, same as Figma's radius variables (one scale).
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
```

## Explained

### Why is `AppPillShape` public but `AppShapes` internal?

Features read Material shapes through `MaterialTheme.shapes.medium`. Nobody needs the `AppShapes` value directly.
`AppPillShape` isn't a Material slot, so components need a public name for it.

### `RoundedCornerShape(percent = 50)` vs `CircleShape`

Both round the short side completely. `CircleShape` is literally `RoundedCornerShape(50)`. We name it "pill" because that's the Figma word, and so a 340 × 52 button doesn't read as a "circle".

### Why are strokes 1.2 and 1.5, not 1?

Figma draws outlines at 1.2 px and focus/error borders at 1.5 px. Compose accepts fractional dp. On a 3× phone, 1.2 dp is about 3.6 px, so the difference from 1 dp is visible.

### Values that change on purpose

| Name | Old | New | Source |
|---|---|---|---|
| `buttonHeight` (legacy) | 45 | 52 | Figma `control/lg` |
| `screenHorizontalPadding` (legacy) | 16 | 20 | Figma `screen-pad` |
| `pagerIndicatorSize` | 8 | 6 | Figma pager dot 22 × 6 |
| `pagerIndicatorSelectedWidth` | 24 | 22 | same |
| `navBar` | 64 | 66 | Figma `size/nav-bar` (team app: `teamNavBar` 64) |

The legacy aliases carry the **new** values. Old screens move to the Figma sizes immediately, even before you rename the calls.

### Which token for which job

| Job | Token |
|---|---|
| Screen side padding | `screenPadding` (20) |
| Gap between fields | `space16` |
| Label → field gap | `space8` |
| Field → helper gap | `space8` (Figma shows 6–8) |
| Section gap | `space24` / `space32` |
| Primary button height | `buttonLg` (52) |
| Danger / compact button | `buttonCompact` (48) |
| Secondary button height | `buttonMd` (44) |
| Small button | `buttonSm` (36) |
| Coupon "Apply" | `buttonXs` (26) |
| Field height | `input` (52), shape `AppPillShape` |
| Search pill (map, header) | `inputSearch` (44) |
| Icon button size | `iconButton` (44); sheet close `iconButtonSm` (36) |
| Chip height | `chip` (34) |
| Badge height | `badge` (20) |
| Option / language row | `optionLg` / `optionMd` / `optionSm` (66 / 52 / 48) |
| Card corners | `radiusCard` |
| Product photo inside a card | `radiusLg` |
| Bottom sheet top corners | `radiusSheet` |
| Bottom nav height | `navBar` (66); team app `teamNavBar` (64) |
| Icon in field / button | `icon` (20) |
| Icon in app bar / bottom nav | `iconLg` (24) |

## Check

- [ ] I renamed old `iconSm` → `icon` **before** replacing the file.
- [ ] I moved both files to `core/theme` with Refactor → Move, and imports updated.
- [ ] App compiles. Deprecation warnings are expected. Fix them screen by screen with Alt+Enter.
- [ ] Old `core/utils/constants/AppDimens.kt` and `AppShapes.kt` are deleted once no screen imports them (doc 14).
- [ ] No `radiusField`, `radiusXl` or `radius2Xl` in new code (only the deprecated aliases).
- [ ] The "Component internals" block is in `AppDimens`, and `thumbOffsetDistance` is renamed to `switchThumbOffset`.
- [ ] No component in `core/common/components` writes `N.dp` (the doc 15 grep returns nothing).
