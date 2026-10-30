# 07 — AppDimens and AppShapes

**Files:**
- `core/designsystem/theme/AppDimens.kt` (moved from `core/utils/constants/AppDimens.kt` and rewritten)
- `core/designsystem/theme/AppShapes.kt` (moved from `core/utils/constants/AppShapes.kt` and rewritten)

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
| | `toast` | size/toast | 58 |
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
| | `focusRing` | focus ring around a field | 3 |
| A11y | `touchTargetMin` | Material minimum | 48 |

### Figma `01c · Size tokens`

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

### One radius scale (Figma: "Radius · one scale")

Figma now has **one** scale, **10 / 14 / 16 / 20 / 28 / full**, with the same values in Foundations and in the Compose chapter. The old conflict (Foundations 14/18/22 vs components 16/20) is gone.

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
package com.example.dukkanapp.core.designsystem.theme

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
    @Deprecated("Use iconSm (16) or icon (20)") val icon2Xs = 12.dp
    @Deprecated("Use controlLg", ReplaceWith("AppDimens.controlLg")) val buttonHeight = 52.dp
    @Deprecated("Use strokeThin", ReplaceWith("AppDimens.strokeThin")) val borderWidth = 1.dp
    @Deprecated("Use strokeThin", ReplaceWith("AppDimens.strokeThin")) val dividerHeight = 1.dp
    @Deprecated("Use screenPadding", ReplaceWith("AppDimens.screenPadding")) val screenHorizontalPadding = 20.dp
    @Deprecated("Checkbox radius; use radiusSm") val radiusXs = 6.dp
    @Deprecated("Not in the Figma scale; use radiusSm") val radius3Xs = 2.dp
    @Deprecated("Not in the Figma scale; use radiusSm") val radius2Xs = 4.dp
    @Deprecated("Use radiusCard", ReplaceWith("AppDimens.radiusCard")) val radiusXl = 20.dp
    @Deprecated("Use radiusSheet", ReplaceWith("AppDimens.radiusSheet")) val radius2Xl = 28.dp
}
```

## Code — `AppShapes.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

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
- [ ] I moved both files to `core/designsystem/theme` with Refactor → Move, and imports updated.
- [ ] App compiles. Deprecation warnings are expected. Fix them screen by screen with Alt+Enter.
- [ ] Old `core/utils/constants/AppShapes.kt` is gone (moved, not duplicated).
- [ ] No `radiusField`, `radiusXl` or `radius2Xl` in new code (only the deprecated aliases).
