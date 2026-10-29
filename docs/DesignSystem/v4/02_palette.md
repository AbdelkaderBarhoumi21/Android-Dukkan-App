# 02 — AppPalette (primitive colors)

**File (new):** `app/src/main/java/com/example/dukkanapp/core/designsystem/theme/AppPalette.kt`
**Replaces:** the `private val Blue50 … Black` lines at the top of `core/utils/constants/AppColors.kt` (the rest of that file moves to doc 04).

## Why

Figma has two layers: **primitive colors** ("blue-600") and **semantic tokens** ("brand/primary").
A primitive answers "which blue?". A semantic token answers "what is it for?".

If screens used `Blue600` directly, a rebrand (or dark mode) would mean editing every screen.
So primitives live in one `internal object`, and only the theme files in the same package read them.
This is the **Dependency Inversion** idea: screens depend on the abstraction (`AppColors.brand`), not on the detail (`#2563EB`).

Naming rule: **hue + tone, never meaning.** `Blue600`, not `PrimaryBlue`. The Kotlin names are the Figma names in PascalCase: `blue-600` → `Blue600`, `night-850` → `Night850`.

## Figma spec

Every value comes from `01 · Foundations → Primitive palette`. Since the latest Figma update, **every color used anywhere** (gradient stops, aurora blobs, dark mode) is a named primitive, so this file now matches Figma one to one. Full table: doc 01, section 1.

| Family | Steps | Used for |
|---|---|---|
| blue | 50 – 950 | brand, links, focus ring, dark brand |
| azure | 100, 200, 300, 400, 500 | gradients (brand, button, screen tint) and the aurora blobs |
| slate | 50 – 900 (no 600) | light neutrals, dark text |
| night | 700, 800, 850, 900, 950 | **dark mode** surfaces and borders |
| green | 100, 500, 600, 700, 950 | success (light + dark) |
| amber | 100, 200, 300, 500, 600, 700, 950 | warning, gold, aurora sun blob |
| red | 50, 100, 200, 400, 600, 950 | error (light + dark) |
| orange | 100, 400, 500, 950 | deals |
| rose | 500 | deal gradient end |

**Renamed since the first version of this doc** (the colors are the same, only the names changed):

| Old name | New name (Figma) | Hex |
|---|---|---|
| `TintBlue` | `Azure100` | `#DCE7FF` |
| `AuroraSky` | `Azure200` | `#A9C4FF` |
| `AuroraBlue` | `Azure300` | `#7EA6FF` |
| `AuroraSun` | `Amber200` | `#FDE68A` |

`Slate600` and `Slate950` are gone: Figma's dark mode uses the new `night` family instead.

## Code

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw Dukkan colors, named by hue and tone (same names as Figma).
 *
 * Only files in this package may read it. Screens and components use [AppColors] instead.
 */
internal object AppPalette {

    // Brand blue
    val Blue50 = Color(0xFFEFF5FF)
    val Blue100 = Color(0xFFDBE7FE)
    val Blue200 = Color(0xFFBFD4FE)
    val Blue300 = Color(0xFF93B4FD)
    val Blue400 = Color(0xFF6090FA)
    val Blue500 = Color(0xFF3B6FF6)
    val Blue600 = Color(0xFF2563EB)
    val Blue700 = Color(0xFF1D4ED8)
    val Blue800 = Color(0xFF1E40AF)
    val Blue900 = Color(0xFF1E3A8A)
    val Blue950 = Color(0xFF172554)

    // Azure: gradients and aurora blobs
    val Azure100 = Color(0xFFDCE7FF)
    val Azure200 = Color(0xFFA9C4FF)
    val Azure300 = Color(0xFF7EA6FF)
    val Azure400 = Color(0xFF4F8BFF)
    val Azure500 = Color(0xFF3B82F6)

    // Neutrals (light surfaces, dark-mode text)
    val Slate50 = Color(0xFFF7F9FB)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)

    // Night: dark-mode surfaces and borders
    val Night700 = Color(0xFF24314F)
    val Night800 = Color(0xFF1A2540)
    val Night850 = Color(0xFF16213A)
    val Night900 = Color(0xFF111A2E)
    val Night950 = Color(0xFF0B1220)

    // Success
    val Green100 = Color(0xFFDCFCE7)
    val Green500 = Color(0xFF22C55E)
    val Green600 = Color(0xFF16A34A)
    val Green700 = Color(0xFF15803D)
    val Green950 = Color(0xFF0F2E1D)

    // Warning, gold, aurora sun
    val Amber100 = Color(0xFFFEF3C7)
    val Amber200 = Color(0xFFFDE68A)
    val Amber300 = Color(0xFFFCD34D)
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)
    val Amber700 = Color(0xFFB45309)
    val Amber950 = Color(0xFF33260A)

    // Error
    val Red50 = Color(0xFFFEF2F2)
    val Red100 = Color(0xFFFEE2E2)
    val Red200 = Color(0xFFFECACA)
    val Red400 = Color(0xFFF87171)
    val Red600 = Color(0xFFDC2626)
    val Red950 = Color(0xFF3A1418)

    // Deals
    val Orange100 = Color(0xFFFFEDD5)
    val Orange400 = Color(0xFFFB923C)
    val Orange500 = Color(0xFFF97316)
    val Orange950 = Color(0xFF3A1E0B)
    val Rose500 = Color(0xFFF43F5E)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}
```

## Notes

- `internal` keeps the palette inside the `app` module. To keep it out of feature packages too, the rule in doc 00 plus the grep in doc 15 catch any `AppPalette.` outside `core/designsystem/theme`.
- `0xFF…`: the first two hex digits are alpha (`FF` = opaque). Transparency is added later with `.copy(alpha = …)`, never by inventing a new hex.
- A few steps (`Blue200`, `Blue300`, `Blue800`, `Red100`, `Slate800`) are in Figma but not used by any token yet. Keep them: they are part of the scale.

## Check

- [ ] File compiles; no other file references `AppPalette` yet.
- [ ] Every hex matches doc 01, section 1 (and Figma `01 · Foundations`).
- [ ] No `TintBlue`, `Aurora…`, `Slate600` or `Slate950` left anywhere.
- [ ] Old `private val Blue50 …` lines are still in `core/utils/constants/AppColors.kt` for now (doc 04 deletes them).
