# 02: Data Modeling Rules (Read Before Any Collection)

Every collection in files 03–06 follows these rules. When two docs seem to disagree, this file wins.

---

## Why

Firestore has no joins and no foreign keys. It is easy to copy data "to save a read", and then the copies drift apart: a product renamed in one place keeps its old name in another. This file decides **once**:

- where each fact lives (single source of truth),
- how documents point at each other (references by id),
- which copies we allow, why, and who keeps them in sync,
- the formats for money, time, enums and translated text.

---

## 1. Single source of truth

| Fact | The ONE place it lives | Everyone else… |
|---|---|---|
| Product name, description, images | `products/{productId}` | stores `productId` |
| Variant price, size, colour, barcode | `products/{productId}/variants/{variantId}` | stores `variantId` |
| Stock in a store, aisle, shelf | `stores/{storeId}/inventory/{variantId}` | reads it there |
| Customer profile | `users/{uid}` | stores `customerId` |
| Address | `users/{uid}/addresses/{addressId}` | stores `addressId` (orders take a **snapshot**, see § 4) |
| Cart contents | `carts/{uid}/items/{variantId}` | nobody copies the cart |
| Coupon rules | `coupons/{code}` | stores `code` |
| Order status | `orders/{orderId}.status` | listens to the order |
| Staff role + store | **Firebase Auth custom claims** (authority) + `staff/{uid}` (editable record) | see [06 § 1](06_team_collections.md#1-staffuid-and-custom-claims) |
| Courier position | `courierLocations/{courierId}` | listens to it |
| Theme, recent searches | phone (DataStore) | never in Firestore |
| Email verified? | Firebase Auth | never copied (auth guide § 4.1) |

**Counts are not stored when a query can count them.** "8 Wishlist" and "3 Coupons" on Account (11.1) use Firestore `count()` aggregation queries (one read per 1000 docs), not a counter field that can drift.

---

## 2. References by id

- A reference is a **string field** holding the other document's id: `brandId: "stride"`, `customerId: "Xk2…"`.
- We do **not** use the Firestore `DocumentReference` type. Plain ids are easier to map in Kotlin, to log, and to send in FCM payloads.
- Field naming: `<entity>Id` for one, `<entity>Ids` for a list (`productIds`).
- Many-to-many with a small, bounded list (≤ 200 items): store an id array on the side that is read (`collections/{id}.productIds`).
- Unbounded one-to-many: a **subcollection** or a query on the child (`orders where customerId == uid`). Never a growing array.

---

## 3. Document ids

| Kind | Id strategy | Example | Why |
|---|---|---|---|
| Owned by a user 1:1 | the `uid` | `users/{uid}`, `wallets/{uid}`, `carts/{uid}`, `couriers/{uid}`, `staff/{uid}` | Rules become `request.auth.uid == docId`, no query needed |
| Natural unique key | the key itself, lowercase | `coupons/WELCOME15` (upper, as typed), `referrals/AMIRA10` (upper), `brands/stride`, `categories/shoes` | Readable, prevents duplicates |
| Child keyed by its parent's id | that id | `stores/{storeId}/inventory/{variantId}`, `carts/{uid}/items/{variantId}`, `users/{uid}/favorites/{productId}`, `referrals/{code}/invites/{inviteeUid}` | Adding the same item twice updates instead of duplicating |
| Everything else | Firestore auto id | `orders/{autoId}`, `users/{uid}/addresses/{autoId}` | Avoids hotspots and guessable ids |
| Per-day aggregates | date `yyyy-MM-dd` in `Africa/Tunis` | `staff/{uid}/dailyStats/2026-10-28` | One doc per day, easy to fetch a week |

The **human order number** (`DK-2048`) is a field (`number`), not the doc id. See [05 § 1](05_order_collections.md#1-ordersorderid).

---

## 4. Intentional denormalization (the only copies allowed)

Anything not in this table must not be copied.

| Copy | Lives in | Source | Why we copy | Who writes it | How it stays correct |
|---|---|---|---|---|---|
| **Order item snapshot** (name, brand, options label, image, unit price, aisle/shelf, barcode) | `orders/{id}/items/{itemId}` | product + variant + inventory | **This is a historical record, not redundancy.** The receipt must show the price paid even if the product price changes tomorrow | `placeOrder` function, once | Never synced. Frozen on purpose |
| **Delivery address snapshot** | `orders/{id}.delivery.address` | `users/{uid}/addresses/{addressId}` | The courier must go where the order was sent, even if the customer edits or deletes the address | `placeOrder`, once | Frozen |
| **Customer contact snapshot** (first name, last-name initial, phone) | `orders/{id}.customer` | `users/{uid}` | Courier and picker can't read `users/{uid}` (privacy); they get only what they need ("Amira B.") | `placeOrder`, once | Frozen |
| **Favorite "as saved"** `savedPrice`, `savedInStock` | `users/{uid}/favorites/{productId}` | product `minPrice`, `inStockStoreIds` at save time | 11.5 "dropped to 215.000 TND", "Back in stock" tab need the **old** value to compare with | client, once at save | **Frozen history.** The live values are always read from the product |
| **Coupon end date** `endsAt` | `users/{uid}/coupons/{code}` | `coupons/{code}.endsAt` | 07.3 Available / Expired tabs and "Ends in 6 days" order with one query | `claimCoupon` / `onUserCreated` / campaign fn | End dates are fixed when published; the admin script that extends a coupon updates the copies in the same run |
| **Invitee name** `inviteeName` | `referrals/{code}/invites/{uid}` | invitee's `users/{uid}` | 11.8 "Sarra M."; the owner can't read other users' profiles | `applyReferralCode`, once | Frozen |
| **Bag short code** `shortCode` | `orders/{id}/bags/{bagId}` | — (generated) | Not a copy; listed so nobody adds a second "code" field elsewhere | `markPacked` | — |
| **Courier snapshot** (name, vehicle, plate, rating) | `orders/{id}.courier` | `staff/{uid}` + `couriers/{uid}` | The customer can't read staff docs; live tracking (10.3) shows these | `acceptOffer` function | Written when assigned. Rating shown is the value at assignment time, which is fine |
| **Product rating** `ratingAvg`, `ratingCount` | `products/{id}` | `products/{id}/reviews` | Every product card shows "4.9 (3.4k)". Averaging thousands of reviews per card is impossible | `onReviewWritten` trigger | Recomputed incrementally in a transaction on each review create/update/delete |
| **Product price summary** `minPrice`, `compareAtPrice`, `discountPercent` | `products/{id}` | `variants` | Cards, sorting "Under 200", filter "On sale" need one number per product | `onVariantWritten` trigger | Recomputed from all variants on each variant write |
| **Product stock flag** `inStockStoreIds` | `products/{id}` | `stores/*/inventory` | Hide sold-out products in list queries | `onInventoryWritten` trigger | Added/removed when quantity crosses 0 |
| **Brand** `followersCount`, `productCount`; **category** `productCount` | `brands/{id}`, `categories/{id}` | follows, products | "12.4k Followers", "2.4k items" | triggers | Incremented/decremented by triggers |
| **Order** `statusGroup` | `orders/{id}` | `status` | The My Orders tabs (All / Active / Delivered / Cancelled / Delivery failed) need one equality filter | status-change function | Set in the same write as `status` |
| **Courier** `visibleTo` (customer uids) | `courierLocations/{courierId}` | active orders | Lets rules allow a customer to read the location **only** of their own courier, without a query | `acceptOffer` / delivery completion | Set on assign, removed on delivered / returned / cancelled |
| **Wallet balances** `walletBalance`, `pointsBalance` | `wallets/{uid}` | `wallets/{uid}/transactions`, `wallets/{uid}/pointsLedger` | Shown on Account, Checkout, Payment methods | functions only | Updated in the **same transaction** that writes the ledger entry |
| **User counters** `ordersCount` | `users/{uid}` | orders | First-order coupon check, "12 Orders" | functions only | `ordersCount` in `placeOrder`'s transaction |
| **Tier** `spend12mMillimes`, `tier`, `nextTier`, `toNextTierMillimes` | `users/{uid}` | delivered orders of the last 12 months + `config/tiers` | "Gold member" (11.1), GOLD20 lock (07.3), checked by `placeOrder` without scanning orders | functions only | `completeDelivery` adds the order and may **upgrade** in its transaction; `nightlyTiers` recomputes the exact 12-month sum and is the only place a tier goes **down** (09 § 8d) |
| **Redelivery slot load** `count` | `stores/{storeId}/slotLoad/{date_HH}` | orders redelivered in that hour | Capacity check without a query | `resolveFailedDelivery` only | Incremented in the same transaction that books the slot |
| **Referral counters** `invitesCount`, `rewardedCount`, `earnedTotal` | `referrals/{code}` | `invites` subcollection | "Your invites · 2 of 10", the 10-invite cap check | functions only | Same transaction as the invite write / reward |
| **Courier cash** `cashInHand` | `couriers/{uid}` | delivered cash orders − `cashDeposits` | C03.4 "Cash in hand · 765.300 TND" | functions only | `completeDelivery` adds, `recordCashDeposit` subtracts, both in a transaction |
| **Stats** `dailyStats`, courier totals (`ratingAvg`, `deliveriesCount`, `todayEarnings`, `weekEarnings`) | `staff/{uid}/dailyStats` (picker), `couriers/{uid}/dailyStats` and `couriers/{uid}` (courier) | orders, earnings ledger | "My shift" and "Earnings" charts must not scan hundreds of orders | triggers | Incremented on each order completion |
| **Search record** (name, brand, subtitle, price summary, rating, `inStockStoreIds`, `isActive`) | Algolia index `products` | `products/{id}` | Firestore can't do text search ([14](14_search_algolia.md)) | the Algolia Firebase extension only | Synced on every product write. Used **only** to find product ids; the product page always reads Firestore |

Rule of thumb: **a copy is OK if it is frozen history, or if a function owns it and updates it in the same transaction as the source.**

---

## 5. Value formats

### 5.1 Money: `Long` in millimes

TND has **3 decimals**. 1 TND = 1000 millimes.

| Shown | Stored |
|---|---|
| `279.000 TND` | `279000` |
| `39.900 TND` | `39900` |
| `−40.000 TND` | `40000` in a field named `discount` (amount fields are never negative; the field name says the direction) |

**Why:** `Double` cannot represent `0.1` exactly. Sums of doubles drift (`0.1 + 0.2 = 0.30000000000000004`). Integers add exactly. Every money field ends in a noun, never a unit suffix: `price`, `subtotal`, `deliveryFee`, `total`.

**Points:** 100 points = 1 TND, so **1 point = 10 millimes** (`MILLIMES_PER_POINT = 10`, see [04 § 5](04_customer_collections.md#points-in-the-cart)). Points are stored as a plain count (`1324`), never converted to money in a document; "worth 13.240 TND" is computed on the phone.

Currency is **always TND** for now, so we don't store it per field. If a second currency ever appears, add `currency` on the order.

Kotlin helpers (`core/utils/money/Money.kt`):

```kotlin
package com.example.dukkanapp.core.utils.money

import java.text.NumberFormat
import java.util.Locale

/** Money in millimes (1 TND = 1000). Never use Double for money. */
@JvmInline
value class Millimes(val value: Long) {
    operator fun plus(other: Millimes) = Millimes(value + other.value)
    operator fun minus(other: Millimes) = Millimes(value - other.value)
    operator fun times(quantity: Int) = Millimes(value * quantity)

    companion object {
        val ZERO = Millimes(0)
    }
}

/**
 * "279.000" with Latin digits in every locale; the screen appends the localized currency label.
 * If the design-system docs already define `formatTnd`, keep that one and drop this; there must be one formatter.
 */
fun Millimes.formatTnd(locale: Locale = Locale.US): String {
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 3
        maximumFractionDigits = 3
        isGroupingUsed = true
    }
    return format.format(value / 1000.0) // display only; never compute with the Double
}
```

### 5.2 Time

| Kind | Type | Example field | Rule |
|---|---|---|---|
| A moment | `Timestamp` | `createdAt`, `placedAt`, `expiresAt` | Written with `FieldValue.serverTimestamp()` by clients; `Timestamp.now()` / `FieldValue.serverTimestamp()` in functions. Never the phone clock for anything that matters |
| A calendar date | `string` `yyyy-MM-dd` | `dateOfBirth` | A birthday has no time zone |
| A day bucket | `string` `yyyy-MM-dd` | `dailyStats` doc id | Computed in `Africa/Tunis` |
| A duration | `number` (seconds or minutes, said in the name) | `avgPickSeconds`, `etaMinutes` | Name carries the unit |

### 5.3 Enums: lowercase snake_case strings

`status: "ready_for_pickup"`, `paymentMethod: "cash"`, `role: "courier"`.

**Why strings and not numbers:** readable in the console and in logs, and adding a value never shifts others. In Kotlin, map with a `fromWire(...)` function that returns an `Unknown` value for anything new, so an old app never crashes on a new status:

```kotlin
enum class OrderStatus(val wire: String) {
    Placed("placed"), Picking("picking"), Packed("packed"), ReadyForPickup("ready_for_pickup"),
    CourierAssigned("courier_assigned"), PickedUp("picked_up"), Arriving("arriving"),
    Delivered("delivered"), Returning("returning"), Failed("failed"),
    AwaitingCustomerPickup("awaiting_customer_pickup"), Collected("collected"),
    Cancelled("cancelled"), Unknown("unknown");

    companion object {
        fun fromWire(value: String?): OrderStatus = entries.firstOrNull { it.wire == value } ?: Unknown
    }
}
```

### 5.4 Translated text: a map `{ar, en, fr}`

```json
"name": { "en": "Shoes", "fr": "Chaussures", "ar": "أحذية" }
```

Kotlin DTO type: `Map<String, String>`. Pick with a helper that falls back to English:

```kotlin
fun Map<String, String>.localized(languageCode: String): String =
    this[languageCode] ?: this["en"] ?: values.firstOrNull().orEmpty()
```

**Why a map in the same document:** one read gives all languages, and switching language in Settings (11.3) needs no new reads. Brand names (`STRIDE`, `SONIQ`) and product model names are **not** translated; they stay a plain `string`.

### 5.5 Booleans and optional fields

- Booleans start with `is`/`has` only when it reads naturally (`isActive`, `isDefault`, `isFragile`).
- Optional value = field absent **or** `null`. DTOs default it to `null`. Rules must accept both.

### 5.6 Locations

`GeoPoint` (`location`), plus `geohash` (string, precision 9) when we need "near me" queries (couriers, stores). Compute the geohash with the `geofire-common` library in functions.

---

## 6. Delete policy

| Data | Delete type | Why |
|---|---|---|
| Products, brands, categories, coupons, banners | **Soft**: `isActive = false` | Old orders, reviews and links still point at them |
| Addresses | **Hard** (client) | Orders keep a snapshot; nothing else points at an address. If it was the default, `defaultAddressId` is cleared by the client in the same batch |
| Cart items, favorites, notifications | **Hard** (client) | Personal, no history value |
| Orders, ledgers (wallet, points, earnings), events, issues, referrals, cash deposits | **Never** | Legal and accounting history |
| Devices | **Hard** on sign-out; function cleans stale ones | Auth guide § 4.2 |
| A whole user (account deletion) | Function `deleteAccount`: deletes the Auth user, profile, subcollections, cart; **anonymizes** orders (keeps totals, removes name/phone) | GDPR-style right to erasure without breaking accounting |

---

## 7. Size and shape limits

| Limit | Value | Our rule |
|---|---|---|
| Document size | 1 MiB | No unbounded arrays. Arrays ≤ 200 items (`productIds`), else subcollection |
| Writes to one document | ~1 per second sustained | Hot counters (order number) use a transaction on one doc; fine at our scale. If we exceed ~1 order/second, switch to sharded counters |
| Index entries per document | 40,000 | Exempt large text fields (`description`) from indexing (08) |
| `in` / `array-contains-any` | 30 values | My Orders "Active" uses `statusGroup == "active"`, not `status in [...]` |

---

## 8. Who may write what

| Writer | May write | Never |
|---|---|---|
| **Customer app** | own profile fields, addresses, cart (incl. `pointsToUse`), favorites, followed brands, stock alerts, notification `readAt`, devices, product review (own, after delivery), waitlist | prices, totals, stock, status, `wallets`, coupons, referrals, anything on `orders` |
| **Picker (team app)** | `orders/{id}/items/{itemId}` pick fields, for orders of **their store** | totals, refunds, status, issues (callable) |
| **Courier (team app)** | own `couriers/{uid}` status/vehicle fields, own `courierLocations/{uid}` position fields | offers, fees, earnings, `cashInHand`, status, issues (callable) |
| **Cloud Functions (Admin SDK)** | everything; bypasses rules | — |
| **Admin / manager** | catalog, coupons, staff, shifts, via an admin web app or console scripts that use functions | — |

Rules in [07](07_security_rules.md) enforce this table.

---

## Check

- [ ] Every money field is a `Long` in millimes.
- [ ] Every time a client writes a moment, it uses `FieldValue.serverTimestamp()`.
- [ ] Every enum has a Kotlin `fromWire` with an `Unknown` fallback.
- [ ] Every copied field appears in the § 4 table, with a named writer.
- [ ] No array can grow without bound.
