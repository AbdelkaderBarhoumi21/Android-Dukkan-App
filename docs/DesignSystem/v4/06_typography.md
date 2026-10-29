# 06 — Typography (Plus Jakarta Sans + Cairo)

**Files:**
- `res/font/plus_jakarta_sans_regular.ttf`, `_medium`, `_semibold`, `_bold`, `_extrabold` (new)
- `res/font/cairo_regular.ttf`, `cairo_medium.ttf`, `cairo_semibold.ttf`, `cairo_bold.ttf` (new)
- `core/designsystem/theme/AppTypography.kt` (rewrite of `core/config/theme/AppTypography.kt`)
- `core/designsystem/theme/AppTextStyles.kt` (new)

**Removes:** the 18 `res/font/konnect_*.otf` files (after doc 08 builds).

## Why

Figma's type chapter is **"Typography · Plus Jakarta Sans (Latin) — 4 px rhythm"** plus **"Typography · Cairo (Arabic) — Same names as Latin, so one style switches by locale. Arabic needs ~1.5× line height. Right-aligned, RTL."**
The current code uses Konnect and Material's default sizes (57 sp display, 32 sp headline), which are tablet-sized. Figma uses a compact mobile scale: 40 / 32 / 28 / 24 / 22 / 20 / 18 / 16 / 15 / 14 / 13 / 12.5 / 12 / 11.5 / 11 / 10.5.

The work splits into two layers:

1. **Material `Typography`** (15 slots). Material widgets read these: `Text` default, `Button` label, `TopAppBar` title, `TextField` text.
2. **`AppTextStyles`** for Figma styles Material has no slot for: caption, overline, price, field text, helper. Squeezing "price" into `titleLarge` would make the code lie about what the text is.

Each layer is built **twice**: once in Plus Jakarta Sans (English, French) and once in Cairo (Arabic). The **names are the same**, so a screen writes `MaterialTheme.typography.headlineLarge` and gets the right font for the app language. `AppTheme` picks the set (doc 08).

Every style has a `lineHeight`, because Figma always gives one and lines would jump between Android versions without it.

## Step 1 — Download the fonts

**Plus Jakarta Sans** (Latin):

1. Google Fonts → **Plus Jakarta Sans** → *Get font* → *Download all*.
2. In the zip, open `static/` and copy these 5 files:

| Zip file | Rename to | Weight |
|---|---|---|
| `PlusJakartaSans-Regular.ttf` | `plus_jakarta_sans_regular.ttf` | 400 |
| `PlusJakartaSans-Medium.ttf` | `plus_jakarta_sans_medium.ttf` | 500 |
| `PlusJakartaSans-SemiBold.ttf` | `plus_jakarta_sans_semibold.ttf` | 600 |
| `PlusJakartaSans-Bold.ttf` | `plus_jakarta_sans_bold.ttf` | 700 |
| `PlusJakartaSans-ExtraBold.ttf` | `plus_jakarta_sans_extrabold.ttf` | 800 |

**Cairo** (Arabic):

1. Google Fonts → **Cairo** → *Get font* → *Download all*.
2. In the zip, open `static/` and copy these 4 files:

| Zip file | Rename to | Weight |
|---|---|---|
| `Cairo-Regular.ttf` | `cairo_regular.ttf` | 400 |
| `Cairo-Medium.ttf` | `cairo_medium.ttf` | 500 |
| `Cairo-SemiBold.ttf` | `cairo_semibold.ttf` | 600 |
| `Cairo-Bold.ttf` | `cairo_bold.ttf` | 700 |

3. Paste all 9 files into `app/src/main/res/font/`.

Android resource names may only contain `a–z`, `0–9` and `_`. A dash or capital letter breaks the build with "invalid resource name".

> Figma's Compose chapter calls the Jakarta files `pjs_*.ttf`. We keep the longer `plus_jakarta_sans_*` names, which say what the font is. Only the file names differ.

Regular (400) Jakarta is not used by any Figma style, but keep it as the fallback for `FontWeight.Normal`, used by Material widgets we don't style. Cairo has **no ExtraBold** in our set: Figma's Arabic styles never go above Bold.

## Figma spec — Latin (Plus Jakarta Sans)

| Slot | Figma style | Weight | Size / line (sp) | Use it for |
|---|---|---|---|---|
| `displayLarge` | Display/Large | ExtraBold | 40 / 48 | splash, big hero |
| `displayMedium` | Display/Medium | ExtraBold | 32 / 40 | big numbers, hero |
| `displaySmall` | Display/Small | ExtraBold | 28 / 36 | "My addresses" large title, onboarding |
| `headlineLarge` | Headline/Large | ExtraBold | 24 / 32 | auth & page titles: "Welcome back", "Verify your email" |
| `headlineMedium` | Headline/Medium | ExtraBold | 22 / 28 | "Order placed!" |
| `headlineSmall` | Headline/Small | ExtraBold | 20 / 26 | sheet titles, "Choose your language" |
| `titleLarge` | Title/Large | Bold | 18 / 24 | section headers "Flash deals", top app bar |
| `titleMedium` | Title/Medium | Bold | 16 / 22 | card titles "Order #DK-2048" |
| `titleSmall` | Title/Small | Bold | 14 / 20 | product name "Air Runner 2", list row title |
| `bodyLarge` | Body/Large | Medium | 15 / 22 | paragraphs |
| `bodyMedium` | Body/Medium | Medium | 14 / 20 | default body, subtitles |
| `bodySmall` | Body/Small | Medium | 13 / 18 | secondary lines, footer prompts |
| `labelLarge` | Label/Large | Bold | 15 / 20 | button labels |
| `labelMedium` | Label/Medium | Bold | 12.5 / 16 | input labels "Email", chip labels |
| `labelSmall` | Label/Small | Bold | 12 / 16 | "See all", small links |
| `fieldText` | Field/Text | SemiBold | 14.5 / 20 | input value |
| `fieldPlaceholder` | (Field/Text, Medium) | Medium | 14.5 / 20 | hint inside a field (Figma atoms draw it Medium) |
| `helper` | Helper | SemiBold | 11.5 / 14 | helper & error: "Verified with Google", "Enter a valid email address" |
| `caption` | Caption | SemiBold | 11 / 14 | "Delivered in 25–35 min", meta |
| `overline` | Overline | ExtraBold | 10.5 / 12, +0.9 sp tracking, UPPERCASE | "NEW ARRIVAL", "SUMMER DROP" |
| `priceLarge` | Price/Large | ExtraBold | 24 / 28 | product details price |
| `priceSmall` | Price/Small | ExtraBold | 15 / 20 | card price, cart line |

All 22 Latin styles are now real Figma styles (Display/Medium, Headline/Medium, Label/Medium, Field/Text and Helper were added in the latest Figma version). Only `fieldPlaceholder` is a weight variant of Field/Text.

## Figma spec — Arabic (Cairo)

Figma defines 5 Arabic styles. The rule it shows: **one weight step lighter than Latin** (ExtraBold → Bold, Bold → SemiBold, SemiBold → Medium, Medium → Regular) and **line height ≈ 1.5× the size**. We apply the same rule to every other slot ("derived").

| Slot | Figma | Cairo weight | Size / line (sp) |
|---|---|---|---|
| `headlineLarge` | **Headline/Large** | Bold | 24 / 36 |
| `titleMedium` | **Title/Medium** | SemiBold | 16 / 26 |
| `bodyMedium` | **Body/Medium** | Regular | 14 / 22 |
| `labelLarge` | **Label/Large** | SemiBold | 15 / 22 |
| `caption` | **Caption** | Medium | 11 / 16 |
| `displayLarge` | derived | Bold | 40 / 56 |
| `displayMedium` | derived | Bold | 32 / 46 |
| `displaySmall` | derived | Bold | 28 / 40 |
| `headlineMedium` | derived | Bold | 22 / 32 |
| `headlineSmall` | derived | Bold | 20 / 30 |
| `titleLarge` | derived | SemiBold | 18 / 28 |
| `titleSmall` | derived | SemiBold | 14 / 22 |
| `bodyLarge` | derived | Regular | 15 / 24 |
| `bodySmall` | **Body/Small** (Compose chapter) | Regular | 13 / 20 |
| `labelMedium` | derived | SemiBold | 12.5 / 18 |
| `labelSmall` | derived | SemiBold | 12 / 18 |
| `fieldText` | derived | Medium | 14.5 / 22 |
| `fieldPlaceholder` | derived | Regular | 14.5 / 22 |
| `helper` | derived | Medium | 11.5 / 16 |
| `overline` | derived | Bold | 10.5 / 16, **no tracking** |
| `priceLarge` | derived | Bold | 24 / 32 |
| `priceSmall` | derived | Bold | 15 / 22 |

Display sizes use ≈ 1.4× instead of 1.5×: at 28 sp and above, 1.5× leaves visibly too much air. Prices stay close to the Latin height because they are digits.

**No letter spacing in Arabic.** Arabic letters join to their neighbours; extra tracking breaks the joins. That's why `overline` drops its +0.9 sp in Arabic.

## Code — `AppTypography.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.R

/** Plus Jakarta Sans, the Dukkan font for Latin scripts (English, French). */
internal val AppFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Cairo, the Dukkan font for Arabic. */
internal val AppArabicFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

/** Turns a Figma spec ("ExtraBold · 24/32") into a [TextStyle]. */
internal fun figmaStyle(
    fontFamily: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

/** Figma's Arabic rule: one weight step lighter than the Latin style. */
internal fun FontWeight.arabicWeight(): FontWeight = when (this) {
    FontWeight.ExtraBold -> FontWeight.Bold
    FontWeight.Bold -> FontWeight.SemiBold
    FontWeight.SemiBold -> FontWeight.Medium
    else -> FontWeight.Normal
}

/**
 * One Figma style in both scripts. [latinWeight] is the Plus Jakarta weight;
 * the Cairo weight is derived with [arabicWeight].
 */
internal fun scriptStyle(
    arabic: Boolean,
    latinWeight: FontWeight,
    size: TextUnit,
    latinLineHeight: TextUnit,
    arabicLineHeight: TextUnit,
    latinLetterSpacing: TextUnit = 0.sp,
): TextStyle =
    if (arabic) {
        figmaStyle(AppArabicFontFamily, latinWeight.arabicWeight(), size, arabicLineHeight)
    } else {
        figmaStyle(AppFontFamily, latinWeight, size, latinLineHeight, latinLetterSpacing)
    }

/** Builds the 15 Material slots. Columns: weight, size, Latin line, Arabic line. */
internal fun appTypography(arabic: Boolean): Typography {
    fun s(weight: FontWeight, size: TextUnit, latinLine: TextUnit, arabicLine: TextUnit) =
        scriptStyle(arabic, weight, size, latinLine, arabicLine)

    return Typography(
        displayLarge = s(FontWeight.ExtraBold, 40.sp, 48.sp, 56.sp),
        displayMedium = s(FontWeight.ExtraBold, 32.sp, 40.sp, 46.sp),
        displaySmall = s(FontWeight.ExtraBold, 28.sp, 36.sp, 40.sp),
        headlineLarge = s(FontWeight.ExtraBold, 24.sp, 32.sp, 36.sp),
        headlineMedium = s(FontWeight.ExtraBold, 22.sp, 28.sp, 32.sp),
        headlineSmall = s(FontWeight.ExtraBold, 20.sp, 26.sp, 30.sp),
        titleLarge = s(FontWeight.Bold, 18.sp, 24.sp, 28.sp),
        titleMedium = s(FontWeight.Bold, 16.sp, 22.sp, 26.sp),
        titleSmall = s(FontWeight.Bold, 14.sp, 20.sp, 22.sp),
        bodyLarge = s(FontWeight.Medium, 15.sp, 22.sp, 24.sp),
        bodyMedium = s(FontWeight.Medium, 14.sp, 20.sp, 22.sp),
        bodySmall = s(FontWeight.Medium, 13.sp, 18.sp, 20.sp),
        labelLarge = s(FontWeight.Bold, 15.sp, 20.sp, 22.sp),
        labelMedium = s(FontWeight.Bold, 12.5.sp, 16.sp, 18.sp),
        labelSmall = s(FontWeight.Bold, 12.sp, 16.sp, 18.sp),
    )
}

/** Built once, not on every recomposition. AppTheme picks one (doc 08). */
internal val LatinTypography: Typography = appTypography(arabic = false)
internal val ArabicTypography: Typography = appTypography(arabic = true)
```

## Code — `AppTextStyles.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

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

internal val LatinTextStyles: AppTextStyles = appTextStyles(arabic = false)
internal val ArabicTextStyles: AppTextStyles = appTextStyles(arabic = true)

internal val LocalAppTextStyles = staticCompositionLocalOf<AppTextStyles> {
    error("AppTextStyles not provided: wrap content in AppTheme")
}
```

## Explained

### Why one function per script and not a `lineHeightScale`?

The first version of this doc multiplied every Latin line height by 1.15 for Arabic. Figma now gives exact Arabic values, and they are **not** a constant ratio: 24/32 → 24/36 is ×1.125, 14/20 → 14/22 is ×1.1, 11/14 → 11/16 is ×1.14, and the weights change too. A single scale can't express that, so each style carries its own Arabic line height. That's still one line per style.

### `bodySmall` and Caption are two different styles

Figma's Compose chapter (`06 · Compose tokens`) now matches the type scale: `bodySmall` = **Body/Small**, Jakarta Medium 13/18 (Arabic: Cairo Regular 13/20), and **Caption** is a separate style, Jakarta SemiBold 11/14 (Arabic: Cairo Medium 11/16), kept outside the Material slots. That is exactly our split: Body/Small in `bodySmall`, Caption in `AppTheme.textStyles.caption`. (An earlier version of the chapter put Caption in `bodySmall`; it is fixed, so there is no conflict any more.)

### Why a `figmaStyle` helper?

Every Figma style is "weight · size/lineHeight". One function turns that into a `TextStyle`, so the styles above are one line each. This is the **Factory** idea from the masroufi docs.

### Prices with a smaller currency

Figma writes "279.000 **TND**" with a smaller "TND". Use `priceLarge` for the number and `caption` (or `labelSmall`) for the currency in a `buildAnnotatedString`. `AppPriceText` (doc 12) does this.

### Why no `fontSize` in feature screens

```kotlin
// Wrong
Text("Flash deals", fontSize = 18.sp, fontWeight = FontWeight.Bold)

// Right
Text("Flash deals", style = MaterialTheme.typography.titleLarge)
```

A hard-coded size also breaks Arabic: it keeps the Latin line height.

## Which style for what

| UI element | Style |
|---|---|
| Screen title (auth, success) | `headlineLarge` (all auth titles are 24 ExtraBold in Figma) |
| Screen subtitle | `bodyMedium` + `colors.textSecondary` (Figma auth subtitles are 13 Medium: `bodySmall` if you want it exact) |
| Section header ("Flash deals") | `titleLarge` |
| "See all" link | `labelSmall` + `colors.brand` |
| Button label | `labelLarge` (M button 14 sp: `labelLarge.copy(fontSize = 14.sp)` inside the button component only) |
| Field label | `labelMedium` + `colors.textPrimary` |
| Field value / placeholder | `textStyles.fieldText` / `textStyles.fieldPlaceholder` |
| Field helper / error | `textStyles.helper` |
| Checkbox label | `labelMedium` with `fontWeight = SemiBold` (Figma 12.5 SemiBold) + `colors.textLabel` |
| Product name on card | `titleSmall` |
| Card price | `textStyles.priceSmall` |
| Badge | `textStyles.overline` without tracking, or `caption` (10.5 Bold in Figma) |
| Footer "Already have an account? Log in" | `bodySmall`, action in `colors.brand` Bold |

## Check

- [ ] 5 Jakarta + 4 Cairo `.ttf` files in `res/font/` with lowercase names.
- [ ] Both files compile.
- [ ] Old `core/config/theme/AppTypography.kt` still exists until doc 08 (the old theme imports it).
- [ ] Arabic preview (doc 08): "مرحبًا بعودتك" renders in Cairo Bold 24 with a 36 sp line, right-aligned.
