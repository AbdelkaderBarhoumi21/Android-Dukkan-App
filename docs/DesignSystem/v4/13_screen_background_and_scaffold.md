# 13: Screen background, scaffold and auth header

**Files:**

| File | Status |
|---|---|
| `core/designsystem/components/layout/AppScreenBackground.kt` | new |
| `core/designsystem/components/layout/AppScaffold.kt` | new, replaces `core/common/components/scaffold/AppScaffold.kt` |
| `features/auth/presentation/components/common/AppAuthHeader.kt` | moved and rewritten from `core/common/components/header/AppAuthHeader.kt` |
| `features/auth/presentation/components/common/AppAuthFooterLink.kt` | moved and rewritten from `core/common/components/footer/AppAuthFooterLink.kt` |
| `features/auth/presentation/components/common/AppOrDivider.kt` | new, replaces `core/common/components/dividers/AppDivider.kt` on auth screens |

> **Why move the header and footer into `features/auth`?** Only auth screens use them. `core/` is for things *every* feature may use. Keeping auth-only UI in the auth feature follows the dependency rule from the project structure guide: features depend on core, never the other way. `AppAccountRecoveryHeader` and `AppAccountRecoveryFooter` become the same `AppAuthHeader` with a tile icon (see below), so delete them after doc 14.

---

## Why

Every Customer-app screen in Figma sits on one of three backgrounds:

| Background | Where in Figma | What it is |
|---|---|---|
| **Aurora tall** | 02.1 Welcome, 02.5 All set, 01.x Onboarding, 04.1 Home header | `background` plus a 520 dp `screenTint` band plus soft color blobs |
| **Aurora short** | 02.2 Log in, 02.4 Forgot password | the same, with a 300 dp band and the sun blob higher up |
| **Aurora compact** | 02.6 Verify your email, 02.7 Not verified yet | the same as short, with a 260 dp band |
| **Plain** | 02.3 Sign up, Cart, Checkout, Account | `background` only (`#F7F9FB` light, `#0B1220` dark) |

Today each screen paints `colorScheme.background` through `AppScaffold`. We keep that one entry point, `AppScaffold(background = …)`, so a screen chooses the background by name and never draws blobs itself.

**Where do the colors come from?** From `AppTheme.gradients` (doc 05): `screenTint`, `bottomFade` and `aurora` (`blue`, `sky`, `sun`). Each blob color already carries its alpha, and the helper `blob(color, center, radius): Brush` builds the radial fade to transparent. **Dark mode is in the tokens**, not in this composable: the dark `screenTint` is blue-950 → night-950, the dark `bottomFade` ends in night-950 and the dark blobs are re-tinted (blue-500 35 %, blue-800 45 %, amber-300 8 %, Figma `13.1b`). This composable never checks the theme and never touches the palette. That keeps the rule from doc 00: *only the theme layer reads primitives.*

---

## Figma spec: aurora (measured on a 393 dp wide frame)

Positions are the blob's bounding square (top-left x, top-left y, size). In code we use the **center** and radius, with x as a fraction of the width so it scales on tablets.

| Layer | Token | Tall (Welcome, Home) | Short (Log in) | Compact (Verify email) |
|---|---|---|---|---|
| Base | `background` | full screen | full screen | full screen |
| Tint band | `gradients.screenTint` (azure-100 → slate-50; dark: blue-950 → night-950) | height 520 | height 300 | height 260 |
| Blob blue | `aurora.blue` (light azure-300 62 %, dark blue-500 35 % → 0 %, radial) | center (0.153 w, 20), r 230 | same | same |
| Blob sky | `aurora.sky` (light azure-200 55 %, dark blue-800 45 %) | center (0.916 w, 60), r 190 | same | same |
| Blob warm (`sun`) | `aurora.sun` (light amber-200 42 %, dark amber-300 8 %) | center (0.763 w, 380), r 150 | center (0.763 w, 250), r 150 | same as short |
| Bottom fade | `gradients.bottomFade` | last 90 dp of the band | last 90 dp | last 90 dp |

There are **exactly 3 blobs**, in light and dark, at the same positions (Figma `13.1b · Header glow · spec`: blue c (60, 20) r 230, sky c (360, 60) r 190, warm c (300, 250) r 150, in screen px of the 393 × 852 frame).

The Figma "white · 35 %" is **not** a header blob: it is the **card highlight**, drawn only inside gradient cards (promo banners, wallet, earnings) with `Modifier.cardHighlight()` from doc 05. It does not belong in this composable.

**Dark mode** comes from the tokens (doc 05, Figma `01b` and `13.1b`): "Tint and fades go to night", and the blobs are re-tinted. Every value is official, so the code below is identical for both modes.

**Arabic (RTL):** mirror the x positions (x becomes w − x). The brand blob then sits behind the back button on the right, like it sits behind it on the left in LTR.

---

## Code

### `AppScreenBackground.kt`

```kotlin
package com.example.dukkanapp.core.designsystem.components.layout

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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.designsystem.theme.AppAurora
import com.example.dukkanapp.core.designsystem.theme.AppTheme

/** Band height and warm-blob position per aurora size (Figma 393 dp frames). */
enum class AppScreenBackground(val bandHeight: Dp, val sunCenterY: Dp) {
    Plain(0.dp, 0.dp),
    AuroraCompact(260.dp, 250.dp),
    AuroraShort(300.dp, 250.dp),
    AuroraTall(520.dp, 380.dp),
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
    val fadeHeight = 90.dp

    Canvas(modifier = Modifier.fillMaxWidth().height(background.bandHeight)) {
        drawRect(brush = gradients.screenTint)

        drawBlob(aurora, aurora.blue, xFraction = 0.153f, y = 20.dp.toPx(), radius = 230.dp.toPx(), isRtl)
        drawBlob(aurora, aurora.sky, xFraction = 0.916f, y = 60.dp.toPx(), radius = 190.dp.toPx(), isRtl)
        drawBlob(aurora, aurora.sun, xFraction = 0.763f, y = background.sunCenterY.toPx(), radius = 150.dp.toPx(), isRtl)

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

| Today | New |
|---|---|
| `containerColor = colorScheme.background` | transparent, drawn over `AppScreenBackgroundBox` |
| Material `TopAppBar` with a grey circle back box | a plain row: `AppIconButton(White)` 44 dp with `shadows.card`, at `screenPadding` from the edge and `space8` below the status bar (Figma y 58) |
| `title` always uses `titleLarge` | optional centered title (`titleMedium`) plus subtitle (`bodySmall`) for app bars like "Checkout · 4 items" |
| no step slot | `topBarActions` slot: the "1/2" progress on sign up, "Help" on order details |
| default insets | `contentWindowInsets = WindowInsets.safeDrawing`, so content stays clear of the status bar, navigation bar **and** keyboard |

```kotlin
package com.example.dukkanapp.core.designsystem.components.layout

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
import com.example.dukkanapp.core.designsystem.components.button.AppIconButton
import com.example.dukkanapp.core.designsystem.components.button.AppIconButtonVariant
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.designsystem.theme.AppTheme

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    background: AppScreenBackground = AppScreenBackground.Plain,
    title: String? = null,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
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
            snackbarHost = snackbarHost,
            content = content,
        )
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
            .height(AppDimens.controlMd),
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
                modifier = Modifier.align(Alignment.Center).padding(horizontal = AppDimens.controlMd + AppDimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center, maxLines = 1)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary, maxLines = 1) }
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

| Screen | Logo | Title | Align |
|---|---|---|---|
| 02.1 Welcome | Dukkan **mark**, 56 dp, brand gradient (no tile) | "Welcome to Dukkan", `headlineLarge` | center |
| 02.2 Log in | Dukkan **mark**, 48 dp | "Welcome back", `headlineLarge` | center |
| 02.3 Sign up, 02.4 Forgot, 02.6/02.7 Verify email | 48 dp **tile** (`radiusMd` 14, Figma "icon tile → md"; `gradients.brand`) with a white Lucide icon (`user-plus`, `lock`, `mail`) | `headlineLarge` | start |

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
import com.example.dukkanapp.core.designsystem.components.logo.AppLogoMark
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppTheme

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

**Logo:** `AppLogoMark(modifier, size)` comes from `core/designsystem/components/logo/AppLogo.kt` (doc 09). Don't create a second logo drawable here. Pass the size with `size = logo.size`. For the Log in screen use `AuthHeaderLogo.Mark(AppDimens.logoMark)` (48 dp).

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
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.button.AppButtonSize
import com.example.dukkanapp.core.designsystem.components.button.AppButtonVariant
import com.example.dukkanapp.core.designsystem.theme.AppTheme

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
import com.example.dukkanapp.core.designsystem.theme.AppDimens
import com.example.dukkanapp.core.designsystem.theme.AppTheme

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
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
