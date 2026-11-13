# 12: Selection controls, badges, prices and feedback

This doc covers the small atoms from Figma page **4.1 · Atoms**, the small molecules from **4.3 · Molecules** (status steps, toast, list rows) and the customer board **14 · Empty & loading states**. The bigger molecules (product card, banners, coupon, option rows, bars, bottom nav, app bars, sheet header) are in **doc 16**. Every size comes from `AppDimens` (doc 07). Each section has the spec table, then the code. Type them in this order: the auth screens (doc 14) only need **12.1 Checkbox**, **12.9 Progress bar** and **12.12 Message banner**. The rest you can type when the Home, Cart and Orders features start (**12.14 Empty state** and **12.15 Skeleton** come with the first list screen).

**Folders (new):**

```
core/common/components/
├── selection/   AppCheckbox.kt · AppRadio.kt · AppSwitch.kt · AppChip.kt
├── display/     AppBadge.kt · AppPriceText.kt · AppQuantity.kt · AppAvatar.kt ·
│                AppPagerIndicator.kt · AppProgressBar.kt · AppStatusSteps.kt
├── feedback/    AppToast.kt · AppToastHost.kt · AppEmptyState.kt · AppSkeleton.kt
├── banner/      AppMessageBanner.kt (already there: rewrite it)
└── list/        AppListRow.kt
```

**Replaces:**

- `core/common/components/checkbox/AppCheckBox.kt`
- `core/common/components/pager/AppPagerIndicator.kt`
- `core/common/components/dividers/AppDivider.kt` (use `HorizontalDivider(color = AppTheme.colors.border)` or `AppOrDivider` from doc 13)

---

## Why

- **One rule for every control: the whole row is the touch target.** Figma draws a 20 dp checkbox, but the user taps the label too. We put `toggleable` / `selectable` on the **row** with the right `Role`, so TalkBack says "Checkbox, Keep me logged in, checked", and the box itself is only a drawing.
- **Material controls don't match the sizes.** The M3 `Switch` is fixed at 52 × 32, and Figma's is 46 × 28. The M3 `Checkbox` draws a 2 dp outline at 18 dp. The custom drawings are ten lines each, and they read only tokens.
- **Variant enums again** (badges, toasts): the screen says *what* (`LowStock`), the component decides *how*.

---

## 12.1 Checkbox

| State | Box (`checkbox` 20, `radiusCheckbox` 6) | Mark |
|---|---|---|
| Off | `card` fill, `borderControl` `strokeControl` (1.6) | none |
| On | `brand` fill, no border | white `check` icon `iconXs` (14), stroke 3 |
| Disabled | 50 % alpha | as above |

The label is 12.5 sp SemiBold in `textLabel`, with a gap of `space8`. The row is at least `touchTargetMin` (48) tall.

> **`borderControl` (Figma `border/control`, Slate500)** is the outline of every unchecked checkbox and radio. It reaches the 3 : 1 contrast that WCAG asks for controls: 4.8 on white, 4.5 on the light background, 3.6 on the dark card, 3.2 on the dark field. `border` and `borderStrong` were too light.

```kotlin
package com.example.dukkanapp.core.common.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

private val CheckboxShape = RoundedCornerShape(AppDimens.radiusCheckbox)

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val fill by animateColorAsState(if (checked) colors.brand else colors.card, label = "checkboxFill")

    Row(
        modifier = modifier
            .heightIn(min = AppDimens.touchTargetMin)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.checkbox)
                .background(fill, CheckboxShape)
                .then(if (checked) Modifier else Modifier.border(AppDimens.strokeControl, colors.borderControl, CheckboxShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(AppIcons.Check),
                    contentDescription = null,
                    tint = colors.onBrand,
                    modifier = Modifier.size(AppDimens.iconXs),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textLabel,
        )
    }
}

/** Convenience overload for a plain string label. */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = AppCheckbox(checked, onCheckedChange, AnnotatedString(label), modifier, enabled)
```

> Your current `AppCheckBox.kt` uses `size(AppDimens.icon)`, `RoundedCornerShape(percent = 30)` and `strokeFocus` + `borderStrong`. Swap them for `checkbox`, `radiusCheckbox`, `strokeControl` + `borderControl`: the box looks the same at 20 dp, but each value now follows its Figma name.

> The `AnnotatedString` overload exists for the sign-up "I agree to the **Terms** and **Privacy Policy**" row, where the two words are links (see doc 14). Tapping a link opens the page, and tapping anywhere else toggles the box.

## 12.2 Radio (used by "Selectable option": payment, language)

| State | Circle | Inner dot |
|---|---|---|
| Off | `radioOff` (20), `card` fill, `borderControl` `strokeControl` (1.6) | none |
| On | `radio` (22), `brand` fill | `radioDot` (9), `card` |

```kotlin
package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Drawing only. Put `Modifier.selectable(selected, role = Role.RadioButton)` on the parent row. */
@Composable
fun AppRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    // The slot is always 22 so the row doesn't move when the radio turns on.
    Box(modifier = modifier.size(AppDimens.radio), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                modifier = Modifier.size(AppDimens.radio).background(colors.brand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(AppDimens.radioDot).background(colors.card, CircleShape))
            }
        } else {
            Box(
                Modifier
                    .size(AppDimens.radioOff)
                    .background(colors.card, CircleShape)
                    .border(AppDimens.strokeControl, colors.borderControl, CircleShape)
            )
        }
    }
}
```

> The dot is `surface/card` in Figma, not `on/primary`: in dark mode it becomes the dark card colour, so the radio reads as a ring. Your `AppRadio.kt` still writes `22.dp` twice: replace both with `AppDimens.radio`.

**Selectable option rows** (payment, language) are full molecules now: see **doc 16 · `AppOptionRow`**.

## 12.3 Switch

`switchWidth` × `switchHeight` (46 × 28), pill shaped. On = `brand` track, off = `border` track. The thumb is `switchThumb` (22) `card` with `shadows.card`, inset by `switchPadding` (3), and it slides by `switchThumbOffset` (18).

```kotlin
package com.example.dukkanapp.core.common.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val track by animateColorAsState(if (checked) colors.brand else colors.border, label = "switchTrack")
    val thumbOffset by animateDpAsState(if (checked) AppDimens.switchThumbOffset else Dp.Hairline, label = "switchThumb")

    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier // the parent row is toggleable (list row with a switch)
    }

    Box(
        modifier = modifier
            .then(toggle)
            .size(width = AppDimens.switchWidth, height = AppDimens.switchHeight)
            .background(track, AppPillShape)
            .padding(AppDimens.switchPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(AppDimens.switchThumb)
                .appShadow(AppTheme.shadows.card, CircleShape)
                .background(colors.card, CircleShape)
        )
    }
}
```

> `offset(x = …)` follows the layout direction, so in Arabic the "on" thumb sits on the left, which is what RTL users expect. `Dp.Hairline` is 0 dp: the "off" position.

## 12.4 Chip

Height `chip` (34), pill shaped, horizontal padding `space12`, label `labelMedium` (12.5 Bold). Optional leading icon `iconXs` (14), gap `space8`.

Figma `4.1 · Chip` has **four** looks. Doc 12 used to have only the first two:

| Variant | Figma example | Fill | Border | Label + icon | Role |
|---|---|---|---|---|---|
| `Outline`, not selected | "On sale" (tag icon) | `card` | `border` `strokeThin` | `textLabel` | `Role.Tab` (choice) |
| `Outline`, selected | "On sale" / "Selected" (check icon) | `brandSoft` | `brand` `strokeThin` | `brandSoftText` | `Role.Tab` |
| `Dark` | "Dark" + `sliders-horizontal` | `inverse` | none | `onInverse` | `Role.Button`: opens the filter sheet |
| `Brand` | "All" | `brand` | none | `onBrand` | `Role.Tab`: the active "All" pill at the start of a category row |

```kotlin
package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class AppChipVariant { Outline, Dark, Brand }

@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    variant: AppChipVariant = AppChipVariant.Outline,
) {
    val c = AppTheme.colors
    val fill: Color
    val stroke: Color?
    val content: Color
    when (variant) {
        AppChipVariant.Outline -> {
            fill = if (selected) c.brandSoft else c.card
            stroke = if (selected) c.brand else c.border
            content = if (selected) c.brandSoftText else c.textLabel
        }
        AppChipVariant.Dark -> { fill = c.inverse; stroke = null; content = c.onInverse }
        AppChipVariant.Brand -> { fill = c.brand; stroke = null; content = c.onBrand }
    }
    val click = if (variant == AppChipVariant.Dark) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
    }

    Row(
        modifier = modifier
            .height(AppDimens.chip)
            .clip(AppPillShape)
            .background(fill)
            .then(if (stroke != null) Modifier.border(AppDimens.strokeThin, stroke, AppPillShape) else Modifier)
            .then(click)
            .padding(horizontal = AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        leadingIcon?.let { Icon(it, null, tint = content, modifier = Modifier.size(AppDimens.iconXs)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
```

Usage on Home / Search:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
    AppChip("Filters", selected = false, onClick = onOpenFilters, variant = AppChipVariant.Dark,
        leadingIcon = painterResource(AppIcons.SlidersHorizontal))
    AppChip("All", selected = category == null, onClick = { onCategory(null) }, variant = AppChipVariant.Brand)
    AppChip("On sale", selected = onSale, onClick = onToggleSale, leadingIcon = painterResource(AppIcons.Tag))
}
```

> Your `AppChip.kt` has no `variant` yet and uses `iconSm` (16) for the icon. Figma draws chip icons at 14 → `AppDimens.iconXs`.

## 12.5 Badge

Height `badge` (20), pill shaped, horizontal padding `space8`, text `labelSmall` (10.5 Bold). Optional leading icon `iconXxs` (12).

| Variant | Fill | Text | Example |
|---|---|---|---|
| `Discount` | `gradients.deal` | `onBrand` | -15% |
| `Hot` | `deal` | `onBrand` | Hot deal |
| `New` | `inverse` | `onInverse` | New |
| `Eta` | `brandSoft` | `brandSoftText` | 25 min |
| `Bestseller` | `brandSoft` | `brandSoftText` | Bestseller |
| `LowStock` | `warningSoft` | `warningText` | Only 2 left |
| `FreeDelivery` | `successSoft` | `successText` | Free delivery |
| `Rating` | `warningSoft` | `warningText` | ★ 4.8 |
| `Code` | `warningSoft` | `warningText` | WELCOME15 (coupon code) |
| `Live` | `brandSoft` | `brandSoftText` | Live (live order card) |
| `Applied` | `successSoft` | `successText` | ✓ Applied (coupon) |

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class AppBadgeVariant { Discount, Hot, New, Eta, Bestseller, LowStock, FreeDelivery, Rating, Code, Live, Applied }

@Composable
fun AppBadge(
    text: String,
    variant: AppBadgeVariant,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
) {
    val c = AppTheme.colors
    val (fill: Brush, content: Color) = when (variant) {
        AppBadgeVariant.Discount -> AppTheme.gradients.deal to c.onBrand
        AppBadgeVariant.Hot -> SolidColor(c.deal) to c.onBrand
        AppBadgeVariant.New -> SolidColor(c.inverse) to c.onInverse
        AppBadgeVariant.Eta, AppBadgeVariant.Bestseller, AppBadgeVariant.Live -> SolidColor(c.brandSoft) to c.brandSoftText
        AppBadgeVariant.LowStock, AppBadgeVariant.Rating, AppBadgeVariant.Code -> SolidColor(c.warningSoft) to c.warningText
        AppBadgeVariant.FreeDelivery, AppBadgeVariant.Applied -> SolidColor(c.successSoft) to c.successText
    }
    val style = MaterialTheme.typography.labelSmall // Figma "Badge/*": M3/labelSmall 10.5/14 Bold

    Row(
        modifier = modifier
            .height(AppDimens.badge)
            .background(fill, AppPillShape)
            .padding(horizontal = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space2),
    ) {
        leadingIcon?.let { Icon(it, null, tint = content, modifier = Modifier.size(AppDimens.iconXxs)) }
        Text(text, style = style, color = content, maxLines = 1)
    }
}
```

## 12.6 Price

Figma `Price`: one line with **two text styles**, the amount and a smaller currency on the same baseline, and the old price struck through in `textTertiary`. Figma has no `Price/*` styles any more:

| Size | Amount | Currency | Use |
|---|---|---|---|
| `Large` | `headlineLarge` 24 | `titleCompact` 15 | product details |
| `Medium` | `titleMedium` 16 | `overline` 10.5 | cart total |
| `Small` | `titleCompact` 15 | `overline` 10.5 | price rows, cart bar |
| `Compact` | `titleSmall` 14 | `overline` 10.5 | small highlighted prices |

 Prices in Firestore are **integer millimes** (1 TND = 1000 millimes), so `279.000 TND` is stored as `279000`. Never store a `Double` for money.

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import java.util.Locale

enum class AppPriceSize { Large, Medium, Small, Compact }

/** 279000 → "279.000". Tunisian dinar has 3 decimals; always Latin digits. */
fun formatTnd(millimes: Long): String =
    String.format(Locale.ROOT, "%d.%03d", millimes / 1000, kotlin.math.abs(millimes % 1000))

@Composable
fun AppPriceText(
    millimes: Long,
    currency: String,
    modifier: Modifier = Modifier,
    oldMillimes: Long? = null,
    size: AppPriceSize = AppPriceSize.Small,
    color: Color = AppTheme.colors.textPrimary,
) {
    val typography = MaterialTheme.typography
    val textStyles = AppTheme.textStyles
    val (amountStyle, currencyStyle) = when (size) {
        AppPriceSize.Large -> typography.headlineLarge to textStyles.titleCompact
        AppPriceSize.Medium -> typography.titleMedium to textStyles.overline
        AppPriceSize.Small -> textStyles.titleCompact to textStyles.overline
        AppPriceSize.Compact -> typography.titleSmall to textStyles.overline
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
        Text(
            text = buildAnnotatedString {
                append(formatTnd(millimes))
                withStyle(currencyStyle.toSpanStyle()) { append(" $currency") }
            },
            style = amountStyle,
            color = color,
            modifier = Modifier.alignByBaseline(),
        )
        if (oldMillimes != null && oldMillimes > millimes) {
            Text(
                text = formatTnd(oldMillimes),
                // Figma product card: `Dukkan/caption` 11 SemiBold. Product details (Large): bodySmall 13.
                style = (if (size == AppPriceSize.Large) typography.bodySmall else textStyles.caption)
                    .copy(textDecoration = TextDecoration.LineThrough),
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
```

## 12.7 Quantity: add button, stepper, count bubble

Figma `4.1 · Qty stepper · Add`: "Add (+) turns into a stepper when the item is in the cart." The file shows six pieces:

| Part | Size | Spec |
|---|---|---|
| Add button | `addButton` 32 circle | `card` fill, `shadows.card`, `plus` `iconSm` (16) in `brand`, stroke 2.8 |
| Add button, filled | `addButton` 32 circle | `brand` fill, `shadows.card`, white `plus` |
| Qty stepper, **quantity 1** | `stepperWidth` × `stepper` (88 × 32) pill, `brand` | **`trash-2`** · `1` · `+` |
| Qty stepper, quantity ≥ 2 | 88 × 32 pill, `brand` | `minus` · `3` · `+` |
| Cart stepper, **quantity 1** | `cartStepperWidth` × `cartStepper` (112 × 52) pill, `field` | white 32 circle with **`trash-2`** (`textPrimary`) · `1` · `brand` 32 circle with white `+` |
| Cart stepper, quantity ≥ 2 | 112 × 52 pill, `field` | white 32 circle with `minus` · `2` · `brand` 32 circle with white `+` |
| Count bubble | `countBubble` 30 circle | `brand` fill, `strokeBubble` (2.5) `card` ring, `labelStrong` `onBrand` |

**Inside the compact stepper** (measured on `Qty stepper`): each end is a `stepperButton` (24) slot, `space4` from the pill edge. The icons are `iconXs` (14), white, stroke 2.4 (trash / minus) and 2.6 (plus). The `+` slot has a **white circle at 22 %** behind it (`card.copy(alpha = 0.22f)`); the left slot has none. The count is `titleSmall` (14/20 ExtraBold) `onBrand`.

**Inside the cart stepper:** `cartStepperPadding` (10) from the edge, two `cartStepperButton` (32) circles: `card` with `minus` in `textPrimary`, and `brand` with a white `plus`. Icons are `iconSm` (16). The count is `titleMedium` (16/22 ExtraBold) `textPrimary`.

### The delete rule

| Quantity | Left button | What a tap does | TalkBack |
|---|---|---|---|
| 0 | (no stepper: Add button) | `onIncrease` | "Add to cart" |
| 1 | **`trash-2`** | `onRemove`: the item leaves the cart | "Remove from cart" |
| ≥ 2 | `minus` | `onDecrease` | "Decrease quantity" |

Why a separate `onRemove` instead of `onDecrease` down to 0? The ViewModel shows the Figma **"Item removed from cart · Undo"** toast (12.11) only on a remove. Going from 3 to 2 shows nothing.

The rule is the same for both styles: Figma now has both `Cart stepper` versions side by side (quantity 1 with the trash in `text/primary`, quantity 2 with the minus).

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppStepperStyle { Compact, Large }

/**
 * Figma `Add button` → `Qty stepper` / `Cart stepper`.
 * 0 → "+" button. 1 → trash · 1 · +. 2+ → − · n · +.
 */
@Composable
fun AppQuantityControl(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppStepperStyle = AppStepperStyle.Compact,
    canIncrease: Boolean = true,
) {
    // Cross-fade between "+" and the stepper, like the Figma prototype.
    AnimatedContent(targetState = quantity == 0, label = "addToStepper", modifier = modifier) { isEmpty ->
        if (isEmpty) {
            AppAddButton(onClick = onIncrease, enabled = canIncrease)
        } else {
            AppStepper(quantity, onIncrease, onDecrease, onRemove, style, canIncrease)
        }
    }
}

/** Figma `Add button`: 32 circle. [filled] = brand fill with a white plus. */
@Composable
fun AppAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppDimens.addButton)
            .appShadow(AppTheme.shadows.card, CircleShape)
            .clip(CircleShape)
            .background(if (filled) c.brand else c.card)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(AppIcons.Plus),
            contentDescription = stringResource(R.string.cd_add_to_cart),
            tint = if (filled) c.onBrand else c.brand,
            modifier = Modifier.size(AppDimens.iconSm),
        )
    }
}

@Composable
private fun AppStepper(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    style: AppStepperStyle,
    canIncrease: Boolean,
) {
    val c = AppTheme.colors
    val isLarge = style == AppStepperStyle.Large
    val isRemove = quantity == 1

    // Every number comes from AppDimens: the two styles only pick different tokens.
    val height = if (isLarge) AppDimens.cartStepper else AppDimens.stepper
    val minWidth = if (isLarge) AppDimens.cartStepperWidth else AppDimens.stepperWidth
    val buttonSize = if (isLarge) AppDimens.cartStepperButton else AppDimens.stepperButton
    val iconSize = if (isLarge) AppDimens.iconSm else AppDimens.iconXs
    val countStyle = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall

    Row(
        modifier = Modifier
            .height(height)
            .widthIn(min = minWidth)
            .background(if (isLarge) c.field else c.brand, AppPillShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = if (isRemove) AppIcons.Trash2 else AppIcons.Minus,
            contentDescription = stringResource(if (isRemove) R.string.cd_remove_from_cart else R.string.cd_decrease),
            onClick = if (isRemove) onRemove else onDecrease,
            circleSize = buttonSize,
            iconSize = iconSize,
            circle = if (isLarge) c.card else Color.Transparent,
            tint = if (isLarge) c.textPrimary else c.onBrand,
        )
        Text(
            text = quantity.toString(),
            style = countStyle,
            color = if (isLarge) c.textPrimary else c.onBrand,
            modifier = Modifier.weight(1f, fill = false),
        )
        StepperButton(
            icon = AppIcons.Plus,
            contentDescription = stringResource(R.string.cd_increase),
            onClick = onIncrease,
            enabled = canIncrease,
            circleSize = buttonSize,
            iconSize = iconSize,
            circle = if (isLarge) c.brand else c.card.copy(alpha = 0.22f),
            tint = c.onBrand,
        )
    }
}

/**
 * The touch area is a square as tall as the pill (32 or 52), so each end is easy to hit.
 * The visible circle inside it is the Figma 24 / 32 slot.
 */
@Composable
private fun StepperButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    circleSize: Dp,
    iconSize: Dp,
    circle: Color,
    tint: Color,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(circleSize).background(circle, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

// ── AppCountBubble.kt (its own file in display/) ──

/** Figma `Count bubble`: 30 brand circle with a 2.5 white ring. */
@Composable
fun AppCountBubble(count: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppDimens.countBubble)
            .background(c.brand, CircleShape)
            .border(AppDimens.strokeBubble, c.card, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = AppTheme.textStyles.labelStrong,
            color = c.onBrand,
        )
    }
}
```

Check the sums: compact = 32 (button) + count + 32 ≥ `stepperWidth` (88), and large = 52 + count + 52 ≥ `cartStepperWidth` (112). `widthIn(min = …)` keeps the Figma width for 1–9 and lets "12" widen the pill instead of overlapping the buttons.

**In the ViewModel (MVI):**

```kotlin
is CartIntent.Increase -> repository.setQuantity(id, current + 1)
is CartIntent.Decrease -> repository.setQuantity(id, current - 1) // only sent when current ≥ 2
is CartIntent.Remove -> {
    repository.remove(id)
    sendEffect(
        CartEffect.ShowToast(
            title = UiText(R.string.toast_item_removed),
            type = AppToastType.Info,
            subtitle = UiText.Raw(product.name),
        ),
    )
}
```

## 12.8 Avatar and rating

| Part | Spec |
|---|---|
| Avatar | `avatarLg` 50 / `avatarHeader` 48 / `avatarMd` 36 / `avatarSm` 28 circle. Photo, or initials on `brandRing` fill with `brandSoftText` text (Bold) |
| Rating | filled `star` `iconXxs` (12) in `gold`, gap `space4`, then "4.8 (1.2k)" in `labelSmall` (10.5 Bold) `textLabel` |
| ETA (optional, after the rating) | gap `space12`, `zap` `iconXxs` in `brand`, then "25 min" in `labelSmall` `brand` |

Figma's `Rating` atom is the whole line **"★ 4.8 (1.2k)  ⚡ 25 min"**, and the product card uses it as is. So `AppRating` takes an optional `eta`.

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppAvatarSize(val size: Dp) {
    Small(AppDimens.avatarSm),
    Medium(AppDimens.avatarMd),
    Header(AppDimens.avatarHeader),
    Large(AppDimens.avatarLg),
}

@Composable
fun AppAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    size: AppAvatarSize = AppAvatarSize.Medium,
) {
    Box(
        modifier = modifier
            .size(size.size)
            .clip(CircleShape)
            .background(AppTheme.colors.brandRing),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.size))
        } else {
            Text(initials.take(2).uppercase(), style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.brandSoftText)
        }
    }
}

/** Figma `Rating`: "★ 4.8 (1.2k)" and, if given, "⚡ 25 min". */
@Composable
fun AppRating(value: String, count: String?, modifier: Modifier = Modifier, eta: String? = null) {
    val c = AppTheme.colors
    val style = MaterialTheme.typography.labelSmall
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(AppIcons.StarFilled), null, tint = c.gold, modifier = Modifier.size(AppDimens.iconXxs))
        Spacer(Modifier.width(AppDimens.space4))
        Text(if (count != null) "$value ($count)" else value, style = style, color = c.textLabel)
        if (eta != null) {
            Spacer(Modifier.width(AppDimens.space12))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
                Icon(painterResource(AppIcons.Zap), null, tint = c.brand, modifier = Modifier.size(AppDimens.iconXxs))
                Text(eta, style = style, color = c.brand)
            }
        }
    }
}
```

> Avatars and product photos come from Firebase Storage URLs. When you start Home, add **Coil 3** (`io.coil-kt.coil3:coil-compose` and `coil-network-okhttp`) and pass `rememberAsyncImagePainter(url)` as `photo`. Ratings use `AppIcons.StarFilled` tinted `colors.gold` (Figma `2.1 · Icons`: "ratings use the filled star in accent/gold"; doc 09).

## 12.9 Pager indicator and progress bar

| Part | Spec |
|---|---|
| Pager dot (inactive) | `pagerIndicatorSize` (6) circle, `border` |
| Pager dot (active) | `pagerIndicatorSelectedWidth` × `pagerIndicatorSize` (22 × 6) pill, `brand`. The width animates |
| Progress bar (sign-up "1/2") | `progressTrack` (4) track, `border`, with a `brand` fill and animated progress |

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppPagerIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                if (selected) AppDimens.pagerIndicatorSelectedWidth else AppDimens.pagerIndicatorSize,
                label = "dotWidth",
            )
            Box(
                Modifier
                    .size(width = width, height = AppDimens.pagerIndicatorSize)
                    .background(if (selected) AppTheme.colors.brand else AppTheme.colors.border, AppPillShape)
            )
        }
    }
}

@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .height(AppDimens.progressTrack)
            .background(AppTheme.colors.border, AppPillShape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(animated, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(AppTheme.colors.brand, AppPillShape)
        )
    }
}
```

## 12.10 Order status steps

Figma "Status steps" are the order timeline: **Placed, Packing, On the way, At door, Done**. Figma `4.3` shows all five positions (`Placed (current)` … `Done (current)`), and they all follow these rules:

| Step | Shape | Fill | Icon |
|---|---|---|---|
| Done | `statusStepDone` (20) circle | `brandRing` | `check` `iconXxs` (12) in `brand`, stroke 3 |
| Current | `statusStepCurrent` (30) tall pill, padding start `space8`, end `space12` | `brand` | the step's icon `iconXs` (14) `onBrand` + label `labelStrong` (12.5 ExtraBold) `onBrand`, gap `space8` |
| Upcoming | `statusStep` (22) circle | `field` | the step's icon `iconXxs` (12) in `textTertiary`, stroke 2.2 |
| Connector | `stepConnector` (2) line, fills the space | `brand` after a done step, `borderStrong` before an upcoming one | — |

| Step | Icon |
|---|---|
| Placed | `Receipt` |
| Packing | `Package` |
| On the way | `Truck` |
| At door | `House` |
| Done | `Check` |

> **Changed from the first version of this doc:** done steps were a solid brand circle with a white check, and upcoming steps were `brandRing` with a brand icon. Figma now draws done = `brandRing` + brand check and upcoming = `field` + grey icon, so only the current step is solid blue.

The values map 1:1 to `orders/{id}.status` (see the Firebase docs).

```kotlin
package com.example.dukkanapp.core.common.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

data class AppStep(val label: String, val icon: Int)

@Composable
fun AppStatusSteps(steps: List<AppStep>, currentIndex: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, step ->
            when {
                index < currentIndex -> Box(
                    Modifier.size(AppDimens.statusStepDone).background(c.brandRing, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(AppIcons.Check), null, tint = c.brand, modifier = Modifier.size(AppDimens.iconXxs)) }

                index == currentIndex -> Row(
                    Modifier
                        .height(AppDimens.statusStepCurrent)
                        .background(c.brand, AppPillShape)
                        .padding(start = AppDimens.space8, end = AppDimens.space12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
                ) {
                    Icon(painterResource(step.icon), null, tint = c.onBrand, modifier = Modifier.size(AppDimens.iconXs))
                    Text(step.label, style = AppTheme.textStyles.labelStrong, color = c.onBrand)
                }

                else -> Box(
                    Modifier.size(AppDimens.statusStep).background(c.field, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(step.icon), null, tint = c.textTertiary, modifier = Modifier.size(AppDimens.iconXxs)) }
            }
            if (index < steps.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = AppDimens.space2)
                        .height(AppDimens.stepConnector)
                        .background(if (index < currentIndex) c.brand else c.borderStrong, AppPillShape)
                )
            }
        }
    }
}
```

> TalkBack: give the whole row one description, for example `Modifier.semantics(mergeDescendants = true) { contentDescription = "Order status: On the way, step 3 of 5" }`, instead of reading five icons.

## 12.11 Toast (Figma `4.4 · Toast`, the "aura toast")

Floating feedback that confirms, warns or offers one quick action, then gets out of the way. It **replaces the old accent-bar toast everywhere**. Figma has 2 component sets (`Toast` and `Toast · RTL`), each with 10 variants: Type (`Success`, `Error`, `Warning`, `Info`, `Loading`) × Theme (`Light`, `Dark`). Component properties: `Title`, `Subtitle`, `Action` (text) and `Show subtitle`, `Show action`, `Show close`, `Show timer` (boolean).

### Figma spec: anatomy and tokens

| Part | Figma value | Our code |
|---|---|---|
| Container | 353 wide (screen − 2 × 20), **64 high**, `radius/card` 20, padding 10 top/bottom/icon side, 12 on the close side, gap `space/12` | `AppDimens.toast`, `radiusCard`, `toastPaddingStart`, `space12` |
| Surface | `surface/card` + status wash: linear gradient, status colour **9 % → 0 at 62 %** (**16 %** in dark) | `card` + `shadows.toast.washAlpha` |
| Border | 1 px, status colour at **18 %** (**24 %** in dark), inner alignment | `shadows.toast.borderAlpha` |
| Shadow | `Elevation/Toast`, `Dark/Elevation/Toast` (3 layers, doc 05) | `shadows.toast.layers`, `topHighlight` |
| Halo icon | 44 halo (status 10 %; **14 % in dark**) → 34 ring (status 16 %; **22 % in dark**) → 24 core (status, drop shadow 0 3 8 status 35 %, inner shadow 0 1 0 white 25 %) → 13 glyph, stroke 1.73 | `toastHalo` → `toastRing` → `toastCore` → `toastGlyph` |
| Glyph colour | `on/primary` (`surface/card` in dark) | `shadows.toast.onStatus` |
| Loading | 20 spinner in the ring, stroke 2.5, track `border/default`, arc status | `toastSpinner`, `toastSpinnerStroke` |
| Title | `M3/titleSmall` 14/20 ExtraBold, `text/primary`, 1 line (RTL: `Arabic/labelLarge`) | `titleSmall` / `labelLarge` |
| Subtitle | `M3/bodySmall` 13/18 Medium, `text/secondary`, max 2 lines (RTL: `Arabic/labelMedium`) | `bodySmall` / `labelMedium` |
| Action | solid pill, `radius/full`, 32 high, padding 8 / 14, status colour fill, `M3/labelMedium` `on/primary`, shadow 0 4 10 −2 status 30 % | `toastAction`, `toastActionPaddingH`, `toastActionShadow*` |
| Close + timer | 28 ring, stroke 2: track `text/tertiary` 25 % + status arc that drains over the duration, × 12 (stroke 1.3) `text/secondary`, 44 touch target | `toastTimer`, `toastTimerStroke`, `iconXxs`, `iconButton` |
| Info / Loading | no status/info token: `brand/primary` + brand colours | `statusColor()` |

Status colours follow the theme automatically:

| Type | Token | Light | Dark | Glyph |
|---|---|---|---|---|
| `Success` | `success` | `#16A34A` | `#22C55E` | check |
| `Error` | `error` | `#DC2626` | `#F87171` | × |
| `Warning` | `warning` | `#D97706` | `#F59E0B` | ! |
| `Info` | `brand` | `#2563EB` | `#6090FA` | i |
| `Loading` | `brand` | `#2563EB` | `#6090FA` | spinner |

> **Dark mode:** the glyph and action label use `surface/card` (`#111A2E`), confirmed on the dark variants. The spec text still says `on/primary`, but the components are correct: white on `#22C55E` would be only about 2.3 : 1.

### Figma spec: behaviour

| Topic | Rule |
|---|---|
| Position | Top: 8 below the status bar. Above the nav bar (12 gap) when the keyboard or a sheet is open |
| Duration | 4 s. 6 s with an action. An error with an action stays until dismissed or acted on |
| Stacking | One at a time. A new toast replaces the current one (no pile-up) |
| Dismiss | Swipe up or sideways, tap close, or the action. Pause the timer while pressed |
| Motion | In: slide 16 px + fade, 240 ms ease-out-back. Out: fade + scale 0.96, 160 ms. Reduced motion: fade only |
| Haptics | Success: light impact. Error: notification error. None for info |
| Accessibility | Live region (polite; assertive for Error). The icon carries meaning with the text, never colour alone. Contrast ≥ 4.5 : 1 for text, ≥ 3 : 1 for the glyph |
| RTL | Use the `Toast · RTL` set: icon right, close left, timer drains right → left |
| Copy | Title ≤ 32 chars, says what happened. Subtitle adds the one detail that matters. Action is one verb |

**Do:** confirm an action the user just took (added, saved, copied, removed). Offer Undo for destructive actions instead of a confirm dialog. Use Warning for soft blockers the user can still act on.

**Don't:** use a toast for errors that block the flow (use an inline error or a sheet). Put two actions or links in the subtitle. Stack toasts or show one on top of a bottom sheet handle.

### Real copy from Figma ("In context")

| Type | Title | Subtitle | Action |
|---|---|---|---|
| Success | Added to cart | Air Runner 2 · EU 42 | View |
| Success | Order placed | Arrives 08:54 – 09:14 | Track |
| Success | Address saved | — | — |
| Success | Copied to clipboard | — | — |
| Error | Couldn't apply coupon | This code has expired | Retry |
| Error | No internet connection | We'll retry automatically | Retry |
| Error | Payment failed | Your card was declined | — |
| Warning | Only 2 left in stock | Order now to get it today | View |
| Warning | Email not verified | Verify to place orders | Verify |
| Info | Item removed from cart | Air Runner 2 · EU 42 | Undo |
| Info | Reset link sent | Check your inbox | — |
| Loading | Placing your order… | 765.300 TND · cash on delivery | — |
| Loading | Uploading photo… | 2 of 3 · 1.2 MB | Cancel |

### Code: `AppToast.kt`

```kotlin
package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppShadow
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.AppToastTokens
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.theme.appShadowLayers

enum class AppToastType { Success, Error, Warning, Info, Loading }

/** Figma has no status/info token: Info and Loading use the brand colour. */
@Composable
internal fun AppToastType.statusColor(): Color {
    val c = AppTheme.colors
    return when (this) {
        AppToastType.Success -> c.success
        AppToastType.Error -> c.error
        AppToastType.Warning -> c.warning
        AppToastType.Info, AppToastType.Loading -> c.brand
    }
}

/**
 * The toast itself: no timer logic, no position. [AppToastHost] adds those.
 * Pass [timerProgress] (1 → 0) to show the draining ring, [onClose] to show the close button.
 */
@Composable
fun AppToast(
    title: String,
    type: AppToastType,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    timerProgress: (() -> Float)? = null,
) {
    val colors = AppTheme.colors
    val tokens = AppTheme.shadows.toast
    val status = type.statusColor()
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val wash = status.copy(alpha = tokens.washAlpha)
    val washBrush = if (isRtl) {
        Brush.horizontalGradient(0.38f to Color.Transparent, 1f to wash)
    } else {
        Brush.horizontalGradient(0f to wash, 0.62f to Color.Transparent)
    }
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .semantics {
                liveRegion = if (type == AppToastType.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
            }
            .heightIn(min = AppDimens.toast)
            .appShadowLayers(tokens.layers, shape)
            .clip(shape)
            .background(colors.card)
            .background(washBrush)
            .border(AppDimens.strokeHairline, status.copy(alpha = tokens.borderAlpha), shape)
            .then(
                tokens.topHighlight?.let { highlight ->
                    Modifier.innerShadow(
                        shape = shape,
                        shadow = Shadow(
                            radius = highlight.blur,
                            color = highlight.color,
                            offset = DpOffset(Dp.Hairline, highlight.offsetY),
                        ),
                    )
                } ?: Modifier,
            )
            // Padding is 10 on the icon side and 12 on the close side; start/end mirror in RTL.
            .padding(
                PaddingValues(
                    start = AppDimens.toastPaddingStart,
                    end = AppDimens.space12,
                    top = AppDimens.toastPaddingStart,
                    bottom = AppDimens.toastPaddingStart,
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        ToastHalo(type = type, status = status, tokens = tokens)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (isRtl) typography.labelLarge else typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = if (isRtl) typography.labelMedium else typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actionText?.let {
            ToastAction(text = it, status = status, onStatus = tokens.onStatus, onClick = onAction)
        }
        if (onClose != null || timerProgress != null) {
            ToastClose(status = status, onClose = onClose, timerProgress = timerProgress, isRtl = isRtl)
        }
    }
}

/** 44 halo → 34 ring → 24 core → 13 glyph. */
@Composable
private fun ToastHalo(type: AppToastType, status: Color, tokens: AppToastTokens) {
    Box(
        modifier = Modifier
            .size(AppDimens.toastHalo)
            .clip(CircleShape)
            .background(status.copy(alpha = tokens.haloAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.toastRing)
                .clip(CircleShape)
                .background(status.copy(alpha = tokens.ringAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            if (type == AppToastType.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.toastSpinner),
                    color = status,
                    trackColor = AppTheme.colors.border,
                    strokeWidth = AppDimens.toastSpinnerStroke,
                )
            } else {
                ToastCore(type = type, status = status, onStatus = tokens.onStatus)
            }
        }
    }
}

@Composable
private fun ToastCore(type: AppToastType, status: Color, onStatus: Color) {
    Box(
        modifier = Modifier
            .size(AppDimens.toastCore)
            .appShadow(
                AppShadow(status.copy(alpha = 0.35f), offsetY = AppDimens.toastCoreShadowY, blur = AppDimens.toastCoreShadowBlur),
                CircleShape,
            )
            .background(status, CircleShape)
            .innerShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = Dp.Hairline,
                    color = Color.White.copy(alpha = 0.25f),
                    offset = DpOffset(Dp.Hairline, AppDimens.strokeHairline),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            AppToastType.Success -> ToastGlyphIcon(AppIcons.Check, onStatus)
            AppToastType.Error -> ToastGlyphIcon(AppIcons.X, onStatus)
            // "!" and "i" are bare glyphs. The Lucide icons of the same name carry their own circle.
            AppToastType.Warning -> ToastGlyphText("!", onStatus)
            AppToastType.Info -> ToastGlyphText("i", onStatus)
            AppToastType.Loading -> Unit
        }
    }
}

@Composable
private fun ToastGlyphIcon(icon: Int, tint: Color) {
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(AppDimens.toastGlyph))
}

@Composable
private fun ToastGlyphText(text: String, color: Color) {
    Text(text = text, style = AppTheme.textStyles.labelStrong, color = color)
}

@Composable
private fun ToastAction(text: String, status: Color, onStatus: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = AppDimens.toastAction)
            .appShadow(
                AppShadow(
                    status.copy(alpha = 0.30f),
                    offsetY = AppDimens.toastActionShadowY,
                    blur = AppDimens.toastActionShadowBlur,
                    spread = AppDimens.toastActionShadowSpread,
                ),
                AppPillShape,
            )
            .clip(AppPillShape)
            .background(status)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppDimens.toastActionPaddingH, vertical = AppDimens.space8),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = onStatus)
    }
}

/** 28 ring inside a 44 touch target: track + status arc that drains, × 12 in the middle. */
@Composable
private fun ToastClose(
    status: Color,
    onClose: (() -> Unit)?,
    timerProgress: (() -> Float)?,
    isRtl: Boolean,
) {
    val colors = AppTheme.colors
    val track = colors.textTertiary.copy(alpha = 0.25f)
    Box(
        modifier = Modifier
            .size(AppDimens.iconButton)
            .then(
                if (onClose != null) {
                    Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClose)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(AppDimens.toastTimer)) {
            val strokePx = AppDimens.toastTimerStroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawCircle(color = track, radius = arcSize.width / 2, style = Stroke(strokePx))
            // Reading the lambda here redraws only the ring on each tick, not the whole toast.
            val progress = timerProgress?.invoke() ?: 1f
            val direction = if (isRtl) -1f else 1f
            drawArc(
                color = status,
                startAngle = -90f,
                sweepAngle = direction * 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
        }
        if (onClose != null) {
            Icon(
                painter = painterResource(AppIcons.X),
                contentDescription = stringResource(R.string.cd_dismiss),
                tint = colors.textSecondary,
                modifier = Modifier.size(AppDimens.iconXxs),
            )
        }
    }
}
```

Note the shape of `ToastCore`: `appShadow` first, then `background`, the same modifier order as cards (doc 05).

### Code: `AppToastHost.kt`

Material's `SnackbarHost` is **not** used. It queues messages (Figma says a new toast replaces the current one), sits at the bottom (Figma says top), and has fixed durations. A small state holder and host are simpler than fighting it.

```kotlin
package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.dukkanapp.core.theme.AppDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DURATION_MS = 4_000L
private const val DURATION_WITH_ACTION_MS = 6_000L
private const val TIMER_TICK_MS = 50L

@Immutable
data class AppToastData(
    val title: String,
    val type: AppToastType,
    val subtitle: String? = null,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null,
    val showClose: Boolean = true,
    internal val id: Long = 0L,
) {
    /** null = stays until dismissed: Loading, and an Error with an action. */
    internal val durationMs: Long?
        get() = when {
            type == AppToastType.Loading -> null
            type == AppToastType.Error && actionText != null -> null
            actionText != null -> DURATION_WITH_ACTION_MS
            else -> DURATION_MS
        }
}

/** One toast at a time: [show] replaces the current one. */
@Stable
class AppToastState {
    var current by mutableStateOf<AppToastData?>(null)
        private set
    private var lastId = 0L

    fun show(toast: AppToastData) {
        lastId += 1
        current = toast.copy(id = lastId)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberAppToastState(): AppToastState = remember { AppToastState() }

/** Top by default. Use [Bottom] when the keyboard or a sheet is open. */
enum class AppToastPlacement { Top, Bottom }

/** Put it last in a full-screen Box, so it draws above the screen. `AppScaffold` does this (doc 13). */
@Composable
fun AppToastHost(
    state: AppToastState,
    modifier: Modifier = Modifier,
    placement: AppToastPlacement = AppToastPlacement.Top,
) {
    val slidePx = with(LocalDensity.current) { AppDimens.toastSlide.roundToPx() }
    val reducedMotion = animationsDisabled()

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.current,
            contentKey = { it?.id },
            transitionSpec = {
                val enter = fadeIn(tween(240))
                val exit = fadeOut(tween(160))
                if (reducedMotion) {
                    enter togetherWith exit
                } else {
                    (enter + slideInVertically(tween(240, easing = EaseOutBack)) { -slidePx }) togetherWith
                        (exit + scaleOut(tween(160), targetScale = 0.96f))
                }
            },
            modifier = Modifier
                .align(if (placement == AppToastPlacement.Top) Alignment.TopCenter else Alignment.BottomCenter)
                .then(
                    if (placement == AppToastPlacement.Top) {
                        Modifier.statusBarsPadding().padding(top = AppDimens.space8)
                    } else {
                        Modifier.navigationBarsPadding().imePadding()
                            .padding(bottom = AppDimens.navBar + AppDimens.space12)
                    },
                )
                .padding(horizontal = AppDimens.screenPadding),
            label = "toast",
        ) { toast ->
            if (toast != null) ToastWithBehaviour(toast = toast, onDismiss = state::dismiss)
        }
    }
}

@Composable
private fun ToastWithBehaviour(toast: AppToastData, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipeThresholdPx = with(LocalDensity.current) { AppDimens.toastSwipeThreshold.toPx() }
    val swipe = remember(toast.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isPressed by remember(toast.id) { mutableStateOf(false) }
    var progress by remember(toast.id) { mutableFloatStateOf(1f) }

    LaunchedEffect(toast.id) {
        when (toast.type) {
            AppToastType.Success -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            AppToastType.Error -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            else -> Unit
        }
    }

    // A delay loop, not an animation: when the user removes animations an animation would
    // finish instantly and the toast would vanish, while a delay still takes the full time.
    LaunchedEffect(toast.id) {
        val total = toast.durationMs ?: return@LaunchedEffect
        var remaining = total
        snapshotFlow { isPressed }.collectLatest { pressed ->
            if (pressed) return@collectLatest
            while (remaining > 0) {
                delay(TIMER_TICK_MS)
                remaining -= TIMER_TICK_MS
                progress = remaining.toFloat() / total
            }
            onDismiss()
        }
    }

    AppToast(
        title = toast.title,
        type = toast.type,
        subtitle = toast.subtitle,
        actionText = toast.actionText,
        onAction = {
            toast.onAction?.invoke()
            onDismiss()
        },
        onClose = if (toast.showClose) onDismiss else null,
        timerProgress = if (toast.durationMs != null) ({ progress }) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = swipe.value.x
                translationY = swipe.value.y
                val distance = maxOf(abs(swipe.value.x), abs(swipe.value.y))
                alpha = 1f - (distance / (swipeThresholdPx * 2)).coerceIn(0f, 1f)
            }
            // Pause the timer while a finger is down, whatever the gesture turns out to be.
            .pointerInput(toast.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            // Swipe up or sideways to dismiss; downward drags are ignored.
            .pointerInput(toast.id) {
                detectDragGestures(
                    onDragEnd = {
                        val value = swipe.value
                        if (abs(value.x) > swipeThresholdPx || value.y < -swipeThresholdPx) {
                            onDismiss()
                        } else {
                            scope.launch { swipe.animateTo(Offset.Zero) }
                        }
                    },
                    onDragCancel = { scope.launch { swipe.animateTo(Offset.Zero) } },
                ) { change, drag ->
                    change.consume()
                    val next = swipe.value + drag
                    scope.launch { swipe.snapTo(Offset(next.x, minOf(next.y, 0f))) }
                }
            },
    )
}
```

`animationsDisabled()` is the helper from 12.15. Change it there from `private` to `internal`, because both files are in the `feedback` package.

### Using it from MVI

The ViewModel doesn't know about Compose. It sends an effect, and the screen shows the toast:

```kotlin
// The ViewModel's effect
data class ShowToast(
    val title: AppUiText,
    val type: AppToastType,
    val subtitle: AppUiText? = null,
) : CartEffect

// The screen
val toastState = rememberAppToastState()
val context = LocalContext.current

AppCollectEffect(viewModel.effect) { effect ->
    when (effect) {
        is CartEffect.ShowToast -> toastState.show(
            AppToastData(
                title = effect.title.asString(context),
                type = effect.type,
                subtitle = effect.subtitle?.asString(context),
            ),
        )
    }
}

AppScaffold(toastState = toastState) { padding -> /* ... */ }
```

Add this string (`values`, `values-ar`, `values-fr`):

| Key | EN | AR | FR |
|---|---|---|---|
| `cd_dismiss` | Dismiss | إغلاق | Fermer |

### Check

- [ ] Toast is 64 high, 353 wide on a 393 frame (`screenPadding` on both sides), radius 20.
- [ ] Light wash is 9 % fading out at 62 %, dark wash is 16 %.
- [ ] Border is the status colour at 18 % light, 24 % dark.
- [ ] Icon shows all three rings: 44 → 34 → 24, with a 13 glyph. In dark, the halo and ring are stronger (14 % / 22 %).
- [ ] Loading shows a spinner in the ring and has no timer.
- [ ] Toast disappears after 4 s; 6 s with an action; an Error with an action stays.
- [ ] Pressing and holding pauses the ring; releasing continues from the same point.
- [ ] A new toast replaces the current one, and nothing stacks.
- [ ] Swiping up or sideways dismisses; swiping down does nothing.
- [ ] With "Remove animations" on, the toast only fades and still stays the full 4 s.
- [ ] Arabic: icon on the right, close on the left, title in Cairo, timer drains the other way.
- [ ] TalkBack reads the toast without moving focus; Error interrupts.

## 12.12 Message banner (restyle of the existing `AppMessageBanner`)

The banner stays the same idea: one message that isn't tied to a field (doc: auth guide 5.4). Only the colors move to tokens, and an icon is added.

| `kind` | Fill | Border | Icon and text |
|---|---|---|---|
| `Error` | `errorSoft` | `errorBorder` | `error` · `circle-alert` |
| `Success` | `successSoft` | none | `successText` · `circle-check` |
| `Info` | `brandSoft` | none | `brandSoftText` · `info` |

```kotlin
package com.example.dukkanapp.core.common.components.banner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.core.theme.AppTheme

enum class AppMessageKind { Error, Success, Info }

@Composable
fun AppMessageBanner(
    message: String,
    modifier: Modifier = Modifier,
    kind: AppMessageKind = AppMessageKind.Error,
) {
    val c = AppTheme.colors
    val (fill, content, icon) = when (kind) {
        AppMessageKind.Error -> Triple(c.errorSoft, c.error, AppIcons.CircleAlert)
        AppMessageKind.Success -> Triple(c.successSoft, c.successText, AppIcons.CircleCheck)
        AppMessageKind.Info -> Triple(c.brandSoft, c.brandSoftText, AppIcons.Info)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // TalkBack announces the message as soon as it appears.
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(AppDimens.radiusMd),
        color = fill,
        contentColor = content,
        border = if (kind == AppMessageKind.Error) BorderStroke(AppDimens.strokeThin, c.errorBorder) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.space16, vertical = AppDimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
        ) {
            Icon(painterResource(icon), null, modifier = Modifier.size(AppDimens.icon))
            Text(text = message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Migration:** `isError = true` becomes `kind = AppMessageKind.Error`, and `isError = false` becomes `kind = AppMessageKind.Success`.

## 12.13 List row (Account, Settings)

Figma `4.3 · List rows`: rows live **inside a white card** (`card`, `radiusCard`, `shadows.card`), separated by a hairline that starts under the text.

| Part | Spec |
|---|---|
| Group card | `card`, `radiusCard`, `shadows.card`, no inner padding (each row is 58 = 10 + 38 tile + 10) |
| Row | `listRow` (58) min height, horizontal padding `listRowPaddingH` (14), gap `space12` |
| Leading tile | `listTileIcon` (38, Figma `size/list-tile-icon`) rounded square, `radiusMd` (14), `field` fill, icon `icon` (20) `textPrimary` |
| Title / subtitle | `bodyStrong` (13.5 Bold) `textPrimary` / `supporting` (11.5 Medium) `textSecondary` |
| Value (optional) | `helper` (11.5 SemiBold) `brand`, e.g. "2 active" |
| Trailing | `chevron-right` `iconXs` (14) `textTertiary`, or an `AppSwitch` |
| Divider | `strokeHairline`, `borderSubtle`, starts at `listRowPaddingH + listTileIcon + space12` (64) |

```kotlin
package com.example.dukkanapp.core.common.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

/** The white card that holds a group of rows. Put [AppListDivider] between rows. */
@Composable
fun AppListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appShadow(AppTheme.shadows.card, shape)
            .clip(shape)
            .background(AppTheme.colors.card),
        content = content,
    )
}

@Composable
fun AppListDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = AppDimens.listRowPaddingH + AppDimens.listTileIcon + AppDimens.space12),
        thickness = AppDimens.strokeHairline,
        color = AppTheme.colors.borderSubtle,
    )
}

@Composable
fun AppListRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colors
    val textStyles = AppTheme.textStyles
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.listRow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppDimens.listRowPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Box(
            Modifier.size(AppDimens.listTileIcon).background(c.field, RoundedCornerShape(AppDimens.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.textPrimary, modifier = Modifier.size(AppDimens.icon))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = textStyles.bodyStrong, color = c.textPrimary)
            subtitle?.let { Text(it, style = textStyles.supporting, color = c.textSecondary) }
        }
        value?.let { Text(it, style = textStyles.helper, color = c.brand) }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(painterResource(AppIcons.ChevronRight), null, tint = c.textTertiary, modifier = Modifier.size(AppDimens.iconXs))
        }
    }
}
```

Usage (Account):

```kotlin
AppListGroup {
    AppListRow(painterResource(AppIcons.Package), "My orders", value = "2 active", onClick = onOrders)
    AppListDivider()
    AppListRow(
        icon = painterResource(AppIcons.Bell),
        title = "Order updates",
        subtitle = "Packing, on the way",
        modifier = Modifier.toggleable(orderUpdates, role = Role.Switch, onValueChange = onToggle),
        trailing = { AppSwitch(orderUpdates, onCheckedChange = null) },
    )
}
```

For a row with a switch, pass `trailing = { AppSwitch(checked, onCheckedChange = null) }` and make the **row** `toggleable(role = Role.Switch)` instead of `clickable`, so the whole row toggles.

> **Changed from the first version of this doc:** the tile was a 36 dp `field` circle with a 16 dp icon, and the text was `titleSmall` / `bodySmall`. Figma draws a 38 dp rounded square (radius 14) with a 20 dp icon and uses `bodyStrong` / `supporting`.

## 12.14 Empty state (`AppEmptyState`)

Figma board **14 · Empty & loading states**: empty lists for orders (14.1), notifications (14.2) and addresses (14.3). Empty search is the separate "No results" screen (3.2.4). They all share one shape:

| Part | Spec |
|---|---|
| Icon tile | `emptyStateTile` (72) square, radius `radiusCard` (20), `brandSoft` fill, Lucide icon `emptyStateIcon` (32) in `brand` |
| Gap | `space20` |
| Title | `headlineSmall` (20/26 ExtraBold), `textPrimary`, centered |
| Gap | `space8` |
| Body | `bodyMedium`, `textSecondary`, centered, max 2–3 lines |
| Gap | `space24` |
| Action (optional) | `AppButton` Primary (14.1 "Start shopping", 14.3 "Add a new address") or Ghost (14.2 "Notification settings") |

| Screen | Icon | Title | Body | Action |
|---|---|---|---|---|
| 14.1 My orders | `Package` | No orders yet | Your orders will show up here once you check out. | Start shopping (Primary) |
| 14.2 Notifications | `Bell` | You're all caught up | Order updates and deals will appear here. | Notification settings (Ghost) |
| 14.3 Addresses | `MapPin` | No saved addresses | Add home or work so checkout takes one tap. | Add a new address (Primary) |

The tile size (72) and icon size (32) are read off the Figma frames: `AppDimens.emptyStateTile` and `AppDimens.emptyStateIcon`.

```kotlin
package com.example.dukkanapp.core.common.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** The action under an empty state. Null = no button. */
data class AppEmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
    val variant: AppButtonVariant = AppButtonVariant.Primary,
)

@Composable
fun AppEmptyState(
    icon: Painter,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: AppEmptyStateAction? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.emptyStateTile)
                .background(AppTheme.colors.brandSoft, RoundedCornerShape(AppDimens.radiusCard)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppTheme.colors.brand, modifier = Modifier.size(AppDimens.emptyStateIcon))
        }
        Spacer(Modifier.height(AppDimens.space20))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppDimens.space8))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(AppDimens.space24))
            AppButton(text = action.label, onClick = action.onClick, variant = action.variant)
        }
    }
}
```

Usage (My orders, when the list is loaded and empty):

```kotlin
AppEmptyState(
    icon = painterResource(AppIcons.Package),
    title = stringResource(R.string.orders_empty_title),
    body = stringResource(R.string.orders_empty_body),
    action = AppEmptyStateAction(stringResource(R.string.action_start_shopping), onClick = { onIntent(OrdersIntent.StartShopping) }),
    modifier = Modifier.fillMaxSize().wrapContentHeight(),
)
```

Show it only when the data has **loaded** and is empty. While loading, show the skeleton (12.15); on error, show `AppMessageBanner` with Retry. Three different states, three different UIs.

## 12.15 Skeleton loading with shimmer (`AppSkeletonBox`, `Modifier.shimmer`)

Figma 14.4 (Home loading) and 14.5 (Product grid loading): grey placeholder blocks in the shape of the real content, with a **left → right shimmer, 1.2 s loop**.

| Part | Spec |
|---|---|
| Block fill | `field` |
| Shimmer highlight | `card` at 60 % alpha, a soft band moving left → right (right → left in Arabic) |
| Duration | 1 200 ms, linear, infinite |
| Shapes | the shape of what will load: product photo `radiusLg`, card `radiusCard`, text lines `AppPillShape` (12–14 dp tall), chips/pills `AppPillShape` |
| Reduced motion | no movement: blocks stay plain `field` |

```kotlin
package com.example.dukkanapp.core.common.components.feedback

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.theme.AppTheme

private const val SHIMMER_DURATION_MS = 1_200

/** True when the user turned animations off (Settings → Accessibility → Remove animations). */
@Composable
internal fun animationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Paints a soft highlight that sweeps across the content every 1.2 s (Figma 14).
 * Put it AFTER clip + background so the highlight stays inside the shape.
 */
fun Modifier.shimmer(): Modifier = composed {
    if (animationsDisabled()) return@composed this

    val highlight = AppTheme.colors.skeletonHighlight // Figma `Skeleton/Shimmer` middle stop
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    drawWithContent {
        drawContent()
        val band = size.width * 0.6f
        // Travels from fully off one edge to fully off the other.
        val travel = -band + (size.width + band * 2) * progress
        val x = if (isRtl) size.width - travel else travel
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x, 0f),
            ),
        )
    }
}

/** One grey placeholder block. Give it a size and the shape of the content it stands for. */
@Composable
fun AppSkeletonBox(shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppTheme.colors.skeleton)
            .shimmer(),
    )
}
```

Example: one product-card skeleton for the 14.5 grid (2 columns):

```kotlin
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        AppSkeletonBox(RoundedCornerShape(AppDimens.radiusLg), Modifier.fillMaxWidth().aspectRatio(1f))
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.4f).height(AppDimens.space12)) // brand
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.8f).height(AppDimens.space16)) // name
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.5f).height(AppDimens.space16)) // price
    }
}
```

The 14.4 Home skeleton is built the same way in the Home feature: a 52 dp pill (search), a row of 6 category circles, a 160 dp `radiusCard` banner, then two `ProductCardSkeleton`s.

> `composed { }` is used because the modifier reads composition locals and runs an animation. For a learning project it's the clearest form. If you later want maximum performance, the same logic can move to a `Modifier.Node`.
AppSkeletonBox: a grey placeholder that shimmers
The code is in doc 12, section 12.15. It's only in the docs so far. No .kt file uses it yet.

What it is
A skeleton is a grey block shown while data loads. It takes the shape of the real content that will appear. A light band sweeps across it to show that something is happening.


Loading...                       Loaded
┌──────────────┐                 ┌──────────────┐
│ ░░░░░░░░░░░░ │  ← photo        │  [ product   │
│ ░░░░░░░░░░░░ │                 │    photo ]   │
└──────────────┘                 └──────────────┘
 ▒▒▒▒▒▒          ← brand          Nike
 ▒▒▒▒▒▒▒▒▒▒▒▒    ← name           Air Max 90
 ▒▒▒▒▒▒▒         ← price          $120
The code is two pieces

@Composable
fun AppSkeletonBox(shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(shape)                          // 2. cut to the shape
            .background(AppTheme.colors.skeleton) // 3. paint it grey
            .shimmer(),                           // 4. add the moving light
    )
}
AppSkeletonBox is an empty Box with three modifiers, applied in this order:

modifier: size and position from the caller (fillMaxWidth(), height(...)).
.clip(shape): cuts the box into a rounded rectangle, a pill or a circle.
.background(...): fills it with the grey skeleton color. Because clip came first, the grey stays inside the shape.
.shimmer(): draws the moving highlight on top.
Order matters here. The doc says "Put shimmer AFTER clip + background so the highlight stays inside the shape". If shimmer came before clip, the light would spill outside the rounded corners.

The parameters
Parameter	Type	Role
shape	Shape	The outline of the content being imitated. Photo: RoundedCornerShape(AppDimens.radiusLg). Text line: AppPillShape. Avatar: CircleShape.
modifier	Modifier = Modifier	The size. A box with no size has nothing to draw, so you always pass one: Modifier.fillMaxWidth().height(16.dp).
shape has no default because every placeholder needs a deliberate shape. modifier has the default Modifier, which is the usual Compose convention.

How .shimmer() works
It is a custom Modifier extension, in this order:


fun Modifier.shimmer(): Modifier = composed {
    if (animationsDisabled()) return@composed this   // (a)
    val highlight = AppTheme.colors.skeletonHighlight // (b)
    val isRtl = ...                                   // (c)
    val progress by rememberInfiniteTransition()...   // (d)
    drawWithContent { ... }                           // (e)
}
(a) Reduced motion: if the user turned animations off in Android settings, return the modifier unchanged. The block stays plain grey with no movement.
(b) Highlight color: taken from the theme, so it works in light and dark.
(c) RTL: in Arabic the sweep goes right → left.
(d) The clock: rememberInfiniteTransition + animateFloat makes a number progress that goes 0 → 1 in 1.2 s, then restarts, forever (RepeatMode.Restart, LinearEasing for a constant speed).
(e) The drawing: drawWithContent runs on every frame:

drawContent()                       // draw the grey box first
val band   = size.width * 0.6f      // the light band is 60% of the width
val travel = -band + (size.width + band * 2) * progress
travel moves the band from fully off the left edge (progress = 0) to fully off the right edge (progress = 1).


progress 0.0   [band]░░░░░░░░░░░░░░░░░░     (off-screen, left)
progress 0.3   ░░░░[ band ]░░░░░░░░░░░░
progress 0.6   ░░░░░░░░░░░░[ band ]░░░░
progress 1.0   ░░░░░░░░░░░░░░░░░░[band]     (off-screen, right)
Then Brush.linearGradient(Transparent → highlight → Transparent) draws the band. Both ends are transparent, so the edges fade out softly.

composed { } is used because the modifier needs composition state (AppTheme, remember, an animation). A plain modifier chain can't do that.

Example: how we use it

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        AppSkeletonBox(RoundedCornerShape(AppDimens.radiusLg),
            Modifier.fillMaxWidth().aspectRatio(1f))                    // photo: square
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.4f).height(AppDimens.space12)) // brand: short
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.8f).height(AppDimens.space16)) // name: long
        AppSkeletonBox(AppPillShape, Modifier.fillMaxWidth(0.5f).height(AppDimens.space16)) // price: medium
    }
}
The widths 0.4, 0.8 and 0.5 make the lines uneven like real text, so the screen looks natural while loading.

Then in a screen, you pick the UI from the state:


when (state) {
    is Loading -> LazyVerticalGrid(...) { items(6) { ProductCardSkeleton() } }
    is Error   -> AppMessageBanner(... Retry ...)
    is Success -> ProductGrid(state.products)
}
The whole flow

Screen state = Loading
      ↓
ProductCardSkeleton → 4 × AppSkeletonBox(shape, size)
      ↓
each box: clip(shape) → background(grey) → shimmer()
      ↓
shimmer: progress goes 0→1 every 1.2 s
      ↓
drawWithContent: grey box + a light band moving across it
      ↓
data arrives → state = Success → skeleton is replaced by the real content
---

## New content-description strings

| Key | EN | AR | FR |
|---|---|---|---|
| `cd_add_to_cart` | Add to cart | أضف إلى السلة | Ajouter au panier |
| `cd_increase` | Increase quantity | زيادة الكمية | Augmenter la quantité |
| `cd_decrease` | Decrease quantity | إنقاص الكمية | Diminuer la quantité |
| `cd_remove_from_cart` | Remove from cart | إزالة من السلة | Retirer du panier |
| `toast_item_removed` | Item removed from cart | تمت إزالة المنتج من السلة | Article retiré du panier |

## Empty-state strings (Figma 14)

| Key | EN | AR | FR |
|---|---|---|---|
| `orders_empty_title` | No orders yet | لا توجد طلبات بعد | Aucune commande pour l'instant |
| `orders_empty_body` | Your orders will show up here once you check out. | ستظهر طلباتك هنا بعد إتمام الشراء. | Vos commandes apparaîtront ici après votre premier achat. |
| `action_start_shopping` | Start shopping | ابدأ التسوق | Commencer vos achats |
| `notifications_empty_title` | You're all caught up | لا جديد حاليًا | Vous êtes à jour |
| `notifications_empty_body` | Order updates and deals will appear here. | ستظهر هنا تحديثات الطلبات والعروض. | Les mises à jour et les offres apparaîtront ici. |
| `action_notification_settings` | Notification settings | إعدادات الإشعارات | Paramètres des notifications |
| `addresses_empty_title` | No saved addresses | لا توجد عناوين محفوظة | Aucune adresse enregistrée |
| `addresses_empty_body` | Add home or work so checkout takes one tap. | أضف عنوان المنزل أو العمل ليصبح الدفع بلمسة واحدة. | Ajoutez votre domicile ou travail pour payer en un geste. |
| `action_add_address` | Add a new address | إضافة عنوان جديد | Ajouter une adresse |

---

## Notes and rules

- **No raw sizes in components.** Even a size used by one component only (the 12 dp badge icon, the 22 dp radio, the 24 dp stepper slot, the 30 dp bubble) lives in `AppDimens` under "Component internals" (doc 07), named `<component><Part>`. A composable never writes `N.dp`.
- **Checkbox, radio and switch never get their own click handler** when they sit in a labeled row. The row owns the click and the semantics.
- **Arrows and chevrons** (`chevron-right`, `arrow-right`, `arrow-left`) must have `android:autoMirrored="true"` in their vector XML so they flip in Arabic.
- **Money** is a `Long` in millimes all the way from Firestore to `AppPriceText`. Format only at the edge.
- **Always 3 decimals**, even in the bottom-nav cart total: Figma now writes "39.900 TND" there too (it used to say "39.1 TND"). Use `formatTnd` everywhere; never shorten.
- **Ratings** use `AppIcons.StarFilled` tinted `colors.gold`.

---

## Compose works in 3 steps, every frame

| Phase              | What it does                                            | Analogy                     |
| ------------------ | ------------------------------------------------------- | --------------------------- |
| 1. **Composition** | Runs your `@Composable` code and decides *what* to show | Writing the plan            |
| 2. **Layout**      | Measures sizes and decides *where* each thing goes      | Placing furniture in a room |
| 3. **Draw**        | Paints the pixels                                       | Painting the picture        |

When an animated value changes, Compose redoes the phase that **reads** that value, and every phase after it.

## What "skip" means

If the value is only read in a later phase, the earlier phases are skipped. That means less work per frame.

### 1. Plain version (what the doc uses)



```kotlin
val thumbOffset by animateDpAsState(...)
Modifier.offset(x = thumbOffset)
```

`thumbOffset` is read directly inside the composable. So on **every animation frame** (about 60 times per second), Compose does composition, then layout, then draw. This is the most work.

### 2. Lambda version



```kotlin
Modifier.offset { IntOffset(thumbOffset.roundToPx(), 0) }
```

The value is read inside the lambda, which runs during **layout**. So composition is **skipped**. Only layout and draw run.

### 3. `graphicsLayer` version



```kotlin
Modifier.graphicsLayer { translationX = thumbOffset.toPx() }
```

The value is read during **draw**. Composition and layout are both **skipped**. This is the cheapest option.

## Picture it



```
Plain offset(x = value):   Composition → Layout → Draw   (all 3, each frame)
offset { lambda }:                       Layout → Draw   (skips composition)
graphicsLayer { }:                                Draw   (skips composition + layout)
```

## Is it worth it here?

For a tiny switch that animates for about 200 ms, no. The extra work is too small to notice, so the doc keeps the simple version.

It matters when many items animate at once, or the animation runs constantly. An example is the shimmer in 12.15, which loops forever. That is why the shimmer reads its progress inside `drawWithContent`, in the draw phase.

## What I got wrong earlier

I said the lambda version "moves at draw time". That is not exact: the lambda `offset` works at **layout** time, and `graphicsLayer` is the one that works at **draw** time. I also said `offset` doesn't change the layout around it. That part is right: it moves the item without changing the parent's size or pushing its neighbors.

## The one rule

**The phase is decided by where you read the state value.**

| Where you read the value                                     | Phase       |
| ------------------------------------------------------------ | ----------- |
| Directly in the composable body                              | Composition |
| Inside a layout lambda (`offset { }`, `layout { }`)          | Layout      |
| Inside a draw lambda (`drawBehind { }`, `graphicsLayer { }`, `Canvas { }`) | Draw        |

Compose tracks which phase reads the value. When the value changes, it reruns only that phase and the ones after it.

## Example: a box that moves when you tap it



```kotlin
@Composable
fun MovingBox() {
    var moved by remember { mutableStateOf(false) }             // (A) state, nothing read yet
    val x by animateDpAsState(if (moved) 100.dp else 0.dp)      // (B) READ in COMPOSITION

    Box(
        modifier = Modifier
            .clickable { moved = !moved }                       // (C) runs on tap, not a phase
            .offset(x = x)                                      // (D) uses x from (B)
            .size(50.dp)                                        // (E) layout: size is measured
            .background(Color.Blue)                             // (F) draw: paints blue
    )
}
```

### Which line belongs to which phase

| Line                           | Phase           | Why                                                          |
| ------------------------------ | --------------- | ------------------------------------------------------------ |
| `var moved by remember {…}`    | Composition     | Runs the composable code                                     |
| `val x by animateDpAsState(…)` | **Composition** | The `by x` delegate reads the value here, in the body        |
| `.clickable { … }`             | none (event)    | The lambda runs only on a tap                                |
| `.offset(x = x)`               | Layout          | `offset` places the box during layout, but it was *given* a value read in composition |
| `.size(50.dp)`                 | Layout          | Measuring size is a layout job                               |
| `.background(Color.Blue)`      | Draw            | Painting is a draw job                                       |

The key: on line `(B)` the value `x` is **read in the composable body**. So every animation frame, Compose reruns **composition, then layout, then draw**. This is the slowest option.

## Same example, reading later

### Version 2: read in layout



```kotlin
@Composable
fun MovingBox2() {
    var moved by remember { mutableStateOf(false) }
    val x = animateDpAsState(if (moved) 100.dp else 0.dp)       // no "by": we hold the State, not the value. Nothing is read yet.

    Box(
        Modifier
            .offset { IntOffset(x.value.roundToPx(), 0) }       // READ in LAYOUT
            .size(50.dp)
            .background(Color.Blue)
    )
}
```

- Without `by`, `x` is a `State` object. Nothing has been read at that line.
- `x.value` is read **inside the `offset { }` lambda**, which runs in the layout phase.
- Each frame: **layout → draw**. Composition is skipped.

### Version 3: read in draw



```kotlin
@Composable
fun MovingBox3() {
    var moved by remember { mutableStateOf(false) }
    val x = animateDpAsState(if (moved) 100.dp else 0.dp)

    Box(
        Modifier
            .size(50.dp)
            .graphicsLayer { translationX = x.value.toPx() }    // READ in DRAW
            .background(Color.Blue)
    )
}
```

- `x.value` is read **inside `graphicsLayer { }`**, which runs in the draw phase.
- Each frame: **draw only**. Composition and layout are skipped.

## Summary

| Version                         | Where the value is read | Work per frame              |
| ------------------------------- | ----------------------- | --------------------------- |
| 1. `offset(x = x)` with `by`    | composable body         | composition + layout + draw |
| 2. `offset { x.value… }`        | layout lambda           | layout + draw               |
| 3. `graphicsLayer { x.value… }` | draw lambda             | draw only                   |

## How to spot it in any code

1. Find the state value (`by` or `.value`).
2. Ask: **am I reading it in the body, or inside a `{ }` lambda?**
3. If it is in a lambda, ask what kind of lambda it is: layout (`offset {}`, `layout {}`) or draw (`graphicsLayer {}`, `drawBehind {}`, `drawWithContent {}`, `Canvas {}`).

That is the whole trick. In the shimmer from 12.15, the progress is read inside `drawWithContent { … }`, so it is a **draw-only** animation, which is right for something that loops forever.



## Check

- [ ] Login "Keep me logged in": tapping the text toggles the box, and TalkBack says "Checkbox, checked".
- [ ] Switch animates, and in Arabic the "on" thumb is on the left.
- [ ] `formatTnd(279000) == "279.000"`, `formatTnd(39900) == "39.900"`, `formatTnd(1500) == "1.500"`.
- [ ] Product card: "+" turns into the blue stepper after the first tap and goes back to "+" at 0.
- [ ] Error banner on login shows a red-soft box with a red border and an alert icon, and TalkBack announces it.
- [ ] Pager indicator on onboarding: the active dot stretches to a 22 dp pill.
- [ ] Empty My orders shows the blue tile, "No orders yet" and a "Start shopping" button, centered.
- [ ] Skeleton grid: a soft highlight sweeps left → right every 1.2 s (right → left in Arabic). With "Remove animations" on, the blocks stay still.
