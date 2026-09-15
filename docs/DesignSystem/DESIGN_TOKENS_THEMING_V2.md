# Pulse App — Design Tokens & Theming Architecture (v2)

Package: `com.example.pulse_app` · Location: `core/theme/`

---

## 0. What changed from v1, and why (SOLID)

Your MindSet example exposed a real flaw in v1: I'd built a **full parallel `AppColorScheme`** duplicating all ~30 Material3 slots, then mapped it *again* into `MaterialTheme.colorScheme`. Two sources of truth for the same data = every new color needs updating twice, and it violates **DRY**.

The fix, straight from your example: **Material3's `ColorScheme` is not a limitation to work around — it's your color model.** Build it directly with `lightColorScheme(...)` / `darkColorScheme(...)`, and only reach for a custom `CompositionLocal` for tokens that have **no Material3 slot at all** — in your case, the 7-step emphasis ramp (`hight/higher/hightest/low/lower/lowest/medium`) per brand family. That's **Interface Segregation** applied to theming: don't force every token through infrastructure it doesn't need.

| Principle | Where it shows up now |
|---|---|
| **SRP** | `Color.kt` owns color, `Type.kt` owns type, `Shape.kt` owns shape, `Dimens.kt` owns sizing, `Theme.kt` only *orchestrates* — no file does two jobs |
| **DRY** | Colors live once (in the `ColorScheme` builders); typography styles are generated from a table, not hand-written 39 times |
| **OCP** | Adding a new emphasis ramp (e.g. a future "Success" brand family) means adding a field to `PulseExtraColors` with a default — zero existing call sites change |
| **ISP** | `AppTheme.extra` exposes *only* the ramps; nothing else is forced through `CompositionLocal` that doesn't need it (see §5 on why `AppDimens` stays a plain object) |
| **DIP** | Composables depend on the `AppTheme` / `MaterialTheme` abstraction, never on a raw `Color(0xFF...)` literal scattered in feature code |

---

## 1. File map (one file, one job)

```
core/theme/
├── Color.kt      — raw palette + Material3 ColorScheme builders + PulseExtraColors
├── Type.kt        — Konnect font family + type scale + Material3 Typography mapping
├── Shape.kt        — Material3 Shapes built from AppDimens radius tokens
├── Dimens.kt       — spacing / sizing plain object (no CompositionLocal — see §5)
├── Theme.kt        — CompositionLocalProvider + MaterialTheme + AppTheme accessor + edge-to-edge
└── components/     — AppButton, AppTextField… consume the above, feature code never touches raw tokens
```

---

## 2. `Color.kt`

```kotlin
package com.example.pulse_app.core.theme

import androidx.compose.ui.graphics.Color

// ─── Raw palette (private-by-convention — nothing outside this file
//     should reference Blue700 etc. directly; go through ColorScheme or extra) ───

// Primary / "Dodger Blue" — ⚠️ verify exact hex against Figma, see prior note
val Blue50 = Color(0xFFF1F9FE); val Blue100 = Color(0xFFE3F1FB); val Blue300 = Color(0xFF90C3FF)
val Blue400 = Color(0xFF4884FE); val Blue500 = Color(0xFF3678FB); val Blue700 = Color(0xFF1946D0)
val Blue800 = Color(0xFF1A3B83); val Blue900 = Color(0xFF1B368D); val Blue950 = Color(0xFF0E2E43)

// Info / Sky — ✅ matches Tailwind "sky" exactly
val Sky50 = Color(0xFFF0F9FF); val Sky100 = Color(0xFFE0F2FE); val Sky300 = Color(0xFF7DD3FC)
val Sky400 = Color(0xFF38BDF8); val Sky500 = Color(0xFF0EA5E9); val Sky700 = Color(0xFF0369A1)
val Sky800 = Color(0xFF075985); val Sky900 = Color(0xFF0C4A6E); val Sky950 = Color(0xFF082F49)

// Neutral / Slate — ✅ matches Tailwind "slate" exactly
val Slate50 = Color(0xFFF8FAFC); val Slate100 = Color(0xFFF1F5F9); val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8); val Slate500 = Color(0xFF64748B); val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155); val Slate800 = Color(0xFF1E293B); val Slate950 = Color(0xFF020617)
val White = Color(0xFFFFFFFF); val NearBlack = Color(0xFF090909)

// Danger / Red — ✅ matches Tailwind "red" exactly
val Red50 = Color(0xFFFEF2F2); val Red100 = Color(0xFFFEE2E2); val Red300 = Color(0xFFFCA5A5)
val Red400 = Color(0xFFF87171); val Red500 = Color(0xFFEF4444); val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C); val Red800 = Color(0xFF991B1B); val Red900 = Color(0xFF7F1D1D); val Red950 = Color(0xFF450A0A)

// ══════════════════════════════════════════════════════════
// The 7-step emphasis ramp — one per brand family. This is the
// part Material3 genuinely has no slot for, so it's the ONLY
// thing that goes through CompositionLocal (see Theme.kt).
// ══════════════════════════════════════════════════════════
data class ColorRamp(
    val hight: Color, val higher: Color, val hightest: Color,
    val low: Color, val lower: Color, val lowest: Color, val medium: Color,
)

val LightPrimaryRamp = ColorRamp(Blue700, Blue900, Blue950, Blue300, Blue100, Blue50, Blue500)
val DarkPrimaryRamp  = ColorRamp(Blue300, Blue100, Blue50, Blue800, Blue800, Blue950, Blue400)

val LightInfoRamp = ColorRamp(Sky700, Sky900, Sky950, Sky300, Sky100, Sky50, Sky500)
val DarkInfoRamp  = ColorRamp(Sky300, Sky100, Sky50, Sky800, Sky800, Sky950, Sky400)

val LightNeutralRamp = ColorRamp(Slate600, Slate700, NearBlack, Slate300, Slate50, White, Slate400)
val DarkNeutralRamp  = ColorRamp(Slate300, Slate100, White, Slate600, Slate800, Slate950, Slate500)

val LightDangerRamp = ColorRamp(Red700, Red900, Red950, Red300, Red100, Red50, Red500)
val DarkDangerRamp  = ColorRamp(Red300, Red100, Red50, Red600, Red800, Red950, Red400)

// ══════════════════════════════════════════════════════════
// Brand tokens Material3 has no slot for.
// Exposed via CompositionLocal in Theme.kt → AppTheme.extra.*
// ══════════════════════════════════════════════════════════
data class PulseExtraColors(
    val primary: ColorRamp = LightPrimaryRamp,
    val info: ColorRamp = LightInfoRamp,
    val neutral: ColorRamp = LightNeutralRamp,
    val danger: ColorRamp = LightDangerRamp,
)

val LightExtraColors = PulseExtraColors(LightPrimaryRamp, LightInfoRamp, LightNeutralRamp, LightDangerRamp)
val DarkExtraColors  = PulseExtraColors(DarkPrimaryRamp, DarkInfoRamp, DarkNeutralRamp, DarkDangerRamp)
```

### Material3 `ColorScheme` builders — the actual source of truth for standard slots

Same density of comments as your MindSet example, so every mapping decision is explicit and revisitable:

```kotlin
package com.example.pulse_app.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private val PulseDarkColors = darkColorScheme(
    // PRIMARY — Button() containers, FAB, active tab indicator, selected
    // switch/checkbox, TextField focus outline.
    primary            = Blue500,   // main brand blue, "Medium" step
    onPrimary          = White,     // text/icon drawn ON primary — must contrast

    // PRIMARY CONTAINER — FilledTonalButton, "selected" chip/card backgrounds
    // that shouldn't be as loud as primary.
    primaryContainer   = Blue800,
    onPrimaryContainer = Blue50,

    // SECONDARY — less prominent accents: FilterChip, secondary buttons.
    secondary          = Sky500,
    onSecondary        = White,

    // BACKGROUND — the root canvas behind everything (Scaffold, when you
    // don't override containerColor).
    background         = Slate950,
    onBackground       = White,

    // SURFACE — raised elements: Card, Sheet, Dialog, TopAppBar,
    // NavigationBar, and Scaffold's *actual* default containerColor
    // (Scaffold defaults to `surface`, not `background`, in M3!).
    surface            = Slate800,
    onSurface          = White,

    // SURFACE VARIANT — OutlinedTextField/TextField container fill,
    // default Chip background, divider-adjacent zones.
    surfaceVariant     = Slate700,
    onSurfaceVariant   = Slate300,  // placeholder/helper text, disabled-ish content

    // OUTLINE — clearly-visible borders: OutlinedButton, OutlinedTextField.
    outline            = Slate600,
    outlineVariant     = Slate700,  // softer dividers, e.g. between list items

    // ERROR — TextField error state, error Snackbar, destructive buttons.
    error              = Red500,
    onError            = White,
    errorContainer     = Red800,
    onErrorContainer   = Red50,
)

private val PulseLightColors = lightColorScheme(
    primary            = Blue500,
    onPrimary          = White,
    primaryContainer   = Blue100,
    onPrimaryContainer = Blue950,
    secondary          = Sky500,
    onSecondary        = White,
    background         = White,
    onBackground       = NearBlack,
    surface            = White,
    onSurface          = NearBlack,
    surfaceVariant     = Slate50,
    onSurfaceVariant   = Slate600,
    outline            = Slate300,
    outlineVariant     = Slate100,
    error              = Red500,
    onError            = White,
    errorContainer     = Red100,
    onErrorContainer   = Red950,
)
```

**When do you reach for `MaterialTheme.colorScheme.X` vs `AppTheme.extra.primary.X`?**
- Standard semantic role (button fill, error border, card background, divider) → `MaterialTheme.colorScheme.*`. You get it for free on every stock component too.
- You need a *specific emphasis step* your designer called out by name in Figma (e.g. "use Primary/Lowest for this badge background") → `AppTheme.extra.primary.lowest`.

---

## 3. `Type.kt`

Same generator approach as v1 (39 hand-written styles is not maintainable — DRY), but adopting your `LineHeightStyle` trick so line-height matches Figma's box model exactly, not Android's default (which adds asymmetric leading):

```kotlin
package com.example.pulse_app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.pulse_app.R

val KonnectFontFamily = FontFamily(
    Font(R.font.konnect_hairline, FontWeight.W100), Font(R.font.konnect_hairline_italic, FontWeight.W100, FontStyle.Italic),
    Font(R.font.konnect_thin, FontWeight.W200), Font(R.font.konnect_thin_italic, FontWeight.W200, FontStyle.Italic),
    Font(R.font.konnect_light, FontWeight.W300), Font(R.font.konnect_light_italic, FontWeight.W300, FontStyle.Italic),
    Font(R.font.konnect_regular, FontWeight.W400), Font(R.font.konnect_italic, FontWeight.W400, FontStyle.Italic),
    Font(R.font.konnect_medium, FontWeight.W500), Font(R.font.konnect_medium_italic, FontWeight.W500, FontStyle.Italic),
    Font(R.font.konnect_semi_bold, FontWeight.W600), Font(R.font.konnect_semi_bold_italic, FontWeight.W600, FontStyle.Italic),
    Font(R.font.konnect_bold, FontWeight.W700), Font(R.font.konnect_bold_italic, FontWeight.W700, FontStyle.Italic),
    Font(R.font.konnect_extra_bold, FontWeight.W800), Font(R.font.konnect_extra_bold_italic, FontWeight.W800, FontStyle.Italic),
    Font(R.font.konnect_black, FontWeight.W900), Font(R.font.konnect_black_italic, FontWeight.W900, FontStyle.Italic),
)

// Matches Figma's "vertical trim" behavior — without this, Compose adds
// extra space above the cap-height using the font's internal metrics,
// so text sits lower than the Figma frame suggests.
private val FigmaLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun konnect(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = KonnectFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = FigmaLineHeightStyle,
)

data class WeightSet(val bold: TextStyle, val medium: TextStyle, val regular: TextStyle)
data class LineHeightSet(val none: WeightSet, val tight: WeightSet, val normal: WeightSet)

data class AppTypography(
    val title1: TextStyle, val title2: TextStyle, val title3: TextStyle,
    val large: LineHeightSet, val regular: LineHeightSet, val small: LineHeightSet, val tiny: LineHeightSet,
)

private data class SizeSpec(val sizeSp: Int, val none: Int, val tight: Int, val normal: Int)

private fun weightSet(spec: SizeSpec, lineHeightSp: Int) = WeightSet(
    bold = konnect(spec.sizeSp, lineHeightSp, FontWeight.Bold),
    medium = konnect(spec.sizeSp, lineHeightSp, FontWeight.Medium),
    regular = konnect(spec.sizeSp, lineHeightSp, FontWeight.Normal),
)

private fun lineHeightSet(spec: SizeSpec) = LineHeightSet(
    none = weightSet(spec, spec.none),
    tight = weightSet(spec, spec.tight),
    normal = weightSet(spec, spec.normal),
)

val PulseTypography = AppTypography(
    title1 = konnect(48, 56, FontWeight.Bold),
    title2 = konnect(32, 36, FontWeight.Bold),
    title3 = konnect(24, 32, FontWeight.Bold),
    large = lineHeightSet(SizeSpec(18, 18, 20, 24)),
    regular = lineHeightSet(SizeSpec(16, 16, 20, 24)),
    small = lineHeightSet(SizeSpec(14, 14, 16, 20)),
    tiny = lineHeightSet(SizeSpec(12, 12, 14, 16)),
)

// Material3 mapping — so stock components (Button, TextField, Snackbar…)
// pick up Konnect automatically without you touching them individually.
val PulseMaterialTypography = Typography(
    headlineLarge = PulseTypography.title1,
    headlineMedium = PulseTypography.title2,
    headlineSmall = PulseTypography.title3,
    titleLarge = PulseTypography.large.normal.bold,
    titleMedium = PulseTypography.regular.normal.bold,
    titleSmall = PulseTypography.small.normal.bold,
    bodyLarge = PulseTypography.large.normal.regular,
    bodyMedium = PulseTypography.regular.normal.regular,
    bodySmall = PulseTypography.small.normal.regular,
    labelLarge = PulseTypography.regular.none.medium,
    labelMedium = PulseTypography.small.none.medium,
    labelSmall = PulseTypography.tiny.none.medium,
)
```

---

## 4. `Shape.kt` — new in v2, borrowed directly from your example

```kotlin
package com.example.pulse_app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.radiusXs),
    small       = RoundedCornerShape(AppDimens.radiusSm),
    medium      = RoundedCornerShape(AppDimens.radiusMd),
    large       = RoundedCornerShape(AppDimens.radiusLg),
    extraLarge  = RoundedCornerShape(AppDimens.radiusXl),
)
```

Wiring radii through `AppDimens` (instead of hardcoding `.dp` again here) keeps radius as **one source of truth** — bump `AppDimens.radiusMd` and every shape using it updates together.

---

## 5. `Dimens.kt` — simplified, matches your naming convention

```kotlin
package com.example.pulse_app.core.theme

import androidx.compose.ui.unit.dp

object AppDimens {
    // Spacing — always multiples of 4
    val spaceXXS = 4.dp
    val spaceXS  = 8.dp
    val spaceS   = 12.dp
    val spaceM   = 16.dp
    val spaceL   = 24.dp
    val spaceXL  = 32.dp
    val spaceXXL = 48.dp

    // Corner radius
    val radiusXs = 4.dp
    val radiusSm = 8.dp
    val radiusMd = 16.dp
    val radiusLg = 24.dp
    val radiusXl = 32.dp
    val radiusFull = 999.dp

    // Component sizes (measure off the mockups, name after the component not the value)
    val buttonHeight    = 56.dp
    val textFieldHeight = 56.dp
    val iconSize        = 24.dp
    val screenPaddingH  = 24.dp
    val borderWidth     = 1.dp
}
```

Still a **plain `object`, not a `CompositionLocal`** — this is deliberate, and worth restating since it's the one place we *don't* mirror the MindSet pattern of wrapping things: dp values don't change between light/dark or at runtime, so there's nothing to "provide" dynamically. Wrapping static data in `CompositionLocal` would just add an unnecessary composition-scope lookup for zero benefit — that's over-engineering, the same smell as the v1 mistake in §0, just in the other direction.

---

## 6. `Theme.kt` — orchestration only

```kotlin
package com.example.pulse_app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalExtraColors = staticCompositionLocalOf { PulseExtraColors() }

/** `AppTheme` is both a function and an object — Kotlin allows the same
 *  name for both, so call sites read naturally: `AppTheme { ... }` to wrap
 *  content, `AppTheme.extra.primary.medium` to read a token. */
object AppTheme {
    val extra: PulseExtraColors @Composable get() = LocalExtraColors.current
    val typography: AppTypography = PulseTypography
    val dimens = AppDimens
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) PulseDarkColors else PulseLightColors
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    // Edge-to-edge: status/nav bars blend with the app background instead
    // of showing the OS default bar color.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PulseMaterialTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

Note what's **not** here anymore versus v1: no `LocalAppColors`, no `LocalAppTypography`. Typography and shapes don't need `CompositionLocal` either — they're not conditional on light/dark or any runtime state, so `PulseMaterialTypography`/`AppShapes` as plain `val`s passed straight into `MaterialTheme(...)` is simpler and correct. Only colors (which *do* branch on dark/light) and only the *extra* ramp (which has no M3 slot) need the composition-scoped mechanism.

---

## 7. Usage

```kotlin
@Composable
fun TaskCard(title: String, isOverdue: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(AppTheme.dimens.spaceM)) {
            Text(title, style = AppTheme.typography.regular.normal.bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(AppTheme.dimens.spaceXS))
            if (isOverdue) {
                Text(
                    "Overdue",
                    style = AppTheme.typography.small.none.medium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// Only reach for `AppTheme.extra` when you need a specific Figma-named step
// that has no Material3 slot:
Box(Modifier.background(AppTheme.extra.primary.lowest, MaterialTheme.shapes.small))
```

---

## 8. Unchanged from v1

- **Fonts setup** (§2 of v1 — rename to `snake_case`, drop into `res/font/`) — no change.
- **MVVM tie-in for dark/light/system preference** (§8 of v1 — `ThemeMode`, `ThemePreferencesRepository`, `ThemeViewModel`) — no change; the ViewModel still only owns *which mode is chosen*, resolved to a `Boolean` before reaching `AppTheme(darkTheme = ...)`.
- **`core/theme/components/`** — still where `AppButton`/`AppTextField` etc. live, still the only place allowed to read raw `Color(0xFF...)`/tokens directly; the rest of the app consumes `components/`.
