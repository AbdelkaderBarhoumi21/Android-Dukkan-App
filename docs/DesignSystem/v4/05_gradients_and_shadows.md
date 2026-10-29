# 05 — Gradients and shadows

**Files (new):**

- `core/designsystem/theme/AppGradients.kt`
- `core/designsystem/theme/AppShadows.kt`

## Why

Figma's primary button is not a flat color. It's a horizontal blue gradient with a blue glow underneath. Auth and home screens sit on a soft "aurora": a blue tint at the top with blurry color blobs.
Material has no token for gradients or colored shadows, so we add two small token classes, provided the same way as `AppColors`:

- `AppGradients`: ready-made `Brush` values. A component writes `Modifier.background(AppTheme.gradients.button, AppPillShape)`.
- `AppShadows`: `AppShadow` value objects, plus one modifier, `Modifier.appShadow(shadow, shape)`.

Both have a light and a dark instance, so a component never checks the mode.

## Figma spec

### Gradients (`01 · Foundations → Gradients`, "every stop is a primitive")

| Field        | Stops                                     | Direction  | Compose brush                                                                      |
| ------------ | ----------------------------------------- | ---------- | ---------------------------------------------------------------------------------- |
| `brand`      | azure-400 `#4F8BFF` → blue-700 `#1D4ED8`  | 135°       | `Brush.linearGradient` top-start → bottom-end                                      |
| `button`     | azure-500 `#3B82F6` → blue-700 `#1D4ED8`  | horizontal | `Brush.horizontalGradient`                                                         |
| `screenTint` | azure-100 `#DCE7FF` → slate-50 `#F7F9FB`  | vertical   | `Brush.verticalGradient`                                                           |
| `bottomFade` | background 0 % → background 100 %         | vertical   | fades the aurora header into the page (the 90 dp band at the bottom of the header) |
| `deal`       | orange-400 `#FB923C` → rose-500 `#F43F5E` | 135°       | `Brush.linearGradient`                                                             |
| `gold`       | amber-300 `#FCD34D` → amber-500 `#F59E0B` | 135°       | `Brush.linearGradient`                                                             |
| `navy`       | blue-900 `#1E3A8A` → blue-950 `#172554`   | 135°       | `Brush.linearGradient`                                                             |
| `imageScrim` | slate-900 0 % → slate-900 72 %            | vertical   | `Brush.verticalGradient`                                                           |

**Aurora blobs (header)**: exactly **3** radial blobs, from the color at the given alpha to fully transparent. Same positions in light and dark (Figma `13.1b · Header glow · spec`); only the colors change.

| Field (`aurora`) | Figma     | Light                      | Dark                      |
| ---------------- | --------- | -------------------------- | ------------------------- |
| `blue`           | Blob blue | azure-300 `#7EA6FF` · 62 % | blue-500 `#3B6FF6` · 35 % |
| `sky`            | Blob sky  | azure-200 `#A9C4FF` · 55 % | blue-800 `#1E40AF` · 45 % |
| `sun`            | Blob warm | amber-200 `#FDE68A` · 42 % | amber-300 `#FCD34D` · 8 % |

Doc 13 (`AppScreenBackground`) places the blobs. This doc only defines the colors and a small brush helper.

**Card highlight** (`cardHighlight`): white · 35 %, a radial glow centered on the **top-right corner** with radius 90, drawn **only inside gradient cards** (promo banners, wallet card, earnings card). Same in light and dark. It is *not* part of the aurora: an earlier version of these docs put a "white blob" in the header, which was wrong. Use `Modifier.cardHighlight()` (code below).

### Shadows (`01 · Foundations → Elevation`: x 0 · y · blur · color)

| Field              | Color             | Offset Y | Blur | Used by                                                                   |
| ------------------ | ----------------- | -------- | ---- | ------------------------------------------------------------------------- |
| `card`             | slate-900 @ 6 %   | 4        | 14   | product cards, white icon buttons, add button                             |
| `float`            | slate-900 @ 12 %  | 10       | 28   | elevated search bar, bottom sheets, floating nav                          |
| `button`           | blue-600 @ 30 %   | 8        | 20   | primary / checkout / slide buttons                                        |
| `deal`             | orange-500 @ 30 % | 8        | 20   | deal button                                                               |
| `screen` (**new**) | slate-900 @ 10 %  | 18       | 44   | the phone frame / full-screen surfaces (e.g. a raised sheet over the map) |

### Dark mode (`01b · Dark mode → Gradients & elevation · dark`)

Figma: "Brand, button, deal and gold gradients stay the same. Tint and fades go to night. Shadows go black; cards add a 1 px night-700 border."

| Token                                                   | Light                                          | Dark                                                       | Source        |
| ------------------------------------------------------- | ---------------------------------------------- | ---------------------------------------------------------- | ------------- |
| `brand`, `button`, `deal`, `gold`, `navy`, `imageScrim` | as above                                       | same                                                       | Figma         |
| `screenTint`                                            | azure-100 → slate-50                           | **blue-950 → night-950**                                   | Figma         |
| `bottomFade`                                            | slate-50 0 % → 100 %                           | **night-950 0 % → 100 %**                                  | Figma         |
| `card` shadow                                           | slate-900 6 %                                  | **black 45 %**, y 4, blur 14, **+ 1 dp night-700 border**  | Figma         |
| `float` shadow                                          | slate-900 12 %                                 | **black 55 %**, y 10, blur 28, **+ 1 dp night-700 border** | Figma         |
| `screen` shadow                                         | slate-900 10 %                                 | **black 60 %**, y 18, blur 44, **+ 1 dp night-700 border** | Figma         |
| `button` shadow                                         | blue-600 30 %                                  | **azure-400 25 %**, y 8, blur 20 (no border)               | Figma         |
| `deal` shadow                                           | orange-500 30 %                                | **orange-400 25 %**, y 8, blur 20 (no border)              | Figma         |
| `aurora` blobs                                          | azure-300 62 %, azure-200 55 %, amber-200 42 % | blue-500 35 %, blue-800 45 %, amber-300 8 %                | Figma `13.1b` |
| `cardHighlight`                                         | white 35 %                                     | same                                                       | Figma `13.1b` |

Figma's rule: **all dark surfaces (card, float/sheet, screen) add a 1 px night-700 border; Button and Deal keep their coloured glow.** Every value in this table is official; nothing is derived any more.

## Code — `AppGradients.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Colors of the 3 blurred blobs behind auth and home headers. Alpha is already applied. */
@Immutable
data class AppAurora(
    val blue: Color,
    val sky: Color,
    val sun: Color,
) {
    /** A radial blob: [color] in the center, fully transparent at [radius] (pixels). */
    fun blob(color: Color, center: Offset, radius: Float): Brush =
        Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        )
}

/** Dukkan gradients. Read them with `AppTheme.gradients.button`. */
@Immutable
data class AppGradients(
    val brand: Brush,
    val button: Brush,
    val screenTint: Brush,
    val bottomFade: Brush,
    val deal: Brush,
    val gold: Brush,
    val navy: Brush,
    val imageScrim: Brush,
    val aurora: AppAurora,
    /** White glow in the top-end corner of gradient cards. Same in light and dark. */
    val cardHighlight: Color,
)

/** 135°: from the top-start corner to the bottom-end corner of whatever it fills. */
private fun diagonal(start: Color, end: Color): Brush =
    Brush.linearGradient(
        colors = listOf(start, end),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

/** Transparent → opaque [color], top to bottom. */
private fun fadeTo(color: Color): Brush =
    Brush.verticalGradient(listOf(color.copy(alpha = 0f), color))

private val SharedBrand = diagonal(AppPalette.Azure400, AppPalette.Blue700)
private val SharedButton = Brush.horizontalGradient(listOf(AppPalette.Azure500, AppPalette.Blue700))
private val SharedDeal = diagonal(AppPalette.Orange400, AppPalette.Rose500)
private val SharedGold = diagonal(AppPalette.Amber300, AppPalette.Amber500)
private val SharedNavy = diagonal(AppPalette.Blue900, AppPalette.Blue950)
private val SharedImageScrim = Brush.verticalGradient(
    listOf(AppPalette.Slate900.copy(alpha = 0f), AppPalette.Slate900.copy(alpha = 0.72f)),
)
private val SharedCardHighlight = AppPalette.White.copy(alpha = 0.35f)

internal val LightAppGradients = AppGradients(
    brand = SharedBrand,
    button = SharedButton,
    screenTint = Brush.verticalGradient(listOf(AppPalette.Azure100, AppPalette.Slate50)),
    bottomFade = fadeTo(AppPalette.Slate50),
    deal = SharedDeal,
    gold = SharedGold,
    navy = SharedNavy,
    imageScrim = SharedImageScrim,
    aurora = AppAurora(
        blue = AppPalette.Azure300.copy(alpha = 0.62f),
        sky = AppPalette.Azure200.copy(alpha = 0.55f),
        sun = AppPalette.Amber200.copy(alpha = 0.42f),
    ),
    cardHighlight = SharedCardHighlight,
)

internal val DarkAppGradients = AppGradients(
    brand = SharedBrand,
    button = SharedButton,
    screenTint = Brush.verticalGradient(listOf(AppPalette.Blue950, AppPalette.Night950)),
    bottomFade = fadeTo(AppPalette.Night950),
    deal = SharedDeal,
    gold = SharedGold,
    navy = SharedNavy,
    imageScrim = SharedImageScrim,
    // Figma 13.1b · Header glow · spec (dark).
    aurora = AppAurora(
        blue = AppPalette.Blue500.copy(alpha = 0.35f),
        sky = AppPalette.Blue800.copy(alpha = 0.45f),
        sun = AppPalette.Amber300.copy(alpha = 0.08f),
    ),
    cardHighlight = SharedCardHighlight,
)

internal val LocalAppGradients = staticCompositionLocalOf<AppGradients> {
    error("AppGradients not provided: wrap content in AppTheme")
}

/**
 * Figma "Card highlight": a soft white glow centered on the top-end corner (top-right in LTR,
 * top-left in RTL), fading out at [radius]. Only for gradient cards: promo banners, wallet, earnings.
 *
 * Put it AFTER `clip(shape)` and `background(gradient)`, so it is drawn on the gradient,
 * under the content, and clipped to the card:
 *
 *     Modifier.clip(shape).background(AppTheme.gradients.deal).cardHighlight(AppTheme.gradients.cardHighlight)
 */
fun Modifier.cardHighlight(color: Color, radius: Dp = 90.dp): Modifier = drawBehind {
    val r = radius.toPx()
    val center = Offset(x = if (layoutDirection == LayoutDirection.Ltr) size.width else 0f, y = 0f)
    drawCircle(
        brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = r),
        radius = r,
        center = center,
    )
}
```

## Code — `AppShadows.kt`

Dark mode cards get a **1 dp night-700 border** next to the black shadow (Figma `01b`). We put that border **inside the shadow token** (`outline`), so every component that already calls `appShadow(AppTheme.shadows.card, shape)` gets the border in dark mode for free. In light mode `outline` is `Color.Unspecified` and nothing is drawn.

```kotlin
package com.example.dukkanapp.core.designsystem.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * One Figma drop shadow: color already includes its alpha.
 * [outline] is an optional 1 dp border drawn with the shadow (dark-mode cards).
 */
@Immutable
data class AppShadow(
    val color: Color,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp = 0.dp,
    val outline: Color = Color.Unspecified,
)

/** Dukkan elevation tokens. Read them with `AppTheme.shadows.card`. */
@Immutable
data class AppShadows(
    val card: AppShadow,
    val float: AppShadow,
    val button: AppShadow,
    val deal: AppShadow,
    val screen: AppShadow,
)

internal val LightAppShadows = AppShadows(
    card = AppShadow(AppPalette.Slate900.copy(alpha = 0.06f), offsetY = 4.dp, blur = 14.dp),
    float = AppShadow(AppPalette.Slate900.copy(alpha = 0.12f), offsetY = 10.dp, blur = 28.dp),
    button = AppShadow(AppPalette.Blue600.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    deal = AppShadow(AppPalette.Orange500.copy(alpha = 0.30f), offsetY = 8.dp, blur = 20.dp),
    screen = AppShadow(AppPalette.Slate900.copy(alpha = 0.10f), offsetY = 18.dp, blur = 44.dp),
)

/** Figma 01b: surfaces go black + 1 dp night-700 border; button and deal keep a coloured glow. */
internal val DarkAppShadows = AppShadows(
    card = AppShadow(
        AppPalette.Black.copy(alpha = 0.45f), offsetY = 4.dp, blur = 14.dp,
        outline = AppPalette.Night700,
    ),
    float = AppShadow(
        AppPalette.Black.copy(alpha = 0.55f), offsetY = 10.dp, blur = 28.dp,
        outline = AppPalette.Night700,
    ),
    button = AppShadow(AppPalette.Azure400.copy(alpha = 0.25f), offsetY = 8.dp, blur = 20.dp),
    deal = AppShadow(AppPalette.Orange400.copy(alpha = 0.25f), offsetY = 8.dp, blur = 20.dp),
    screen = AppShadow(
        AppPalette.Black.copy(alpha = 0.60f), offsetY = 18.dp, blur = 44.dp,
        outline = AppPalette.Night700,
    ),
)

internal val LocalAppShadows = staticCompositionLocalOf<AppShadows> {
    error("AppShadows not provided: wrap content in AppTheme")
}

/**
 * Draws a Figma-style drop shadow behind the content, plus the token's 1 dp outline if it has one.
 * Put it BEFORE `clip` / `background` in the modifier chain, so the shadow is not clipped
 * and the outline is drawn on top of the background.
 */
fun Modifier.appShadow(shadow: AppShadow, shape: Shape): Modifier {
    val withShadow = dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = shadow.blur,
            color = shadow.color,
            spread = shadow.spread,
            offset = DpOffset(x = 0.dp, y = shadow.offsetY),
        ),
    )
    return if (shadow.outline.isSpecified) {
        withShadow.border(width = 1.dp, color = shadow.outline, shape = shape)
    } else {
        withShadow
    }
}
```

## Explained

### Usage: a card

```kotlin
Box(
    modifier = Modifier
        .appShadow(AppTheme.shadows.card, RoundedCornerShape(AppDimens.radiusCard)) // 1. shadow (+ dark border)
        .clip(RoundedCornerShape(AppDimens.radiusCard))                            // 2. then clip
        .background(AppTheme.colors.card),                                         // 3. then paint
)
```

Modifier order matters. `clip` cuts away everything outside the shape, so a shadow placed after it would be cut away too. The border is drawn by a modifier that sits **outside** `background`, so it paints after (on top of) the card color.

### Why put the dark border in the shadow token?

Because it's part of the same visual idea: "how does this card lift off the page?". In light mode a soft shadow does it; on a near-black page a shadow is barely visible, so Figma adds a hairline. If the border were a separate token, every card call site would need an `if (dark)` and someone would forget one. **Open/closed:** components don't change, only the dark token does.

### Why `Offset.Infinite` for 135°?

`Brush.linearGradient(start = Offset.Zero, end = Offset.Infinite)` stretches from the top-left corner to the bottom-right corner of the area it paints, whatever its size. That's Figma's 135°. One brush works for a 44 dp icon button and a full-width banner.

### Why `dropShadow` and not `Modifier.shadow(elevation)`?

`Modifier.shadow(elevation)` draws Material's physical shadow: you choose a height, not a blur, an offset or a color, and it looks grey and sharp.
`Modifier.dropShadow` (Compose UI 1.9+, included in our BOM `2026.02.01`) takes the exact Figma values: color, blur, spread and offset.

**Fallback** if `dropShadow` is not available in your Compose version:

```kotlin
Modifier.shadow(
    elevation = shadow.offsetY,
    shape = shape,
    ambientColor = shadow.color,
    spotColor = shadow.color,
)
```

It approximates the colored glow, but the blur won't match Figma. Note it in doc 15 if you use it.

### Why not put shadows in `AppDimens`?

A shadow is four values that only make sense together (color + y + blur + spread). Splitting them across files invites mismatches. A value object keeps them together.

## Notes

- Gradients do not mirror in RTL. 135° goes from the top-**left** in both directions. Figma's brand moments (logo, banners) don't need mirroring. If a horizontal gradient must follow reading direction, flip it in the component with `LocalLayoutDirection`.
- Brushes are created once (top-level `val`s), not inside composables, so they aren't allocated on every recomposition.
- Pressed buttons have **no lift** (Figma `03b`): the button component simply skips `appShadow` while pressed (doc 10).

## Check

- [ ] Both files compile.
- [ ] `import androidx.compose.ui.draw.dropShadow` resolves. If not, use the fallback and log the gap in doc 15.
- [ ] Every color comes from `AppPalette`.
- [ ] Dark preview: a card, a bottom sheet (`float`) and a `screen` surface each have a black shadow **and** a thin `#24314F` outline; the primary button glow is a soft azure and the deal glow a soft orange, with no outline.
- [ ] A promo banner shows the white highlight in its top-right corner (top-left in Arabic); the header aurora has only 3 blobs.

This file is the **gradients layer** of the design system: the same pattern as `AppColors`, but for gradients instead of single colors. I'll go through it piece by piece.
The big picture
---------------

    AppGradients  = a bag of ready-made gradients (brand, button, deal...)
    Light/Dark    = two instances, same names, slightly different values
    LocalAppGradients = the "drawer" (CompositionLocal)
    AppTheme.gradients.button = how a screen reads one

Screens never build a gradient. They only write `AppTheme.gradients.button`.
What is a `Brush`?
------------------

A `Brush` is "how to paint an area". A `Color` paints one flat color. A `Brush` can paint a gradient, which is a smooth blend from one color to another.
`diagonal(start, end)`
----------------------

    Brush.linearGradient(colors = listOf(start, end), start = Offset.Zero, end = Offset.Infinite)

It blends from `start` to `end` along a diagonal, from the top-left corner to the bottom-right corner (135° in Figma).
    ┌──────────┐
    │start     │      start color  ──────►  end color
    │    ╲     │      (diagonal)
    │      ╲ end│
    └──────────┘

It's used for `brand`, `deal`, `gold` and `navy`.
`fadeTo(color)`
---------------

    Brush.verticalGradient(listOf(color.copy(alpha = 0f), color))

It goes from fully transparent at the top to solid `color` at the bottom.
    top:    invisible
              ↓
    bottom: solid color

It's used for `bottomFade`. For example, a list scrolls under a bottom bar, and `fadeTo(background)` softly hides the content so it doesn't end in a hard line.
What is an aurora blob?
-----------------------

`AppAurora` is the soft, glowing, blurry colored circles behind the header of the auth and home screens, like northern lights.
    ┌────────────────────┐
    │  🔵 blue blob      │
    │        🔷 sky blob │   ← blurry colored circles
    │  🟡 sun blob       │
    │                    │
    │   Welcome back     │
    └────────────────────┘

* `blue`, `sky` and `sun` are the three blob colors, with their transparency already applied.
* `blob(color, center, radius)` makes one blob. It's a **radial gradient**: the color is solid in the center and fades to transparent at the edge, which gives the glow.

In light mode the blobs are bright (azure and amber, about 40–60% opaque). In dark mode they're subtle (blue at 35%, and amber only 8%), so they don't glare on the dark background.
`cardHighlight`
---------------

This is a soft white glow in the **top corner of gradient cards**, such as promo banners and the wallet card. It makes the card look glossy.
    ┌─────────────────(✨)┐   ← white glow at the top-end corner
    │  50% OFF            │
    │  Deal card          │
    └─────────────────────┘

There are two parts:

1. `cardHighlight: Color` in `AppGradients` is just the white at 35% opacity. It's the same in light and dark.
2. `Modifier.cardHighlight(color, radius)` draws it:
   * `drawBehind` draws behind the content, on top of the background.
   * The center is the top-end corner: `x = width` in LTR, `x = 0` in RTL, so it mirrors automatically for Arabic.
   * `drawCircle` with a radial gradient gives a white center that fades to transparent at 90dp.

The order matters (as the doc comment says):
    Modifier
      .clip(shape)                            // 1. cut the card shape
      .background(AppTheme.gradients.deal)    // 2. paint the gradient
      .cardHighlight(AppTheme.gradients.cardHighlight)  // 3. glow on top, clipped to the card

If you put the glow before `clip`, it spills outside the rounded corners.
Why `Shared...` and `Light.../Dark...`?
---------------------------------------

Some gradients are identical in both modes (`brand`, `button`, `deal`, `gold`, `navy`), so they're defined once as `SharedX` and reused. Only the ones that must change are different: `screenTint`, `bottomFade` and `aurora`. They use the light background `Slate50` or the dark `Night950`.

Is there a part you want me to go deeper on, for example `Offset.Infinite` or how the radial gradient works?
