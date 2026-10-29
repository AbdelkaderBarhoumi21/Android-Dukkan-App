# 06: Team Collections (Picker + Courier)

`staff`, custom claims, `couriers`, `courierLocations`, `dispatchOffers`, `shifts`, `staff/{uid}/dailyStats`, `couriers/{uid}/earnings`, `couriers/{uid}/payouts`, `cashDeposits`.

Screens (Figma page "Team app · Picker & Courier"): S01 (sign in), S02 (profile & offline), F01/F02/F03 (picker), C01/C02/C03 (courier). The roles are called **picker** and **courier** everywhere: claims, `staff.role`, code and copy.

---

## Why

- **Staff are invited, never self-registered.** S01.2: "Use the work email your manager invited", "Your role is set by your manager". A manager creates the account; the app has no sign-up for staff.
- **Role lives in custom claims.** Claims are inside the signed ID token, so security rules can read them for free (`request.auth.token.role`) without an extra document read, and a user cannot change them.
- **Location and status are separate docs.** A courier's GPS point changes every few seconds; their profile doesn't. Separate docs mean listeners on the profile don't fire on every GPS update, and rules can expose the location to exactly one customer.

---

## 1. `staff/{uid}` and custom claims

### The Figma mentions two sources. Here is the rule.

The Team cover says *"role from custom claims picks the picker or courier flow"*, while the S01 section note and S01.2 say *"read staff/{uid}.role → picker | courier"*. Our resolution stays: both exist, with different jobs, and **the claim is the authority**:

| | Custom claims `{role, storeId}` | `staff/{uid}` document |
|---|---|---|
| Job | **Authority**: rules and the app's routing trust only this | **Record**: the editable HR data a manager sees and changes |
| Written by | function `onStaffWritten` (Admin SDK) | admin tool via callable `upsertStaff` (managers/admins only) |
| Read by | rules (`request.auth.token.role`), the team app after sign-in (`getIdToken().claims`) | the team app for display ("Courier · Dukkan Lac 2 · shift 11:00–15:00") |
| Can a user change it? | No | No (rules deny client writes) |

Flow: manager saves `staff/{uid}` → trigger `onStaffWritten` sets claims to match → the team app refreshes its token (`getIdToken(true)`) and opens the right flow. **The claim is a copy of `staff.role`, owned by one function** (allowed by 02 § 4).

**Why not read `staff/{uid}.role` in rules?** Every rule check would cost a document read (`get()`) and add latency on every request. Claims are free and already verified.

**Claims shape** (keep it tiny, the token is limited to 1000 bytes of claims):

```json
{ "role": "courier", "storeId": "lac2" }
```

`role` values: `picker`, `courier`, `manager`, `admin`. Customers have **no** `role` claim. That is how rules tell them apart.

### `staff/{uid}` fields

**Id:** the staff member's Auth `uid`.

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `email` | string | ✓ | `"karim.b@dukkan.tn"` | "Invite found · Dukkan Lac 2" |
| `firstName` / `lastName` | string | ✓ | `"Karim"` / `"Ben Ali"` | "Welcome back, Karim", "Karim Ben Ali" |
| `phone` | string | ✓ | | Customer "Call" button goes through a masked number later |
| `photoUrl` | string? | | | Avatar |
| `role` | string | ✓ | `"courier"` | Mirrors the claim |
| `storeId` | string | ✓ | `"lac2"` | Mirrors the claim |
| `storeName` | string | ✓ | `"Dukkan Lac 2"` | **Allowed copy** of `stores/{id}.name`, written by `upsertStaff`. S02.1 "Picker · Dukkan Lac 2" without a second read. Store names don't change; a rename script would update staff too |
| `isActive` | bool | ✓ | `true` | Disabled staff: the function also disables the Auth user |
| `invitedBy` | string | ✓ | manager uid | Audit |
| `createdAt` / `updatedAt` | Timestamp | ✓ | | |

**Invite flow:** the manager calls `upsertStaff({email, role, storeId, …})` → the function creates the Auth user (`admin.auth().createUser`), writes `staff/{uid}`, sets claims, and sends a password-setup link (`generatePasswordResetLink`). S01.2 "Invite found" is shown after the user types an email: the callable `lookupInvite(email)` returns the store name (it reveals only that an invite exists for that exact email; rate-limited).

Forgot password for staff: S01.2 says "Ask your store manager". The manager triggers a reset link from the admin tool.

### Team app routing after sign-in (S01.3 / S01.4)

```kotlin
val claims = auth.currentUser!!.getIdToken(/* forceRefresh = */ true).await().claims
when (claims["role"] as? String) {
    "picker" -> navigateAsRoot(PickerHome)   // S01.4 "Opening picker…"
    "courier" -> navigateAsRoot(CourierHome) // S01.3 "Opening courier…"
    else -> { auth.signOut(); showError(R.string.error_not_staff) } // a customer account in the team app
}
```

---

## 2. `couriers/{uid}` (live courier status)

**Id:** the courier's uid. Created by `onStaffWritten` when `role == "courier"`.

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `status` | string | ✓ | `offline` / `online` / `on_run` | **courier** (offline↔online), fn (`on_run`) | C01.1 "Slide to go online", C01.2 |
| `onlineSince` | Timestamp? | | | courier | "Online · 2h 14m" |
| `vehicleType` | string | ✓ | `scooter` / `bike` / `car` | courier | "Lac 2 · Bike", "Scooter · 214 TU 7781" |
| `plate` | string? | | `"214 TU 7781"` | courier | |
| `zoneId` | string | ✓ | `"lac"` | courier | Where they're working |
| `activeOrderIds` | array<string> | ✓ | `["o1"]` | fn | Current run, at most `MAX_ACTIVE_ORDERS = 2`, both from the same store (multi-stop, see 09 § 6) |
| `ratingAvg` | number | ✓ | `4.9` | fn | "4.9 rating", "Top courier" |
| `ratingCount` | number | ✓ | `1180` | fn | |
| `deliveriesCount` | number | ✓ | `1240` | fn | "Your courier · 1,240 deliveries" |
| `acceptRate` | number | ✓ | `0.96` | fn | "Accept rate 96%" (last 50 offers) |
| `weekEarnings` | number (millimes) | ✓ | `186500` | fn | "186.500 TND this week" (reset Monday by a scheduled fn) |
| `todayEarnings` | number | ✓ | `45500` | fn | "Today 45.500 TND" |
| `cashInHand` | number (millimes) | ✓ | `765300` | fn | C03.4 "Cash in hand · 765.300 TND · Return it at Dukkan Hub · Lac 2 by 22:00". `completeDelivery` adds the order total for cash orders; `recordCashDeposit` subtracts. Replaces the earlier `cashToSettle` name |
| `cashDueBy` | Timestamp? | | today 22:00 | fn | "by 22:00": set when `cashInHand` goes above 0, cleared at 0. A scheduled job alerts the manager if it's still > 0 after the deadline |
| `updatedAt` | Timestamp | ✓ | | both | |

Rules: the courier may update **only** `status` (`offline`/`online`, never `on_run`), `onlineSince`, `vehicleType`, `plate`, `zoneId`, `updatedAt`. Everything else is function-owned.

---

## 3. Courier stats and earnings

### `couriers/{uid}/earnings/{entryId}` (ledger)

| Field | Type | Example | Why |
|---|---|---|---|
| `type` | string | `delivery_fee` / `tip` / `boost` / `adjustment` | C01.4 breakdown "Delivery fees · Tips · Boost bonus" |
| `amount` | number (millimes) | `6000` | |
| `orderId` | string? | | |
| `dayKey` | string | `"2026-10-28"` | Daily bars |
| `createdAt` | Timestamp | | |

Written by `completeDelivery` (fee + boost) and `rateDelivery` (tip), in the same transaction that updates `couriers/{uid}.todayEarnings`/`weekEarnings` and the daily stat.

C02.5 "YOU EARNED +7.500 TND · Fee 6.000 · Tip 1.500 · On time": the result of `completeDelivery` returns these numbers, so the screen doesn't need to query.

### `couriers/{uid}/dailyStats/{yyyy-MM-dd}`

| Field | Type | Example | Screen |
|---|---|---|---|
| `runs` | number | `4` | C02.5 "4 Runs" |
| `earned` | number | `52000` | "52.000 TND today" |
| `fees` / `tips` / `boosts` | number | | C01.4 breakdown |
| `onlineMinutes` | number | `134` | "31h online" (sum over the week) |
| `onTimeRuns` | number | `4` | "100% On time" |

C01.4 Week view = 7 doc reads (`whereIn(documentId(), last7Days)`). "Best: Fri 41.000" and "+12% vs last" are computed on the phone from 14 days.

### `couriers/{uid}/payouts/{weekId}` (C03.4 "Payouts")

**Figma map says `payouts/{courierId}`.** That would be **one** document per courier, which can only hold the current payout; C03.4 shows a **history** ("26 Oct – 1 Nov · Pending", "19 – 25 Oct · Paid 27 Oct", …). So we keep one doc per week **under** the courier. Same meaning as the designer's name (payouts belong to a courier), and history is kept. The path also makes the rule `isOwner(courierId)` work without a query.

**Id:** ISO week key `"2026-W44"`. One payout per courier per week, so re-running the weekly job can't create duplicates.

| Field | Type | Example | Why |
|---|---|---|---|
| `periodStart` / `periodEnd` | string `yyyy-MM-dd` | `"2026-10-26"` / `"2026-11-01"` | "26 Oct – 1 Nov" |
| `runs` | number | `42` | "42 runs · fees, tips, boost" |
| `amount` | number (millimes) | `186500` | |
| `status` | string | `pending` / `scheduled` / `paid` / `failed` | "Pending", "Paid 27 Oct" |
| `scheduledFor` | Timestamp | Mon 2 Nov | "Next payout · Mon 2 Nov" |
| `paidAt` | Timestamp? | | "Paid 27 Oct" |
| `bankName` / `accountLast4` | string | `"BIAT"` / `"41"` | "To BIAT ••41". Full account numbers are not stored in Firestore |
| `createdAt` | Timestamp | | |

Created by `resetWeeklyEarnings` (Monday 00:00) from the week's `earnings` ledger. "Cash out" (C01.4) calls `requestPayout` for an early payout. "Monthly statements are emailed as PDF" = a scheduled function on the 1st of the month (no Firestore data).

### `cashDeposits/{depositId}` (cash returned to the hub)

**Id:** auto. **Top-level** because the **manager** works the list for a store ("who still owes cash tonight"), across couriers.

| Field | Type | Example | Why |
|---|---|---|---|
| `courierId` | string | uid | |
| `storeId` | string | `"lac2"` | Manager query |
| `amount` | number (millimes) | `765300` | |
| `orderIds` | array<string> | | Which cash orders this covers (≤ 50 per deposit) |
| `receivedBy` | string | manager uid | |
| `createdAt` | Timestamp | | |

Written only by the callable `recordCashDeposit` (manager role), which subtracts `amount` from `couriers/{uid}.cashInHand` in the same transaction. Couriers read their own deposits; nobody writes from a client.

**Why cash is not netted out of payouts automatically:** the cash physically sits in the courier's pocket. Mixing it with bank payouts hides missing cash. Two separate flows (bank payout for earnings, cash deposit at the hub) keep each one auditable.

---

## 4. `courierLocations/{courierId}`

**Id:** courier uid. Written by the courier app every ~5 s while `online` or `on_run`, and paused when offline (battery).

| Field | Type | Req | Example | Written by | Why |
|---|---|---|---|---|---|
| `location` | GeoPoint | ✓ | | courier | Map pin (10.3, C02) |
| `geohash` | string | ✓ | | courier | "Nearest online couriers" query in `dispatchOrder` |
| `heading` | number | ✓ | `82` | courier | Rotate the scooter icon |
| `speedKph` | number | ✓ | `24` | courier | |
| `stopsRemaining` | number | ✓ | `1` | courier | "1 stop before you" |
| `visibleTo` | array<string> | ✓ | `[customerUid]` | **fn only** | Rules: which customers may read |
| `updatedAt` | Timestamp | ✓ | server | courier | Hide a stale pin (> 60 s) |

**Why not `couriers/{id}/location` like the Figma cover?** That path is a collection, not a document. A single top-level doc per courier is simpler for listeners and for the rule "customer in `visibleTo`".

**Cost note:** 1 write / 5 s / courier ≈ 720 writes per hour on shift. For 50 couriers × 8 h that is 288,000 writes/day (≈ 0.52 USD/day at current pricing). Fine. If it grows, raise the interval when the courier is not moving.

---

## 5. `dispatchOffers/{offerId}`

A delivery offer sent to one courier (C01.3 "New order · 24s · Slide to accept").

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `orderId` | string | ✓ | | |
| `orderNumber` | string | ✓ | `"DK-2048"` | Header |
| `courierId` | string | ✓ | uid | The courier listens `where courierId == me && status == "pending"` |
| `status` | string | ✓ | `pending` / `accepted` / `declined` / `expired` / `taken` | |
| `fee` | number | ✓ | `7500` | "TND fee 7.500" |
| `boostMultiplier` | number | ✓ | `1.5` | "×1.5 boost" |
| `distanceMeters` | number | ✓ | `2800` | "Distance 2.8 km" |
| `etaMinutes` | number | ✓ | `14` | "Est. time ~14 min" |
| `itemCount` / `bagCount` | number | ✓ | `4` / `2` | "4 items · 2 bags" |
| `paymentLabel` | string | ✓ | `cash` / `paid_online` | "Cash on delivery" |
| `pickup` | map | ✓ | `{name:"Dukkan Hub Lac 2", address:"Rue du Lac Leman", distanceMeters:1200}` | "Pick up · … · 1.2 km away" |
| `dropoff` | map | ✓ | `{name:"Amira B.", area:"Rue du Lac Biwa 12, Lac 2", distanceMeters:1600}` | "Drop off · Amira B." (first name + initial only) |
| `expiresAt` | Timestamp | ✓ | now + 30 s | The countdown "24s" = `expiresAt - now` |
| `createdAt` | Timestamp | ✓ | | |

Written only by functions. The courier **accepts** with the callable `acceptOffer(offerId)` or declines with `declineOffer(offerId)`. `acceptOffer` runs a transaction: the offer is still `pending` and not expired, and the order has no `courierId`. Then it sets the order's courier, marks other offers for the same order `taken`, and sets `couriers/{uid}.status = "on_run"`.

**Why not let the courier write `status: "accepted"` on the offer?** Two couriers could accept the same order at the same moment. The transaction in the function guarantees one winner.

An expired offer is set to `expired` by a scheduled function (every minute) or lazily by the next `dispatchOrder` run, which then offers the order to the next courier.

---

## 6. `shifts/{shiftId}`

| Field | Type | Req | Example | Why / screen |
|---|---|---|---|---|
| `staffId` | string | ✓ | uid | |
| `storeId` | string | ✓ | `"lac2"` | |
| `role` | string | ✓ | `"picker"` | |
| `startsAt` / `endsAt` | Timestamp | ✓ | 09:00 / 17:00 | "shift 09:00–17:00" (S01.4, F02.3) |
| `clockInAt` / `clockOutAt` | Timestamp? | | | "3h 12m on shift" (F01.1) |
| `status` | string | ✓ | `scheduled` / `active` / `done` / `missed` | |

Planned by managers (admin tool). Clock-in happens with the callable `clockIn` (checks the phone is near the store, using the location in the call) so a picker can't clock in from home.

---

## 7. Picker stats: `staff/{uid}/dailyStats/{yyyy-MM-dd}`

| Field | Type | Example | Screen |
|---|---|---|---|
| `ordersPicked` | number | `31` | F01.1 "31 Picked today", F02.3 |
| `goal` | number | `40` | "/ 40 goal" (copied from the shift plan at clock-in) |
| `itemsPicked` / `itemsScannedCorrectly` | number | | "99.2% Accuracy" = correct / picked |
| `totalPickSeconds` | number | | "Avg pick time 7:40" = total / ordersPicked |
| `swaps` | number | `2` | "Swaps today" |
| `missedSla` | number | `0` | Orders packed after `dueAt` |
| `byHour` | map<string, number> | `{"09":3, "10":5}` | "Today by hour" bars |

Updated by `markPacked` in a transaction. The queue counts ("6 To pick") are a `count()` query, not a stored number.

---

## 8. Picking: missing item flow (F01.4)

```
Picker taps "Can't find it"
   │
   ├── order.missingItemRule == "swap_similar"
   │       → app proposes a similar in-stock variant (same category, ±20% price)
   │       → picker confirms → resolveMissingItem({orderId, itemId, action:"swap", variantId})
   │
   ├── "call_me"  → app shows "Call the customer" (dial customer.phone) → after the call,
   │                picker chooses swap or remove → resolveMissingItem(...)
   │
   └── "remove"   → resolveMissingItem({orderId, itemId, action:"remove"})
                    → item.pickStatus = "removed", refundAmount, totals.refunded += …
                    → card/wallet: partial refund; cash: courier collects less
```

The function also writes an order event, decrements the right inventory, and pushes the customer ("1 swap · customer notified").

**Swap pricing rule:** the customer never pays more than the original line. If the substitute costs less, the difference is refunded.

### Manual barcode (F03.1 "Type the barcode")

When a label won't scan, the picker types the digits. **Matching is done on the phone, with no backend call:**
- The item already holds its expected `barcode` (order item snapshot, 05 § 2).
- As the picker types, the sheet compares the typed prefix with it: "6 194 003 773 · 10/13 · Matches the first 10 digits of the expected item".
- "Confirm item" is enabled only when all **13 digits** match **and** the EAN-13 check digit is valid. The check catches a typo that happens to match another product's prefix.
- Confirming writes the same pick fields as a scan (`pickStatus: picked`), plus `pickMethod: "typed"` so accuracy stats can tell typed picks from scanned ones.

```kotlin
/** EAN-13: the last digit is a checksum of the first 12 (weights 1,3,1,3…). */
fun isValidEan13(code: String): Boolean {
    if (code.length != 13 || !code.all(Char::isDigit)) return false
    val sum = code.take(12).mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
    val check = (10 - sum % 10) % 10
    return check == code.last() - '0'
}

fun matchesExpected(typed: String, expected: String): Boolean =
    typed.length == 13 && typed == expected && isValidEan13(typed)
```

Add `pickMethod` (`scanned` / `typed`) to the picker's allowed item fields in 07.

---

## 8b. Staff profile, sign-out guard and offline (S02)

### S02.1 Staff profile

All from data we already have: `staff/{uid}` (name, email, "Picker · Dukkan Lac 2"), `staff/{uid}/dailyStats/{today}` ("31 Picked today", "99.2% Accuracy"), the active `shifts` doc ("09–17"). "Language", "Shift & order alerts" and "Scanner beep" are **phone settings** (DataStore), not Firestore: they belong to the device, not the person. "App version 1.0.0 (12)" = `BuildConfig`.

### S02.2 Sign-out guard

"You're on shift until 17:00 · 2 orders are assigned to you · They go back to the queue after 10 min · Hand over picked bags first."

- The sheet shows when the staff member has an **active shift** or **assigned orders** (`orders where staffIds array-contains uid and statusGroup == "active"`, `count()`).
- **"End shift & sign out"** → callable `endShift` (09):
  - clocks out (`shifts.clockOutAt`, `status: done`);
  - a courier goes `offline`;
  - every order the person holds is **released**: picker orders in `picking` go back to `placed` with `pickerId` cleared (scanned items keep their `pickStatus`, so the next picker continues); courier orders not yet `picked_up` lose their `courierId` and are re-dispatched.
  - Then the app signs out.
- **"Stay signed in"** closes the sheet.
- **Safety net, for the "after 10 min" in the copy:** a scheduled job every 2 minutes (`requeueAbandonedOrders`) releases orders in `picking` whose latest item `pickedAt` (or `pickingStartedAt` if nothing was picked yet) is older than **10 minutes**, and courier orders not yet `picked_up` whose `courierLocations/{uid}.updatedAt` is older than 10 minutes. Only a handful of orders are in those states at once, so one small query per order is fine. No "last seen" field is added: the pick writes and location writes already carry the time. This covers staff who just close the app or lose their phone.

### S02.3 Offline

"Keep working — scans and photos are saved on this phone and sync when you're back · Waiting to sync · 3 items · Bag B-15 scanned / Door photo · #DK-2051 / Delivered · #DK-2048".

| Action | How it works offline |
|---|---|
| Picker scan (`pickStatus`) | **Direct Firestore write** → the SDK's offline persistence queues it automatically |
| Photos (packing, door) | Storage uploads don't queue by themselves → the **outbox** keeps the file path and uploads later |
| Callables (`scanBag`, `completeDelivery`, `markPacked`, `confirmPickup`, …) | **Do not queue.** A callable is an HTTPS request; offline it fails immediately → the **outbox** |

**The outbox** (team app, `core/sync/`):
- A Room table `pending_actions(id, type, payloadJson, requestId, createdAt, attempts)`.
- A **WorkManager** worker with a `NetworkType.CONNECTED` constraint replays the rows in order.
- Every replayed callable carries a **`requestId`** (a UUID created when the action happened). The function stores processed ids in `orders/{id}/requests/{requestId}`, or checks the target state, and returns the **same result** if it sees an id twice. A retry after a timeout can't deliver twice or pay the courier twice. See 09 § 10.
- "Waiting to sync · 3 items" is the row count; each row shows its label and time.
- "New offers can't reach you while offline": the phone can't tell the server it's offline (any write would only be sent once it's back online). So `dispatchOrder` **skips couriers whose `courierLocations/{uid}.updatedAt` is older than 60 s**. A courier with no connection stops sending location, which marks them unreachable without any extra field.

Room is preferred over DataStore here: the queue holds several rows that must stay in order and be removed one by one. That's a table, not a key-value pair.

---

## 9. Kotlin DTOs (team app)

```kotlin
package com.example.dukkanteam.features.courier.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class StaffDto(
    @DocumentId val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String? = null,
    val role: String = "",
    val storeId: String = "",
    val storeName: String = "",
)

data class CourierDto(
    @DocumentId val uid: String = "",
    val status: String = "offline",
    val onlineSince: Timestamp? = null,
    val vehicleType: String = "",
    val plate: String? = null,
    val zoneId: String = "",
    val activeOrderIds: List<String> = emptyList(),
    val ratingAvg: Double = 0.0,
    val deliveriesCount: Long = 0,
    val acceptRate: Double = 0.0,
    val weekEarnings: Long = 0,
    val todayEarnings: Long = 0,
    val cashInHand: Long = 0,
    val cashDueBy: Timestamp? = null,
)

data class PayoutDto(
    @DocumentId val weekId: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val runs: Int = 0,
    val amount: Long = 0,
    val status: String = "",
    val scheduledFor: Timestamp? = null,
    val paidAt: Timestamp? = null,
    val bankName: String = "",
    val accountLast4: String = "",
)

data class CourierLocationDto(
    val location: GeoPoint? = null,
    val heading: Double = 0.0,
    val speedKph: Double = 0.0,
    val stopsRemaining: Int = 0,
    val updatedAt: Timestamp? = null,
)

data class DispatchOfferDto(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val status: String = "",
    val fee: Long = 0,
    val boostMultiplier: Double = 1.0,
    val distanceMeters: Long = 0,
    val etaMinutes: Int = 0,
    val itemCount: Int = 0,
    val bagCount: Int = 0,
    val paymentLabel: String = "",
    val pickup: Map<String, Any> = emptyMap(),
    val dropoff: Map<String, Any> = emptyMap(),
    val expiresAt: Timestamp? = null,
)

data class DailyStatsDto(
    @DocumentId val dayKey: String = "",
    val runs: Int = 0,
    val earned: Long = 0,
    val fees: Long = 0,
    val tips: Long = 0,
    val boosts: Long = 0,
    val onlineMinutes: Int = 0,
    val onTimeRuns: Int = 0,
)
```

The customer app reads `CourierLocationDto` only (tracking). The rest lives in the team app module.

---

## 10. Writing the courier location (team app)

```kotlin
suspend fun publishLocation(uid: String, point: Location, stopsRemaining: Int): Unit = guardException {
    firestore.collection("courierLocations").document(uid).set(
        mapOf(
            "location" to GeoPoint(point.latitude, point.longitude),
            "geohash" to GeoFireUtils.getGeoHashForLocation(GeoLocation(point.latitude, point.longitude)),
            "heading" to point.bearing.toDouble(),
            "speedKph" to point.speed * 3.6,
            "stopsRemaining" to stopsRemaining,
            "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(), // never overwrite visibleTo (function-owned)
    ).await()
}
```

`GeoFireUtils` is in `com.firebase:geofire-android-common`. Run this from a **foreground service** with a persistent notification ("You're online"), which Android requires for background location.

---

## Check

- [ ] A customer account cannot open the team app (no `role` claim → signed out).
- [ ] Changing `staff/{uid}.role` updates the claim; the app sees it after `getIdToken(true)`.
- [ ] A courier cannot write `visibleTo`, earnings, or offer status (emulator tests).
- [ ] Two couriers accepting the same order: exactly one wins.
- [ ] Location writes stop when the courier goes offline.
- [ ] `endShift` releases every held order; `requeueAbandonedOrders` catches staff who vanish for 10 min.
- [ ] The outbox replays callables with the same `requestId`; a double replay changes nothing.
- [ ] Typed barcodes are accepted only on a full 13-digit match with a valid EAN-13 check digit.
- [ ] `cashInHand` returns to 0 after `recordCashDeposit`; payouts keep one doc per week.
