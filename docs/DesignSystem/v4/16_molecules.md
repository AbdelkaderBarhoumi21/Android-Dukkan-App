# 16: Molecules (product card, banners, coupon, options, bars, navigation, sheet header)

Figma page **Design System → 4.3 · Molecules** has components that docs 10–13 never covered. This doc adds them. Each section has the spec measured in the Figma file, then the code. Every size is an `AppDimens` token (doc 07, "Component internals"); no composable writes `N.dp`.

Type them when the feature that needs them starts. Home, Search and Category need 16.1–16.4; Cart and Checkout need 16.5–16.7; every main tab needs 16.8–16.9.

**Folders (new):**

```
core/common/components/
├── card/        AppProductCard.kt · AppCategoryTile.kt · AppBrandLogo.kt · AppPromoBanner.kt · AppCouponCard.kt · AppLiveOrderCard.kt
├── selection/   AppOptionRow.kt
├── bar/         AppCartBar.kt · AppBottomNav.kt
├── scaffold/    AppLargeTopBar.kt · AppHomeHeader.kt
└── sheet/       AppSheetHeader.kt
```

| # | Figma layer | Composable |
|---|---|---|
| 16.1 | `Product card/*` | `AppProductCard` |
| 16.2 | `Category tile/*`, `Category card/*`, `Brand/*` | `AppCategoryTile`, `AppCategoryCard`, `AppBrandLogo` |
| 16.3 | `Promo banner/*` | `AppPromoBanner` |
| 16.4 | `Coupon/*` | `AppCouponCard` |
| 16.5 | `Option/*` | `AppOptionRow` (payment + language) |
| 16.6 | `Live order` | `AppLiveOrderCard` |
| 16.7 | `Cart bar`, `Checkout bar` | `AppCartBar` (dark + light) |
| 16.8 | `App bar/Large`, `Home header` | `AppLargeTopBar`, `AppHomeHeader` (`App bar/Default` is `AppScaffold`'s top bar, doc 13) |
| 16.9 | `Bottom nav/*` | `AppBottomNav` |
| 16.10 | `Sheet header` | `AppSheetHeader` |

---

## 16.1 Product card

| Part | Figma | Token |
|---|---|---|
| Card | 170 × 236, `surface/card`, `radius/card`, `Elevation/Card` | `productCardWidth`, `radiusCard`, `shadows.card` |
| Photo | 154 × 124, inset 8, `radius/lg`, `surface/field` behind the image | `productPhotoHeight`, `space8`, `radiusLg` |
| Deal badge | top-start, 16 from the card edge (8 inside the photo) | `AppBadge(Discount)`, `space8` |
| Wishlist | `size/wishlist` 28 circle, `card` at 92 %, top 18 / end 18, heart `icon/sm` 16 `textPrimary` (filled `iconWishlist` when saved) | `wishlist`, `iconSm` |
| Add / stepper | bottom-end of the photo, 8 inside it | `AppQuantityControl` (12.7) |
| Brand | `M3/labelSmall` 10.5 Bold `textTertiary`, uppercase | `labelSmall` |
| Name | `Dukkan/bodyStrong` 13.5 Bold `textPrimary`, 1 line | `bodyStrong` |
| Rating + ETA | `Rating` atom | `AppRating(eta = …)` |
| Price | `titleCompact` 15 + `overline` "TND", old price `caption` struck | `AppPriceText(size = Small)` |
| Text padding | 12 start/end, 8 under the photo, 12 at the bottom | `space12`, `space8` |

```kotlin
package com.example.dukkanapp.core.common.components.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.display.AppBadge
import com.example.dukkanapp.core.common.components.display.AppBadgeVariant
import com.example.dukkanapp.core.common.components.display.AppPriceSize
import com.example.dukkanapp.core.common.components.display.AppPriceText
import com.example.dukkanapp.core.common.components.display.AppQuantityControl
import com.example.dukkanapp.core.common.components.display.AppRating
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

/** Everything the card shows. Built by the ViewModel's mapper, so the card stays dumb. */
data class AppProductCardUi(
    val brand: String,
    val name: String,
    val photo: Painter?,
    val priceMillimes: Long,
    val oldPriceMillimes: Long?,
    val discountLabel: String?,   // "-15%"
    val rating: String,           // "4.9"
    val ratingCount: String?,     // "3.4k"
    val eta: String?,             // "25 min"
    val quantityInCart: Int,
    val isWishlisted: Boolean,
)

@Composable
fun AppProductCard(
    product: AppProductCardUi,
    currency: String,
    onClick: () -> Unit,
    onWishlistChange: (Boolean) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val cardShape = RoundedCornerShape(AppDimens.radiusCard)
    val photoShape = RoundedCornerShape(AppDimens.radiusLg)

    Column(
        modifier = modifier
            .width(AppDimens.productCardWidth)
            .appShadow(AppTheme.shadows.card, cardShape)
            .clip(cardShape)
            .background(c.card)
            .clickable(onClick = onClick)
            .padding(AppDimens.space8),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(AppDimens.productPhotoHeight)
                .clip(photoShape)
                .background(c.field),
        ) {
            product.photo?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            }
            product.discountLabel?.let {
                AppBadge(it, AppBadgeVariant.Discount, Modifier.align(Alignment.TopStart).padding(AppDimens.space8))
            }
            WishlistButton(
                saved = product.isWishlisted,
                onChange = onWishlistChange,
                modifier = Modifier.align(Alignment.TopEnd).padding(AppDimens.space8),
            )
            AppQuantityControl(
                quantity = product.quantityInCart,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
                onRemove = onRemove,
                modifier = Modifier.align(Alignment.BottomEnd).padding(AppDimens.space8),
            )
        }
        Column(Modifier.padding(horizontal = AppDimens.space4, vertical = AppDimens.space4)) {
            Text(product.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            Spacer(Modifier.height(AppDimens.space2))
            Text(
                product.name,
                style = AppTheme.textStyles.bodyStrong,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(AppDimens.space4))
            AppRating(product.rating, product.ratingCount, eta = product.eta)
            Spacer(Modifier.height(AppDimens.space8))
            AppPriceText(
                millimes = product.priceMillimes,
                currency = currency,
                oldMillimes = product.oldPriceMillimes,
                size = AppPriceSize.Small,
            )
        }
    }
}

/** Figma `Wishlist`: 28 circle, card at 92 %, 16 heart. */
@Composable
private fun WishlistButton(saved: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppDimens.wishlist)
            .clip(CircleShape)
            .background(c.card.copy(alpha = 0.92f))
            .toggleable(value = saved, role = Role.Checkbox, onValueChange = onChange),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (saved) AppIcons.HeartFilled else AppIcons.Heart),
            contentDescription = stringResource(R.string.cd_wishlist),
            tint = if (saved) c.iconWishlist else c.textPrimary,
            modifier = Modifier.size(AppDimens.iconSm),
        )
    }
}
```

The card's 8 padding (`space8`) plus the text column's 4 (`space4`) gives Figma's 12 text inset. 8 + 4 below the photo is Figma's 9 (rounded). 170 − 2 × 8 = 154 photo width, exactly Figma.

> **Grid:** in a 2-column grid on a 393 frame, `screenPadding` 20 + 170 + 13 + 170 + 20. Use `LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(AppDimens.space12))` and let the card fill its cell (`Modifier.fillMaxWidth()` instead of `width(productCardWidth)`) so small phones don't overflow.
>
> `AppIcons.HeartFilled` doesn't exist yet: add the filled Lucide heart next to `StarFilled` (doc 09) and tint it `iconWishlist`.

## 16.2 Category tile, category card, brand logo

| Part | Figma | Token |
|---|---|---|
| Category tile | 64 photo, `radius/card`, `field` behind, label `Dukkan/helper` 11.5 SemiBold `textLabel`, gap ~6 | `categoryTile`, `radiusCard`, `helper`, `space8` |
| Category card | 170 × 150 card, `Elevation/Card`; photo 154 × 92 inset 8, `radius/md`; name `bodyStrong`; "2,400 items" `caption` `textTertiary`; `arrow-right` 12 `brand` bottom-end | `productCardWidth`, `categoryCardPhoto`, `iconXxs` |
| Brand logo | 62 white circle with `Elevation/Card`, 54 photo inside (4 inset), name `labelMedium` `textLabel` under it | `brandLogo`, `brandPhoto` |

```kotlin
package com.example.dukkanapp.core.common.components.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

/** Figma `Category tile/*`: the round-cornered photo row on Home. */
@Composable
fun AppCategoryTile(label: String, photo: Painter?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    Column(
        modifier = modifier.width(AppDimens.categoryTile).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Box(Modifier.size(AppDimens.categoryTile).clip(shape).background(AppTheme.colors.field)) {
            photo?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        }
        Text(label, style = AppTheme.textStyles.helper, color = AppTheme.colors.textLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Figma `Category card/*`: the 2-column grid on the Categories tab. */
@Composable
fun AppCategoryCard(
    name: String,
    itemCount: String,
    photo: Painter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val cardShape = RoundedCornerShape(AppDimens.radiusCard)
    Column(
        modifier = modifier
            .appShadow(AppTheme.shadows.card, cardShape)
            .clip(cardShape)
            .background(c.card)
            .clickable(onClick = onClick)
            .padding(AppDimens.space8),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(AppDimens.categoryCardPhoto)
                .clip(RoundedCornerShape(AppDimens.radiusMd))
                .background(c.field),
        ) {
            photo?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = AppDimens.space4, end = AppDimens.space4, top = AppDimens.space8),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = AppTheme.textStyles.bodyStrong, color = c.textPrimary, maxLines = 1)
                Text(itemCount, style = AppTheme.textStyles.caption, color = c.textTertiary)
            }
            Icon(painterResource(AppIcons.ArrowRight), null, tint = c.brand, modifier = Modifier.size(AppDimens.iconXxs))
        }
    }
}

/** Figma `Brand/*`: white circle with the brand photo, name under it. */
@Composable
fun AppBrandLogo(name: String, photo: Painter?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(AppDimens.brandLogo).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.space4),
    ) {
        Box(
            Modifier
                .size(AppDimens.brandLogo)
                .appShadow(AppTheme.shadows.card, CircleShape)
                .background(AppTheme.colors.card, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            photo?.let {
                Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(AppDimens.brandPhoto).clip(CircleShape))
            }
        }
        Text(name, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textLabel, maxLines = 1)
    }
}
```

> The category-card photo uses `radius/md` (14) in Figma, bound to the variable on every card and screen.

## 16.3 Promo banner

| Part | Figma | Token |
|---|---|---|
| Card | 353 × 164, `radius/card` | `promoBanner`, `radiusCard` |
| Fill + shadow | **Deal**: `Gradient/Deal` + `Elevation/Deal` ("FLASH SALE"). **Navy**: `Gradient/Navy` + `Elevation/Button` ("COLLECTION") | `gradients.deal` / `gradients.navy`, `shadows.deal` / `shadows.button` |
| Highlight | soft white glow top-end (doc 05) | `Modifier.cardHighlight(...)` |
| Text column | padding 18 start / 20 top; overline `Dukkan/overline` → 2 lines `M3/headlineSmall` 20 ExtraBold → `Dukkan/supporting` 11.5, all `onBrand` | `promoPadding`, `space20` |
| CTA | white pill 28 tall, padding 12, `M3/labelMedium` + `arrow-right` 12; text `text/flash` on Deal, `brand/soft-text` on Navy | `promoCta`, `iconXxs`, `textFlash` / `brandSoftText` |
| Photo | 126 × 140, end 12, top 12, no clip at the bottom | `promoPhotoWidth`, `promoPhotoHeight`, `space12` |

```kotlin
package com.example.dukkanapp.core.common.components.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.theme.cardHighlight
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppPromoStyle { Deal, Navy }

@Composable
fun AppPromoBanner(
    overline: String,       // "FLASH SALE"
    title: String,          // "Up to 40% off\naudio & tech"
    supporting: String,     // "Ends tonight at 23:59"
    cta: String,            // "Grab deals"
    photo: Painter?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppPromoStyle = AppPromoStyle.Deal,
) {
    val c = AppTheme.colors
    val gradients = AppTheme.gradients
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    val fill = if (style == AppPromoStyle.Deal) gradients.deal else gradients.navy
    val shadow = if (style == AppPromoStyle.Deal) AppTheme.shadows.deal else AppTheme.shadows.button
    val ctaText = if (style == AppPromoStyle.Deal) c.textFlash else c.brandSoftText

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.promoBanner)
            .appShadow(shadow, shape)
            .clip(shape)
            .background(fill)
            .cardHighlight(gradients.cardHighlight)
            .clickable(onClick = onClick),
    ) {
        photo?.let {
            Image(
                it, null, contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppDimens.space12, end = AppDimens.space12)
                    .size(width = AppDimens.promoPhotoWidth, height = AppDimens.promoPhotoHeight),
            )
        }
        Column(
            Modifier
                .fillMaxHeight()
                .padding(start = AppDimens.promoPadding, top = AppDimens.space20, bottom = AppDimens.space16)
                .padding(end = AppDimens.promoPhotoWidth),
        ) {
            Text(overline, style = AppTheme.textStyles.overline, color = c.onBrand)
            Spacer(Modifier.height(AppDimens.space4))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = c.onBrand, maxLines = 2)
            Spacer(Modifier.height(AppDimens.space4))
            Text(supporting, style = AppTheme.textStyles.supporting, color = c.onBrand, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .height(AppDimens.promoCta)
                    .background(c.card, AppPillShape)
                    .padding(horizontal = AppDimens.space12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space4),
            ) {
                Text(cta, style = MaterialTheme.typography.labelMedium, color = ctaText)
                Icon(painterResource(AppIcons.ArrowRight), null, tint = ctaText, modifier = Modifier.size(AppDimens.iconXxs))
            }
        }
    }
}
```

> `onBrand` is used for the text even in dark mode: both gradients stay saturated in dark (doc 05), so white keeps its contrast. The CTA's `card` turns dark navy in dark mode and the CTA text colours switch with it (`textFlash` → Orange400).

## 16.4 Coupon

A ticket: a coloured stub on the start side, a dashed perforation with two half-circle notches, and the details on a white body.

| Part | Figma | Token |
|---|---|---|
| Body | 353 × 96, `surface/card`, `radius/card`, `Elevation/Card` | `coupon`, `radiusCard`, `shadows.card` |
| Stub | first 82 dp, `Gradient/Deal` (available) or `Gradient/Brand` (applied) | `couponStub`, `gradients.deal` / `gradients.brand` |
| Stub text | "15%" `M3/headlineSmall` 20 ExtraBold + "OFF" `Dukkan/overline`, centred, `onBrand` | — |
| Notches | 16 circles at the stub edge, top and bottom (cut out of the ticket) | `couponNotch` |
| Perforation | dashed line 1.5, dash 3 / gap 4, `card`, 72 tall | `strokeFocus`, `couponDash`, `couponDashGap` |
| Details | 14 after the stub: code `AppBadge(Code)`, title `bodyStrong`, "Max 40 TND off" `caption` `textSecondary`, footer `clock` 12 + "Ends in 3 days" `caption` `textTertiary` | `space12`, `iconXxs` |
| Info | `info` top-end, `textTertiary` (Figma 15) | `iconSm` (16) |
| Action | **available**: `AppButton(Primary, ExtraSmall)` "Apply" (`size/button-xs` 26). **applied**: `AppBadge(Applied)` with a check | `AppButtonSize.ExtraSmall` |

```kotlin
package com.example.dukkanapp.core.common.components.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.common.components.display.AppBadge
import com.example.dukkanapp.core.common.components.display.AppBadgeVariant
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppCouponCard(
    code: String,          // "WELCOME15"
    discount: String,      // "15%"
    offLabel: String,      // "OFF"
    title: String,         // "15% off your first order"
    limit: String,         // "Max 40 TND off"
    expiry: String,        // "Ends in 3 days"
    isApplied: Boolean,
    applyLabel: String,
    appliedLabel: String,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The ticket outline: a rounded rect minus two notches on the stub edge.
    val ticket = with(density) {
        val radius = AppDimens.radiusCard.toPx()
        val notch = AppDimens.couponNotch.toPx() / 2
        val stub = AppDimens.couponStub.toPx()
        GenericShape { size, _ ->
            val x = if (isRtl) size.width - stub else stub
            val body = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius))) }
            val holes = Path().apply {
                addOval(Rect(Offset(x, 0f), notch))
                addOval(Rect(Offset(x, size.height), notch))
            }
            op(body, holes, PathOperation.Difference)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.coupon)
            .appShadow(AppTheme.shadows.card, ticket)
            .clip(ticket)
            .background(c.card),
    ) {
        // Stub
        Box(
            Modifier
                .width(AppDimens.couponStub)
                .fillMaxHeight()
                .background(if (isApplied) AppTheme.gradients.brand else AppTheme.gradients.deal),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(discount, style = MaterialTheme.typography.headlineSmall, color = c.onBrand)
                Text(offLabel, style = AppTheme.textStyles.overline, color = c.onBrand)
            }
        }
        // Perforation (on the stub edge, between the notches)
        Canvas(Modifier.width(AppDimens.strokeFocus).fillMaxHeight().padding(vertical = AppDimens.space12)) {
            drawLine(
                color = c.border,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = size.width,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(AppDimens.couponDash.toPx(), AppDimens.couponDashGap.toPx()),
                ),
            )
        }
        // Details
        Row(
            Modifier.weight(1f).fillMaxHeight().padding(start = AppDimens.space12, end = AppDimens.space12, top = AppDimens.space12, bottom = AppDimens.space8),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.space2)) {
                AppBadge(code, AppBadgeVariant.Code)
                Text(title, style = AppTheme.textStyles.bodyStrong, color = c.textPrimary, maxLines = 1)
                Text(limit, style = AppTheme.textStyles.caption, color = c.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
                    Icon(painterResource(AppIcons.Clock), null, tint = c.textTertiary, modifier = Modifier.size(AppDimens.iconXxs))
                    Text(expiry, style = AppTheme.textStyles.caption, color = c.textTertiary)
                }
            }
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                Icon(painterResource(AppIcons.Info), null, tint = c.textTertiary, modifier = Modifier.size(AppDimens.iconSm))
                if (isApplied) {
                    AppBadge(appliedLabel, AppBadgeVariant.Applied, leadingIcon = painterResource(AppIcons.Check))
                } else {
                    AppButton(text = applyLabel, onClick = onApply, size = AppButtonSize.ExtraSmall)
                }
            }
        }
    }
}
```

> Figma draws the perforation in `surface/card` over the stub. Here it sits between the stub and the body, so it's drawn in `border` to stay visible on white. The `appShadow` takes the ticket shape, so the notches cast no shadow, like Figma.

## 16.5 Option row (payment, language)

Figma `Option/*`: pill rows with a leading tile, two lines of text and a radio at the end. The **whole row** is the radio (`Modifier.selectable`, `Role.RadioButton`).

| Part | Payment (`Option/Card`, `Option/Cash on delivery`) | Language (`Option/English`, `Option/Français`) |
|---|---|---|
| Height | `size/option-lg` 66 → `optionLg` | `size/option-sm` 48 → `optionSm` (settings **and** onboarding 01.2), content centred |
| Shape | `radius/full` → `AppPillShape` | same |
| Off | `card` fill, `border` `strokeThin` (1.2) | same |
| On | `brandSoft` fill, `brand` `strokeControl` (1.6) | same |
| Leading | `optionTile` 40 circle (`size/option-tile`): `field` (off) / `card` (on), icon `icon` (20): `textLabel` off / `brand` on | `languageBadge` 32 circle: `field` + `textSecondary` "FR" (off) / `brand` + `onBrand` "EN" (on), `labelMedium` |
| Padding | start 12, end 12, text starts at 64 (12 + 40 + 12) | start 14 |
| Title / subtitle | `bodyStrong` `textPrimary` / `supporting` `textSecondary` | same |
| Trailing | `AppRadio` (12.2) | same |

```kotlin
package com.example.dukkanapp.core.common.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

/** What sits at the start of the row: an icon tile (payment) or a 2-letter badge (language). */
sealed interface AppOptionLeading {
    data class IconTile(val icon: Painter) : AppOptionLeading
    data class Code(val text: String) : AppOptionLeading
}

@Composable
fun AppOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: AppOptionLeading,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val c = AppTheme.colors
    val isLanguage = leading is AppOptionLeading.Code
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isLanguage) AppDimens.optionSm else AppDimens.optionLg)
            .clip(AppPillShape)
            .background(if (selected) c.brandSoft else c.card)
            .border(
                width = if (selected) AppDimens.strokeControl else AppDimens.strokeThin,
                color = if (selected) c.brand else c.border,
                shape = AppPillShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        when (leading) {
            is AppOptionLeading.IconTile -> Box(
                Modifier.size(AppDimens.optionTile).background(if (selected) c.card else c.field, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(leading.icon, null, tint = if (selected) c.brand else c.textLabel, modifier = Modifier.size(AppDimens.icon))
            }
            is AppOptionLeading.Code -> Box(
                Modifier.size(AppDimens.languageBadge).background(if (selected) c.brand else c.field, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(leading.text, style = MaterialTheme.typography.labelMedium, color = if (selected) c.onBrand else c.textSecondary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.textStyles.bodyStrong, color = c.textPrimary)
            subtitle?.let { Text(it, style = AppTheme.textStyles.supporting, color = c.textSecondary) }
        }
        AppRadio(selected = selected)
    }
}
```

Usage:

```kotlin
// Checkout → Pay with
AppOptionRow("Cash on delivery", selected = method == Cash, onClick = { onIntent(SelectPayment(Cash)) },
    leading = AppOptionLeading.IconTile(painterResource(AppIcons.Banknote)), subtitle = "Pay when your order arrives")

// Settings → Language
AppOptionRow("English", selected = lang == "en", onClick = { onIntent(SetLanguage("en")) },
    leading = AppOptionLeading.Code("EN"), subtitle = "Phone language")
```

Wrap a group in `Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppDimens.space12))` so TalkBack says "1 of 3".

> Language rows are 48 everywhere: the components, App settings and the onboarding screen **01.2 · Language** are all bound to `size/option-sm`. `heightIn` still lets a row grow when the user picks a large font size. Use `AppOptionRow` on the onboarding language screen too, with `space12` between rows.

## 16.6 Live order card

Figma `Live order` (Home, when an order is on its way):

| Part | Figma | Token |
|---|---|---|
| Card | 353 × 78, `card`, `radius/card`, `Elevation/Card`, padding 14 | `radiusCard`, `listRowPaddingH` |
| Tile | 44, `radius/md`, `Gradient/Brand`, `truck` 20 `onBrand` | `liveOrderTile`, `radiusMd`, `icon` |
| Title | "Arriving in 12 min" `M3/titleSmall` `textPrimary` + `AppBadge(Live)` | `titleSmall` |
| Progress | 4 segments, 5 tall, gap 6, `brand` done / `border` to go | `liveSegment`, `liveSegmentGap` |
| Subtitle | "#DK-2048 · Karim is on his way" `supporting` `textSecondary` | `supporting` |
| Trailing | `chevron-right` 14 `textLabel` | `iconXs` |

```kotlin
package com.example.dukkanapp.core.common.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.common.components.display.AppBadge
import com.example.dukkanapp.core.common.components.display.AppBadgeVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppLiveOrderCard(
    title: String,
    liveLabel: String,
    subtitle: String,
    stepsDone: Int,
    stepCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(AppDimens.radiusCard)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appShadow(AppTheme.shadows.card, shape)
            .clip(shape)
            .background(c.card)
            .clickable(onClick = onClick)
            .padding(AppDimens.listRowPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Box(
            Modifier.size(AppDimens.liveOrderTile).background(AppTheme.gradients.brand, RoundedCornerShape(AppDimens.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(AppIcons.Truck), null, tint = c.onBrand, modifier = Modifier.size(AppDimens.icon))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.space4)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1)
                AppBadge(liveLabel, AppBadgeVariant.Live)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.liveSegmentGap)) {
                repeat(stepCount) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(AppDimens.liveSegment)
                            .background(if (index < stepsDone) c.brand else c.border, AppPillShape)
                    )
                }
            }
            Text(subtitle, style = AppTheme.textStyles.supporting, color = c.textSecondary, maxLines = 1)
        }
        Icon(painterResource(AppIcons.ChevronRight), null, tint = c.textLabel, modifier = Modifier.size(AppDimens.iconXs))
    }
}
```

## 16.7 Cart bar and checkout bar

Two floating pills that sit above the bottom nav / at the bottom of the cart:

| | `Cart bar` (Home, Product: "you have items") | `Checkout bar` (Cart) |
|---|---|---|
| Size | 353 × `cartBar` 64 | 353 × `checkoutBar` 66 |
| Fill | `surface/inverse` | `surface/card` |
| Shadow | `Elevation/Float · sheet` | same |
| Thumbnails | 2 overlapping photo circles, 34 and 32, the front one with a white ring | same |
| Count | "3 items" `caption` `textTertiary` | "4 items" `caption` `textSecondary` |
| Total | `titleCompact` + `overline` "TND", `onInverse` | `textPrimary` |
| Button | `AppButton(Primary, Medium)` 124 wide, "Cart →" | `AppButton(Primary, Compact)` 136 wide, "Checkout →" |
| Padding | `space8` around the button, `space12` start | same |

```kotlin
package com.example.dukkanapp.core.common.components.bar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.common.components.display.AppPriceSize
import com.example.dukkanapp.core.common.components.display.AppPriceText
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppCartBarStyle { Dark, Light }

@Composable
fun AppCartBar(
    itemCountLabel: String,      // "3 items"
    totalMillimes: Long,
    currency: String,
    buttonLabel: String,         // "Cart" / "Checkout"
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnails: List<Painter> = emptyList(),
    style: AppCartBarStyle = AppCartBarStyle.Dark,
) {
    val c = AppTheme.colors
    val isDark = style == AppCartBarStyle.Dark
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isDark) AppDimens.cartBar else AppDimens.checkoutBar)
            .appShadow(AppTheme.shadows.float, AppPillShape)
            .background(if (isDark) c.inverse else c.card, AppPillShape)
            .padding(start = AppDimens.space12, end = AppDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Thumbnails(thumbnails.take(2), ring = if (isDark) c.inverse else c.card)
        Column(Modifier.weight(1f)) {
            Text(itemCountLabel, style = AppTheme.textStyles.caption, color = if (isDark) c.textTertiary else c.textSecondary)
            AppPriceText(totalMillimes, currency, size = AppPriceSize.Small, color = if (isDark) c.onInverse else c.textPrimary)
        }
        AppButton(
            text = buttonLabel,
            onClick = onClick,
            size = if (isDark) AppButtonSize.Medium else AppButtonSize.Compact,
            trailingIcon = painterResource(AppIcons.ArrowRight),
        )
    }
}

/** Up to two photo circles, the second one drawn over the first with a ring in the bar colour. */
@Composable
private fun Thumbnails(photos: List<Painter>, ring: Color) {
    Box {
        photos.forEachIndexed { index, photo ->
            Image(
                photo, null, contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = AppDimens.space20 * index)
                    .size(AppDimens.cartThumb)
                    .clip(CircleShape)
                    .border(AppDimens.strokeBubble, ring, CircleShape),
            )
        }
    }
}
```

## 16.8 Large app bar and home header

`App bar/Default` (back · centred "Checkout" + "4 items · Dukkan Lac 2" · action) is `AppScaffold`'s top bar: doc 13, now with `titleLarge` + `supporting` like Figma. The other two:

| | `App bar/Large` (My orders, Account) | `Home header` |
|---|---|---|
| Row | 44 tall, `screenPadding` sides | 48 tall |
| Start | title `M3/displaySmall` 28 ExtraBold `textPrimary` | `AppAvatar(Header)` 48 with `shadows.card`, then "Good morning, Amira" `helper` `textSecondary` over `map-pin` 14 `brand` + "Lac 2, Tunis" `titleSmall` |
| End | `AppIconButton(White)` 44 (search) | capsule 88 × 44, `card` 80 %, `shadows.card`, `heart` + `bell` 20 `textPrimary` |
| Below | — | headline `M3/headlineLarge` 24, 2 lines ("What are you shopping / for today?"), then `AppSearchBar(Elevated)` with the filter button |
| Background | `Plain` | `AuroraShort`-style band, 270 tall (doc 13) |

```kotlin
package com.example.dukkanapp.core.common.components.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.display.AppAvatar
import com.example.dukkanapp.core.common.components.display.AppAvatarSize
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

/** Figma `App bar/Large`: big title on the start side, icon buttons on the end. */
@Composable
fun AppLargeTopBar(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPadding)
            .padding(top = AppDimens.space8)
            .height(AppDimens.iconButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = AppTheme.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}

/** Figma `Home header` top row: avatar, greeting + location, wishlist / notifications capsule. */
@Composable
fun AppHomeHeaderRow(
    greeting: String,
    location: String,
    avatar: Painter?,
    initials: String,
    onLocationClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(AppDimens.avatarHeader),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        AppAvatar(initials, photo = avatar, size = AppAvatarSize.Header, modifier = Modifier.appShadow(AppTheme.shadows.card, AppPillShape))
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = onLocationClick)) {
            Text(greeting, style = AppTheme.textStyles.helper, color = c.textSecondary, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.space2)) {
                Icon(painterResource(AppIcons.MapPin), null, tint = c.brand, modifier = Modifier.size(AppDimens.iconXs))
                Text(location, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1)
            }
        }
        Row(
            Modifier
                .width(AppDimens.headerActionsWidth)
                .height(AppDimens.iconButton)
                .appShadow(AppTheme.shadows.card, AppPillShape)
                .clip(AppPillShape)
                .background(c.card.copy(alpha = 0.8f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CapsuleIcon(AppIcons.Heart, stringResource(R.string.cd_wishlist), onWishlistClick)
            CapsuleIcon(AppIcons.Bell, stringResource(R.string.cd_notifications), onNotificationsClick)
        }
    }
}

@Composable
private fun RowScope.CapsuleIcon(icon: Int, description: String, onClick: () -> Unit) {
    Icon(
        painterResource(icon), description, tint = AppTheme.colors.textPrimary,
        modifier = Modifier
            .weight(1f)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AppDimens.space12)
            .size(AppDimens.icon),
    )
}
```

The Home screen stacks them: `AppScaffold(background = AuroraShort)` → `AppHomeHeaderRow` → `space16` → headline (`headlineLarge`) → `space16` → `AppSearchBar(variant = Elevated, onFilterClick = …)`.

## 16.9 Bottom navigation

Figma `Bottom nav/*`: "floating white bar with a raised cart (count + total). Active tab = blue pill."

| Part | Figma | Token |
|---|---|---|
| Bar | 361 × `size/nav-bar` 66, `card`, `radius/full`, `Elevation/Float · sheet`, 16 from the screen sides | `navBar`, `navBarInset`, `shadows.float` |
| Fade | 140 tall `Fade/Background` behind the bar (content melts into `background`) | `gradients.bottomFade` |
| Tab, inactive | icon 20 `textSecondary` (stroke 2) over the label `Dukkan/caption` 11 `textSecondary` | `icon`, `caption` |
| Tab, active | icon 20 `onBrand` on a `brand` pill 44 × 28, label `Dukkan/overline` 10.5 ExtraBold `brand` | `navActivePillWidth`, `navActivePillHeight`, `overline` |
| Cart (centre) | 70 `card` ring with `shadows.float` → 58 `Gradient/Brand` circle → `shopping-bag` 24 `onBrand`; raised 30 above the bar | `navCartRing`, `navCartButton`, `iconLg` |
| Cart count | `size/count-bubble-sm` 20 circle, `accent/deal`, 2.5 white ring, top-end of the ring | `countBubbleSm`, `deal`, `strokeBubble` |
| Cart total | "39.900 TND" `Dukkan/overline` `textPrimary` under the cart | `overline` |
| Tabs | Home `house` · Categories `layout-grid` · (cart) · Offers `percent` · Account `user-round` | — |

```kotlin
package com.example.dukkanapp.core.common.components.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.core.common.components.display.formatTnd
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

data class AppNavTab(val label: String, val icon: Int)

@Composable
fun AppBottomNav(
    tabs: List<AppNavTab>,          // 4 tabs: 2 before the cart, 2 after
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    cartCount: Int,
    cartTotalMillimes: Long,
    currency: String,
    cartDescription: String,        // "Cart, 3 items, 39.900 TND"
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.gradients.bottomFade)
            .navigationBarsPadding()
            .padding(horizontal = AppDimens.navBarInset, vertical = AppDimens.space8),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(AppDimens.navBar)
                .appShadow(AppTheme.shadows.float, AppPillShape)
                .background(c.card, AppPillShape)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                if (index == tabs.size / 2) {
                    // Room for the raised cart, with its total as the "label".
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(AppDimens.navActivePillHeight))
                        Text("${formatTnd(cartTotalMillimes)} $currency", style = AppTheme.textStyles.overline, color = c.textPrimary, maxLines = 1)
                    }
                }
                NavTab(tab, selected = index == selectedIndex, onClick = { onTabSelected(index) }, modifier = Modifier.weight(1f))
            }
        }
        NavCartButton(
            count = cartCount,
            description = cartDescription,
            onClick = onCartClick,
            // Raised: the ring's centre sits on the bar's top edge plus a little.
            modifier = Modifier.align(Alignment.TopCenter).offset(y = -AppDimens.space24),
        )
    }
}

@Composable
private fun NavTab(tab: AppNavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val pill by animateColorAsState(if (selected) c.brand else c.card, label = "navPill")
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.space2),
    ) {
        Box(
            Modifier.size(width = AppDimens.navActivePillWidth, height = AppDimens.navActivePillHeight).background(pill, AppPillShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(tab.icon), null, tint = if (selected) c.onBrand else c.textSecondary, modifier = Modifier.size(AppDimens.icon))
        }
        Text(
            tab.label,
            style = if (selected) AppTheme.textStyles.overline else AppTheme.textStyles.caption,
            color = if (selected) c.brand else c.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun NavCartButton(count: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(modifier = modifier.size(AppDimens.navCartRing)) {
        Box(
            Modifier
                .size(AppDimens.navCartRing)
                .appShadow(AppTheme.shadows.float, CircleShape)
                .background(c.card, CircleShape)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(AppDimens.navCartButton).background(AppTheme.gradients.brand, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(AppIcons.ShoppingBag), null, tint = c.onBrand, modifier = Modifier.size(AppDimens.iconLg))
            }
        }
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(AppDimens.countBubbleSm)
                    .background(c.deal, CircleShape)
                    .border(AppDimens.strokeBubble, c.card, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (count > 9) "9+" else count.toString(), style = AppTheme.textStyles.micro, color = c.onBrand)
            }
        }
    }
}
```

> Only 4 tabs are real tabs (`Role.Tab` in a `selectableGroup`). The cart is a button: it opens the cart screen, it never becomes "selected". Its content description reads the count and the total, because the badge and total are hidden from TalkBack inside the button.

## 16.10 Sheet header

Figma `Sheet header` (top of every bottom sheet: "Pay with", "Choose address"…):

| Part | Figma | Token |
|---|---|---|
| Sheet | `surface/card` (`sheet` in dark), `radius/sheet` top corners, `Elevation/Float · sheet` | `colors.sheet`, `radiusSheet` |
| Handle | 40 × 4.5 pill, `surface/handle`, 10 from the top, centred | `sheetHandleWidth`, `sheetHandleHeight`, `handle` |
| Title | `M3/headlineSmall` 20 ExtraBold `textPrimary`, 32 from the top | `headlineSmall` |
| Subtitle | `supporting` `textSecondary` | `supporting` |
| Close | `size/icon-button-sm` 36 `field` circle, `x` 16, end 20, top 28 | `AppIconButton(Field, iconButtonSm)` |
| Padding | `screenPadding` (20) | — |

```kotlin
package com.example.dukkanapp.core.common.components.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppIconButton
import com.example.dukkanapp.core.common.components.buttons.AppIconButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

/** The handle. Pass it as `dragHandle = { AppSheetHandle() }` to `ModalBottomSheet`. */
@Composable
fun AppSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = AppDimens.space8)
            .size(width = AppDimens.sheetHandleWidth, height = AppDimens.sheetHandleHeight)
            .background(AppTheme.colors.handle, AppPillShape)
    )
}

@Composable
fun AppSheetHeader(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding)
            .padding(top = AppDimens.space16, bottom = AppDimens.space8),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.textPrimary)
            subtitle?.let { Text(it, style = AppTheme.textStyles.supporting, color = AppTheme.colors.textSecondary) }
        }
        AppIconButton(
            icon = painterResource(AppIcons.X),
            contentDescription = stringResource(R.string.cd_close),
            onClick = onClose,
            variant = AppIconButtonVariant.Field,
            size = AppDimens.iconButtonSm,
        )
    }
}
```

Usage:

```kotlin
ModalBottomSheet(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(topStart = AppDimens.radiusSheet, topEnd = AppDimens.radiusSheet),
    containerColor = AppTheme.colors.sheet,
    dragHandle = { AppSheetHandle() },
) {
    AppSheetHeader(title = "Pay with", subtitle = "Total 765.300 TND", onClose = onDismiss)
    // AppOptionRow list (16.5)
}
```

---

## New strings

| Key | EN | AR | FR |
|---|---|---|---|
| `cd_wishlist` | Wishlist | المفضلة | Favoris |
| `cd_notifications` | Notifications | الإشعارات | Notifications |
| `cd_close` | Close | إغلاق | Fermer |

(`cd_close` may already exist from the auth sheets; keep one.)

## Check

- [ ] No `N.dp` in any file of this doc: every size is an `AppDimens` token.
- [ ] Product card: 170 wide, photo 154 × 124 with radius 16, badge top-start, heart top-end, + / stepper bottom-end of the photo. Quantity 1 shows the trash.
- [ ] Promo banner: Deal (orange → rose, orange CTA text) and Navy (blue CTA text) both match `4.3`, light and dark.
- [ ] Coupon: notches cut through the shadow; "Apply" button when available, green "Applied" badge with a check when applied.
- [ ] Option rows: whole row selects, border 1.6 brand when selected; TalkBack says "Radio button, selected, Cash on delivery, 1 of 2".
- [ ] Bottom nav: active tab is a blue pill with a white icon and a blue overline label; cart is raised with the orange count and total under it; nothing is cut off by the gesture bar.
- [ ] Arabic: every row and bar mirrors (stub on the right, arrows flip because their vectors are `autoMirrored`).
