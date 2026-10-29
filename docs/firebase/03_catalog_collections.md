# 03: Catalog Collections

`zones`, `stores`, `stores/{storeId}/inventory`, `categories`, `brands`, `products`, `products/{productId}/variants`, `products/{productId}/reviews`, `banners`, `collections`, `coupons`, `zoneWaitlist`.

The customer app **reads** all of these. Only the admin (console scripts, functions) **writes** them, except reviews and the waitlist.

"Written by" values: **admin** = admin tool / seed script, **fn** = Cloud Function, **client** = customer app.

---

## Why the catalog is shaped this way

- **Quick commerce, not a marketplace.** Figma shows "Dukkan Lac 2 is packing", "Dukkan Hub · Lac 2", aisles and shelves. Orders are packed in **our own dark stores**. Brands (STRIDE, SONIQ…) are *official brand stores* inside the app, not sellers. So stock lives **per store**, and products are global.
- **Product vs variant.** "Air Runner 2" is one product. "EU 42, Blue" is a variant with its own price, SKU, barcode and stock (06.2 "Only 2 left" on EU 42, "Sold out" on EU 44).
- **Zone decides store.** The delivery address falls inside a zone polygon (03.2 "In delivery zone", 03.6 "Outside our zone"); the zone points to the store that serves it. The cart is tied to that store ("3 items · Dukkan Lac 2").

---

## 1. `zones/{zoneId}`

**Id:** slug, e.g. `lac`, `menzah`, `ennasr`, `centre_ville`.

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `name` | map | ✓ | `{en:"Lac", fr:"Lac", ar:"البحيرة"}` | admin | 03.6 "Dukkan delivers in Lac, Menzah…" |
| `storeId` | string | ✓ | `"lac2"` | admin | Which store packs orders for this zone |
| `polygon` | array<GeoPoint> | ✓ | `[…]` (≤ 200 points) | admin | Point-in-polygon check on the phone (03.2) and again in `placeOrder` |
| `center` | GeoPoint | ✓ | `36.84, 10.27` | admin | "Closest zone is 2.4 km away" (03.6) |
| `etaMinMinutes` / `etaMaxMinutes` | number | ✓ | `25` / `35` | admin | "25 min", "Now · 25–35 min" |
| `deliveryFee` | number (millimes) | ✓ | `5000` | admin | Charged when below the free threshold |
| `freeDeliveryThreshold` | number (millimes) | ✓ | `150000` | admin | "Free delivery over 150 TND" (04.1, 08.1) |
| `isActive` | bool | ✓ | `true` | admin | Turn a zone off |
| `sortOrder` | number | ✓ | `1` | admin | List order |

```json
{
  "name": { "en": "Lac", "fr": "Lac", "ar": "البحيرة" },
  "storeId": "lac2",
  "polygon": ["<GeoPoint>", "..."],
  "center": "<GeoPoint 36.8432, 10.2731>",
  "etaMinMinutes": 25, "etaMaxMinutes": 35,
  "deliveryFee": 5000, "freeDeliveryThreshold": 150000,
  "isActive": true, "sortOrder": 1
}
```

**Live demand** ("Busy zones now · 12 orders waiting · ×1.5", C01.1) changes every minute, so it lives in its own doc `zones/{zoneId}/live/current`, written by a scheduled function:

| Field | Type | Example |
|---|---|---|
| `ordersWaiting` | number | `12` |
| `surgeMultiplier` | number | `1.5` |
| `updatedAt` | Timestamp | |

**Why a separate doc:** zone docs are cached by every customer app. Changing them every minute would push a new snapshot to everyone.

### `zoneWaitlist/{autoId}` (03.6 "Notify me when you arrive")

| Field | Type | Example | Written by |
|---|---|---|---|
| `uid` | string | | client (must equal `request.auth.uid`) |
| `location` | GeoPoint | La Marsa | client |
| `nearestZoneId` | string | `"lac"` | client |
| `createdAt` | Timestamp | server | client |

Create-only. Admin reads it to plan new zones; a function pushes the user when a zone opens nearby.

---

## 2. `stores/{storeId}`

**Id:** slug, e.g. `lac2`.

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `name` | string | ✓ | `"Dukkan Lac 2"` | Cart header (08.1), order placed (09.3) |
| `shortName` | string | ✓ | `"Lac 2"` | Team app headers ("Lac 2 · 3h 12m on shift") |
| `hubName` | string | ✓ | `"Dukkan Hub · Lac 2"` | Courier "Pick up · Dukkan Hub Lac 2" (C01.3, C02.1) |
| `address` | string | ✓ | `"Rue du Lac Biwa"` | Courier navigation |
| `location` | GeoPoint | ✓ | | Distance "1.2 km away" |
| `geohash` | string | ✓ | `"snd2…"` | Nearest-store queries in functions |
| `zoneIds` | array<string> | ✓ | `["lac"]` | Which zones it serves |
| `isOpen` | bool | ✓ | `true` | Closed store = no checkout |
| `openingHours` | map | ✓ | `{mon:"08:00-22:00", …}` (local Tunis time) | Redelivery slots and "Ready now · open until 22:00" (10.7) are computed from it by `getRedeliverySlots` (09) |
| `redeliverySlotCapacity` | number | | `6` | Max redeliveries per 1-hour slot (default 6 when missing) |
| `redeliveryLeadMinutes` | number | | `60` | First redelivery slot starts at least this many minutes from now (default 60 when missing) |
| `pickupShelves` | array<string> | | `["R-10","R-11","R-12"]` | "Shelf R-12 · scan at handover" (F02.2) |
| `phone` | string | | | "Call your store manager" (S01.3) |

`location` and `geohash` together let a function find the nearest store; the phone never queries stores by distance.

### `stores/{storeId}/slotLoad/{yyyy-mm-dd_HH}` (redelivery capacity)

One small counter per store and local hour, e.g. `stores/lac2/slotLoad/2026-10-29_20`.

| Field | Type | Example | Written by |
|---|---|---|---|
| `count` | number | `3` | `resolveFailedDelivery` (`+1`, in its transaction) |
| `slotStart` | Timestamp | 20:00 | same |

**Why:** two customers choosing the last free slot at the same moment must not both get it. A counter doc read and incremented in one transaction makes that impossible. The doc id is built from the slot time, so it is read **by id**: no query and no index. Server-only (rules: no client access); `cleanupSlotLoad` deletes counters older than 2 days.

---

## 3. `stores/{storeId}/inventory/{variantId}`

**Id:** the `variantId`, so each variant has exactly one stock record per store.

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `productId` | string | ✓ | `"air-runner-2"` | admin | Query "all stock of a product" (size sheet 06.2) |
| `quantity` | number | ✓ | `2` | admin, **fn** (decrement on order) | "Only 2 left", "Sold out — notify me" |
| `aisle` | string | ✓ | `"B"` | admin | Picking route "By aisle" (F01.2) |
| `aisleName` | map | ✓ | `{en:"Shoes", …}` | admin | "B · Shoes" |
| `shelf` | string | ✓ | `"6"` | admin | "Aisle A · Shelf 6" (F01.3) |
| `updatedAt` | Timestamp | ✓ | | admin/fn | |

**Why stock is not on the variant:** the same variant has different stock in Lac 2 and Menzah 6. Putting it on the variant would force one variant doc per store, which duplicates price and barcode.

**Low-stock threshold:** "Only N left" shows when `quantity <= 3` (a UI constant, or Remote Config later).

```json
{ "productId": "air-runner-2", "quantity": 2, "aisle": "B",
  "aisleName": { "en": "Shoes", "fr": "Chaussures", "ar": "أحذية" },
  "shelf": "6", "updatedAt": "<Timestamp>" }
```

---

## 4. `categories/{categoryId}`

**Id:** slug (`fashion`, `shoes`, `sneakers`, `tech_audio`).

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `name` | map | ✓ | `{en:"Shoes", …}` | Tiles (04.1, 05.1) |
| `subtitle` | map | | `{en:"sneakers, runners, slides"}` | 05.2 header |
| `parentId` | string? | | `null` (top) / `"shoes"` (chip) | Top-level departments vs filter chips ("All · Sneakers · Running · Casual · Kids") |
| `level` | number | ✓ | `0` / `1` | Query top-level with `level == 0` |
| `imageUrl` | string | ✓ | Storage URL | "Real photos in rounded tiles, never icons" |
| `productCount` | number | ✓ | `980` | **fn** (trigger). "980 items" |
| `sortOrder` | number | ✓ | `2` | |
| `isActive` | bool | ✓ | `true` | |

Two levels are enough for the Figma design. A product stores the **path** of its category ids (see `categoryPath` below), so one query serves a department and a chip.

---

## 5. `brands/{brandId}`

**Id:** slug (`stride`, `soniq`, `hora`, `luna`, `bloom`, `urban`, `nomad`).

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `name` | string | ✓ | `"STRIDE"` | Not translated |
| `sortName` | string | ✓ | `"stride"` | **Admin tool**, derived from `name` (lowercase, accents removed). All brands A–Z (07.4) sorts on it: Firestore sorts strings by code point, so `"BLOOM"` and `"bloom"` would not sort together |
| `initial` | string | ✓ | `"S"` | **Admin tool**. First letter of `sortName`, uppercase. Section headers "B · C · H · L · N" and the A–Z side index (07.4). Stored so the client doesn't re-derive it differently per language |
| `categoryLabel` | map | ✓ | `{en:"Beauty"}` | 07.4 row "Beauty · 42 products" |
| `tagline` | map | ✓ | `{en:"Official store · Sneakers & running"}` | 05.3 |
| `logoUrl` | string | ✓ | | Brand circles (04.2) |
| `coverUrl` | string | | | Brand store header |
| `about` | map | | | 05.3 "About" tab |
| `ratingAvg` | number | ✓ | `4.9` | **fn**, from product reviews |
| `followersCount` | number | ✓ | `12400` | **fn**, from `users/*/followedBrands` |
| `productCount` | number | ✓ | `86` | **fn** |
| `isFeatured` | bool | ✓ | `true` | "Top brands" row |
| `sortOrder` | number | ✓ | | |
| `isActive` | bool | ✓ | | |

The brand coupon on 05.3 ("STRIDE10") is a normal coupon with `scope.brandIds: ["stride"]` (see § 10). The brand doc does not copy it.

**All brands (07.4):** "86 official stores" = `count()` on `brands where isActive == true`. "Featured" row = `isFeatured == true orderBy sortOrder`. The list = `isActive == true orderBy sortName`, loaded fully (86 small docs, about 30 KB, cached). Tapping a letter in the A–Z index scrolls to the first brand with that `initial`; no extra query. "Search brands" filters the loaded list on the phone (brands are not in Algolia; the list is tiny).

---

## 6. `products/{productId}`

**Id:** slug (`air-runner-2`) or auto id. Slugs make deep links readable (`dukkan://product/air-runner-2`).

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `name` | string | ✓ | `"Air Runner 2"` | admin | Card title |
| `subtitle` | map | | `{en:"Breathable Knit"}` | admin | "Air Runner 2 — Breathable Knit" (06.1) |
| `description` | map | | | admin | Details (exempt from indexing) |
| `brandId` | string | ✓ | `"stride"` | admin | Brand label on card, brand store query |
| `brandName` | string | ✓ | `"STRIDE"` | admin | **Allowed copy**: written at product creation by the same admin tool; brand names do not change. If one ever does, the rename script updates products. Saves one read per card |
| `categoryPath` | array<string> | ✓ | `["shoes","sneakers"]` | admin | `array-contains` serves department and chip |
| `images` | array<string> | ✓ | 4 URLs (≤ 10) | admin | Gallery "1 / 4" |
| `optionTypes` | array<string> | ✓ | `["size"]` or `["colour","size"]` | admin | Which pickers to show; "Size (EU)" |
| `sizeGuideUrl` | string | | | admin | "Size guide" |
| `badges` | array<string> | | `["bestseller"]` | admin | "Bestseller", "New" chips. Values: `bestseller`, `new`, `hot_deal` |
| `isFragile` | bool | ✓ | `false` | admin | Picker "fragile · pack last" |
| `minPrice` | number | ✓ | `279000` | **fn** | Card price, "Under 200" filter |
| `compareAtPrice` | number? | | `329000` | **fn** | Struck price |
| `discountPercent` | number | ✓ | `15` | **fn** | "-15%" badge, "On sale" filter (`> 0`) |
| `ratingAvg` | number | ✓ | `4.9` | **fn** | "4.9" |
| `ratingCount` | number | ✓ | `3400` | **fn** | "(3.4k)" |
| `popularityScore` | number | ✓ | `8123` | **fn** (nightly) | "Popular" sort, "Recommended" fallback |
| `inStockStoreIds` | array<string> | ✓ | `["lac2"]` | **fn** | List only products available in the cart's store |
| `prepMinutes` | number | | `10` | admin | Watches show "35 min" (extra prep) vs default zone ETA. Card ETA = zone ETA + `prepMinutes` |
| `isActive` | bool | ✓ | `true` | admin | Soft delete |
| `publishedAt` | Timestamp | ✓ | | admin | "New" tab sort |
| `updatedAt` | Timestamp | ✓ | | admin/fn | |

```json
{
  "name": "Air Runner 2",
  "subtitle": { "en": "Breathable Knit", "fr": "Maille respirante", "ar": "نسيج قابل للتنفس" },
  "brandId": "stride", "brandName": "STRIDE",
  "categoryPath": ["shoes", "sneakers"],
  "images": ["https://…/1.webp", "https://…/2.webp"],
  "optionTypes": ["colour", "size"],
  "badges": ["bestseller"], "isFragile": false,
  "minPrice": 279000, "compareAtPrice": 329000, "discountPercent": 15,
  "ratingAvg": 4.9, "ratingCount": 3400, "popularityScore": 8123,
  "inStockStoreIds": ["lac2"], "prepMinutes": 0,
  "isActive": true, "publishedAt": "<Timestamp>", "updatedAt": "<Timestamp>"
}
```

### 6.1 `products/{productId}/variants/{variantId}`

**Id:** SKU (`77310`) or auto id.

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `options` | map | ✓ | `{size:"42", colour:"Blue"}` | Size sheet (06.2), cart line "Blue · EU 42" |
| `optionsLabel` | map | ✓ | `{en:"Blue · EU 42"}` | Ready-made label for cart / order snapshot |
| `sku` | string | ✓ | `"77310"` | "SKU 77310" (F01.3) |
| `barcode` | string | ✓ | `"6194003773100"` | Scan to confirm (F01.3). EAN-13 |
| `price` | number | ✓ | `279000` | **The** price |
| `compareAtPrice` | number? | | `329000` | Struck price |
| `imageUrl` | string? | | | Colour-specific image |
| `isActive` | bool | ✓ | `true` | |
| `sortOrder` | number | ✓ | `42` | Size order 40, 41, 42… |

Stock is **not** here (see § 3). Flash sale prices are just a lower `price` + a `compareAtPrice`, written by the admin and reverted by a scheduled function when the flash collection ends.

### 6.2 `products/{productId}/reviews/{uid}`

**Id:** the reviewer's `uid`, so **one review per customer per product** without a query.

| Field | Type | Req | Example | Written by | Why |
|---|---|---|---|---|---|
| `rating` | number 1–5 | ✓ | `5` | client | |
| `comment` | string? | | max 500 chars | client | |
| `authorName` | string | ✓ | `"Amira B."` | client | Allowed copy, frozen. The reviewer may rename later; old review keeps the old name, like every store |
| `orderId` | string | ✓ | | client | Rules check the order is `delivered`, belongs to the author, and contains this product |
| `createdAt` / `updatedAt` | Timestamp | ✓ | server | client | |

The trigger `onReviewWritten` updates `ratingAvg` / `ratingCount` on the product and the brand.

---

## 7. `banners/{bannerId}`

Hero carousel (04.1 "SUMMER DROP · Up to 40% off new sneakers"), category banner (05.1 "New season, new fits").

| Field | Type | Req | Example | Why |
|---|---|---|---|---|
| `placement` | string | ✓ | `home_hero` / `categories_top` / `offers_hero` | Which screen |
| `style` | string | ✓ | `brand` / `deal` / `navy` | Figma banner variants (Brand / Deal / Navy gradients) |
| `eyebrow` | map | | `{en:"SUMMER DROP"}` | Overline text |
| `title` | map | ✓ | `{en:"Up to 40% off new sneakers"}` | |
| `subtitle` | map | | `{en:"Free delivery over 150 TND"}` | |
| `ctaLabel` | map | ✓ | `{en:"Shop now"}` | |
| `imageUrl` | string | ✓ | | Product cut-out |
| `target` | map | ✓ | `{type:"collection", id:"summer-drop"}` | Where a tap goes. `type`: `collection`, `category`, `brand`, `product`, `url` |
| `zoneIds` | array<string> | | `[]` = all | Local promos |
| `startsAt` / `endsAt` | Timestamp | ✓ | | Scheduling (filter on the client after the query) |
| `sortOrder` | number | ✓ | | |
| `isActive` | bool | ✓ | | |

---

## 8. `collections/{collectionId}`

Curated lists: "Weekend edit · 42 picks", "Work from home", "Cozy corner", **and** flash deals.

| Field | Type | Req | Example | Why |
|---|---|---|---|---|
| `kind` | string | ✓ | `editorial` / `flash_sale` | Flash sales show a countdown |
| `title` | map | ✓ | `{en:"Weekend edit"}` | |
| `subtitle` | map | | `{en:"42 picks · fashion, bags & sneakers"}` | |
| `coverUrl` | string | ✓ | | |
| `productIds` | array<string> | ✓ | ≤ 200 ids, ordered | Grid on 07.2. Fetch products by id in chunks of 30 (`whereIn(documentId())`) |
| `filterCategoryIds` | array<string> | | `["bags","tops","sneakers","watches"]` | Chips "All 42 · Bags · Tops…" (filter on the client) |
| `startsAt` / `endsAt` | Timestamp | ✓ | | "Ends in 02:14:38", "Ends tonight at 23:59" |
| `isFeatured` | bool | ✓ | | Home "Collections" row |
| `sortOrder` | number | ✓ | | |
| `isActive` | bool | ✓ | | |

**Why `productIds` is an array and not a field on the product:** a product can be in many collections, and editors reorder collections often. The array is bounded (≤ 200) and lives on the side we read.

---

## 9. "Recommended for you", "Buy it again", "Flash deals"

These are **queries or computed lists**, not stored collections:

| Row | Source |
|---|---|
| Flash deals (04.1) | The active `collections` doc with `kind == "flash_sale"` |
| Recommended for you (04.2) | v1: top `popularityScore` in the user's most-ordered categories (client-side). Later: a function writes `users/{uid}/private/recommendations` |
| Buy it again (08.3) | Distinct `productId`s from the user's last delivered orders' items (client reads its own orders) |

---

## 10. `coupons/{code}`

**Id:** the code as the user types it, uppercase (`WELCOME15`, `DUKKAN50`, `FLASH20`, `STRIDE10`).

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `title` | map | ✓ | `{en:"15% off your first order"}` | Coupon ticket |
| `subtitle` | map | | `{en:"All categories · once per account"}` | Second line on 07.3 tickets ("Up to 40 TND · new customers", "Orders over 80 TND", "Gold members only") |
| `type` | string | ✓ | `percent` / `fixed` / `free_delivery` | FREESHIP = `free_delivery` |
| `value` | number | ✓ | `15` (percent) / `50000` (millimes) / `0` (free_delivery) | |
| `maxDiscount` | number? | | `40000` | "Max 40 TND off" |
| `minSubtotal` | number | ✓ | `0` / `300000` | "50 TND over 300", "Spend 120 TND more to unlock" (computed on the client: `minSubtotal - cartSubtotal`) |
| `scope` | map | ✓ | `{brandIds:["stride"]}` or `{categoryIds:["tech_audio"]}` or `{}` | "10% off all STRIDE sneakers", "20% off audio & tech" |
| `firstOrderOnly` | bool | ✓ | `true` | WELCOME15 |
| `minTier` | string? | | `"gold"` | GOLD20 "Gold members only · Reach Gold to unlock" (07.3). Compared with `users/{uid}.tier` in the order `member < silver < gold` (tiers come from 12-month spend, 04 § 1 and 09 § 8d). The ticket shows **Locked** when the user's tier is lower |
| `perUserLimit` | number | ✓ | `1` | DUKKAN50 "once per account" |
| `totalLimit` | number? | | `5000` | |
| `redeemedCount` | number | ✓ | | **fn** |
| `isPublic` | bool | ✓ | `true` | Public coupons appear to everyone on Offers (07.1); private ones only via `users/{uid}/coupons` |
| `startsAt` / `endsAt` | Timestamp | ✓ | | "Ends in 3 days" |
| `isActive` | bool | ✓ | | |

`coupons/{code}/redemptions/{uid}`: written by `placeOrder` (fields `orderId`, `count`, `lastAt`). Checking `perUserLimit` reads this one doc. A cancelled order gives a **still-valid** coupon back: `count − 1` here and `redeemedCount − 1` on the coupon, in the cancel transaction (05 § 5.3). Expired coupons are not restored.

**The client never computes the final discount.** It shows an estimate; `placeOrder` re-checks every rule and returns the real totals.

Figma examples, as seed data:

| Code | `type` | `value` | `minSubtotal` | Other |
|---|---|---|---|---|
| WELCOME15 | percent | 15 | 0 | `maxDiscount 40000`, `firstOrderOnly`, `isPublic false` (given by `onUserCreated`) |
| DUKKAN50 | fixed | 50000 | 300000 | `perUserLimit 1`, `scope {}`, `isPublic true` |
| FREESHIP | free_delivery | 0 | 80000 | `isPublic true` |
| GOLD20 | percent | 20 | 0 | `minTier "gold"`, `isPublic true` |
| FLASH20 | percent | 20 | 0 | `scope {categoryIds:["tech_audio"]}` |

---

## 10b. Help center (11.7): `helpTopics/{topicId}` + `helpArticles/{articleId}`

Read-only content written by the admin tool. Small, cached, readable by any signed-in user.

**`helpTopics/{topicId}`** (`orders_delivery`, `returns_refunds`, `payments_wallet`, `coupons_points`, `account_security`, `using_app`):

| Field | Type | Example | Why |
|---|---|---|---|
| `title` | map | `{en:"Orders & delivery"}` | Topic tiles |
| `icon` | string | `"truck"` | Lucide icon name |
| `sortOrder` | number | `1` | |
| `isActive` | bool | `true` | |

**`helpArticles/{articleId}`**:

| Field | Type | Example | Why |
|---|---|---|---|
| `topicId` | string | `"returns_refunds"` | Topic screen query |
| `title` | map | `{en:"When do I get my refund?"}` | |
| `body` | map | Markdown per language | Article screen |
| `keywords` | map<string, array> | `{en:["refund","money back"]}` | "Search help articles": filtered on the phone (a few dozen articles; no Algolia needed) |
| `sortOrder` | number | | |
| `isActive` | bool | | |

**Other 11.7 rows:**
- "Your latest order · #DK-2048 · Get help" = the newest active order (08, query on `orders`). "Get help" opens 10.2 with the help sheet.
- "Chat with us" (v1) = an **external intent**: WhatsApp (`https://wa.me/216XXXXXXXX?text=Order%20DK-2048`) or email. A live chat (Intercom, Firebase-based tickets) is out of scope for v1, so there is no `supportTickets` collection yet.

---

## 11. Kotlin DTOs (data layer)

Put them in each feature's `data/model/` (`features/catalog/data/model/`). Every field has a default so Firestore's `toObject()` can create it, and `@DocumentId` fills the id.

```kotlin
package com.example.dukkanapp.features.catalog.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class ZoneDto(
    @DocumentId val id: String = "",
    val name: Map<String, String> = emptyMap(),
    val storeId: String = "",
    val polygon: List<GeoPoint> = emptyList(),
    val center: GeoPoint? = null,
    val etaMinMinutes: Int = 0,
    val etaMaxMinutes: Int = 0,
    val deliveryFee: Long = 0,
    val freeDeliveryThreshold: Long = 0,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = false,
    val sortOrder: Int = 0,
)

data class InventoryDto(
    @DocumentId val variantId: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val aisle: String = "",
    val aisleName: Map<String, String> = emptyMap(),
    val shelf: String = "",
    val updatedAt: Timestamp? = null,
)

data class CategoryDto(
    @DocumentId val id: String = "",
    val name: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val parentId: String? = null,
    val level: Int = 0,
    val imageUrl: String = "",
    val productCount: Long = 0,
    val sortOrder: Int = 0,
)

data class BrandDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortName: String = "",
    val initial: String = "",
    val categoryLabel: Map<String, String> = emptyMap(),
    val tagline: Map<String, String> = emptyMap(),
    val logoUrl: String = "",
    val coverUrl: String? = null,
    val ratingAvg: Double = 0.0,
    val followersCount: Long = 0,
    val productCount: Long = 0,
    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,
)

data class ProductDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val subtitle: Map<String, String> = emptyMap(),
    val brandId: String = "",
    val brandName: String = "",
    val categoryPath: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val optionTypes: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    @get:PropertyName("isFragile") @set:PropertyName("isFragile")
    var isFragile: Boolean = false,
    val minPrice: Long = 0,
    val compareAtPrice: Long? = null,
    val discountPercent: Int = 0,
    val ratingAvg: Double = 0.0,
    val ratingCount: Long = 0,
    val inStockStoreIds: List<String> = emptyList(),
    val prepMinutes: Int = 0,
)

data class VariantDto(
    @DocumentId val id: String = "",
    val options: Map<String, String> = emptyMap(),
    val optionsLabel: Map<String, String> = emptyMap(),
    val sku: String = "",
    val barcode: String = "",
    val price: Long = 0,
    val compareAtPrice: Long? = null,
    val imageUrl: String? = null,
    val sortOrder: Int = 0,
)

data class BannerDto(
    @DocumentId val id: String = "",
    val placement: String = "",
    val style: String = "",
    val eyebrow: Map<String, String> = emptyMap(),
    val title: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val ctaLabel: Map<String, String> = emptyMap(),
    val imageUrl: String = "",
    val target: Map<String, String> = emptyMap(),
    val startsAt: Timestamp? = null,
    val endsAt: Timestamp? = null,
    val sortOrder: Int = 0,
)

data class CollectionDto(
    @DocumentId val id: String = "",
    val kind: String = "",
    val title: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val coverUrl: String = "",
    val productIds: List<String> = emptyList(),
    val filterCategoryIds: List<String> = emptyList(),
    val startsAt: Timestamp? = null,
    val endsAt: Timestamp? = null,
)

data class CouponDto(
    @DocumentId val code: String = "",
    val title: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val type: String = "",
    val value: Long = 0,
    val maxDiscount: Long? = null,
    val minSubtotal: Long = 0,
    val scope: Map<String, List<String>> = emptyMap(),
    val firstOrderOnly: Boolean = false,
    val minTier: String? = null,
    val perUserLimit: Int = 1,
    val endsAt: Timestamp? = null,
)

data class HelpTopicDto(
    @DocumentId val id: String = "",
    val title: Map<String, String> = emptyMap(),
    val icon: String = "",
    val sortOrder: Int = 0,
)

data class HelpArticleDto(
    @DocumentId val id: String = "",
    val topicId: String = "",
    val title: Map<String, String> = emptyMap(),
    val body: Map<String, String> = emptyMap(),
    val keywords: Map<String, List<String>> = emptyMap(),
    val sortOrder: Int = 0,
)
```

**Why `@get:PropertyName("isActive")` on booleans:** Kotlin generates a getter named `isActive()` for a property `isActive`, and Firestore's mapper strips the `is` and looks for a field `active`. The annotation forces the name `isActive` both ways. Only Boolean fields starting with `is` need it.

---

## Check

- [ ] Every catalog doc in the seed has `isActive`, `sortOrder` and translated maps for `ar`, `en`, `fr`.
- [ ] Stock is only in `stores/{storeId}/inventory/{variantId}`.
- [ ] Prices are only in variants; `minPrice`/`discountPercent` on products are written by the trigger.
- [ ] Reviews use the reviewer `uid` as the doc id.
- [ ] Every brand has `sortName` and `initial`; 07.4 sorts on `sortName`.
- [ ] Tier-gated coupons have `minTier`; FREESHIP is `free_delivery` with `minSubtotal 80000`.
- [ ] The DTOs compile and map a seeded document with `toObject()`.
