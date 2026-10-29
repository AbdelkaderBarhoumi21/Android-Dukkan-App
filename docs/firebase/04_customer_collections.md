# 04: Customer Collections

`users/{uid}` (extended) and its subcollections, `wallets/{uid}` (money and points), `referrals/{code}` and `carts/{uid}`.

> **Supersedes section 4 of the auth guide.** All existing fields keep their names: `email`, `displayName`, `photoUrl`, `phoneNumber`, `authProviders`, `languageCode`, `marketingNotificationsEnabled`, `createdAt`, `updatedAt`, `lastLoginAt`. The devices subcollection is unchanged.

"Written by": **client** = customer app (rules whitelist), **fn** = Cloud Function only, **sync** = the auth feature's sign-in sync.

---

## Why

- Everything that belongs to one customer **and that the customer may edit** lives under `users/{uid}`. Rules become one line (`isOwner(uid)`), and deleting an account means deleting one tree (plus `wallets/{uid}` and `carts/{uid}`).
- **Money and points live in `wallets/{uid}`**, a separate document that **only functions write** (Figma map: `wallets/{uid}` "balance, refunds, points"). Before, `walletBalance` and `pointsBalance` sat inside `users/{uid}` next to client-editable fields like `firstName`, so every profile rule needed a long "you may not touch these" list. With a separate doc:
  - `users/{uid}` rules only list what the client **may** change.
  - `wallets/{uid}` rules are one line: owner reads, nobody writes.
  - The Account screen (11.1) and Payment methods (11.6) listen to one small doc for "45 TND", "1,324 points · worth 13.240 TND", without re-downloading the profile.
- **The cart is a top-level `carts/{uid}`**, not a subcollection of the user. It changes many times per minute while shopping, and the Home bottom bar listens to it ("3 · 39.900 TND"). Keeping it out of `users/{uid}` means profile listeners don't fire on every +/-, and functions can query carts (abandoned-cart pushes) without collection-group queries.
- **Every balance change has a ledger entry** written in the same transaction (02 § 4).

---

## 1. `users/{uid}` (extended)

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `email` | string | ✓ | `"amira.bensalah@gmail.com"` | sync | Auth guide |
| `displayName` | string? | | `"Amira Ben Salah"` | sync, client | Auth guide. Rebuilt by the client as `firstName + " " + lastName` whenever those change |
| `firstName` | string? | | `"Amira"` | client | "Good morning, Amira", "You're all set, Amira!" (02.5), Edit profile (11.2), "Hi Amira, how can we help?" (11.7) |
| `lastName` | string? | | `"Ben Salah"` | client | Edit profile |
| `photoUrl` | string? | | Storage URL | sync, client | Avatar |
| `phoneNumber` | string? | | `"+21622345678"` | client | Edit profile "Phone (optional)". **E.164** format (`+216` + 8 digits; the app normalizes "22 345 678"). Required at checkout (`PHONE_REQUIRED`): the courier calls it and the WhatsApp door message goes to it |
| `whatsappOptIn` | bool | ✓ | `true` | client | Consent for WhatsApp **delivery** messages ("courier at your door", 09 § 8c). Set to `true` **only when the customer ticks** the unchecked box "Send me delivery updates on WhatsApp" (checkout 09.1 and 11.2 under the phone field), or turns on the switch in 11.3 → Notifications "WhatsApp delivery messages". Explicit opt-in is the safe reading of Meta's policy and data-protection law. Separate from `pushPrefs` so a customer can keep push and stop WhatsApp. Default `false` |
| `dateOfBirth` | string? | | `"1998-03-14"` | client | Edit profile. Calendar date, no time zone |
| `authProviders` | array<string> | ✓ | `["google.com"]` | sync | "Verified with Google" (11.2) |
| `languageCode` | string | ✓ | `"en"` | sync, client | Language row (11.1, 11.3) |
| `marketingNotificationsEnabled` | bool | ✓ | `false` | client | **Email** marketing consent: "Send me deals by email" (02.3). Name kept from the auth guide |
| `pushPrefs` | map | ✓ | `{orderUpdates:true, flashDeals:true, priceDrops:true}` | client | App settings toggles (11.3). Order updates can be muted but are on by default |
| `defaultAddressId` | string? | | `"a1"` | client | "Default" badge (03.4), first address at checkout |
| `defaultPaymentMethod` | string | ✓ | `cash` / `wallet` / `card:{methodId}` | client | "Default for new orders" (11.6). Default `cash`. `card:` only once Konnect is live |
| `referralCode` | string | ✓ | `"AMIRA10"` | fn (on create) | "Your code AMIRA10" (11.8). Same string as the `referrals/{code}` doc id |
| `referredBy` | string? | | `"SARRA10"` | fn (`applyReferralCode`) | The code this user joined with. Set once |
| `tier` | string | ✓ | `"gold"` | fn | "Gold member" (11.1), "Gold members only · Reach Gold to unlock" (07.3). Values: `member`, `silver`, `gold` (rule below) |
| `spend12mMillimes` | number | ✓ | `1612400` | fn | Spend on delivered orders in the last 12 months (allowed copy, 02) |
| `nextTier` | string? | | `null` / `"gold"` | fn | Hint "x TND to Gold" (null when already Gold) |
| `toNextTierMillimes` | number | ✓ | `0` | fn | Same hint, amount left |
| `tierUpdatedAt` | Timestamp? | | | fn | When the tier last changed |
| `freeDeliveriesLeft` | number | ✓ | `3` | fn | "3 free deliveries" welcome gift (02.5) |
| `ordersCount` | number | ✓ | `12` | fn | "12 Orders", first-order coupon check |
| `createdAt` | Timestamp | ✓ | | sync (once) | Auth guide |
| `updatedAt` | Timestamp | ✓ | | sync, client | Auth guide |
| `lastLoginAt` | Timestamp | ✓ | | sync | Auth guide |

**Moved out (was here before this update):** `pointsBalance` and `walletBalance` now live in `wallets/{uid}` (§ 7).

### Customer tiers (decided: total spend)

| Tier | 12-month spend | Shown as |
|---|---|---|
| `member` | below 500 TND | no badge |
| `silver` | from 500 TND | "Silver member" |
| `gold` | from 1,500 TND | "Gold member" (11.1), unlocks GOLD20 (07.3) |

- **Spend** = sum of `totals.total` of **delivered** or **collected** orders in the **last 12 months**. Cancelled and failed orders never count; refunds after delivery (`orders/{id}.refundedMillimes`) are subtracted.
- **Thresholds** live in `config/tiers` (`{silverMillimes: 500000, goldMillimes: 1500000, windowMonths: 12}`), readable by everyone signed in, writable by nobody from the app. Marketing can change them without an app update.
- **Up immediately, down only at night:** `completeDelivery` adds the order to `spend12mMillimes` and upgrades the tier in the same transaction. Only the nightly job `nightlyTiers` recomputes the exact 12-month window and may lower a tier. So a customer never loses Gold in the middle of an order or checkout.
- All tier fields are **function-only**: the client can't write them (rules, 07). Code in 09 § 8d.

**Not stored (single source elsewhere):**

| Not stored | Because |
|---|---|
| Favorites count, coupon count, active orders count | `count()` queries (02 § 1) |
| Theme (light/dark/system) | Per phone, DataStore (11.3) |
| Recent searches, "Clear search history", recently viewed | Per phone, DataStore (05b.1). See [14 § 6](14_search_algolia.md#6-empty-query-screen-05b1) |
| Email verified | Firebase Auth |
| FCM token | `devices` subcollection |

**Who creates the server fields?** The auth sync creates the doc with the auth guide's fields. A Firestore trigger `onUserCreated` (09) then adds `referralCode`, `tier: "member"`, `spend12mMillimes: 0`, `nextTier: "silver"`, `toNextTierMillimes: 500000`, `freeDeliveriesLeft: 3`, `ordersCount: 0`, `defaultPaymentMethod: "cash"`, `pushPrefs` defaults, creates `wallets/{uid}` with zero balances, creates `referrals/{code}`, and gives the `WELCOME15` coupon. Until the trigger runs (about a second), the client must treat missing values as defaults; the DTO defaults handle that.

```json
{
  "email": "amira.bensalah@gmail.com",
  "displayName": "Amira Ben Salah",
  "firstName": "Amira", "lastName": "Ben Salah",
  "photoUrl": null, "phoneNumber": "+21622345678", "dateOfBirth": "1998-03-14",
  "authProviders": ["google.com"],
  "languageCode": "en",
  "marketingNotificationsEnabled": false,
  "pushPrefs": { "orderUpdates": true, "flashDeals": true, "priceDrops": true },
  "defaultAddressId": "a1",
  "defaultPaymentMethod": "cash",
  "whatsappOptIn": true,
  "referralCode": "AMIRA10", "referredBy": null,
  "tier": "gold", "spend12mMillimes": 1612400, "nextTier": null, "toNextTierMillimes": 0,
  "freeDeliveriesLeft": 3, "ordersCount": 12,
  "createdAt": "<Timestamp>", "updatedAt": "<Timestamp>", "lastLoginAt": "<Timestamp>"
}
```

### `users/{uid}/private/pushQuota`

Written by functions only, never read by the client. It keeps "Flash deals, max 3 per week" true.

| Field | Type | Example |
|---|---|---|
| `weekKey` | string | `"2026-W44"` |
| `flashDealsSent` | number | `2` |

**Why a separate private doc:** the user can read `users/{uid}`. Server bookkeeping does not belong in the profile the client downloads.

---

## 2. `users/{uid}/addresses/{addressId}`

**Id:** auto. Screens 03.1–03.5, checkout (09.1). Empty state 14.3 "No saved addresses" = the query returns nothing.

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `label` | string | ✓ | `home` / `work` / `family` / `other` | "Save as" chips (03.3) |
| `customLabel` | string? | | `"Mom's place"` | Shown instead of the label when set |
| `street` | string | ✓ | `"Rue du Lac Biwa 12"` | Reverse-geocoded, editable |
| `area` | string | ✓ | `"Les Berges du Lac"` | |
| `city` | string | ✓ | `"Tunis"` | |
| `postalCode` | string? | | `"1053"` | |
| `location` | GeoPoint | ✓ | `36.8432, 10.2731` | The pin (03.2) |
| `zoneId` | string | ✓ | `"lac"` | Computed on the phone from zone polygons; `placeOrder` re-checks |
| `buildingType` | string | ✓ | `apartment` / `house` / `office` | |
| `building` | string? | | `"B12"` | |
| `floor` | string? | | `"3"` | "Apt · Floor 3" |
| `apartment` | string? | | `"7"` | |
| `riderNote` | string? | | `"Blue door"` | Free text |
| `dropOff` | string | ✓ | `ring_bell` / `leave_at_door` / `call_me` | Chips (03.3) |
| `voiceNotePath` | string? | | `users/{uid}/voice/{addressId}.m4a` | 0:08 voice note (03.3). Storage path, see 11 |
| `recipientName` | string? | | | "Ordering for someone else" |
| `recipientPhone` | string? | | | Same |
| `createdAt` / `updatedAt` | Timestamp | ✓ | server | |

**Default address:** stored once in `users/{uid}.defaultAddressId`, **not** as an `isDefault` flag on each address. With a flag, changing the default means updating two docs and you can end up with two defaults. "Set as default address" (03.5) updates one field.

**Delete** (03.5): hard delete. If it was the default, clear `defaultAddressId` in the **same batch**.

---

## 3. `users/{uid}/devices/{installationId}`

**Unchanged from the auth guide § 4.2.** The push feature fills `fcmToken`. See [10](10_fcm_push_notifications.md).

---

## 4. Favorites, followed brands, stock alerts

### `users/{uid}/favorites/{productId}` (wishlist, 11.5)

**Renamed from `wishlist`** to match the Figma Firebase map (`users/{uid}/favorites`). The UI still calls it "Wishlist"; only the collection name changed.

| Field | Type | Req | Example | Written by | Why |
|---|---|---|---|---|---|
| `productId` | string | ✓ | `"cherry-mini-bag"` | client | Same as the id. Needed as a **field** for the collection-group query of the price-drop function |
| `savedPrice` | number (millimes) | ✓ | `259000` | client (once, at save) | **Allowed copy, frozen** (02 § 4). The product's `minPrice` when the heart was tapped. "Cherry Mini Bag dropped to 215.000 TND" = live `minPrice < savedPrice` |
| `savedInStock` | bool | ✓ | `false` | client (once) | Was the product in the cart store's `inStockStoreIds` when saved? "Back in stock" tab = `savedInStock == false` and in stock now |
| `addedAt` | Timestamp | ✓ | server | client | List order; the heart on cards |

**Why these two copies are fine:** they are not "the price" or "the stock". They are history ("what was true when you saved it"). The current price and stock are always read from `products/{id}`, so nothing can go stale. Nobody updates them after creation, so no sync logic is needed.

**Tabs (11.5):** "All · 8", "On sale · 3", "Back in stock · 1".
- All: the query on `favorites` ordered by `addedAt desc`.
- On sale / Back in stock: **filtered on the phone** after the product docs are loaded (`product.discountPercent > 0`, or `!savedInStock && storeId in product.inStockStoreIds`). A wishlist has a few dozen items at most, so a client filter costs no extra reads, and Firestore can't compare a field of one document with a field of another anyway.

**Price-drop push:** `onVariantWritten` (09) recomputes `minPrice`; if it went down, a collection-group query `favorites where productId == id` finds who saved it, and a push "Price drop on your wishlist" is sent (respecting `pushPrefs.priceDrops`). The inbox entry is written too (11.4).

### `users/{uid}/followedBrands/{brandId}`

`followedAt: Timestamp`, `brandId: string`. The "Follow" button on 05.3. A trigger updates `brands/{id}.followersCount`.

### `users/{uid}/stockAlerts/{variantId}`

| Field | Type | Example |
|---|---|---|
| `productId` | string | `"air-runner-2"` |
| `variantId` | string | `"ar2-blue-44"` |
| `storeId` | string | `"lac2"` |
| `createdAt` | Timestamp | |

"Sold out — notify me" (06.2). When inventory goes from 0 to > 0, a function sends a push and deletes the alert.

---

## 5. `carts/{uid}` + `carts/{uid}/items/{variantId}`

### `carts/{uid}`

| Field | Type | Req | Example | Written by | Why |
|---|---|---|---|---|---|
| `storeId` | string | ✓ | `"lac2"` | client | "4 items · Dukkan Lac 2". From the selected address's zone. Changing address to another zone asks the user to move or clear the cart |
| `addressId` | string? | | `"a1"` | client | Selected "Deliver to" |
| `couponCode` | string? | | `"WELCOME15"` | client | Coupon row; `placeOrder` validates |
| `pointsToUse` | number | ✓ | `1250` / `0` | client | "Save 12.500 TND · Use 1,250 points" toggle (09.1). Figma map: "carts/{uid} · items, coupon, points". `placeOrder` re-checks it against `wallets/{uid}.pointsBalance` and the order cap |
| `updatedAt` | Timestamp | ✓ | server | client | Abandoned-cart push (later) |

### `carts/{uid}/items/{variantId}`

| Field | Type | Req | Example | Why |
|---|---|---|---|---|
| `productId` | string | ✓ | `"air-runner-2"` | Load product name/image |
| `variantId` | string | ✓ | `"ar2-blue-42"` | Same as the doc id, kept as a field for easy mapping |
| `quantity` | number | ✓ | `1` | Stepper. Rules: 1–20 |
| `addedAt` | Timestamp | ✓ | server | Line order |

**No price, no name in the cart.** The cart screen reads products, variants and the store inventory (all cached), so it always shows the current price and stock. The Home bottom bar total ("39.900 TND") is computed on the phone from cached variant prices. `placeOrder` computes the real total on the server.

**Item count = units.** "4 items" in 08.1 counts units (Tee × 2 counts twice): `items.sumOf { it.quantity }`.

**Why the doc id is the variant id:** tapping "Add" twice cannot create two lines; the second tap updates `quantity` (use `FieldValue.increment(1)`).

**Offline:** carts are written with normal Firestore writes, so they work offline and sync later. That is why Figma 12.4 can say "Your cart is saved on this phone".

### Points in the cart

The constant is **100 points = 1 TND**, so **1 point = 10 millimes** (02 § 5.1). One place in Kotlin and one in TypeScript:

```kotlin
// core/common/money/Points.kt
const val MILLIMES_PER_POINT = 10L

fun pointsToMillimes(points: Long): Long = points * MILLIMES_PER_POINT
```

```typescript
// firebase/functions/src/shared/money.ts
export const MILLIMES_PER_POINT = 10;
```

Checkout example (09.1, the fixed Figma numbers for #DK-2048):

| Line | Millimes | Shown |
|---|---|---|
| Subtotal | 817800 | 817.800 TND |
| Coupon WELCOME15 (15 %, max 40 TND) | −40000 | −40.000 TND |
| Points 1,250 × 10 | −12500 | "Save 12.500 TND · Use 1,250 points" |
| Delivery | 0 | Free (over 150 TND) |
| **Total** | **765300** | **765.300 TND** |

Cash: "You'll hand 770 TND · change 4.700" → `cashTendered = 770000`, change computed `770000 − 765300 = 4700`.

---

## 6. `users/{uid}/notifications/{notificationId}` (inbox, 11.4)

**Id:** auto. Written by functions at the same time as the push. Empty state 14.2 "You're all caught up" = no documents.

| Field | Type | Req | Example | Written by | Why |
|---|---|---|---|---|---|
| `category` | string | ✓ | `orders` / `offers` / `account` | fn | Tabs "All · Orders · Offers · Account" |
| `type` | string | ✓ | `order_on_the_way`, `delivery_failed`, `refund_issued`, `flash_sale`, `price_drop`, `coupon_expiring`, `referral_reward`, `welcome` | fn | Icon and deep link (10 § 4) |
| `title` | string | ✓ | `"Karim is on the way"` | fn | Already in the user's language (rendered by the function) |
| `body` | string | ✓ | `"Order #DK-2048 · arriving in 12 min"` | fn | |
| `imageUrl` | string? | | | fn | |
| `deepLink` | string | ✓ | `"dukkan://orders/abc123"` | fn | Tap target |
| `createdAt` | Timestamp | ✓ | | fn | "Today 12:31" |
| `readAt` | Timestamp? | | | **client** (only this field) | "2 unread" |

**Why the text is stored already translated:** the inbox is history. A message reads the way it was sent, and the client doesn't need a template engine.

Unread count: `count()` query `where readAt == null`.

---

## 7. `wallets/{uid}`: wallet, points and their ledgers

**Id:** the user's uid. **Written only by functions.** Created by `onUserCreated` with zero balances.

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `walletBalance` | number (millimes) | ✓ | `45000` | "45 TND Wallet" (11.1), "Balance 45.000 TND · refunds land here" (11.6), "Balance 45.000 TND — not enough" (09.2) |
| `pointsBalance` | number | ✓ | `1324` | "+74 points earned · 1,324 total" (09.3), "1,324 points · worth 13.240 TND" (11.6, 07.3) |
| `updatedAt` | Timestamp | ✓ | | |

"Worth 13.240 TND" is **computed** (`pointsToMillimes(1324) = 13240`), not stored.

### `wallets/{uid}/transactions/{txId}` (was `users/{uid}/walletTransactions`)

| Field | Type | Example | Why |
|---|---|---|---|
| `amount` | number (millimes, signed) | `+62400` / `-765300` | |
| `reason` | string | `refund` / `referral_reward` / `order_payment` / `top_up` / `adjustment` | "History" (11.6) |
| `orderId` | string? | the order's doc id | A wallet refund of a cancelled wallet-paid order ("→ Dukkan wallet · the refund is instant"). Figma's 10.6 example #DK-1990 is cash, so it creates **no** transaction |
| `referralCode` | string? | `"AMIRA10"` | For `referral_reward` |
| `balanceAfter` | number | `45000` | Audit trail; makes bugs easy to find |
| `createdAt` | Timestamp | | |

### `wallets/{uid}/pointsLedger/{entryId}` (was `users/{uid}/pointsLedger`)

| Field | Type | Example | Why |
|---|---|---|---|
| `delta` | number | `+74` / `-1250` | Earned or spent |
| `reason` | string | `order_earned` / `order_redeemed` / `review_bonus` / `refund` / `expired` | |
| `orderId` | string? | | |
| `balanceAfter` | number | `1324` | |
| `createdAt` | Timestamp | | |

**Rule:** the function that writes a ledger entry updates `wallets/{uid}.walletBalance` / `pointsBalance` **in the same transaction**. That is what makes the balance copy safe (02 § 4).

```json
// wallets/amiraUid
{ "walletBalance": 45000, "pointsBalance": 1324, "updatedAt": "<Timestamp>" }
```

---

## 8. Payment methods (11.6) and `users/{uid}/paymentMethods/{methodId}`

**What 11.6 shows for v1:**

| Row | Source |
|---|---|
| Dukkan wallet card "45.000 TND + 1,324 points · worth 13.240 TND", "History" | `wallets/{uid}` + `transactions` |
| "Default for new orders" radio: Cash on delivery / Dukkan wallet | `users/{uid}.defaultPaymentMethod` (client writes) |
| "Credit or debit card · Visa, Mastercard and e-Dinar via Konnect · Soon" | Static. Shown disabled until Remote Config `cardPaymentsEnabled` is true |

Cash on delivery and wallet are not payment-method docs; they are always-available choices.

**After launch (Konnect):** saved cards go in `users/{uid}/paymentMethods/{methodId}`:

| Field | Type | Example | Why |
|---|---|---|---|
| `provider` | string | `"konnect"` | Which payment provider issued the token |
| `providerToken` | string | `"pm_…"` | The provider's saved-card id. **Not a card number** |
| `brand` | string | `"visa"` / `"mastercard"` / `"edinar"` | Icon |
| `last4` | string | `"4242"` | Label |
| `expMonth` / `expYear` | number | `12` / `2028` | Hide expired cards |
| `createdAt` | Timestamp | | |

Written **only by a function** after Konnect confirms the card. The card number, CVC and expiry never touch our servers or Firestore (PCI rules). The app opens Konnect's own card form.

---

## 9. Coupons: `users/{uid}/coupons/{code}` (07.3 "My coupons")

Coupons this customer has in their pocket: "Your coupons · 3 ready to use" (07.1), "Coupons for you" (04.2), coupon sheet (08.2), and the **My coupons** screen (07.3) with tabs **Available · 3 / Used · 1 / Expired · 2**.

| Field | Type | Example | Written by | Why |
|---|---|---|---|---|
| `code` | string | `"WELCOME15"` | fn | Same as id |
| `source` | string | `welcome` / `referral` / `claimed` / `campaign` | fn | `claimed` = typed in "Enter a promo code → Add" |
| `status` | string | `available` / `used` / `expired` | fn | The three tabs |
| `endsAt` | Timestamp | | fn | **Allowed copy** of `coupons/{code}.endsAt` (02 § 4), so the Available/Expired tabs and "Ends in 6 days" sort with one query. Coupon end dates don't change after publishing; if an admin extends one, the admin script updates these copies |
| `usedOrderId` | string? | | fn | |
| `usedAt` | Timestamp? | | fn | "Used" tab order |
| `assignedAt` | Timestamp | | fn | |

The other rules (discount, minimum, scope, `minTier`) are **read from `coupons/{code}`**, not copied. A redemption record also goes to `coupons/{code}/redemptions/{uid}` (03 § 10).

**How each 07.3 ticket gets its state:**

| Ticket | Data | State |
|---|---|---|
| WELCOME15 "Up to 40 TND · new customers · Ends in 6 days" | `firstOrderOnly`, `maxDiscount 40000` | Apply |
| DUKKAN50 "50 TND off over 300 TND · All categories · once per account" | `type fixed`, `value 50000`, `minSubtotal 300000`, `perUserLimit 1`, `scope {}` | Apply |
| FREESHIP "Free express delivery · Orders over 80 TND" | `type free_delivery`, `minSubtotal 80000` | Apply |
| GOLD20 "Gold members only · Reach Gold to unlock" | `minTier "gold"` | **Locked** when `users/{uid}.tier` is below gold |

**Expiry:** a scheduled function (09, daily) sets `status = expired` on `users/*/coupons` where `endsAt < now` and `status == available`, and sends `coupon_expiring` pushes 48 h before the end.

**"Enter a promo code → Add":** calls the `claimCoupon` callable (09). It checks the code exists, is active, is not expired, the tier and first-order rules, then writes `users/{uid}/coupons/{code}` with `source: claimed`. The client never writes here.

---

## 10. Referrals: `referrals/{code}` + `invites` (11.8)

"Give 10 TND, get 10 TND · Friends get 10 TND off their first order over 50 TND · You get 10 TND in your wallet when it's delivered · Your invites 2 of 10 · Max 10 invites · 100 TND".

### `referrals/{code}`

**Id:** the code itself (`"AMIRA10"`), so a typed code is a direct `get`, and two users can never own the same code. Top-level because the **invitee** looks it up by code, not by owner.

| Field | Type | Req | Example | Why |
|---|---|---|---|---|
| `code` | string | ✓ | `"AMIRA10"` | Same as id |
| `ownerUid` | string | ✓ | `"amiraUid"` | Who gets rewarded |
| `rewardMillimes` | number | ✓ | `10000` | "get 10 TND" (to the owner, wallet) |
| `inviteeDiscountMillimes` | number | ✓ | `10000` | "Friends get 10 TND off" |
| `minFirstOrderMillimes` | number | ✓ | `50000` | "first order over 50 TND" |
| `maxInvites` | number | ✓ | `10` | "Max 10 invites · 100 TND" |
| `invitesCount` | number | ✓ | `2` | "Your invites · 2 of 10" |
| `rewardedCount` | number | ✓ | `1` | |
| `earnedTotal` | number (millimes) | ✓ | `10000` | |
| `createdAt` | Timestamp | ✓ | | |

The reward values sit on the doc (not as constants) so a campaign can raise them for new codes without a release; existing codes keep their promise.

### `referrals/{code}/invites/{inviteeUid}`

| Field | Type | Example | Why |
|---|---|---|---|
| `inviteeName` | string | `"Sarra M."` | **Allowed copy, frozen** (first name + initial at join time). The owner can't read other users' profiles |
| `status` | string | `pending` / `rewarded` / `expired` | "Joined · waiting for first order · Pending", "Ordered on 22 Oct · +10 TND" |
| `joinedAt` | Timestamp | | |
| `firstOrderId` | string? | | |
| `rewardedAt` | Timestamp? | | |

**Flow:**
1. `onUserCreated` creates `referrals/{CODE}` for every new user (code = first name in capitals + digits, retried in a transaction until free).
2. A new user types a friend's code on sign-up or later → `applyReferralCode` callable: code exists, not their own, `users/{uid}.referredBy` is empty, `ordersCount == 0`, `invitesCount < maxInvites`. It sets `referredBy`, creates the invite (`pending`), increments `invitesCount`, and gives the invitee a private `REF-{CODE}` coupon (`fixed 10000`, `minSubtotal 50000`, `firstOrderOnly`).
3. When the invitee's **first** order is delivered and its subtotal ≥ 50 TND, `onOrderStatusChanged` → `rewardReferral` credits the owner's wallet (ledger `referral_reward`) and marks the invite `rewarded`, all in one transaction. Stops once `rewardedCount × rewardMillimes` reaches 100 TND.

**Nobody writes `referrals` from the client.** The owner reads their own code and invites (rules check `ownerUid`).

---

## 11. Kotlin DTOs

```kotlin
package com.example.dukkanapp.features.account.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/** Extends the auth feature's UserDto. Keep one class: move this into the shared user data package. */
data class UserDto(
    val email: String = "",
    val displayName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val authProviders: List<String> = emptyList(),
    val languageCode: String = "",
    val marketingNotificationsEnabled: Boolean = false,
    val pushPrefs: Map<String, Boolean> = emptyMap(),
    val defaultAddressId: String? = null,
    val defaultPaymentMethod: String = "cash",
    val referralCode: String = "",
    val referredBy: String? = null,
    val whatsappOptIn: Boolean = false,
    val tier: String = "member",
    val spend12mMillimes: Long = 0,
    val nextTier: String? = null,
    val toNextTierMillimes: Long = 0,
    val tierUpdatedAt: Timestamp? = null,
    val freeDeliveriesLeft: Int = 0,
    val ordersCount: Long = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastLoginAt: Timestamp? = null,
)

data class WalletDto(
    val walletBalance: Long = 0,
    val pointsBalance: Long = 0,
    val updatedAt: Timestamp? = null,
)

data class WalletTransactionDto(
    @DocumentId val id: String = "",
    val amount: Long = 0,
    val reason: String = "",
    val orderId: String? = null,
    val referralCode: String? = null,
    val balanceAfter: Long = 0,
    val createdAt: Timestamp? = null,
)

data class AddressDto(
    @DocumentId val id: String = "",
    val label: String = "home",
    val customLabel: String? = null,
    val street: String = "",
    val area: String = "",
    val city: String = "",
    val postalCode: String? = null,
    val location: GeoPoint? = null,
    val zoneId: String = "",
    val buildingType: String = "apartment",
    val building: String? = null,
    val floor: String? = null,
    val apartment: String? = null,
    val riderNote: String? = null,
    val dropOff: String = "ring_bell",
    val voiceNotePath: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class FavoriteDto(
    @DocumentId val id: String = "",
    val productId: String = "",
    val savedPrice: Long = 0,
    val savedInStock: Boolean = true,
    val addedAt: Timestamp? = null,
)

data class CartDto(
    val storeId: String = "",
    val addressId: String? = null,
    val couponCode: String? = null,
    val pointsToUse: Long = 0,
    val updatedAt: Timestamp? = null,
)

data class CartItemDto(
    @DocumentId val variantId: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val addedAt: Timestamp? = null,
)

data class InboxNotificationDto(
    @DocumentId val id: String = "",
    val category: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val deepLink: String = "",
    val createdAt: Timestamp? = null,
    val readAt: Timestamp? = null,
)

data class PaymentMethodDto(
    @DocumentId val id: String = "",
    val provider: String = "",
    val brand: String = "",
    val last4: String = "",
    val expMonth: Int = 0,
    val expYear: Int = 0,
)

data class UserCouponDto(
    @DocumentId val code: String = "",
    val source: String = "",
    val status: String = "available",
    val endsAt: Timestamp? = null,
    val usedOrderId: String? = null,
    val usedAt: Timestamp? = null,
    val assignedAt: Timestamp? = null,
)

data class ReferralDto(
    @DocumentId val code: String = "",
    val ownerUid: String = "",
    val rewardMillimes: Long = 0,
    val inviteeDiscountMillimes: Long = 0,
    val minFirstOrderMillimes: Long = 0,
    val maxInvites: Int = 0,
    val invitesCount: Int = 0,
    val rewardedCount: Int = 0,
    val earnedTotal: Long = 0,
)

data class ReferralInviteDto(
    @DocumentId val inviteeUid: String = "",
    val inviteeName: String = "",
    val status: String = "pending",
    val joinedAt: Timestamp? = null,
    val firstOrderId: String? = null,
    val rewardedAt: Timestamp? = null,
)
```

`providerToken` is deliberately missing from `PaymentMethodDto`: the app never needs it. Functions pass it to the provider.

---

## 12. Writing examples

**Add to cart** (06.1 "Add · 279.000 TND"):

```kotlin
suspend fun addToCart(uid: String, storeId: String, productId: String, variantId: String): Unit = guardException {
    val cartRef = firestore.collection("carts").document(uid)
    firestore.batch()
        .set(cartRef, mapOf("storeId" to storeId, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        .set(
            cartRef.collection("items").document(variantId),
            mapOf(
                "productId" to productId,
                "variantId" to variantId,
                "quantity" to FieldValue.increment(1),
                "addedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
        .commit()
        .await()
}
```

> `addedAt` is overwritten on every add with `merge`. If you want the first-added time to stay, use a transaction that only sets it when the doc doesn't exist. For a cart, "last touched" order is fine.

**Use points** (09.1 toggle):

```kotlin
suspend fun setPointsToUse(uid: String, points: Long): Unit = guardException {
    firestore.collection("carts").document(uid)
        .set(mapOf("pointsToUse" to points, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        .await()
}
```

**Save to favorites** (heart on a product card):

```kotlin
suspend fun addFavorite(uid: String, productId: String, minPrice: Long, inStockHere: Boolean): Unit = guardException {
    firestore.collection("users").document(uid)
        .collection("favorites").document(productId)
        .set(
            mapOf(
                "productId" to productId,
                "savedPrice" to minPrice,
                "savedInStock" to inStockHere,
                "addedAt" to FieldValue.serverTimestamp(),
            ),
        )
        .await()
}
```

`set` without merge on purpose: saving again after removing starts a new "saved at this price" history.

**Set default address** (03.5):

```kotlin
suspend fun setDefaultAddress(uid: String, addressId: String): Unit = guardException {
    firestore.collection("users").document(uid)
        .update(mapOf("defaultAddressId" to addressId, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
}
```

**Mark notification read** (11.4):

```kotlin
suspend fun markRead(uid: String, notificationId: String): Unit = guardException {
    firestore.collection("users").document(uid)
        .collection("notifications").document(notificationId)
        .update("readAt", FieldValue.serverTimestamp())
        .await()
}
```

---

## Check

- [ ] Existing auth fields unchanged; the auth sync still works after the rules update in 07.
- [ ] `onUserCreated` fills server fields, creates `wallets/{uid}` and `referrals/{code}`; the app shows defaults while they are missing.
- [ ] No balance field left in `users/{uid}`; Account and Payment methods read `wallets/{uid}`.
- [ ] Default address is one field on the user, not a flag.
- [ ] Cart lines are keyed by `variantId` and hold no price; `pointsToUse` is the only points field in the cart.
- [ ] Favorites keep `savedPrice`/`savedInStock` frozen; live price and stock come from the product.
- [ ] Balances change only in a function transaction together with a ledger entry.
- [ ] `users/{uid}/coupons` and `referrals` are never written by the client.
- [ ] No card data anywhere in Firestore.
