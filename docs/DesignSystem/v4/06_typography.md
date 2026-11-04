# 06 — Typography (Plus Jakarta Sans + Cairo)

**Files:**
- `res/font/plus_jakarta_sans_regular.ttf`, `_medium`, `_semibold`, `_bold`, `_extrabold` (new)
- `res/font/cairo_regular.ttf`, `cairo_medium.ttf`, `cairo_semibold.ttf`, `cairo_bold.ttf` (new)
- `core/theme/AppTypography.kt` (rewrite of `core/config/theme/AppTypography.kt`)
- `core/theme/AppTextStyles.kt` (new)

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

Regular (400) Jakarta is not used by any Figma style, but keep it as the fallback for `FontWeight.Normal`, used by Material widgets we don't style. Cairo has **no ExtraBold** in our set: Figma's Arabic styles never go above Bold.

## Figma spec — Latin (Plus Jakarta Sans)

| Slot | Figma style | Weight | Size / line (sp) | Use it for |
|---|---|---|---|---|
| `displayLarge` | M3/displayLarge | ExtraBold | 40 / 48, −2 % | hero prices, big numbers |
| `displayMedium` | M3/displayMedium | ExtraBold | 32 / 40, −2 % | order total, OTP digits |
| `displaySmall` | M3/displaySmall | ExtraBold | 28 / 36, −2 % | onboarding & success titles |
| `headlineLarge` | M3/headlineLarge | ExtraBold | 24 / 32, −2 % | screen hero titles: "Welcome back", "Verify your email" |
| `headlineMedium` | M3/headlineMedium | ExtraBold | 22 / 28, −1.5 % | sheet & page titles |
| `headlineSmall` | M3/headlineSmall | ExtraBold | 20 / 26, −1 % | section titles, card amounts |
| `titleLarge` | M3/titleLarge | **ExtraBold** | **17** / 24, −1 % | top app bar titles |
| `titleMedium` | M3/titleMedium | **ExtraBold** | 16 / 22 | card titles, list headers |
| `titleSmall` | M3/titleSmall | **ExtraBold** | 14 / 20 | product names, row titles, toast title |
| `bodyLarge` | M3/bodyLarge | Medium | 15 / 22 | long paragraphs, subtitles |
| `bodyMedium` | M3/bodyMedium | Medium | 14 / 20 | body copy, descriptions |
| `bodySmall` | M3/bodySmall | Medium | 13 / 18 | secondary lines, meta, toast subtitle |
| `labelLarge` | M3/labelLarge | Bold | 15 / 20 | buttons |
| `labelMedium` | M3/labelMedium | Bold | 12.5 / 16 | chips, tabs, links, small buttons, input labels, toast action |
| `labelSmall` | M3/labelSmall | Bold | **10.5 / 14** | badges, nav labels, tags |
| `titleCompact` | Dukkan/titleCompact | ExtraBold | 15 / 20 | compact headers, price rows |
| `labelStrong` | Dukkan/labelStrong | ExtraBold | 12.5 / 16 | emphasised small labels, pills |
| `bodyStrong` | Dukkan/bodyStrong | Bold | 13.5 / 18 | row values, bold body |
| `supporting` | Dukkan/supporting | Medium | 11.5 / 16 | supporting lines |
| `micro` | Dukkan/micro | Medium | 10.5 / 14 | smallest meta text |
| `fieldText` | Dukkan/fieldText | SemiBold | 14.5 / 20 | text inside inputs |
| `helper` | Dukkan/helper | SemiBold | 11.5 / 14 | input helper & error text |
| `caption` | Dukkan/caption | SemiBold | 11 / 14 | "Delivered in 25–35 min", meta |
| `overline` | Dukkan/overline | ExtraBold | 10.5 / 12, +8.6 % (≈ 0.9 sp), UPPERCASE | section overlines: "NEW ARRIVAL" |

These are real Figma text styles: `M3/<slot>` (15 Material slots) and `Dukkan/<name>` (9 extras), read straight from the file. Tracking is in **em**: Figma −2 % = `(-0.02).em`. **`Price/*` are gone from the file:** a price is now two regular styles (table below). There's no placeholder style either: inputs use `bodyMedium`.

## Figma spec — Arabic (Cairo)

Figma now defines **every style** in Cairo (`Arabic/*`): the 15 Material slots and the 9 `Dukkan/*` extras. Nothing here is derived any more.

| Slot | Figma | Cairo weight | Size / line (sp) |
|---|---|---|---|
| `headlineLarge` | **Headline/Large** | Bold | 24 / 36 |
| `titleMedium` | **Title/Medium** | SemiBold | 16 / 26 |
| `bodyMedium` | **Body/Medium** | Regular | 14 / 22 |
| `labelLarge` | **Label/Large** | SemiBold | 15 / 22 |
| `caption` | **Caption** | Medium | 11 / 16 |
| `displayLarge` | **Arabic/displayLarge** | Bold | 40 / 60 |
| `displayMedium` | **Arabic/displayMedium** | Bold | 32 / 48 |
| `displaySmall` | **Arabic/displaySmall** | Bold | 28 / 42 |
| `headlineMedium` | **Arabic/headlineMedium** | Bold | 22 / 34 |
| `headlineSmall` | **Arabic/headlineSmall** | Bold | 20 / 30 |
| `titleLarge` | **Arabic/titleLarge** | SemiBold | 17 / 26 |
| `titleSmall` | **Arabic/titleSmall** | SemiBold | 14 / 22 |
| `bodyLarge` | **Arabic/bodyLarge** | Regular | 15 / 24 |
| `bodySmall` | **Arabic/bodySmall** | Regular | 13 / 20 |
| `labelMedium` | **Arabic/labelMedium** | SemiBold | **13** / 20 (toast subtitle) |
| `labelSmall` | **Arabic/labelSmall** | SemiBold | 10.5 / 16 |
| `titleCompact` | **Arabic/titleCompact** | Bold | 15 / 24 |
| `labelStrong` | **Arabic/labelStrong** | Bold | 12.5 / 20 |
| `bodyStrong` | **Arabic/bodyStrong** | SemiBold | 13.5 / 22 |
| `supporting` | **Arabic/supporting** | Regular | 11.5 / 18 |
| `micro` | **Arabic/micro** | Regular | 10.5 / 16 |
| `fieldText` | **Arabic/fieldText** | Medium | 14.5 / 22 |
| `helper` | **Arabic/helper** | Medium | 11.5 / 18 |
| `overline` | **Arabic/overline** | Bold | 10.5 / 16, **no tracking** |

**No letter spacing in Arabic.** Arabic letters join to their neighbours; extra tracking breaks the joins. That's why `overline` drops its tracking in Arabic.

## Code — `AppTypography.kt`

```kotlin
package com.example.dukkanapp.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
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
    // Figma centers text in its line box, with no extra font padding above or below.
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/** Figma's Arabic rule: one weight step lighter than the Latin style. */
internal fun FontWeight.arabicWeight(): FontWeight = when (this) {
    FontWeight.ExtraBold -> FontWeight.Bold
    FontWeight.Bold -> FontWeight.SemiBold
    FontWeight.SemiBold -> FontWeight.Medium
    else -> FontWeight.Normal
}

/**
 * One Figma style in both scripts. [latinWeight] is the Plus Jakarta weight.
 * Cairo defaults to one step lighter, unless Figma gives its own [cairoWeight] or [cairoSize].
 */
internal fun scriptStyle(
    arabic: Boolean,
    latinWeight: FontWeight,
    size: TextUnit,
    latinLineHeight: TextUnit,
    arabicLineHeight: TextUnit,
    latinLetterSpacing: TextUnit = 0.sp,
    cairoWeight: FontWeight = latinWeight.arabicWeight(),
    cairoSize: TextUnit = size,
): TextStyle =
    if (arabic) {
        figmaStyle(AppArabicFontFamily, cairoWeight, cairoSize, arabicLineHeight)
    } else {
        figmaStyle(AppFontFamily, latinWeight, size, latinLineHeight, latinLetterSpacing)
    }

/** Builds the 15 Material slots. Columns: weight, size, Latin line, Arabic line, then optional tracking. */
internal fun appTypography(arabic: Boolean): Typography {
    fun s(
        weight: FontWeight,
        size: TextUnit,
        latinLine: TextUnit,
        arabicLine: TextUnit,
        tracking: TextUnit = 0.sp,
        cairoWeight: FontWeight = weight.arabicWeight(),
        cairoSize: TextUnit = size,
    ) = scriptStyle(arabic, weight, size, latinLine, arabicLine, tracking, cairoWeight, cairoSize)

    return Typography(
        displayLarge = s(FontWeight.ExtraBold, 40.sp, 48.sp, 60.sp, tracking = (-0.02).em),
        displayMedium = s(FontWeight.ExtraBold, 32.sp, 40.sp, 48.sp, tracking = (-0.02).em),
        displaySmall = s(FontWeight.ExtraBold, 28.sp, 36.sp, 42.sp, tracking = (-0.02).em),
        headlineLarge = s(FontWeight.ExtraBold, 24.sp, 32.sp, 36.sp, tracking = (-0.02).em),
        headlineMedium = s(FontWeight.ExtraBold, 22.sp, 28.sp, 34.sp, tracking = (-0.015).em),
        headlineSmall = s(FontWeight.ExtraBold, 20.sp, 26.sp, 30.sp, tracking = (-0.01).em),
        titleLarge = s(FontWeight.ExtraBold, 17.sp, 24.sp, 26.sp, tracking = (-0.01).em, cairoWeight = FontWeight.SemiBold),
        titleMedium = s(FontWeight.ExtraBold, 16.sp, 22.sp, 26.sp, cairoWeight = FontWeight.SemiBold),
        titleSmall = s(FontWeight.ExtraBold, 14.sp, 20.sp, 22.sp, cairoWeight = FontWeight.SemiBold),
        bodyLarge = s(FontWeight.Medium, 15.sp, 22.sp, 24.sp),
        bodyMedium = s(FontWeight.Medium, 14.sp, 20.sp, 22.sp),
        bodySmall = s(FontWeight.Medium, 13.sp, 18.sp, 20.sp),
        labelLarge = s(FontWeight.Bold, 15.sp, 20.sp, 22.sp),
        labelMedium = s(FontWeight.Bold, 12.5.sp, 16.sp, 20.sp, cairoSize = 13.sp),
        labelSmall = s(FontWeight.Bold, 10.5.sp, 14.sp, 16.sp),
    )
}

/** Built once, not on every recomposition. AppTheme picks one (doc 08). */
internal val LatinTypography: Typography = appTypography(arabic = false)
internal val ArabicTypography: Typography = appTypography(arabic = true)
```

## Code — `AppTextStyles.kt`

```kotlin
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
```

## Explained

### Why one function per script and not a `lineHeightScale`?

The first version of this doc multiplied every Latin line height by 1.15 for Arabic. Figma now gives exact Arabic values, and they are **not** a constant ratio: 24/32 → 24/36 is ×1.125, 14/20 → 14/22 is ×1.1, 11/14 → 11/16 is ×1.14, and the weights change too. A single scale can't express that, so each style carries its own Arabic line height. That's still one line per style.

### `bodySmall` and Caption are two different styles

`1.3 · Typography` confirms the split: `bodySmall` = Jakarta Medium 13/18 (Arabic: Cairo Regular 13/20), and **Caption** is a separate style, Jakarta SemiBold 11/14 (Arabic: Cairo Medium 11/16), kept outside the Material slots. That is exactly our split: Body/Small in `bodySmall`, Caption in `AppTheme.textStyles.caption`. (An earlier version of the chapter put Caption in `bodySmall`; it is fixed, so there is no conflict any more.)

### Why a `figmaStyle` helper?

Every Figma style is "weight · size/lineHeight". One function turns that into a `TextStyle`, so the styles above are one line each. This is the **Factory** idea from the masroufi docs.

### Prices with a smaller currency

Figma writes "279.000 **TND**" with a smaller "TND", and it's two text styles in one line. `AppPriceText` (doc 12) does this:

| Size | Amount | Currency | Example |
|---|---|---|---|
| `Large` | `headlineLarge` 24 | `titleCompact` 15 | product details |
| `Medium` | `titleMedium` 16 | `overline` 10.5 | cart total |
| `Small` | `titleCompact` 15 | `overline` 10.5 | price rows, cart bar |
| `Compact` | `titleSmall` 14 | `overline` 10.5 | small highlighted prices |

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
| "See all" link | `labelMedium` + `colors.brand` (`labelSmall` is now 10.5, for badges) |
| Button label | `labelLarge` (M button 14 sp: `labelLarge.copy(fontSize = 14.sp)` inside the button component only) |
| Field label | `labelMedium` + `colors.textPrimary` |
| Field value / placeholder | `textStyles.fieldText` / `bodyMedium` (14 Medium) |
| Field helper / error | `textStyles.helper` |
| Checkbox label | `labelMedium` with `fontWeight = SemiBold` (Figma 12.5 SemiBold) + `colors.textLabel` |
| Product name on card | `titleSmall` |
| Card price | `AppPriceText` (doc 12) |
| Badge, nav label, tag | `labelSmall` (10.5 / 14 Bold) |
| Row value, bold body | `textStyles.bodyStrong` |
| Price row, compact header | `textStyles.titleCompact` |
| Toast title / subtitle / action | `titleSmall` / `bodySmall` / `labelMedium` (Arabic: `labelLarge` / `labelMedium` / `labelMedium`) |
| Footer "Already have an account? Log in" | `bodySmall`, action in `colors.brand` Bold |

## Check

- [ ] 5 Jakarta + 4 Cairo `.ttf` files in `res/font/` with lowercase names.
- [ ] Both files compile.
- [ ] Old `core/config/theme/AppTypography.kt` still exists until doc 08 (the old theme imports it).
- [ ] Arabic preview (doc 08): "مرحبًا بعودتك" renders in Cairo Bold 24 with a 36 sp line, right-aligned.
