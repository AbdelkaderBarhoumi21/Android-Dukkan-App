# 03 — AppColors (semantic colors, light + dark)

**File (new):** `core/theme/AppColors.kt`
**Replaces:** `core/utils/constants/AppExtendedColors.kt` (delete it at the end of doc 08).

## Why

Material's `ColorScheme` has about 35 slots, but Figma speaks a different language: `surface/field`, `text/tertiary`, `accent/deal`, `brand/soft`.
Material has no slot for deal, gold, success or warning, and forcing "deal" into `secondary` makes code hard to read.

So Dukkan has **one semantic class**, `AppColors`, whose fields are the Figma names in camelCase.
It replaces the old `AppExtendedColors` (success, warning, info, sale, favorite), which was a second, partial layer next to `ColorScheme`.
Doc 04 then **derives** Material's `ColorScheme` from `AppColors`, so there is one source of truth.

- **Value object:** `@Immutable data class`. Compose can skip recomposition because it knows the value never changes.
- **Light and dark are two instances** of the same class. Widgets never ask "am I dark?".
- **Fail loud:** if someone forgets `AppTheme`, the local throws an error with a clear message instead of drawing wrong colors.

## Figma spec

Light values: `1.1 · Colour & effects → Semantic tokens · light`. Dark values: `1.2 · Dark mode → Semantic tokens · dark`. Both are **official Figma values** now. Same names in both modes; only the values change.

| Field | Figma name | Light | Dark | Used by |
|---|---|---|---|---|
| `background` | surface/background | Slate50 `#F7F9FB` | Night950 `#0B1220` | screen background |
| `card` | surface/card | White | Night900 `#111A2E` | cards, outline buttons, focused input |
| `field` | surface/field | Slate100 `#F1F5F9` | Night800 `#1A2540` | input fill, search field, icon button "field", disabled fill |
| `sheet` | surface/sheet (**new**) | White | Night850 `#16213A` | bottom sheets, dialogs, menus |
| `textPrimary` | text/primary | Slate900 `#0F172A` | Slate100 `#F1F5F9` | titles, values |
| `textLabel` | text/label (**new**) | Slate700 `#334155` | Slate300 `#CBD5E1` | checkbox labels, chip labels, trust row |
| `textSecondary` | text/secondary | Slate500 `#64748B` | Slate400 `#94A3B8` | subtitles, body copy |
| `textTertiary` | text/tertiary | Slate400 `#94A3B8` | Slate500 `#64748B` | placeholders, "or", hints, idle icons, disabled text |
| `border` | border/default | Slate200 `#E2E8F0` | Night700 `#24314F` | outline buttons, chips, cards, switch off, dark card outline |
| `borderStrong` | border/strong | Slate300 `#CBD5E1` | Slate600 `#475569` | stronger dividers and outlines (no longer used by checkbox / radio) |
| `borderControl` | border/control (**new**) | Slate500 `#64748B` | Slate500 `#64748B` | unchecked checkbox and radio outline: ≥ 3 : 1 on white (4.8), background (4.5), dark card (3.6), dark field (3.2) |
| `brand` | brand/primary | Blue600 `#2563EB` | Blue400 `#6090FA` | links, selected, focus border, checkbox on |
| `brandPressed` | brand/pressed | Blue700 `#1D4ED8` | Blue500 `#3B6FF6` | pressed fill |
| `brandSoft` | brand/soft | Blue50 `#EFF5FF` | Blue950 `#172554` | tonal button, selected chip, info badge |
| `brandSoftText` | brand/soft-text (**new**) | Blue700 `#1D4ED8` | Blue300 `#93B4FD` | text and icons **on `brandSoft`**: tonal label, "25 min" / "Bestseller" badges, avatar initials, numbered step circles |
| `brandRing` | brand/ring (**new**) | Blue100 `#DBE7FE` | Blue900 `#1E3A8A` | 3 dp focus ring, avatar initials fill |
| `onBrand` | on/primary | White | White | text and icons on `brand` and on brand gradients |
| `success` | status/success | Green600 `#16A34A` | Green500 `#22C55E` | success border, helper, check icon |
| `successText` | status/success-text (**new**) | Green700 `#15803D` | Green500 `#22C55E` | text on `successSoft` ("Free delivery" badge, success banner) |
| `successSoft` | status/success-soft | Green100 `#DCFCE7` | Green950 `#0F2E1D` | "Free delivery" badge, "Check your inbox" card |
| `warning` | status/warning | Amber600 `#D97706` | Amber500 `#F59E0B` | warning icon |
| `warningText` | status/warning-text | Amber700 `#B45309` | Amber300 `#FCD34D` | text on `warningSoft` |
| `warningSoft` | status/warning-soft | Amber100 `#FEF3C7` | Amber950 `#33260A` | "Only 2 left", rating badge |
| `error` | status/error | Red600 `#DC2626` | Red400 `#F87171` | error border, helper, danger label |
| `errorSoft` | status/error-soft | Red50 `#FEF2F2` | Red950 `#3A1418` | input in error, danger button fill |
| `errorBorder` | status/error-border | Red200 `#FECACA` | Red400 at 50 % (`#F87171` · 50 %) | danger button border |
| `deal` | accent/deal | Orange500 `#F97316` | Orange400 `#FB923C` | "Hot deal" badge, timers |
| `gold` | accent/gold | Amber500 `#F59E0B` | Amber300 `#FCD34D` | filled stars, points, Gold member |

### Newer Figma tokens

| Field | Figma | Light | Dark | Used for |
|---|---|---|---|---|
| `iconWishlist` | icon/wishlist | Red500 `#EF4444` | Red400 `#F87171` | filled wishlist heart |
| `borderSubtle` | border/subtle | Slate150 `#EEF2F6` | Night800 `#1A2540` | dividers inside cards |
| `handle` | surface/handle | Slate250 `#D5DCE6` | Night700 `#24314F` | bottom-sheet drag handle |
| `skeleton` | surface/skeleton | SkeletonBase `#E9EEF4` | Night800 `#1A2540` | loading blocks |
| `skeletonHighlight` | `Skeleton/Shimmer` middle stop | SkeletonHighlight `#F6F8FB` | Night700 `#24314F` (`Dark/Skeleton/Shimmer`) | shimmer sweep |
| `textPoints` | text/points | Amber800 `#92400E` | Amber300 `#FCD34D` | "1 stop before you", "+74 points earned" |
| `textFlash` | text/flash | Orange600 `#EA580C` | Orange400 `#FB923C` | label on the white flash-sale CTA |
| `iconSilver` | icon/silver | Slate600 `#475569` | Slate400 `#94A3B8` | Silver member badge icon |

### Fields Figma does not define (derived by us)

These fields exist in our code because components need them, but Figma has no semantic token for them. The values follow the same rules as Figma's own dark mode. Everything else in this doc is an official Figma value.

| Field | Light | Dark | Why this value |
|---|---|---|---|
| `dealSoft` | Orange100 | Orange950 | Deal tint. Both are Figma primitives, but there is no semantic token |
| `inverse` | Slate900 | Slate100 | Dark button, toast, "New" badge. Flips to light in dark mode |
| `onInverse` | White | Slate900 | Text on `inverse` |
| `scrim` | Slate900 | Black | Behind sheets and dialogs (the component adds alpha) |
| `shadow` | Slate900 | Black | Figma `1.2`: "Shadows go black" |

### What Figma's dark mode does (so you can read the values)

1. **Surfaces come from the new `night` family:** background 950, card 900, sheet 850, field 800. Each layer is one step lighter, so cards stand out from the background.
2. **Text flips to slate:** primary slate-100, label 300, secondary 400, tertiary 500.
3. **Brand moves to blue-400** "for 4.5 : 1 contrast on night-950" (Figma's note). `brandPressed` is blue-500.
4. **Soft fills are dedicated dark primitives** (green-950, amber-950, red-950, blue-950), not alpha tricks.
5. **`onBrand` stays white**, because the brand, button, deal and gold gradients are the same in both modes (doc 05).

## Code

```kotlin
package com.example.dukkanapp.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dukkan semantic colors. Field names follow the Figma token names.
 *
 * Read them with `AppTheme.colors.brand` inside a composable.
 */
@Immutable
data class AppColors(
    // Surfaces
    val background: Color,
    val card: Color,
    val field: Color,
    val sheet: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textLabel: Color,

    // Lines
    val border: Color,
    val borderStrong: Color,
    val borderControl: Color,   // Figma border/control: unchecked checkbox / radio outline

    // Brand
    val brand: Color,
    val brandPressed: Color,
    val brandSoft: Color,
    val brandSoftText: Color,
    val brandRing: Color,
    val onBrand: Color,

    // Status
    val success: Color,
    val successText: Color,
    val successSoft: Color,
    val warning: Color,
    val warningText: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val errorBorder: Color,

    // Commerce accents
    val deal: Color,
    val dealSoft: Color,
    val gold: Color,

    // Inverse and overlays
    val inverse: Color,
    val onInverse: Color,
    val scrim: Color,
    val shadow: Color,

    // Newer tokens
    val iconWishlist: Color,
    val iconSilver: Color,
    val borderSubtle: Color,
    val handle: Color,
    val skeleton: Color,
    val skeletonHighlight: Color,
    val textPoints: Color,
    val textFlash: Color,
)

internal val LightAppColors = AppColors(
    background = AppPalette.Slate50,
    card = AppPalette.White,
    field = AppPalette.Slate100,
    sheet = AppPalette.White,

    textPrimary = AppPalette.Slate900,
    textSecondary = AppPalette.Slate500,
    textTertiary = AppPalette.Slate400,
    textLabel = AppPalette.Slate700,

    border = AppPalette.Slate200,
    borderStrong = AppPalette.Slate300,
    borderControl = AppPalette.Slate500,

    brand = AppPalette.Blue600,
    brandPressed = AppPalette.Blue700,
    brandSoft = AppPalette.Blue50,
    brandSoftText = AppPalette.Blue700,
    brandRing = AppPalette.Blue100,
    onBrand = AppPalette.White,

    success = AppPalette.Green600,
    successText = AppPalette.Green700,
    successSoft = AppPalette.Green100,
    warning = AppPalette.Amber600,
    warningText = AppPalette.Amber700,
    warningSoft = AppPalette.Amber100,
    error = AppPalette.Red600,
    errorSoft = AppPalette.Red50,
    errorBorder = AppPalette.Red200,

    deal = AppPalette.Orange500,
    dealSoft = AppPalette.Orange100,
    gold = AppPalette.Amber500,

    inverse = AppPalette.Slate900,
    onInverse = AppPalette.White,
    scrim = AppPalette.Slate900,
    shadow = AppPalette.Slate900,

    iconWishlist = AppPalette.Red500,
    iconSilver = AppPalette.Slate600,
    borderSubtle = AppPalette.Slate150,
    handle = AppPalette.Slate250,
    skeleton = AppPalette.SkeletonBase,
    skeletonHighlight = AppPalette.SkeletonHighlight,
    textPoints = AppPalette.Amber800,
    textFlash = AppPalette.Orange600,
)

/** Official Figma `1.2 · Dark mode` values. Fields marked "derived" are ours (see the table above). */
internal val DarkAppColors = AppColors(
    background = AppPalette.Night950,
    card = AppPalette.Night900,
    field = AppPalette.Night800,
    sheet = AppPalette.Night850,

    textPrimary = AppPalette.Slate100,
    textSecondary = AppPalette.Slate400,
    textTertiary = AppPalette.Slate500,
    textLabel = AppPalette.Slate300,

    border = AppPalette.Night700,
    borderStrong = AppPalette.Slate600,
    borderControl = AppPalette.Slate500,

    brand = AppPalette.Blue400,
    brandPressed = AppPalette.Blue500,
    brandSoft = AppPalette.Blue950,
    brandSoftText = AppPalette.Blue300,
    brandRing = AppPalette.Blue900,
    onBrand = AppPalette.White,

    success = AppPalette.Green500,
    successText = AppPalette.Green500,
    successSoft = AppPalette.Green950,
    warning = AppPalette.Amber500,
    warningText = AppPalette.Amber300,
    warningSoft = AppPalette.Amber950,
    error = AppPalette.Red400,
    errorSoft = AppPalette.Red950,
    errorBorder = AppPalette.Red400.copy(alpha = 0.5f),

    deal = AppPalette.Orange400,
    dealSoft = AppPalette.Orange950, // derived
    gold = AppPalette.Amber300,

    inverse = AppPalette.Slate100, // derived
    onInverse = AppPalette.Slate900, // derived
    scrim = AppPalette.Black,
    shadow = AppPalette.Black,

    iconWishlist = AppPalette.Red400,
    iconSilver = AppPalette.Slate400,
    borderSubtle = AppPalette.Night800,
    handle = AppPalette.Night700,
    skeleton = AppPalette.Night800,
    skeletonHighlight = AppPalette.Night700,
    textPoints = AppPalette.Amber300,
    textFlash = AppPalette.Orange400,
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors not provided: wrap content in AppTheme")
}
```

## Explained

### Why `staticCompositionLocalOf` and not `compositionLocalOf`?

The colors change only when the theme switches (light ↔ dark), which is rare.
`staticCompositionLocalOf` doesn't track every reader. When the value changes, the whole subtree recomposes once.
That's cheaper for values that almost never change. Material uses the same approach for its own colors.

### Why `error(...)` as the default?

The old code used `staticCompositionLocalOf { LightExtendedColors }`. A preview without `AppTheme` then silently used light colors, which hides bugs in dark mode.
With `error(...)`, a missing theme crashes immediately and the message tells you the fix.

### Where did `lerp` go?

The old `AppExtendedColors.kt` had a `lerp` function for animated theme changes. Nothing called it. If you later animate the light/dark switch, add it back next to `AppColors` with one line per field. YAGNI until then.

### Where did `info`, `sale`, `favorite` go?

| Old | New | Reason |
|---|---|---|
| `info` / `infoContainer` | `brand` / `brandSoft` | Figma uses soft blue for info badges ("25 min", "Bestseller") |
| `sale` / `onSale` | `deal` + `onBrand`, or the `deal` gradient | Figma `accent/deal` |
| `favorite` | `iconWishlist` | Figma `icon/wishlist`: the heart is `#EF4444`, not `status/error` |
| `successContainer` | `successSoft` | Figma naming |
| `onSuccessContainer` | `successText` | Figma naming |

## Notes

- `AppColors` is a class name. The old `core/utils/constants/AppColors.kt` is a **file** with no class of that name, so the two don't clash, but doc 04 deletes the old file anyway.
- `scrim` is opaque on purpose. The sheet adds its own alpha (`colors.scrim.copy(alpha = 0.4f)`), the same way Material's scrim works.
- **Dark `errorBorder` is Red400 at 50 %.** `Color.copy(alpha = …)` is the only alpha in this file: Figma defines this token with opacity, so we keep it as Figma wrote it. On the dark danger button (Red950 fill) the border is now visible.
- **Text on `brandSoft` uses `brandSoftText`**, never `brandPressed`. In light mode both are Blue700, but in dark mode `brandSoftText` is Blue300 on Blue950 (about 7 : 1), so the 10.5 sp badges stay readable. `brandPressed` is only for the pressed fill.
- **`successText` / `warningText`** are Figma's names (`status/success-text`, `status/warning-text`). They replace our older `successStrong` / `warningStrong`; if you already typed those, rename them.

## Check

- [ ] `AppColors.kt` compiles.
- [ ] Every value comes from `AppPalette` (no `Color(0x…)` in this file).
- [ ] Dark values match Figma `1.2 · Dark mode` (except the 6 derived fields).
- [ ] Badges and the tonal button use `brandSoftText` for their label, not `brandPressed`.
- [ ] I can say why dark `brand` is Blue400 and not Blue600.
