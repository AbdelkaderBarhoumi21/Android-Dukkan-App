# 10: Buttons (`AppButton`, `AppSocialButton`, `AppIconButton`)

**Files (new):**

- `core/theme/AppButtonsStyle.kt`
- `core/common/components/buttons/AppButton.kt`
- `core/common/components/buttons/AppSocialButton.kt`
- `core/common/components/buttons/AppIconButton.kt`

**Replaces:** `core/common/components/buttons/AppPrimaryButton.kt`, `core/common/components/buttons/AppTextButton.kt`, `features/auth/presentation/components/authoptions/AuthButton.kt`. Delete them at the end of [14_auth_screens_refactor.md](14_auth_screens_refactor.md), once no screen uses them.

**Needs first:** docs 00–09. You use `AppTheme.colors`, `AppTheme.gradients`, `AppTheme.shadows`, `Modifier.appShadow`, `AppDimens`, `AppPillShape` and `AppIcons` from `core.theme`.

---

## Why

Today every button is styled at the place it is used:

- `AppPrimaryButton` uses a flat `colorScheme.primary`.
- `AuthButton` takes 5 color parameters, and `AuthOptionsSection` passes `Color.Transparent` and `outline` by hand.

Figma has one pill button with **7 variants** and **5 sizes** (plus a separate slide-to-confirm button, see below). So we build one composable and pass it a *variant*. The variant decides the colors. The screen never chooses a color.

- **Open/closed:** a new look (for example "Gold") is one new enum value and one line in `appButtonStyle`. No screen changes.
- **Single responsibility:** `appButtonStyle()` only maps a variant to colors. `AppButtonContainer` only draws a pill and handles clicks. `AppButton` only lays out the label, icons and spinner.
- **Strategy pattern:** the variant enum is the strategy and `appButtonStyle` resolves it.

### Why not `Button(colors = ButtonDefaults.buttonColors(...))`?

The Figma primary button is a **gradient** (`Gradient/Button`) with a **colored shadow** (`Elevation/Button`, blue at 30 %).

- `ButtonDefaults.buttonColors` only accepts a `Color`, not a `Brush`.
- Material `elevation` draws a grey shadow, and you can't tint it on API < 28.

So we draw the pill ourselves: `appShadow`, then `clip`, then `background(brush)`, then `border`, then `clickable`. We still get a ripple, the button role for TalkBack, and the disabled state, because `clickable` provides all three.

---

## Figma spec: `Button/*`

All variants use the pill shape (`AppPillShape`), a label in `labelLarge` (Bold) and icons at `AppDimens.icon` (20 dp).

| Variant   | Fill               | Border               | Label / icon    | Shadow           | Used for                                |
| --------- | ------------------ | -------------------- | --------------- | ---------------- | --------------------------------------- |
| `Primary` | `gradients.button` | none                 | `onBrand`       | `shadows.button` | Main action: Log in, Continue, Checkout |
| `Deal`    | `gradients.deal`   | none                 | `onBrand`       | `shadows.deal`   | Flash-deal CTA ("Grab deals")           |
| `Dark`    | `inverse`          | none                 | `onInverse`     | none             | Cart bar "Cart", strong neutral CTA     |
| `Tonal`   | `brandSoft`        | none                 | `brandSoftText` | none             | Secondary action: Apply, Add photo      |
| `Outline` | `card`             | `border` 1.2 dp      | `textPrimary`   | none             | Cancel, Choose another address          |
| `Ghost`   | transparent        | none                 | `brand`         | none             | Text link: Forgot password?, See all    |
| `Danger`  | `errorSoft`        | `errorBorder` 1.2 dp | `error`         | none             | Cancel order, Delete address            |

| Size         | Figma token                             | Height | Horizontal padding | Label size           | Used for                  |
| ------------ | --------------------------------------- | ------ | ------------------ | -------------------- | ------------------------- |
| `Large`      | `size/button-lg` → `buttonLg`           | 52     | `space24`          | 15 sp (`labelLarge`) | primary & outline buttons |
| `Compact`    | `size/button-compact` → `buttonCompact` | 48     | `space24`          | 13.5 sp (`bodyStrong`) | danger / compact buttons  |
| `Medium`     | `size/button-md` → `buttonMd`           | 44     | `space20`          | 13.5 sp (`bodyStrong`) | secondary buttons         |
| `Small`      | `size/button-sm` → `buttonSm`           | 36     | `space16`          | 12.5 sp (`labelMedium`) | small buttons             |
| `ExtraSmall` | `size/button-xs` → `buttonXs`           | 26     | `space12`          | 12.5 sp (`labelMedium`) | inline "Apply" on coupons |

Label styles come from the Figma atoms (`Button/primary M`, `Button/primary S`, `Button/checkout`, `Button/danger`). `ExtraSmall`'s label and padding are our choice, because no atom shows that size.

**`size/button-slide` (58 dp, "slide to order / accept")** is **not** an `AppButtonSize`. It is a swipe-to-confirm control with a draggable thumb, so it gets its own composable (`AppSlideButton`, using `AppDimens.buttonSlide` and `gradients.button`) when the checkout screen needs it. It isn't built yet.

Use the Figma name in new code (`buttonLg`), not the older `controlLg`. They have the same value.

**States (Figma `4.2 · Atom states`).** "Default · Pressed · Disabled · Loading for the 3 main variants":

| State        | Primary                                                                 | Tonal            | Outline                      | Other variants (our rule, same idea)                          |
| ------------ | ----------------------------------------------------------------------- | ---------------- | ---------------------------- | ------------------------------------------------------------- |
| Default      | `gradients.button` + `shadows.button`                                   | `brandSoft`      | `card` + `border`            | as the table above                                            |
| **Pressed**  | **solid `brandPressed` fill, no shadow** ("no lift")                    | `brandRing` fill | **`border` fill**, `border` stays | Deal: gradient, no shadow. Dark / Ghost / Danger: ripple only |
| **Disabled** | `field` fill, `textTertiary` label, no border, no shadow                | same             | same                         | same (Ghost keeps a transparent fill)                         |
| **Loading**  | fill stays; **the spinner replaces the trailing icon, the label stays** | same             | same                         | same                                                          |

Rules that apply to every variant:

- **Pressed = no lift.** The shadow disappears while the finger is down, and there is no scale animation. The ripple still shows.
- **Loading** ignores clicks and TalkBack reads "Loading". If the button has no trailing icon, the spinner takes the trailing icon's place (16 dp, stroke 2.4, after the label), so the label shifts a little. That's Figma's "spinner replaces the trailing icon, label stays".

**Social button (`Social/Google`, `Social/Facebook`):** `card` fill, `border` 1.2 dp, height 52, label `labelLarge` 15 Bold `textPrimary`, untinted brand logo at 20 dp.

**Icon button (`Icon button/*`):** a 44 dp circle (`iconButton`, Figma `size/icon-button`) with a 20 dp icon.

| Variant | Fill                               | Icon          | Shadow         |
| ------- | ---------------------------------- | ------------- | -------------- |
| `White` | `card`                             | `textPrimary` | `shadows.card` |
| `Glass` | `card` at 72 % alpha (over photos) | `textPrimary` | `shadows.card` |
| `Soft`  | `brandSoft`                        | `brand`       | none           |
| `Field` | `field`                            | `textPrimary` | none           |
| `Brand` | `gradients.brand`                  | `onBrand`     | `shadows.card` |
| `Dark`  | `inverse`                          | `onInverse`   | `shadows.card` |

---

## Code

### `core/theme/AppButtonsStyle.kt`

```kotlin
package com.example.dukkanapp.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

enum class AppButtonVariant { Primary, Deal, Dark, Tonal, Outline, Ghost, Danger }
enum class AppButtonSize(
    val height: Dp,
    val horizontalPadding: Dp
) {
    Large(AppDimens.buttonLg, AppDimens.space24),
    Compact(AppDimens.buttonCompact, AppDimens.space24),
    Medium(AppDimens.buttonMd, AppDimens.space20),
    Small(AppDimens.buttonSm, AppDimens.space16),
    ExtraSmall(AppDimens.buttonXs, AppDimens.space12),
}

/** Everything a pill needs to draw itself. Built by [appButtonStyle], never by screens. */
@Immutable
internal class AppButtonStyle(
    val background: Brush,
    val content: Color,
    val border: BorderStroke?,
    val shadow: AppShadow?,
)

/**
 * Maps a variant + state to colors (Figma `4.1 · Atoms` + `4.2 · Atom states`).
 * [pressed]: finger down. Pressed buttons have no shadow ("no lift").
 */
@Composable
internal fun appButtonStyle(
    variant: AppButtonVariant, enabled: Boolean, pressed: Boolean = false
): AppButtonStyle {
    val colors = AppTheme.colors
    val gradients = AppTheme.gradients
    val shadows = AppTheme.shadows

    if (!enabled) {
        val fill = if (variant == AppButtonVariant.Ghost) Color.Transparent else colors.field
        return AppButtonStyle(
            background = SolidColor(fill),
            content = colors.textTertiary,
            border = null,
            shadow = null,
        )
    }


    return when (variant) {
        AppButtonVariant.Primary -> AppButtonStyle(
            background = if (pressed) SolidColor(colors.brandPressed) else gradients.button,
            content = colors.onBrand,
            border = null,
            shadow = if (pressed) null else shadows.button,
        )

        AppButtonVariant.Deal -> AppButtonStyle(
            background = gradients.deal,
            content = colors.onBrand,
            border = null,
            shadow = if (pressed) null else shadows.deal,
        )

        AppButtonVariant.Dark -> AppButtonStyle(
            background = SolidColor(colors.inverse),
            content = colors.onInverse,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Tonal -> AppButtonStyle(
            background = SolidColor(if (pressed) colors.brandRing else colors.brandSoft),
            content = colors.brandSoftText,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Outline -> AppButtonStyle(
            background = SolidColor(if (pressed) colors.border else colors.card),
            content = colors.textPrimary,
            border = BorderStroke(AppDimens.strokeThin, colors.border),
            shadow = null,
        )

        AppButtonVariant.Ghost -> AppButtonStyle(
            background = SolidColor(Color.Transparent),
            content = colors.brand,
            border = null,
            shadow = null,
        )

        AppButtonVariant.Danger -> AppButtonStyle(
            background = SolidColor(colors.errorSoft),
            content = colors.error,
            border = BorderStroke(AppDimens.strokeThin, colors.errorBorder),
            shadow = null,
        )
    }
}

/** Label style per size: real Figma text styles, read from the atoms. */
@Composable
internal fun appButtonTextStyle(size: AppButtonSize): TextStyle = when (size) {
    AppButtonSize.Large -> MaterialTheme.typography.labelLarge
    AppButtonSize.Compact, AppButtonSize.Medium -> AppTheme.textStyles.bodyStrong
    AppButtonSize.Small, AppButtonSize.ExtraSmall -> MaterialTheme.typography.labelMedium
}
```

### ``MutableInteractionSource`

C'est un **canal de messages** entre le doigt de l'utilisateur et ton bouton. Quand l'utilisateur touche, relâche ou annule, Compose y envoie un événement (`PressInteraction.Press`, `Release`, `Cancel`). Toi, tu peux écouter ce canal.
    val interactionSource = remember { MutableInteractionSource() }

* `MutableInteractionSource()` crée le canal.
* `remember { ... }` le garde entre les recompositions. Sans `remember`, un nouveau canal serait créé à chaque redessin et l'état serait perdu.

On le branche ensuite sur le bouton :
    Modifier.clickable(interactionSource = interactionSource, ...)

`clickable` envoie alors ses événements dans ce canal, et on les lit avec `collectIsPressedAsState()`.
`collectIsPressedAsState()`
---------------------------

    val isPressed by interactionSource.collectIsPressedAsState()

Cette fonction écoute le canal et le transforme en un `State<Boolean>` :

* `true` pendant que le doigt est posé sur le bouton
* `false` quand il est relâché ou annulé

Quand la valeur change, Compose redessine le bouton. C'est ce qui permet à `appButtonStyle(variant, enabled, pressed = isPressed)` de changer les couleurs au moment de l'appui.
Pourquoi `by` ?
---------------

Sans `by`, tu écrirais `.value` à chaque lecture :
    val isPressed = interactionSource.collectIsPressedAsState()
    if (isPressed.value) ...      // il faut .value

Avec `by`, Kotlin ajoute `.value` à ta place :
    val isPressed by interactionSource.collectIsPressedAsState()
    if (isPressed) ...            // un simple Boolean

Il faut importer `androidx.compose.runtime.getValue`. C'est ce qu'on appelle un **délégué de propriété**.
Le flux complet
---------------

    Doigt appuie
       → clickable envoie Press dans interactionSource
       → collectIsPressedAsState() met isPressed = true
       → Compose redessine AppButton
       → appButtonStyle(pressed = true) donne la couleur "pressed" et pas d'ombre
    Doigt relâche
       → Release → isPressed = false → retour à la couleur normale

Équivalent Flutter
------------------

C'est proche de `InkWell` avec `onHighlightChanged`, ou de `WidgetState.pressed` dans `ButtonStyle`. Ici tu le fais à la main parce que ton bouton est un `Box` personnalisé. Les boutons Material gèrent déjà cela en interne.

### `AppButton.kt`

```kotlin
package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonStyle
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appButtonStyle
import com.example.dukkanapp.core.theme.appButtonTextStyle
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Large,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val style = appButtonStyle(variant, enabled, pressed = isPressed && !isLoading)
    val loadingDescription = stringResource(R.string.cd_loading)

    AppButtonContainer(
        onClick = onClick,
        style = style,
        interactionSource = interaction,
        height = size.height,
        horizontalPadding = size.horizontalPadding,
        enabled = enabled,
        isLoading = isLoading,
        modifier = modifier.semantics {
            if (isLoading) stateDescription = loadingDescription
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)
        ) {
            leadingIcon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(AppDimens.icon)
                )
            }
            Text(
                text = text,
                style = appButtonTextStyle(size),
                color = style.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // trailing icon replace loading
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.iconSm),
                    color = style.content,
                    strokeWidth = AppDimens.strokeSpinner,
                )

                trailingIcon != null -> Icon(
                    painter = trailingIcon,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(AppDimens.icon)
                )
            }
        }
    }


}

@Composable
internal fun AppButtonContainer(
    onClick: () -> Unit,
    style: AppButtonStyle,
    interactionSource: MutableInteractionSource,
    height: Dp,
    horizontalPadding: Dp,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .then(
                if (style.shadow != null) Modifier.appShadow(
                    style.shadow,
                    AppPillShape
                ) else Modifier
            )
            .clip(AppPillShape)
            .background(style.background)
            .then(
                if (style.border != null) Modifier.border(style.border, AppPillShape) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = style.content),
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun AppButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            AppButtonVariant.entries.forEach { variant ->
                AppButton(
                    text = variant.name,
                    onClick = {},
                    variant = variant,
                    trailingIcon = painterResource(AppIcons.ArrowRight),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AppButton(text = "Compact", onClick = {}, size = AppButtonSize.Compact)
            AppButton(text = "Medium", onClick = {}, size = AppButtonSize.Medium)
            AppButton(text = "Small", onClick = {}, size = AppButtonSize.Small)
            AppButton(text = "Apply", onClick = {}, size = AppButtonSize.ExtraSmall)
            AppButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = "Continue",
                onClick = {},
                isLoading = true,
                trailingIcon = painterResource(AppIcons.ArrowRight),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

### `AppSocialButton.kt`

```kotlin
package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.appButtonStyle

/**
 * "Continue with Google / Facebook". Same pill as [AppButton] Outline,
 * but the logo keeps its brand colors (Image, not a tinted Icon).
 */
@Composable
fun AppSocialButton(
    text: String,
    logo: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val style = appButtonStyle(AppButtonVariant.Outline, enabled, pressed = isPressed && !isLoading)

    AppButtonContainer(
        onClick = onClick,
        style = style,
        interactionSource = interaction,
        height = AppDimens.buttonLg,
        horizontalPadding = AppDimens.space16,
        enabled = enabled,
        isLoading = isLoading,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
        ) {
            Image(
                painter = logo,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.icon)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = style.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

### `AppIconButton.kt`

```kotlin
package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppShadow
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow

enum class AppIconButtonVariant { White, Glass, Soft, Field, Brand, Dark }

@Composable
fun AppIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppIconButtonVariant = AppIconButtonVariant.White,
    size: Dp = AppDimens.iconButton,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val gradients = AppTheme.gradients
    val cardShadow = AppTheme.shadows.card
    val (fill: Brush, tint: Color, shadow: AppShadow?) = when (variant) {
        AppIconButtonVariant.White -> Triple(
            SolidColor(colors.card),
            colors.textPrimary,
            cardShadow
        )

        AppIconButtonVariant.Glass -> Triple(
            SolidColor(colors.card.copy(alpha = 0.72f)),
            colors.textPrimary,
            cardShadow
        )

        AppIconButtonVariant.Soft -> Triple(SolidColor(colors.brandSoft), colors.brand, null)
        AppIconButtonVariant.Field -> Triple(SolidColor(colors.field), colors.textPrimary, null)
        AppIconButtonVariant.Brand -> Triple(gradients.brand, colors.onBrand, cardShadow)
        AppIconButtonVariant.Dark -> Triple(
            SolidColor(colors.inverse),
            colors.onInverse,
            cardShadow
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (shadow != null) Modifier.appShadow(shadow, AppPillShape) else Modifier)
            .clip(
                CircleShape
            )
            .background(fill)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = ripple(color = tint),
                interactionSource = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textTertiary,
            modifier = Modifier.size(AppDimens.icon)
        )
    }
}
```

### New string (all three languages)

| Key          | EN      | AR           | FR         |
| ------------ | ------- | ------------ | ---------- |
| `cd_loading` | Loading | جارٍ التحميل | Chargement |

---

## Notes and rules

- **Width is the caller's job.** `AppButton` wraps its content. Screens add `Modifier.fillMaxWidth()` for full-width CTAs, or `Modifier.weight(1f)` for the Google/Facebook pair. The old `AppPrimaryButton` forced `fillMaxWidth()`, so it could not sit in a `Row`.
- **The order of modifiers matters:** shadow, then clip, then background, then border, then clickable, then padding.
  - Clipping before the shadow cuts the shadow off.
  - Padding before `clickable` makes the edges dead to touch.
- **Touch target.** `Large` (52) and `Compact` (48) are fine. `Medium` (44) is slightly under 48 dp but is Figma's secondary button size. `Small` (36) and `ExtraSmall` (26) are well below 48 dp. Use them only inside dense rows that already have space around them (product card, coupon "Apply"), or add `Modifier.minimumInteractiveComponentSize()` at the call site. For `ExtraSmall` always add it: 26 dp is too small to tap reliably.
- **Which size where:** primary and outline buttons use `Large`, danger and compact buttons use `Compact`, secondary buttons use `Medium`, small buttons use `Small`, and the inline coupon "Apply" uses `ExtraSmall`. Icons inside an `ExtraSmall` button don't fit a 20 dp icon well, so keep it text only.
- **Icons are `Painter`s.** Pass `painterResource(AppIcons.X)`, so Lucide drawables and brand logos use the same API.
- **No `onBrand` on Deal?** The deal gradient is orange to pink, and white text on it passes 3:1 for 15 sp Bold (large-text rule). Use Deal only for short labels.
- **Ripple:** `ripple()` is the Material 3 API (`androidx.compose.material3.ripple`). Don't use `rememberRipple()`, which is deprecated.

### Old to new

| Old call                                                                               | New call                                                                                 |
| -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `AppPrimaryButton(text, onClick, isLoading = x)`                                       | `AppButton(text, onClick, Modifier.fillMaxWidth(), isLoading = x)`                       |
| `AppTextButton(onClick, text)`                                                         | `AppButton(text, onClick, variant = AppButtonVariant.Ghost, size = AppButtonSize.Small)` |
| `AuthButton(text, icon = google, containerColor = Transparent, borderColor = outline)` | `AppSocialButton(text, logo = painterResource(AppIcons.Google), onClick)`                |
| `AuthButton(email, containerColor = primary, ...)`                                     | `AppButton(text, onClick, leadingIcon = painterResource(AppIcons.Mail))`                 |
| `AppScaffold` back `Box`                                                               | `AppIconButton(painterResource(AppIcons.ArrowLeft), cd, onClick)` (doc 13)               |

---

## 

1. The flow of `AppButtonStyle`

-------------------------------

`AppButtonStyle` is a small bag of 4 values that says how to **paint** the button. The button composable never picks colors itself. It asks a function.
    Screen: AppButton(variant = Primary, enabled = true, isLoading = false)
                │
                ├─ watches touch → isPressed (true while the finger is down)
                ▼
       appButtonStyle(variant, enabled, pressed)      ← the "decider"
                │   returns
                ▼
       AppButtonStyle(background, content, border, shadow)   ← the answer
                ▼
       AppButtonContainer draws the pill using that answer
       Text and Icon use style.content for their color

**The 4 fields (the answer):**

| Field        | Type            | What it is                                                                                        | Example (Primary) |
| ------------ | --------------- | ------------------------------------------------------------------------------------------------- | ----------------- |
| `background` | `Brush`         | Fill of the pill. `Brush` because it can be a gradient or one color (`SolidColor` wraps a color). | the blue gradient |
| `content`    | `Color`         | Color of the text, icons, spinner and ripple                                                      | white (`onBrand`) |
| `border`     | `BorderStroke?` | Outline, or `null` for none                                                                       | `null`            |
| `shadow`     | `AppShadow?`    | Colored glow, or `null` for none                                                                  | blue glow         |

**The 3 inputs of `appButtonStyle`:**

| Param     | Meaning                                                                                                                                                                |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `variant` | Which look: Primary, Deal, Dark, Tonal, Outline, Ghost, Danger                                                                                                         |
| `enabled` | If false, every variant becomes grey (`field` fill, `textTertiary` label, no border, no shadow). This check is first because disabled looks the same for all variants. |
| `pressed` | Finger is down. Primary turns solid `brandPressed` and the glow disappears ("no lift").                                                                                |

Two small details: `@Immutable` tells Compose the style won't change by itself, so it can skip extra work. `internal` means only the design-system module can build styles, and screens can't.

2. Compose also has Elevated, Filled, Tonal, Outlined and Text buttons

----------------------------------------------------------------------

Yes. They're the same family as Flutter:

| Flutter              | Compose (Material 3) |
| -------------------- | -------------------- |
| `ElevatedButton`     | `ElevatedButton`     |
| `FilledButton`       | `Button`             |
| `FilledButton.tonal` | `FilledTonalButton`  |
| `OutlinedButton`     | `OutlinedButton`     |
| `TextButton`         | `TextButton`         |

They map closely to our variants: Primary ≈ `Button`, Tonal ≈ `FilledTonalButton`, Outline ≈ `OutlinedButton`, Ghost ≈ `TextButton`.

3. Why we don't use them

------------------------

Because of what Figma asks for on the Primary button:

* **Gradient fill:** the `colors` parameter of these buttons takes a `Color`, not a `Brush`, so you can't pass a gradient.
* **Colored shadow:** Material's elevation draws a grey shadow, and you can't tint it on older Android versions (below API 28). Figma's glow is blue at 30%.
* **Two systems:** you could hack a gradient on top with `Modifier.background(...)`, but then you fight Material's built-in defaults (its colors, minimum size, shadow, pressed behavior) on every button. Seven variants made this messy.

So the doc builds the pill itself.

4. Why a `Box`

--------------

A `Box` is just an empty rectangle you can stack things in. We add what a button needs, step by step:
    Box(
        Modifier
            .height(height)
            .appShadow(...)        // 1. glow behind
            .clip(AppPillShape)    // 2. round the corners
            .background(brush)     // 3. fill (gradient or color)
            .border(...)           // 4. outline
            .clickable(role = Role.Button, ripple...)  // 5. tap, ripple, TalkBack "button"
            .padding(horizontal = ...)
    )

`Modifier.clickable` gives the ripple, the disabled state and the "Button" role for TalkBack, so we lose nothing important. Material's own `Button` is also built from a rectangle plus a row inside, so we're doing the same job with full control.

The order matters. If `clip` came before the shadow, the glow would be cut off. The doc's notes section explains this.
In short

--------

* **`AppButtonStyle`** = the answer to "what colors does this button use right now?", computed from the variant, `enabled` and `pressed`.
* **Not `ElevatedButton`, `Button` and so on**, because they can't do the gradient and the colored glow from Figma.
* **A `Box`** gives us full control, and `clickable` still provides ripple, disabled state and accessibility.

## Check

- [ ] `AppButtonPreview` shows 7 pills. Primary and Deal have a colored glow under them, and Outline has a thin grey border.
- [ ] The size previews measure 52 (Large), 48 (Compact), 44 (Medium), 36 (Small) and 26 (ExtraSmall) dp high, like Figma `size/button-*`.
- [ ] Press and hold Primary: the gradient becomes solid `#1D4ED8` and the glow disappears (no lift). Tonal turns `#DBE7FE`, Outline turns `#F1F5F9`.
- [ ] Disabled: grey `field` fill (`#F1F5F9` light, `#1A2540` dark), grey label, no glow.
- [ ] Loading: the label stays, the spinner takes the trailing arrow's place. Tapping it does nothing.
- [ ] TalkBack on a loading button says "Log in, Loading, button".
- [ ] Arabic (RTL): the trailing arrow icon sits on the left. Use a mirrored icon (`autoMirror` in the vector XML) for arrows.
- [ ] `grep -rn "ButtonDefaults.buttonColors" app/src/main/java` returns nothing outside `core/common/components/buttons/`.
