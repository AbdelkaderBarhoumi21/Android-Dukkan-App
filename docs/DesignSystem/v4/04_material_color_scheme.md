# 04 — Material ColorScheme built from AppColors

**File (new):** `core/designsystem/theme/AppColorScheme.kt`
**Replaces:** `AppLightColorScheme` / `AppDarkColorScheme` in `core/utils/constants/AppColors.kt`.
Keep the old file until doc 08 (the old `AppTheme` still imports it). Doc 08 deletes it.

## Why

Material widgets (`Button`, `Checkbox`, `Switch`, `TopAppBar`, `ModalBottomSheet`, `Snackbar`, ripples, text selection handles…) don't know `AppColors`.
They read `MaterialTheme.colorScheme`. If we leave it at its defaults, a stray `Switch` turns purple.

So we fill every Material slot **from `AppColors`**, never from the palette directly.
Change `AppColors.brand` once and both our components and Material widgets follow. That's **single source of truth**.

### Why no `dynamicColor`

The current `AppTheme` has `dynamicColor: Boolean = false` with Android 12 wallpaper colors.
Dukkan is a shop with a brand blue. Wallpaper colors would turn the buttons green or pink on some phones while the gradients, badges and Figma screens stay blue.
We remove the parameter completely (doc 08), so nobody turns it on by accident.

## Figma spec (slot mapping)

| Material slot | From `AppColors` | Why |
|---|---|---|
| `primary` | `brand` | Filled buttons, checkbox, switch, progress, cursor |
| `onPrimary` | `onBrand` | |
| `primaryContainer` | `brandSoft` | Tonal surfaces |
| `onPrimaryContainer` | `brandSoftText` | Text on soft blue (Figma tonal button) |
| `inversePrimary` | `brandRing` | Action color inside a dark snackbar |
| `secondary` | `deal` | The commerce accent |
| `onSecondary` | `onBrand` | White on orange |
| `secondaryContainer` | `dealSoft` | Also the selected pill of `NavigationBar` |
| `onSecondaryContainer` | `textPrimary` | Readable on the light orange |
| `tertiary` | `gold` | Stars, points |
| `onTertiary` | `AppPalette.Slate900` | White on amber is only about 2 : 1 |
| `tertiaryContainer` | `warningSoft` | |
| `onTertiaryContainer` | `warningText` | |
| `background` / `onBackground` | `background` / `textPrimary` | |
| `surface` / `onSurface` | `card` / `textPrimary` | |
| `surfaceVariant` / `onSurfaceVariant` | `field` / `textSecondary` | |
| `surfaceTint` | `card` | Removes Material's blue tint on elevated surfaces (Figma cards are pure white) |
| `inverseSurface` / `inverseOnSurface` | `inverse` / `onInverse` | Snackbar, tooltip |
| `error` / `onError` | `error` / `onBrand` | |
| `errorContainer` / `onErrorContainer` | `errorSoft` / `error` | |
| `outline` / `outlineVariant` | `border` / `border` | Figma has one border color |
| `scrim` | `scrim` | |
| `surfaceBright` / `surfaceDim` | `card` / `field` | |
| `surfaceContainerLowest` / `Low` | `card` | Cards |
| `surfaceContainer` / `surfaceContainerHigh` | `sheet` | `ModalBottomSheet`, `AlertDialog`, `DropdownMenu` read these. Figma `surface/sheet` (white light, night-850 dark) |
| `surfaceContainerHighest` | `field` | Filled text fields, search bar |

## Code

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Builds Material's [ColorScheme] from Dukkan's semantic colors so Material widgets
 * and Dukkan components always agree.
 */
internal fun AppColors.toColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brandSoftText,
            inversePrimary = brandRing,
            secondary = deal,
            onSecondary = onBrand,
            secondaryContainer = dealSoft,
            onSecondaryContainer = textPrimary,
            tertiary = gold,
            onTertiary = AppPalette.Slate900,
            tertiaryContainer = warningSoft,
            onTertiaryContainer = warningText,
            background = background,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = field,
            onSurfaceVariant = textSecondary,
            surfaceTint = card,
            inverseSurface = inverse,
            inverseOnSurface = onInverse,
            error = error,
            onError = onBrand,
            errorContainer = errorSoft,
            onErrorContainer = error,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = card,
            surfaceDim = field,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = sheet,
            surfaceContainerHigh = sheet,
            surfaceContainerHighest = field,
        )
    } else {
        lightColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brandSoftText,
            inversePrimary = brandRing,
            secondary = deal,
            onSecondary = onBrand,
            secondaryContainer = dealSoft,
            onSecondaryContainer = textPrimary,
            tertiary = gold,
            onTertiary = AppPalette.Slate900,
            tertiaryContainer = warningSoft,
            onTertiaryContainer = warningText,
            background = background,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = field,
            onSurfaceVariant = textSecondary,
            surfaceTint = card,
            inverseSurface = inverse,
            inverseOnSurface = onInverse,
            error = error,
            onError = onBrand,
            errorContainer = errorSoft,
            onErrorContainer = error,
            outline = border,
            outlineVariant = border,
            scrim = scrim,
            surfaceBright = card,
            surfaceDim = field,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = sheet,
            surfaceContainerHigh = sheet,
            surfaceContainerHighest = field,
        )
    }
```

Always use named arguments here. `lightColorScheme` has more than 40 parameters, and a positional list would silently shift a color into the wrong slot if the library adds one.

## Explained

### Why two builders when the mapping is the same?

`lightColorScheme` and `darkColorScheme` only differ in the **defaults** for slots we don't pass (for example the new `primaryFixed` family). Using the matching builder keeps those defaults sensible. Every slot we care about is mapped from `AppColors`, so light/dark differences come from `LightAppColors` / `DarkAppColors`, not from this file.

### Why is this an extension function?

`AppColors.toColorScheme(isDark)` reads like a conversion ("turn these colors into a Material scheme"). It also keeps `AppColors` itself free of Material imports.

### Why `surfaceTint = card`?

When a Material surface has `tonalElevation > 0`, Material3 blends `surfaceTint` over it. With the default (`primary`), white cards turn slightly blue. Setting the tint to the card color makes the blend invisible. Figma uses shadows for depth (doc 05), not tint.

## Notes

- `onTertiary` is the only line that reads `AppPalette` directly, because no semantic token means "dark text on amber". It's allowed: this file is in the theme package.
- In dark mode `sheet` (Night850) sits between `card` (Night900) and `field` (Night800), so a bottom sheet is visibly lighter than the screen behind it. In light mode `card` and `sheet` are both white; the scrim and the sheet shadow separate them.
- Material `NavigationBar` paints its selected pill with `secondaryContainer`. Figma's bottom bar uses a **blue** pill, so the components fork builds its own bottom bar instead of relying on this slot.

## Check

- [ ] File compiles; both branches use named arguments.
- [ ] No `dynamicLightColorScheme` / `dynamicDarkColorScheme` import remains in the new theme package.
- [ ] Old `core/utils/constants/AppColors.kt` still exists until doc 08.
