# 12: Selection controls, badges, prices and feedback

This doc covers the small atoms and molecules from Figma pages **4.1 · Atoms**, **4.3 · Molecules** and the customer board **14 · Empty & loading states**. Each section has the spec table, then the code. Type them in this order: the auth screens (doc 14) only need **12.1 Checkbox**, **12.9 Progress bar** and **12.12 Message banner**. The rest you can type when the Home, Cart and Orders features start (**12.14 Empty state** and **12.15 Skeleton** come with the first list screen).

**Folders (new):**

```
core/designsystem/components/
├── selection/   AppCheckbox.kt · AppRadio.kt · AppSwitch.kt · AppChip.kt
├── display/     AppBadge.kt · AppPriceText.kt · AppQuantity.kt · AppAvatar.kt · AppRating.kt
│                AppPagerIndicator.kt · AppProgressBar.kt · AppStatusSteps.kt
├── feedback/    AppToast.kt · AppToastHost.kt · AppMessageBanner.kt · AppEmptyState.kt · AppSkeleton.kt
└── layout/      AppListRow.kt
```

**Replaces:**

- `core/common/components/checkbox/AppCheckBox.kt`
- `core/common/components/pager/AppPagerIndicator.kt`
- `core/common/components/banner/AppMessageBanner.kt`
- `core/common/components/dividers/AppDivider.kt` (use `HorizontalDivider(color = AppTheme.colors.border)` or `AppOrDivider` from doc 13)

---

## Why

- **One rule for every control: the whole row is the touch target.** Figma draws a 20 dp checkbox, but the user taps the label too. We put `toggleable` / `selectable` on the **row** with the right `Role`, so TalkBack says "Checkbox, Keep me logged in, checked", and the box itself is only a drawing.
- **Material controls don't match the sizes.** The M3 `Switch` is fixed at 52 × 32, and Figma's is 46 × 28. The M3 `Checkbox` draws a 2 dp outline at 18 dp. The custom drawings are ten lines each, and they read only tokens.
- **Variant enums again** (badges, toasts): the screen says *what* (`LowStock`), the component decides *how*.

---

## 12.1 Checkbox

| State | Box (20 dp, radius 6) | Mark |
|---|---|---|
| Off | `card` fill, `borderStrong` 1.5 dp | none |
| On | `brand` fill, no border | white `check` icon 14 dp |
| Disabled | 50 % alpha | as above |

The label is 12.5 sp SemiBold in `textLabel`, with a gap of `space8`. The row is at least 48 dp tall.

```kotlin
package com.example.dukkanapp.core.designsystem.components.selection

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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.theme.AppTheme

private val CheckboxShape = RoundedCornerShape(percent = 30)

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
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.icon)
                .background(fill, CheckboxShape)
                .then(
                    if (checked) Modifier
                    else Modifier.border(AppDimens.strokeFocus, colors.borderStrong, CheckboxShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(AppIcons.Check),
                    contentDescription = null,
                    tint = colors.onBrand,
                    modifier = Modifier.size(14.dp),
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

> The `AnnotatedString` overload exists for the sign-up "I agree to the **Terms** and **Privacy Policy**" row, where the two words are links (see doc 14). Tapping a link opens the page, and tapping anywhere else toggles the box.

## 12.2 Radio (used by "Selectable option": payment, language)

| State | Circle | Inner dot |
|---|---|---|
| Off | 20 dp, `card` fill, `borderStrong` 1.5 dp | none |
| On | 22 dp, `brand` fill | 8 dp `onBrand` |

```kotlin
package com.example.dukkanapp.core.designsystem.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

/** Drawing only. Put `Modifier.selectable(selected, role = Role.RadioButton)` on the parent row. */
@Composable
fun AppRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                modifier = Modifier.size(22.dp).background(colors.brand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(AppDimens.space8).background(colors.onBrand, CircleShape))
            }
        } else {
            Box(
                Modifier
                    .size(AppDimens.icon)
                    .background(colors.card, CircleShape)
                    .border(AppDimens.strokeFocus, colors.borderStrong, CircleShape)
            )
        }
    }
}
```

**Selectable option card** (Figma `Option/Cash on delivery (selected)`): a `card` box with **`AppPillShape`** (Figma radius table: "option → full") and a `border` 1.2 dp; when selected, `brandSoft` fill with a `brand` 1.5 dp border. The layout is a leading 36 dp `AppIconButton(Soft)`-style tile, then the title (`titleSmall`) and subtitle (`bodySmall`, `textSecondary`), then `AppRadio` at the end. Build it in the feature (Language, Checkout) using `Modifier.selectable(...)` on the card.

## 12.3 Switch

46 × 28, pill shaped. On = `brand` track, off = `border` track. The thumb is 22 dp `card` with `shadows.card`, and it slides with an animation.

```kotlin
package com.example.dukkanapp.core.designsystem.components.selection

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
import androidx.compose.ui.unit.dp
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
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "switchThumb")

    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier // the parent row is toggleable (list row with a switch)
    }

    Box(
        modifier = modifier
            .then(toggle)
            .size(width = 46.dp, height = 28.dp)
            .background(track, AppPillShape)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .appShadow(AppTheme.shadows.card, CircleShape)
                .background(colors.card, CircleShape)
        )
    }
}
```

> `offset(x = …)` follows the layout direction, so in Arabic the "on" thumb sits on the left, which is what RTL users expect.

## 12.4 Chip

Height 34, pill shaped, padding `space12`, label `labelMedium` (12.5 Bold).

| State | Fill | Border | Label |
|---|---|---|---|
| Default | `card` | `border` 1.2 dp | `textLabel` |
| Selected | `brandSoft` | `brand` 1.2 dp | `brandSoftText` |

```kotlin
package com.example.dukkanapp.core.designsystem.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
) {
    val colors = AppTheme.colors
    val fill = if (selected) colors.brandSoft else colors.card
    val stroke = if (selected) colors.brand else colors.border
    val content = if (selected) colors.brandSoftText else colors.textLabel

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(AppPillShape)
            .background(fill)
            .border(AppDimens.strokeThin, stroke, AppPillShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4),
    ) {
        leadingIcon?.let { Icon(it, null, tint = content, modifier = Modifier.size(AppDimens.iconSm)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
```

## 12.5 Badge

Height 20, pill shaped, horizontal padding `space8`, text 10.5 sp Bold.

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

```kotlin
package com.example.dukkanapp.core.designsystem.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class AppBadgeVariant { Discount, Hot, New, Eta, Bestseller, LowStock, FreeDelivery, Rating }

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
        AppBadgeVariant.Eta, AppBadgeVariant.Bestseller -> SolidColor(c.brandSoft) to c.brandSoftText
        AppBadgeVariant.LowStock, AppBadgeVariant.Rating -> SolidColor(c.warningSoft) to c.warningText
        AppBadgeVariant.FreeDelivery -> SolidColor(c.successSoft) to c.successText
    }
    val style = MaterialTheme.typography.labelSmall // Figma "Badge/*": M3/labelSmall 10.5/14 Bold

    Row(
        modifier = modifier
            .height(20.dp)
            .background(fill, AppPillShape)
            .padding(horizontal = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space2),
    ) {
        leadingIcon?.let { Icon(it, null, tint = content, modifier = Modifier.size(12.dp)) }
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
package com.example.dukkanapp.core.designsystem.components.display

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
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
```

## 12.7 Quantity: add button, stepper, count bubble

| Part | Spec |
|---|---|
| Add button (not in cart) | 32 dp circle, `card` fill, `shadows.card`, `plus` icon in `brand` |
| Add button (in cart) | 32 dp circle, `brand` fill, white `plus` |
| Qty stepper (card) | 88 × 32 pill, `brand` fill, `minus` · count (`titleSmall` 14 ExtraBold, `onBrand`) · `plus` |
| Cart stepper (cart line) | 112 × 52 pill, `field` fill, count `titleMedium` 16 ExtraBold `textPrimary`, icons `textPrimary` |
| Count bubble (on cart) | 30 dp circle, `brand` fill, 2.5 dp `card` border, `labelStrong` 12.5 ExtraBold `onBrand` |

The rule from Figma is: the **Add (+) button turns into the stepper when the item is in the cart.**

```kotlin
package com.example.dukkanapp.core.designsystem.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.components.button.AppIconButton
import com.example.dukkanapp.core.designsystem.components.button.AppIconButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class AppStepperStyle { Compact, Large }

/** Shows "+" when quantity is 0, the stepper otherwise. */
@Composable
fun AppQuantityControl(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppStepperStyle = AppStepperStyle.Compact,
) {
    if (quantity == 0) {
        AppIconButton(
            icon = painterResource(AppIcons.Plus),
            contentDescription = stringResource(R.string.cd_add_to_cart),
            onClick = onIncrease,
            variant = AppIconButtonVariant.White,
            size = 32.dp,
            modifier = modifier,
        )
        return
    }
    val c = AppTheme.colors
    val isLarge = style == AppStepperStyle.Large
    val fill = if (isLarge) c.field else c.brand
    val content = if (isLarge) c.textPrimary else c.onBrand
    val countStyle = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall

    Row(
        modifier = modifier
            .size(width = if (isLarge) 112.dp else 88.dp, height = if (isLarge) AppDimens.controlLg else 32.dp)
            .background(fill, AppPillShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onDecrease) {
            Icon(painterResource(AppIcons.Minus), stringResource(R.string.cd_decrease), tint = content, modifier = Modifier.size(AppDimens.iconSm))
        }
        Text(quantity.toString(), style = countStyle, color = content)
        IconButton(onClick = onIncrease) {
            Icon(painterResource(AppIcons.Plus), stringResource(R.string.cd_increase), tint = content, modifier = Modifier.size(AppDimens.iconSm))
        }
    }
}

@Composable
fun AppCountBubble(count: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(30.dp)
            .background(c.brand, CircleShape)
            .border(2.5.dp, c.card, CircleShape),
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

> `IconButton` keeps the 48 dp touch target, but inside a 32 dp-tall pill it overlaps the edges. That is intended: the user can tap a little outside the pill.

## 12.8 Avatar and rating

| Part | Spec |
|---|---|
| Avatar | 50 / 36 / 28 dp circle. Photo, or initials on `brandRing` fill with `brandSoftText` text (Bold) |
| Rating | `star` icon in `gold` (filled), then "4.8 (1.2k)" in `caption`, `textLabel` |

```kotlin
package com.example.dukkanapp.core.designsystem.components.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AppTheme.colors.brandRing),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(initials.take(2).uppercase(), style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.brandSoftText)
        }
    }
}

@Composable
fun AppRating(value: String, count: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space2),
    ) {
        Icon(painterResource(AppIcons.StarFilled), null, tint = AppTheme.colors.gold, modifier = Modifier.size(12.dp))
        Text(
            text = if (count != null) "$value ($count)" else value,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textLabel,
        )
    }
}
```

> Avatars and product photos come from Firebase Storage URLs. When you start Home, add **Coil 3** (`io.coil-kt.coil3:coil-compose` and `coil-network-okhttp`) and pass `rememberAsyncImagePainter(url)` as `photo`. Ratings use `AppIcons.StarFilled` tinted `colors.gold` (Figma `2.1 · Icons`: "ratings use the filled star in accent/gold"; doc 09).

## 12.9 Pager indicator and progress bar

| Part | Spec |
|---|---|
| Pager dot (inactive) | 6 × 6, `border` |
| Pager dot (active) | 22 × 6 pill, `brand`. The width animates |
| Progress bar (sign-up "1/2") | 4 dp track, `border`, with a `brand` fill and animated progress |

```kotlin
package com.example.dukkanapp.core.designsystem.components.display

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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppPagerIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 22.dp else 6.dp, label = "dotWidth")
            Box(
                Modifier
                    .size(width = width, height = 6.dp)
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
            .height(4.dp)
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

Figma "Status steps" are the order timeline: **Placed, Packing, On the way, At door, Done**.

- A done step is a 20 dp `brand` circle with a white check.
- The current step is a `brand` pill with an icon and the label (11.5 ExtraBold `onBrand`).
- An upcoming step is a 20 dp `brandRing` circle with its icon in `brand`.
- Connectors are 2 dp lines: `brand` if done, `border` if not.

The values map 1:1 to `orders/{id}.status` (see the Firebase docs).

```kotlin
package com.example.dukkanapp.core.designsystem.components.display

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

data class AppStep(val label: String, val icon: Int)

@Composable
fun AppStatusSteps(steps: List<AppStep>, currentIndex: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, step ->
            when {
                index < currentIndex -> Box(
                    Modifier.size(AppDimens.icon).background(c.brand, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(AppIcons.Check), null, tint = c.onBrand, modifier = Modifier.size(12.dp)) }

                index == currentIndex -> Row(
                    Modifier.height(AppDimens.icon + AppDimens.space4).background(c.brand, AppPillShape)
                        .padding(horizontal = AppDimens.space8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.space4),
                ) {
                    Icon(painterResource(step.icon), null, tint = c.onBrand, modifier = Modifier.size(12.dp))
                    Text(step.label, style = AppTheme.textStyles.labelStrong, color = c.onBrand)
                }

                else -> Box(
                    Modifier.size(AppDimens.icon).background(c.brandRing, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(step.icon), null, tint = c.brand, modifier = Modifier.size(12.dp)) }
            }
            if (index < steps.lastIndex) {
                Box(
                    Modifier.weight(1f).height(2.dp).padding(horizontal = AppDimens.space2)
                        .background(if (index < currentIndex) c.brand else c.border)
                )
            }
        }
    }
}
```

## 12.11 Toast (Figma `4.4 · Toast`, the "aura toast")

Floating feedback that confirms, warns or offers one quick action, then gets out of the way. It **replaces the old accent-bar toast everywhere**. Figma has 2 component sets (`Toast` and `Toast · RTL`), each with 10 variants: Type (`Success`, `Error`, `Warning`, `Info`, `Loading`) × Theme (`Light`, `Dark`). Component properties: `Title`, `Subtitle`, `Action` (text) and `Show subtitle`, `Show action`, `Show close`, `Show timer` (boolean).

### Figma spec: anatomy and tokens

| Part | Figma value | Our code |
|---|---|---|
| Container | 353 wide (screen − 2 × 20), **64 high**, `radius/card` 20, padding 10 top/bottom/icon side, 12 on the close side, gap `space/12` | `AppDimens.toast`, `radiusCard`, `space12` |
| Surface | `surface/card` + status wash: linear gradient, status colour **9 % → 0 at 62 %** (**16 %** in dark) | `card` + `shadows.toast.washAlpha` |
| Border | 1 px, status colour at **18 %** (**24 %** in dark), inner alignment | `shadows.toast.borderAlpha` |
| Shadow | `Elevation/Toast`, `Dark/Elevation/Toast` (3 layers, doc 05) | `shadows.toast.layers`, `topHighlight` |
| Halo icon | 44 halo (status 10 %; **14 % in dark**) → 34 ring (status 16 %; **22 % in dark**) → 24 core (status, drop shadow 0 3 8 status 35 %, inner shadow 0 1 0 white 25 %) → 13 glyph, stroke 1.73 | `ToastHalo`, `ToastCore` |
| Glyph colour | `on/primary` (`surface/card` in dark) | `shadows.toast.onStatus` |
| Loading | 20 spinner in the ring, stroke 2.5, track `border/default`, arc status | `CircularProgressIndicator` |
| Title | `M3/titleSmall` 14/20 ExtraBold, `text/primary`, 1 line (RTL: `Arabic/labelLarge`) | `titleSmall` / `labelLarge` |
| Subtitle | `M3/bodySmall` 13/18 Medium, `text/secondary`, max 2 lines (RTL: `Arabic/labelMedium`) | `bodySmall` / `labelMedium` |
| Action | solid pill, `radius/full`, 32 high, padding 8 / 14, status colour fill, `M3/labelMedium` `on/primary`, shadow 0 4 10 −2 status 30 % | `ToastAction` |
| Close + timer | 28 ring, stroke 2: track `text/tertiary` 25 % + status arc that drains over the duration, × 12 (stroke 1.3) `text/secondary`, 44 touch target | `ToastClose` |
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
package com.example.dukkanapp.core.designsystem.components.feedback

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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.designsystem.icon.AppIcons
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
            .border(1.dp, status.copy(alpha = tokens.borderAlpha), shape)
            .then(
                tokens.topHighlight?.let { highlight ->
                    Modifier.innerShadow(
                        shape = shape,
                        shadow = Shadow(
                            radius = highlight.blur,
                            color = highlight.color,
                            offset = DpOffset(0.dp, highlight.offsetY),
                        ),
                    )
                } ?: Modifier,
            )
            // Padding is 10 on the icon side and 12 on the close side; start/end mirror in RTL.
            .padding(PaddingValues(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)),
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
            .size(44.dp)
            .clip(CircleShape)
            .background(status.copy(alpha = tokens.haloAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(status.copy(alpha = tokens.ringAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            if (type == AppToastType.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = status,
                    trackColor = AppTheme.colors.border,
                    strokeWidth = 2.5.dp,
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
            .size(24.dp)
            .appShadow(AppShadow(status.copy(alpha = 0.35f), offsetY = 3.dp, blur = 8.dp), CircleShape)
            .background(status, CircleShape)
            .innerShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 0.dp, color = Color.White.copy(alpha = 0.25f), offset = DpOffset(0.dp, 1.dp)),
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
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
}

@Composable
private fun ToastGlyphText(text: String, color: Color) {
    Text(text = text, style = AppTheme.textStyles.labelStrong, color = color)
}

@Composable
private fun ToastAction(text: String, status: Color, onStatus: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .appShadow(
                AppShadow(status.copy(alpha = 0.30f), offsetY = 4.dp, blur = 10.dp, spread = (-2).dp),
                AppPillShape,
            )
            .clip(AppPillShape)
            .background(status)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
        Canvas(Modifier.size(28.dp)) {
            val strokePx = 2.dp.toPx()
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
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
```

Note the shape of `ToastCore`: `appShadow` first, then `background`, the same modifier order as cards (doc 05).

### Code: `AppToastHost.kt`

Material's `SnackbarHost` is **not** used. It queues messages (Figma says a new toast replaces the current one), sits at the bottom (Figma says top), and has fixed durations. A small state holder and host are simpler than fighting it.

```kotlin
package com.example.dukkanapp.core.designsystem.components.feedback

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
import androidx.compose.ui.unit.dp
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
    val slidePx = with(LocalDensity.current) { 16.dp.roundToPx() }
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
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
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
package com.example.dukkanapp.core.designsystem.components.feedback

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
import com.example.dukkanapp.core.designsystem.icon.AppIcons
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

The row is 56 dp minimum with horizontal padding `space16`. It has a leading 36 dp `field` circle with an icon in `textPrimary`, then the title (`titleSmall`) and optional subtitle (`bodySmall`, `textSecondary`). At the end there is a value (`labelMedium`, `brand`) and a `chevron-right` (`textTertiary`), or an `AppSwitch`.

```kotlin
package com.example.dukkanapp.core.designsystem.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.designsystem.icon.AppIcons
import com.example.dukkanapp.core.theme.AppTheme

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppDimens.space16, vertical = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Box(Modifier.size(AppDimens.controlSm).background(c.field, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.textPrimary, modifier = Modifier.size(AppDimens.iconSm))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
        }
        value?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = c.brand) }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(painterResource(AppIcons.ChevronRight), null, tint = c.textTertiary, modifier = Modifier.size(AppDimens.iconSm))
        }
    }
}
```

For a row with a switch, pass `trailing = { AppSwitch(checked, onCheckedChange = null) }` and make the **row** `toggleable(role = Role.Switch)` instead of `clickable`, so the whole row toggles.

## 12.14 Empty state (`AppEmptyState`)

Figma board **14 · Empty & loading states**: empty lists for orders (14.1), notifications (14.2) and addresses (14.3). Empty search is the separate "No results" screen (3.2.4). They all share one shape:

| Part | Spec |
|---|---|
| Icon tile | 72 dp square, radius `radiusCard` (20), `brandSoft` fill, Lucide icon 32 dp in `brand` |
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

The tile size (72) and icon size (32) are read off the Figma frames; they belong to this component only.

```kotlin
package com.example.dukkanapp.core.designsystem.components.feedback

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
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.core.designsystem.components.button.AppButton
import com.example.dukkanapp.core.designsystem.components.button.AppButtonVariant
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
                .size(72.dp)
                .background(AppTheme.colors.brandSoft, RoundedCornerShape(AppDimens.radiusCard)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppTheme.colors.brand, modifier = Modifier.size(32.dp))
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
package com.example.dukkanapp.core.designsystem.components.feedback

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

---

## New content-description strings

| Key | EN | AR | FR |
|---|---|---|---|
| `cd_add_to_cart` | Add to cart | أضف إلى السلة | Ajouter au panier |
| `cd_increase` | Increase quantity | زيادة الكمية | Augmenter la quantité |
| `cd_decrease` | Decrease quantity | إنقاص الكمية | Diminuer la quantité |

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

- **Small raw sizes** (`12.dp` icons in badges and steps, `14.dp` check, `22.dp` radio, `46×28` switch, `30.dp` bubble, `32.dp` stepper) are the exact Figma sizes of *one* component, so each stays in its file. If a size appears in a second component, promote it to `AppDimens`.
- **Checkbox, radio and switch never get their own click handler** when they sit in a labeled row. The row owns the click and the semantics.
- **Arrows and chevrons** (`chevron-right`, `arrow-right`, `arrow-left`) must have `android:autoMirrored="true"` in their vector XML so they flip in Arabic.
- **Money** is a `Long` in millimes all the way from Firestore to `AppPriceText`. Format only at the edge.
- **Always 3 decimals**, even in the bottom-nav cart total: Figma now writes "39.900 TND" there too (it used to say "39.1 TND"). Use `formatTnd` everywhere; never shorten.
- **Ratings** use `AppIcons.StarFilled` tinted `colors.gold`.

---

## Check

- [ ] Login "Keep me logged in": tapping the text toggles the box, and TalkBack says "Checkbox, checked".
- [ ] Switch animates, and in Arabic the "on" thumb is on the left.
- [ ] `formatTnd(279000) == "279.000"`, `formatTnd(39900) == "39.900"`, `formatTnd(1500) == "1.500"`.
- [ ] Product card: "+" turns into the blue stepper after the first tap and goes back to "+" at 0.
- [ ] Error banner on login shows a red-soft box with a red border and an alert icon, and TalkBack announces it.
- [ ] Pager indicator on onboarding: the active dot stretches to a 22 dp pill.
- [ ] Empty My orders shows the blue tile, "No orders yet" and a "Start shopping" button, centered.
- [ ] Skeleton grid: a soft highlight sweeps left → right every 1.2 s (right → left in Arabic). With "Remove animations" on, the blocks stay still.
