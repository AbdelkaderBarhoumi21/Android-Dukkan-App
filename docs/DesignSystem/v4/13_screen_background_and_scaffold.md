# 13: Screen background, scaffold and auth header

**Files:**
How the aurora background works (simple version)
The big picture
Think of the screen as layers of paper stacked on top of each other:


Layer 3:  your screen content (buttons, text)
Layer 2:  AppAuroraHeader   <- the colorful glow at the top
Layer 1:  plain background color
Step by step flow
Step 1: A screen calls AppScaffold


AppScaffold(background = AppScreenBackground.AuroraShort) { ... }
The screen only picks a name: Plain, AuroraCompact, AuroraShort, or AuroraTall. It never draws anything itself.

Step 2: AppScaffold calls AppScreenBackgroundBox
This is a Box, which stacks things on top of each other. It does 3 things:

Paints the full screen with the plain background color.
If the choice is not Plain, it draws AppAuroraHeader.
Then it draws your screen content on top.
Step 3: AppAuroraHeader draws the glow
It uses a Canvas. The height of the glow comes from the enum: tall is 520 dp, short is 300 dp, compact is 260 dp.

What is Canvas?
A Canvas is a blank drawing board. Normal Compose gives you ready-made things like Text and Button. A Canvas lets you draw your own shapes: circles, rectangles, gradients.


Canvas(modifier = Modifier.fillMaxWidth().height(300.dp)) {
    // draw here
}
The board is as wide as the screen and 300 dp tall. Inside the { } you get a DrawScope. It has the drawing tools (drawRect, drawCircle) and size, which is the board's width and height.

What is drawRect?
It paints a rectangle. Here it fills the whole board with the screenTint gradient (light blue fading to a pale color):


drawRect(brush = gradients.screenTint)
A brush is just "how to paint": a solid color or a gradient.

This is the base of the glow.

What is drawBlob?
It's a helper function written by the doc, not part of Compose. It draws one soft, fuzzy circle (a "blob") of color.


drawCircle(
    brush = aurora.blob(color, center, radius),
    radius = radius,
    center = center,
)
center is where the circle sits.
radius is how big it is.
aurora.blob(...) makes a radial gradient: the color is strong in the middle and fades to transparent at the edge. That's why it looks soft instead of like a hard circle.
The header draws 3 blobs:

Blob	Where	Color
blue	top left	blue
sky	top right	light blue
sun	lower in the band	warm yellow
Together with the tint band, they create the "aurora" look.

Why xFraction, and what about RTL?

val x = size.width * (if (isRtl) 1f - xFraction else xFraction)
xFraction = 0.153 means "15% from the left". It's a fraction, so it works on any screen width (phone or tablet).
For Arabic (right-to-left), 1 - 0.153 = 0.847, so the blob flips to the right side. That mirrors the layout.
Why .toPx()?
Compose has two units:

dp is a density-independent size. 20 dp looks about the same physical size on every phone.
px is real screen pixels.
Inside a Canvas, drawing functions work in pixels. drawCircle(radius = ...) wants pixels, not dp. So you convert:


AppDimens.auroraBlueRadius.toPx()   // dp -> px
Example: 230 dp is about 230 px on a normal phone, but about 690 px on a high-density phone (3x). Without .toPx() the glow would look tiny on dense screens.

Rule: design in dp, draw in px, convert with toPx().

The bottom fade (last step)
The band has a hard bottom edge, which looks ugly. So at the end the code draws a fade from transparent to the background color over the last 90 dp:


val fadeTop = size.height - fadeHeight.toPx()   // where the fade starts
translate(top = fadeTop) {                      // move the pen down to there
    drawRect(brush = gradients.bottomFade, size = Size(size.width, fadeHeight.toPx()))
}
translate moves the drawing origin down, so the rectangle is drawn at the bottom of the band.
The rectangle is exactly 90 dp tall, so the gradient is squeezed into just that strip.
Result: the glow melts into the background with no visible edge.
Order of drawing (matters!)
Later drawing goes on top of earlier drawing:

drawRect(screenTint): base tint
blue blob
sky blob
sun blob
bottom fade: covers the lower edge
One-line summary
The screen picks a name, AppScreenBackgroundBox stacks a plain color plus AppAuroraHeader, and AppAuroraHeader uses a Canvas to draw a tint rectangle, 3 soft circles (blobs) and a bottom fade. All sizes are converted from dp to px, because Canvas draws in pixels.

| File                                                                | Status                                                                        |
| ------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `core/common/components/scaffold/AppScreenBackground.kt`            | new                                                                           |
| `core/common/components/scaffold/AppScaffold.kt`                    | new, replaces `core/common/components/scaffold/AppScaffold.kt`                |
| `features/auth/presentation/components/common/AppAuthHeader.kt`     | moved and rewritten from `core/common/components/header/AppAuthHeader.kt`     |
| `features/auth/presentation/components/common/AppAuthFooterLink.kt` | moved and rewritten from `core/common/components/footer/AppAuthFooterLink.kt` |
| `features/auth/presentation/components/common/AppOrDivider.kt`      | new, replaces `core/common/components/dividers/AppDivider.kt` on auth screens |

> **Why move the header and footer into `features/auth`?** Only auth screens use them. `core/` is for things *every* feature may use. Keeping auth-only UI in the auth feature follows the dependency rule from the project structure guide: features depend on core, never the other way. `AppAccountRecoveryHeader` and `AppAccountRecoveryFooter` become the same `AppAuthHeader` with a tile icon (see below), so delete them after doc 14.

---

## Why

Every Customer-app screen in Figma sits on one of three backgrounds:

| Background         | Where in Figma                                                | What it is                                                         |
| ------------------ | ------------------------------------------------------------- | ------------------------------------------------------------------ |
| **Aurora tall**    | 02.1 Welcome, 02.5 All set, 01.x Onboarding, 04.1 Home header | `background` plus a 520 dp `screenTint` band plus soft color blobs |
| **Aurora short**   | 02.2 Log in, 02.4 Forgot password                             | the same, with a 300 dp band and the sun blob higher up            |
| **Aurora compact** | 02.6 Verify your email, 02.7 Not verified yet                 | the same as short, with a 260 dp band                              |
| **Plain**          | 02.3 Sign up, Cart, Checkout, Account                         | `background` only (`#F7F9FB` light, `#0B1220` dark)                |

Today each screen paints `colorScheme.background` through `AppScaffold`. We keep that one entry point, `AppScaffold(background = …)`, so a screen chooses the background by name and never draws blobs itself.

**Where do the colors come from?** From `AppTheme.gradients` (doc 05): `screenTint`, `bottomFade` and `aurora` (`blue`, `sky`, `sun`). Each blob color already carries its alpha, and the helper `blob(color, center, radius): Brush` builds the radial fade to transparent. **Dark mode is in the tokens**, not in this composable: the dark `screenTint` is blue-950 → night-950, the dark `bottomFade` ends in night-950 and the dark blobs are re-tinted (blue-500 35 %, blue-800 45 %, amber-300 8 %, Figma `13.1b`). This composable never checks the theme and never touches the palette. That keeps the rule from doc 00: *only the theme layer reads primitives.*

---

## Figma spec: aurora (measured on a 393 dp wide frame)

Positions are the blob's bounding square (top-left x, top-left y, size). In code we use the **center** and radius, with x as a fraction of the width so it scales on tablets.

| Layer             | Token                                                                     | Tall (Welcome, Home)         | Short (Log in)               | Compact (Verify email) |
| ----------------- | ------------------------------------------------------------------------- | ---------------------------- | ---------------------------- | ---------------------- |
| Base              | `background`                                                              | full screen                  | full screen                  | full screen            |
| Tint band         | `gradients.screenTint` (azure-100 → slate-50; dark: blue-950 → night-950) | height 520                   | height 300                   | height 260             |
| Blob blue         | `aurora.blue` (light azure-300 62 %, dark blue-500 35 % → 0 %, radial)    | center (0.153 w, 20), r 230  | same                         | same                   |
| Blob sky          | `aurora.sky` (light azure-200 55 %, dark blue-800 45 %)                   | center (0.916 w, 60), r 190  | same                         | same                   |
| Blob warm (`sun`) | `aurora.sun` (light amber-200 42 %, dark amber-300 8 %)                   | center (0.763 w, 380), r 150 | center (0.763 w, 250), r 150 | same as short          |
| Bottom fade       | `gradients.bottomFade`                                                    | last 90 dp of the band       | last 90 dp                   | last 90 dp             |

There are **exactly 3 blobs**, in light and dark, at the same positions (Figma `13.1b · Header glow · spec`: blue c (60, 20) r 230, sky c (360, 60) r 190, warm c (300, 250) r 150, in screen px of the 393 × 852 frame).

The Figma "white · 35 %" is **not** a header blob: it is the **card highlight**, drawn only inside gradient cards (promo banners, wallet, earnings) with `Modifier.cardHighlight()` from doc 05. It does not belong in this composable.

**Dark mode** comes from the tokens (doc 05, Figma `1.2` and `13.1b`): "Tint and fades go to night", and the blobs are re-tinted. Every value is official, so the code below is identical for both modes.

**Arabic (RTL):** mirror the x positions (x becomes w − x). The brand blob then sits behind the back button on the right, like it sits behind it on the left in LTR.

---

## Code

### `AppScreenBackground.kt`

```kotlin
package com.example.dukkanapp.core.common.components.scaffold

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.theme.AppAurora
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Band height and warm-blob position per aurora size (Figma 393 dp frames). */
enum class AppScreenBackground(val bandHeight: Dp, val sunCenterY: Dp) {
    Plain(Dp.Hairline, Dp.Hairline), // Hairline = 0 dp: no aurora band
    AuroraCompact(AppDimens.auroraCompact, AppDimens.auroraSunYShort),
    AuroraShort(AppDimens.auroraShort, AppDimens.auroraSunYShort),
    AuroraTall(AppDimens.auroraTall, AppDimens.auroraSunYTall),
}

@Composable
fun AppScreenBackgroundBox(
    background: AppScreenBackground,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (background != AppScreenBackground.Plain) {
            AppAuroraHeader(background)
        }
        content()
    }
}

@Composable
private fun AppAuroraHeader(background: AppScreenBackground) {
    val gradients = AppTheme.gradients
    val aurora = gradients.aurora
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val fadeHeight = AppDimens.auroraFade

    Canvas(modifier = Modifier.fillMaxWidth().height(background.bandHeight)) {
        drawRect(brush = gradients.screenTint)

        drawBlob(aurora, aurora.blue, xFraction = 0.153f, y = AppDimens.auroraBlueY.toPx(), radius = AppDimens.auroraBlueRadius.toPx(), isRtl)
        drawBlob(aurora, aurora.sky, xFraction = 0.916f, y = AppDimens.auroraSkyY.toPx(), radius = AppDimens.auroraSkyRadius.toPx(), isRtl)
        drawBlob(aurora, aurora.sun, xFraction = 0.763f, y = background.sunCenterY.toPx(), radius = AppDimens.auroraSunRadius.toPx(), isRtl)

        // Melt the band into the plain background so there is no visible edge.
        val fadeTop = size.height - fadeHeight.toPx()
        translate(top = fadeTop) {
            drawRect(brush = gradients.bottomFade, size = Size(size.width, fadeHeight.toPx()))
        }
    }
}

/** [color] already holds its alpha (light or dark, from the tokens). */
private fun DrawScope.drawBlob(
    aurora: AppAurora,
    color: Color,
    xFraction: Float,
    y: Float,
    radius: Float,
    isRtl: Boolean,
) {
    val x = size.width * (if (isRtl) 1f - xFraction else xFraction)
    val center = Offset(x, y)
    drawCircle(
        brush = aurora.blob(color, center, radius),
        radius = radius,
        center = center,
    )
}
```

> `bottomFade` is a `Brush.verticalGradient` without explicit start/end, so it stretches over whatever it paints. We `translate` to the top of the fade zone and draw a rect exactly 90 dp tall, so the brush covers just that band.

> Blob sizes and positions are *art direction* for this one composable, so they stay here as raw `dp`, not in `AppDimens`. Nothing else should use them.

### `AppScaffold.kt`

What changes from today's `AppScaffold`:

| Today                                            | New                                                                                                                                            |
| ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `containerColor = colorScheme.background`        | transparent, drawn over `AppScreenBackgroundBox`                                                                                               |
| Material `TopAppBar` with a grey circle back box | a plain row: `AppIconButton(White)` 44 dp with `shadows.card`, at `screenPadding` from the edge and `space8` below the status bar (Figma y 58) |
| `title` always uses `titleLarge`                 | optional centered title (`titleMedium`) plus subtitle (`bodySmall`) for app bars like "Checkout · 4 items"                                     |
| no step slot                                     | `topBarActions` slot: the "1/2" progress on sign up, "Help" on order details                                                                   |
| default insets                                   | `contentWindowInsets = WindowInsets.safeDrawing`, so content stays clear of the status bar, navigation bar **and** keyboard                    |

```kotlin
package com.example.dukkanapp.core.common.components.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppIconButton
import com.example.dukkanapp.core.common.components.buttons.AppIconButtonVariant
import com.example.dukkanapp.core.common.components.feedback.AppToastHost
import com.example.dukkanapp.core.common.components.feedback.AppToastPlacement
import com.example.dukkanapp.core.common.components.feedback.AppToastState
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    background: AppScreenBackground = AppScreenBackground.Plain,
    title: String? = null,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    toastState: AppToastState? = null,
    toastPlacement: AppToastPlacement = AppToastPlacement.Top,
    content: @Composable (PaddingValues) -> Unit,
) {
    AppScreenBackgroundBox(background = background, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                val hasTopBar = onNavigateBack != null || title != null
                if (hasTopBar) {
                    AppTopBar(
                        title = title,
                        subtitle = subtitle,
                        onNavigateBack = onNavigateBack,
                        actions = topBarActions,
                    )
                }
            },
            bottomBar = bottomBar,
            content = content,
        )
        // Drawn over the whole screen, not in Scaffold's snackbar slot, which only sits at the bottom.
        toastState?.let { AppToastHost(state = it, placement = toastPlacement) }
    }
}

@Composable
private fun AppTopBar(
    title: String?,
    subtitle: String?,
    onNavigateBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPadding)
            .padding(top = AppDimens.space8)
            .height(AppDimens.iconButton),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            if (onNavigateBack != null) {
                AppIconButton(
                    icon = painterResource(AppIcons.ArrowLeft),
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onNavigateBack,
                    variant = AppIconButtonVariant.White,
                )
            }
            // Actions get the rest of the row (sign-up progress uses weight(1f) inside).
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space12, Alignment.End),
                content = actions,
            )
        }
        if (title != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = AppDimens.iconButton + AppDimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Figma `App bar/Default`: M3/titleLarge 17 ExtraBold + Dukkan/supporting 11.5.
                Text(title, style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center, maxLines = 1)
                subtitle?.let { Text(it, style = AppTheme.textStyles.supporting, color = AppTheme.colors.textSecondary, maxLines = 1) }
            }
        }
    }
}
```

**Edge-to-edge is required.** The aurora must run *under* the status bar, as in Figma, where the "9:41" sits on the blue glow. Make sure `MainActivity.onCreate` calls `enableEdgeToEdge()` before `setContent` (it is the default for apps targeting SDK 35+, but call it explicitly). Because of `WindowInsets.safeDrawing`, the `PaddingValues` a screen receives already contain the status bar, top bar, nav bar and IME. Always apply them:

```kotlin
AppScaffold(background = AppScreenBackground.AuroraShort, onNavigateBack = ...) { padding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenPadding)
    ) { ... }
}
```

**Keyboard:** with `safeDrawing` the content padding grows when the keyboard opens. Combined with `verticalScroll`, the focused field stays visible. Remove any `android:windowSoftInputMode="adjustResize"` workaround you added in the manifest. Edge-to-edge plus insets already handle it.

### `AppAuthHeader.kt`

Figma uses three header looks:

| Screen                                            | Logo                                                                                                                             | Title                                | Align  |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ------ |
| 02.1 Welcome                                      | Dukkan **mark**, 56 dp, brand gradient (no tile)                                                                                 | "Welcome to Dukkan", `headlineLarge` | center |
| 02.2 Log in                                       | Dukkan **mark**, 48 dp                                                                                                           | "Welcome back", `headlineLarge`      | center |
| 02.3 Sign up, 02.4 Forgot, 02.6/02.7 Verify email | 48 dp **tile** (`radiusMd` 14, Figma "icon tile → md"; `gradients.brand`) with a white Lucide icon (`user-plus`, `lock`, `mail`) | `headlineLarge`                      | start  |

The subtitle is always `bodySmall` (13/18 Medium) in `textSecondary`. The gap is `space8` between title and subtitle, and `space24` between the logo and the title. Every auth title in Figma is now **24 ExtraBold** = `headlineLarge` (Cairo Bold 24/36 in Arabic, Figma 13.4).

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.common.components.AppLogoMark
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

sealed interface AuthHeaderLogo {
    /** The Dukkan awning mark, no background. */
    data class Mark(val size: Dp) : AuthHeaderLogo

    /** 48 dp gradient tile holding a white Lucide icon. */
    data class Tile(val icon: Int) : AuthHeaderLogo
}

@Composable
fun AppAuthHeader(
    title: String,
    subtitle: String,
    logo: AuthHeaderLogo,
    modifier: Modifier = Modifier,
    centered: Boolean = logo is AuthHeaderLogo.Mark,
) {
    val colors = AppTheme.colors
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = align) {
        when (logo) {
            // Reuses the logo from doc 09: one source for the brand mark.
            is AuthHeaderLogo.Mark -> AppLogoMark(modifier = Modifier.size(logo.size))

            is AuthHeaderLogo.Tile -> Box(
                modifier = Modifier
                    .size(AppDimens.logoMark)
                    .background(AppTheme.gradients.brand, RoundedCornerShape(AppDimens.radiusMd)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(logo.icon), null, tint = colors.onBrand, modifier = Modifier.size(AppDimens.iconLg))
            }
        }
        Spacer(Modifier.height(AppDimens.space24))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            textAlign = textAlign,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AppDimens.space8))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = textAlign,
        )
    }
}
```

**Logo:** `AppLogoMark(modifier, size)` comes from `core/common/components/AppLogoMark.kt` (doc 09). Don't create a second logo drawable here. Pass the size with `size = logo.size`. For the Log in screen use `AuthHeaderLogo.Mark(AppDimens.logoMark)` (48 dp).

### `AppAuthFooterLink.kt`

Figma: "Already have an account? **Log in**". The prompt is `bodySmall` in `textSecondary`, and the action is `bodySmall` Bold in `brand`. It sits at the bottom of the screen, 16 dp above the navigation bar.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppAuthFooterLink(
    promptText: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        // Ghost button = brand text, ripple, Role.Button and a real touch area.
        AppButton(
            text = actionText,
            onClick = onActionClick,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
        )
    }
}
```

> The old footer used `clickable(indication = null)`: no ripple, a tiny touch area and no `Role.Button`. A Ghost `AppButton` fixes all three. The Ghost label is 12.5 sp Bold, which matches the Figma 13 sp closely enough.

### `AppOrDivider.kt`

Figma shows "or" (Welcome) and "or continue with" (Log in) in 12 sp SemiBold `textTertiary`, centered, with no lines. We offer optional hairlines for longer forms.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppOrDivider(text: String, modifier: Modifier = Modifier, showLines: Boolean = false) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12, Alignment.CenterHorizontally),
    ) {
        if (showLines) HorizontalDivider(Modifier.weight(1f), thickness = AppDimens.strokeThin, color = AppTheme.colors.border)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.textTertiary,
        )
        if (showLines) HorizontalDivider(Modifier.weight(1f), thickness = AppDimens.strokeThin, color = AppTheme.colors.border)
    }
}
```

---

## Notes and rules

- **One background per screen, chosen by name.** Screens never call `Canvas` or `Brush.radialGradient` for decoration.
- **Don't put `background(...)` on a screen's root `Column`.** It hides the aurora. The scaffold already painted the background.
- **Horizontal screen padding is `AppDimens.screenPadding` (20).** The old screens used `size2Xs` (16).
- **Back button position** is Figma's x 20, y 58: `screenPadding`, then status bar + `space8`. On a 24 dp status bar that lands at y 32. Figma's iPhone frame has a 47 dp status bar (47 + 8 + 3 for the shadow ≈ 58). Don't hard-code 58.
- **Dark mode:** the white back button uses `shadows.card`, which in dark mode also draws the 1 dp night-700 outline (doc 05). Nothing to do in this file. Figma `13.2 · Log in · dark` is the reference.

---



Screen background, scaffold and auth components
-----------------------------------------------

### What is “Aurora”?

It’s named after the aurora borealis (northern lights). In this design it means **soft, blurry, colored glows at the top of the screen**, like light bleeding through fog. It’s a visual style from the Figma design, not an Android or Compose feature.

An aurora header is built from 4 layers, painted bottom to top:
    ┌─────────────────────────────┐
    │ ◉ blue blob      ◉ sky blob │  ← 2 radial glows (top-left, top-right)
    │                             │
    │              ◉ sun blob     │  ← warm yellow glow, lower down
    │ ░░░░░░░░ bottomFade ░░░░░░░ │  ← last 90 dp fades into the background
    ├─────────────────────────────┤
    │   plain background color    │  ← the rest of the screen
    └─────────────────────────────┘
       (the whole top area has the screenTint gradient behind the blobs)

| Layer       | What it is                                                                                |
| ----------- | ----------------------------------------------------------------------------------------- |
| Base        | Plain `background` color (`#F7F9FB` light, `#0B1220` dark)                                |
| Tint band   | A vertical gradient (`screenTint`) over the top 260 to 520 dp                             |
| 3 blobs     | Radial gradients: blue, sky, and warm “sun”. Each fades from a color to transparent       |
| Bottom fade | A gradient over the last 90 dp so the band melts into the background with no visible edge |

There are three sizes, and screens pick one by name:

| Name            | Band height | Used on                                   |
| --------------- | ----------- | ----------------------------------------- |
| `AuroraTall`    | 520 dp      | Welcome, All set, Onboarding, Home header |
| `AuroraShort`   | 300 dp      | Log in, Forgot password                   |
| `AuroraCompact` | 260 dp      | Verify your email                         |
| `Plain`         | none        | Sign up, Cart, Checkout, Account          |

The colors are not written in this file. They come from the theme tokens (`AppTheme.gradients`). Dark mode is handled there too, so the code never checks `isSystemInDarkTheme()`.

* * *

### 1. `AppScreenBackground.kt`

It holds two things.

#### a) The enum

kotlin
    enum class AppScreenBackground(val bandHeight: Dp, val sunCenterY: Dp) {
        Plain(Dp.Hairline, Dp.Hairline),
        AuroraCompact(AppDimens.auroraCompact, AppDimens.auroraSunYShort),
        AuroraShort(AppDimens.auroraShort, AppDimens.auroraSunYShort),
        AuroraTall(AppDimens.auroraTall, AppDimens.auroraSunYTall),
    }

Each entry carries the only two values that change between variants: the **band height** and the **Y position of the yellow blob**. The blue and sky blobs are identical everywhere. A screen just says `AuroraShort` and never draws anything itself.

#### b) `AppScreenBackgroundBox`

kotlin
    Box(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (background != AppScreenBackground.Plain) {
            AppAuroraHeader(background)   // drawn first = behind
        }
        content()                          // drawn after = on top
    }

A `Box` stacks its children. The aurora is drawn first and the screen content on top. Its `content: @Composable BoxScope.() -> Unit` uses the same scope pattern as your `ColumnScope` question: the caller can use `Modifier.align(...)` inside it.

#### c) `AppAuroraHeader` (private) and `drawBlob`

This is the actual drawing, on a `Canvas`:

kotlin
    Canvas(modifier = Modifier.fillMaxWidth().height(background.bandHeight)) {
        drawRect(brush = gradients.screenTint)          // 1. tint band
        drawBlob(... aurora.blue ...)                   // 2. three glows
        drawBlob(... aurora.sky ...)
        drawBlob(... aurora.sun ...)
        translate(top = fadeTop) {                      // 3. bottom fade
            drawRect(brush = gradients.bottomFade, size = Size(size.width, fadeHeight.toPx()))
        }
    }

`drawBlob` computes the position and draws a circle with a radial gradient:

kotlin
    val x = size.width * (if (isRtl) 1f - xFraction else xFraction)
    drawCircle(brush = aurora.blob(color, center, radius), radius = radius, center = center)

Three details matter here:

| Detail                                                | Why                                                                                                                                                                                   |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `xFraction` (0.153, 0.916, 0.763) instead of fixed dp | The glows scale with screen width, so tablets still look right                                                                                                                        |
| `1f - xFraction` when RTL                             | In Arabic the layout mirrors, so the blue glow moves to the top-right, behind the mirrored back button                                                                                |
| `translate(top = fadeTop)` for the fade               | `bottomFade` has no explicit start/end, so it stretches over whatever rect it paints. Translating to the fade zone and drawing exactly 90 dp makes the gradient cover only that strip |

* * *

### `AppScaffold.kt` (the piece that uses the background)

You didn’t list it, but the other files depend on it.

kotlin
    AppScreenBackgroundBox(background = background, modifier = modifier) {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing, ...)
    }

* The Material `Scaffold` is set to **transparent**, so the aurora behind it stays visible.
* `safeDrawing` insets make the `padding` you receive include status bar, nav bar and keyboard. That’s why you must always apply `.padding(padding)`.
* The custom `AppTopBar` replaces Material’s `TopAppBar` with a white round back button, an optional centered title and subtitle, and an `actions` slot.
* `topBarActions: @Composable RowScope.() -> Unit` is again the scope pattern: inside the slot you can use `Modifier.weight(1f)`.
* Edge-to-edge (`enableEdgeToEdge()`) is required so the aurora also paints under the status bar.

Usage:

kotlin
    AppScaffold(background = AppScreenBackground.AuroraShort, onNavigateBack = { ... }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) { ... }
    }

* * *

### 2. `AppAuthHeader.kt`

It’s the block at the top of auth screens: **logo, then title, then subtitle**.

#### The sealed interface

kotlin
    sealed interface AuthHeaderLogo {
        data class Mark(val size: Dp) : AuthHeaderLogo   // the brand logo, no background
        data class Tile(val icon: Int) : AuthHeaderLogo  // 48 dp gradient square with a white icon
    }

A `sealed interface` lists every allowed logo type. Inside, `when (logo)` must cover both cases, and the compiler checks it.

#### The three looks in Figma

| Screen                                 | `logo`                                          | Alignment                        |
| -------------------------------------- | ----------------------------------------------- | -------------------------------- |
| Welcome (56 dp), Log in (48 dp)        | `Mark(size)`                                    | centered                         |
| Sign up, Forgot password, Verify email | `Tile(icon)` with `user-plus`, `lock` or `mail` | start (left, or right in Arabic) |

#### The key line

kotlin
    centered: Boolean = logo is AuthHeaderLogo.Mark

The default alignment follows the logo type: a Mark means centered, a Tile means start-aligned. You can still override it.

#### The layout

kotlin
    Column(horizontalAlignment = align) {
        // logo: Mark or Tile
        Spacer(24 dp)
        Text(title, headlineLarge, semantics { heading() })
        Spacer(8 dp)
        Text(subtitle, bodySmall, textSecondary)
    }

`semantics { heading() }` tells TalkBack that the title is a heading, so it reads “Welcome back, heading”.

Usage:

kotlin
    AppAuthHeader(
        title = "Welcome back",
        subtitle = "Log in to continue",
        logo = AuthHeaderLogo.Mark(AppDimens.logoMark),
    )

    AppAuthHeader(
        title = "Create account",
        subtitle = "It takes a minute",
        logo = AuthHeaderLogo.Tile(AppIcons.UserPlus),
    )

* * *

### 3. `AppAuthFooterLink.kt`

It’s the bottom line **“Already have an account? Log in”**.

kotlin
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(promptText, bodySmall, textSecondary)        // "Already have an account?"
        AppButton(text = actionText, onClick = onActionClick,
                  variant = AppButtonVariant.Ghost, size = AppButtonSize.Small)   // "Log in"
    }

The two parts are separate parameters (`promptText`, `actionText`), so each language translates them independently. The action is a **Ghost `AppButton`** (text only, brand color) instead of a clickable `Text`.

|                    | Old footer                             | New footer         |
| ------------------ | -------------------------------------- | ------------------ |
| Implementation     | `clickable(indication = null)` on text | Ghost `AppButton`  |
| Ripple feedback    | No                                     | Yes                |
| Touch area         | Tiny (just the text)                   | Proper button size |
| Accessibility role | None                                   | `Role.Button`      |

Usage:

kotlin
    AppAuthFooterLink(
        promptText = "Already have an account?",
        actionText = "Log in",
        onActionClick = onLoginClick,
    )

* * *

### 4. `AppOrDivider.kt`

It’s the **“or”** or **“or continue with”** separator between the main button and social login buttons.

kotlin
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
        if (showLines) HorizontalDivider(Modifier.weight(1f), ...)
        Text(text, labelMedium SemiBold, textTertiary)
        if (showLines) HorizontalDivider(Modifier.weight(1f), ...)
    }

| `showLines`                    | Result                                                                                        |
| ------------------------------ | --------------------------------------------------------------------------------------------- |
| `false` (default, as in Figma) | `or` (centered text only)                                                                     |
| `true`                         | `──────── or ────────` (a line on each side, each taking `weight(1f)` of the remaining space) |

The `weight(1f)` works because the code is inside a `Row`, so it uses `RowScope`.

It replaces the generic `AppDivider` on auth screens because it’s specialized (text plus optional lines) and only auth uses it.

kotlin
    AppOrDivider(text = "or continue with")
    AppOrDivider(text = "or", showLines = true)

* * *

### How it all fits together on one screen

kotlin
    AppScaffold(
        background = AppScreenBackground.AuroraShort,     // picks the aurora
        onNavigateBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenPadding)) {

            AppAuthHeader(title = "Welcome back", subtitle = "...", logo = AuthHeaderLogo.Mark(AppDimens.logoMark))
            // ... fields + main button ...
            AppOrDivider(text = "or continue with")
            // ... social buttons ...
            AppAuthFooterLink(promptText = "New here?", actionText = "Sign up", onActionClick = onSignUp)
        }
    }

### Why the files are placed where they are

| File                                                 | Folder                            | Reason                       |
| ---------------------------------------------------- | --------------------------------- | ---------------------------- |
| `AppScreenBackground`, `AppScaffold`                 | `core/common/components/scaffold` | Every feature uses them      |
| `AppAuthHeader`, `AppAuthFooterLink`, `AppOrDivider` | `features/auth/.../common`        |  |

## Check

- [ ] Log in (light): soft blue glow top-left, sky glow top-right, a hint of yellow at about 1/3 height, and no hard edge where the glow ends.
- [ ] Log in (dark): deep navy tint (`#172554` → `#0B1220`), three soft blue/navy glows with a faint warm one, cards with a thin `#24314F` outline, like Figma 13.2.
- [ ] No white blob in any header (the white glow is only the card highlight inside gradient cards).
- [ ] Verify email uses `AuroraCompact` (260 dp band).
- [ ] Arabic: the blue glow moves to the top-right, behind the (mirrored) back button.
- [ ] The status bar icons are drawn over the aurora (edge-to-edge), and the back button doesn't overlap them.
- [ ] Open the keyboard on the password field: the screen scrolls so the field and the Log in button can be reached.
- [ ] TalkBack on the title says "Welcome back, heading".
- [ ] `grep -rn "core.common.components.header" app/src/main/java` returns nothing after doc 14.
