# 05: Order Collections

`orders/{orderId}` and its subcollections `items`, `events`, `bags`, `issues`, `private`, plus `counters/orders`. This is where the three apps meet: the customer places, the picker packs, the courier delivers.

---

## Why

- **Only functions create and move orders.** The price, coupon, stock and status of an order are money. A client-side write would let a modified app pay 1 millime for headphones. The single exception is the picker's per-item pick result (high frequency, works offline in the aisle), which rules limit to a few fields.
- **The order is a snapshot.** Items, address, customer contact and courier info are frozen copies (02 § 4). The receipt never changes when the catalog does.
- **Status lives in one field.** All screens (customer tracking, picker queue, courier run) listen to the same `status`. The timeline (`events`) is an append-only log of how it got there.

---

## 1. `orders/{orderId}`

**Id:** Firestore auto id. **Human number:** field `number` = `"DK-2048"`, from `counters/orders` (§ 7).

**Why not use `DK-2048` as the id:** auto ids spread writes across Firestore's storage, and they can't be guessed. Rules protect the doc anyway, but an id that isn't sequential leaks less (how many orders we do per day).

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `number` | string | ✓ | `"DK-2048"` | `placeOrder` | Everywhere "#DK-2048" |
| `customerId` | string | ✓ | uid | `placeOrder` | Rules, My Orders query |
| `storeId` | string | ✓ | `"lac2"` | `placeOrder` | Picker queue query |
| `zoneId` | string | ✓ | `"lac"` | `placeOrder` | Dispatch |
| `status` | string | ✓ | `"picking"` | functions | § 5 |
| `statusGroup` | string | ✓ | `active` / `delivered` / `cancelled` / `failed` | functions | My Orders tabs (10.1, 10.5): All · Active · Delivered · Cancelled · Delivery failed |
| `itemCount` | number | ✓ | `4` | `placeOrder` | "4 items" (quantity sum) |
| `productIds` | array<string> | ✓ | `["air-runner-2", …]` (≤ 50) | `placeOrder` | Frozen list. Lets the review rule check "you bought this" with one `get()`, and powers "Buy it again" |
| `lineCount` | number | ✓ | `3` | `placeOrder` | Picker "3 items" (distinct lines) |
| `hasFragile` | bool | ✓ | `true` | `placeOrder` | "1 fragile", bubble bag |
| `missingItemRule` | string | ✓ | `swap_similar` / `call_me` / `remove` | `placeOrder` (from checkout choice) | F01.2 "If something is missing: Swap for similar" |
| `customerNote` | string? | | `"Call me if anything is missing"` | `placeOrder` | F01.2 |
| `totals` | map | ✓ | see below | `placeOrder` (+ refunds fn) | 10.2 summary |
| `couponCode` | string? | | `"WELCOME15"` | `placeOrder` | |
| `pointsUsed` | number | ✓ | `1250` | `placeOrder` | "Use 1,250 points" |
| `pointsEarned` | number | ✓ | `74` | `placeOrder` | "+74 points earned" (credited on delivery) |
| `payment` | map | ✓ | see below | functions | Cash / card / wallet |
| `delivery` | map | ✓ | see below | `placeOrder` | Address snapshot, ETA |
| `customer` | map | ✓ | `{firstName:"Amira", lastInitial:"B", phone:"+216…"}` | `placeOrder` | Courier & picker see "Amira B." (C01.3, C02.3, C03.2), never `users/{uid}`. Only the initial of the last name is copied: staff need no more (privacy, 02 § 4) |
| `pickerId` | string? | | uid | `startPicking` | Cleared by `endShift` / `requeueAbandonedOrders` (06 § 8b) |
| `pickingStartedAt` | Timestamp? | | | `startPicking` | Abandoned-order check |
| `courierId` | string? | | uid | `acceptOffer` | Rules for courier access, tracking |
| `courier` | map? | | `{name:"Karim B.", vehicleType:"scooter", plate:"214 TU 7781", ratingAvg:4.9, deliveriesCount:1240, photoUrl}` | `acceptOffer` | Live tracking (10.3) |
| `staffIds` | array<string> | ✓ | `[pickerId, courierId]` | functions | One rule check `request.auth.uid in staffIds` |
| `bagCount` | number | ✓ | `2` | `markPacked` | "2 bags", "Bags 1/2 and 2/2" |
| `pickupShelf` | string? | | `"R-12"` | `markReady` | F02.2 "Shelf R-12" |
| `packingPhotoPath` | string? | | Storage path | `markPacked` | F02.1 "Photo of sealed bags" |
| `doorPhotoPath` | string? | | Storage path | `completeDelivery` | C02.4 "Photo at the door" |
| `dueAt` | Timestamp | ✓ | | `placeOrder` | Picker "Express · due 10:30", SLA |
| `etaFrom` / `etaTo` | Timestamp | ✓ | 12:30 / 12:45 | functions | "Arrives 12:30 – 12:45", updated as the run progresses |
| `rating` | map? | | `{stars:5, tags:["fast","friendly"], comment, ratedAt}` | `rateDelivery` | 10.4 |
| `tip` | number | ✓ | `2000` | `rateDelivery` | 10.4 "Say thanks with a tip" (millimes) |
| `cancel` | map? | | `{by:"customer", reason:"ordered_by_mistake", at, refundMillimes:0, refundTo:"none", couponRestored:true}` | `cancelOrder` / `resolveFailedDelivery` | 10.6 #DK-1990 "Reason: ordered by mistake · Payment · Nothing to pay · Coupon FREESHIP is back in My coupons". `reason` values in § 5.3 |
| `arrivedAt` | Timestamp? | | | `markArriving` | Starts the 5-min door wait (C03.2 "04:12 wait time left"), 10.7 "18:08 Arrived" |
| `doorWait` | map? | | `{startedAt, callAttempts:2, whatsappSentAt}` | `reportIssue`, `logContactAttempt` | C03.2 "Attempts: Call 1, Call 2, message sent"; 10.7 "18:10 Called ×2 · message". `whatsappSentAt` is null when the WhatsApp message could not be sent (09 § 8c) |
| `refundedMillimes` | number | | `0` | `resolveMissingItem` after delivery, support | Subtracted from tier spend (09 § 8d) |
| `failure` | map? | | `{reason:"customer_not_answering", returnedAt, resolveBy}` | `returnToStore` | 10.7 "We couldn't reach you … back at Dukkan Lac 2". `resolveBy` = deadline for the customer's choice (§ 5.2) |
| `resolution` | map? | | `{choice:"redeliver", slotStart, slotEnd, at}` / `{choice:"store_pickup", readyUntil, at}` | `resolveFailedDelivery` | 10.7 "Deliver again today 20:00 – 21:00 · free" / "Ready now · open until 22:00". Slots come from `getRedeliverySlots` (09) |
| `fulfilment` | string | ✓ | `delivery` / `store_pickup` | `placeOrder`, `resolveFailedDelivery` | `store_pickup` only after a failed delivery for v1 |
| `placedAt` | Timestamp | ✓ | | `placeOrder` | "Today 12:05" |
| `updatedAt` | Timestamp | ✓ | | functions | |
| `deliveredAt` | Timestamp? | | | `completeDelivery` | "Delivered at 12:38" |

### `totals` map (millimes)

| Key | Example | Shown as |
|---|---|---|
| `subtotal` | `817800` | "Subtotal 817.800 TND" |
| `deliveryFee` | `0` | "Delivery Free" |
| `couponDiscount` | `40000` | "Coupon WELCOME15 −40.000 TND" |
| `pointsDiscount` | `12500` | "Save 12.500 TND" (points) |
| `refunded` | `0` | Missing/removed items (F01.4 "Refund 459.000 TND") |
| `total` | `765300` | "Total · cash" = subtotal + fee − discounts − refunded |

`tip` is **not** part of `total`: it goes 100% to the courier (10.4) and is paid separately.

### `payment` map

| Key | Type | Example | Why |
|---|---|---|---|
| `method` | string | `cash` / `card` / `wallet` | 09.2 "Pay with" |
| `paymentMethodId` | string? | | Saved card |
| `status` | string | `pending` / `authorized` / `paid` / `refunded` / `failed` | Courier C01.3 "Cash on delivery" (cash, `pending`) vs "Paid online" (card/wallet, `paid`) |
| `cashTendered` | number? | `770000` | "You'll hand 770 TND · change 4.700" (customer's estimate, shown to the courier) |
| `cashCollected` | number? | `770000` | C02.4 "Received 770.000 TND" |
| `changeGiven` | number? | `4700` | C02.4 "Give change 4.700 TND" |
| `providerRef` | string? | | Card payment id (server) |

### `delivery` map (frozen address snapshot)

| Key | Example |
|---|---|
| `addressId` | `"a1"` (the source, for "deliver here again") |
| `label` | `"Home"` |
| `line1` | `"Rue du Lac Biwa 12"` |
| `line2` | `"Bldg B12, floor 3, apt 7"` |
| `area` / `city` | `"Les Berges du Lac"` / `"Tunis"` |
| `location` | GeoPoint |
| `dropOff` | `ring_bell` |
| `riderNote` | |
| `voiceNotePath` | |
| `recipientName` / `recipientPhone` | for "ordering for someone else" |
| `slot` | `express` / `scheduled` |
| `slotStart` / `slotEnd` | Timestamp (scheduled: "Slot 18:00", redelivery "17:00 – 18:00") |
| `distanceMeters` | `1600` (store → customer, C01.3 "1.6 km") |

---

## 2. `orders/{orderId}/items/{itemId}`

**Id:** auto, or `variantId` (one line per variant, same as the cart). We use the **variant id** for the same anti-duplicate reason as the cart.

| Field | Type | Req | Example | Written by | Why / screen |
|---|---|---|---|---|---|
| `productId` | string | ✓ | | `placeOrder` | Reorder, reviews |
| `variantId` | string | ✓ | | `placeOrder` | |
| `name` | string | ✓ | `"Pulse Max Headphones"` | `placeOrder` | Snapshot |
| `brandName` | string | ✓ | `"SONIQ"` | `placeOrder` | F01.3 "SONIQ · Space grey" |
| `optionsLabel` | map | ✓ | `{en:"Space grey"}` | `placeOrder` | |
| `imageUrl` | string | ✓ | | `placeOrder` | |
| `sku` / `barcode` | string | ✓ | `"77310"` / `"6194003773100"` | `placeOrder` | Scan to confirm |
| `aisle` / `aisleName` / `shelf` | string/map/string | ✓ | `"A"`, `{en:"Audio & tech"}`, `"6"` | `placeOrder` | Picking route (F01.2, F01.3) |
| `isFragile` | bool | ✓ | | `placeOrder` | "pack last" |
| `unitPrice` | number | ✓ | `459000` | `placeOrder` | Receipt |
| `quantity` | number | ✓ | `1` | `placeOrder` | "×1" |
| `lineTotal` | number | ✓ | `459000` | `placeOrder` | |
| `pickStatus` | string | ✓ | `pending` / `picked` / `swapped` / `missing` / `removed` | **picker** (rules) / fn | F01.3, F01.4 |
| `pickedQuantity` | number | ✓ | `1` | **picker** | Partial picks |
| `pickedAt` | Timestamp? | | | **picker** | "scanned at 10:14" |
| `pickMethod` | string? | | `scanned` / `typed` | **picker** | F03.1 manual barcode; accuracy stats |
| `substitute` | map? | | `{productId, variantId, name:"Studio Headphones", unitPrice:289000}` | `resolveMissingItem` fn | F01.4 swap |
| `refundAmount` | number | ✓ | `0` | `resolveMissingItem` fn | F01.4 remove |

**Picker writes vs function writes:** scanning "picked" writes `pickStatus`, `pickedQuantity`, `pickedAt`, `pickMethod` directly (offline-safe, instant). "Can't find it" → swap/remove changes **money**, so it goes through the callable `resolveMissingItem` (09), which also notifies the customer ("1 swap · customer notified", F02.1).

---

## 3. `orders/{orderId}/events/{eventId}` (timeline)

**Id:** auto. **Append only**, written by functions.

| Field | Type | Example | Why |
|---|---|---|---|
| `type` | string | `status_changed` / `item_swapped` / `item_removed` / `courier_assigned` / `call_attempt` / `whatsapp_sent` / `whatsapp_failed` / `issue_reported` / `refund_issued` / `note` | 10.7 timeline for #DK-2051 "18:08 Arrived · 18:10 Called ×2 · message · 18:13 Wait ended" |
| `status` | string? | `"packed"` | For status events |
| `actorRole` | string | `customer` / `picker` / `courier` / `system` | |
| `actorId` | string? | uid | |
| `message` | map? | `{en:"Dukkan Lac 2 is packing 3 of 4 items"}` | Shown on 10.2 |
| `at` | Timestamp | | Status steps with times |

**Why a subcollection and not an array on the order:** it grows during the order life, the customer listens to the order doc (tracking), and we don't want every event to resend the whole timeline.

---

## 4. `orders/{orderId}/bags/{bagId}`

**Id:** the bag label, e.g. `B-14`.

| Field | Type | Example | Written by | Why |
|---|---|---|---|---|
| `index` | number | `1` | `markPacked` | "Bags 1/2" |
| `kind` | string | `dry` / `fashion` / `fragile` | `markPacked` | "Bag B-14 · Dry & grocery" (C02.2) |
| `qrPayload` | string | `"dukkan:bag:{orderId}:B-14"` | `markPacked` | Printed label (F02.2) |
| `shortCode` | string | `"DK7K2Q"` | `markPacked` | The 6 characters printed **under** the QR, typed when the label won't scan (C03.1 "6 characters printed under the QR") |
| `scannedAt` | Timestamp? | | `scanBag` fn | C02.2 "Scanned" / "Waiting" |
| `scannedBy` | string? | courier uid | `scanBag` fn | |

**Note vs Figma:** the Team cover lists a top-level `bags/{id}` collection. We put bags **under the order** because a bag never exists without its order, and the QR payload already contains the `orderId`, so no lookup by QR is needed. One fewer top-level collection and one fewer rule.

**`shortCode` (C03.1 "Type the bag code"):**
- **Generated by `markPacked`** from an alphabet without look-alike characters (`ABCDEFGHJKMNPQRSTUVWXYZ23456789`, no `0/O`, `1/I/L`), 6 characters, about 10⁹ combinations.
- **Unique per order only.** The courier is already on the order's run screen, so `scanBag({orderId, shortCode})` compares the typed code with the bags of **that** order. No global index or lookup is needed, and 6 random characters make a collision inside one order (2–3 bags) practically impossible. `markPacked` re-draws if two bags of the same order collide anyway.
- The sheet shows "Looks like bag B-15 · #DK-2048" as soon as the typed prefix matches one of the order's bags, which the courier app already has.

---

## 4b. `orders/{orderId}/issues/{issueId}` (C03.3 "Report a problem")

**Id:** auto. Figma map: `orders/{id}/issues · type, note, photo, at`.

| Field | Type | Req | Example | Why |
|---|---|---|---|---|
| `type` | string | ✓ | `customer_not_answering` / `wrong_address` / `refused` / `damaged` / `other` | The 5 radio options |
| `note` | string? | | max 500 chars | "Something else" free text |
| `photoPath` | string? | | `orders/{id}/issues/{issueId}.jpg` | Damaged package proof (11) |
| `createdBy` | string | ✓ | courier uid | |
| `role` | string | ✓ | `courier` / `picker` / `customer` | Same collection for future customer-side issues ("Need help?") |
| `status` | string | ✓ | `open` / `resolved` | Support works the list |
| `createdAt` | Timestamp | ✓ | server | |

**Written by the callable `reportIssue`** (09), not by direct client writes. Why: a report has side effects (an `events` entry, a support alert, and for `customer_not_answering` it starts the door-wait flow). One callable keeps them together; rules stay "read only" for issues.

---

## 4c. Courier at the door: wait timer, attempts, return to store (C03.2, 10.7)

1. "I've arrived" → `markArriving` sets `status: arriving` and `arrivedAt`.
2. If nobody opens, the courier taps "Customer not answering?" (C02.4) → `reportIssue({type:"customer_not_answering"})`. The function sets `doorWait.startedAt`, sends the customer a high-priority push **and**, if the customer opted in, a **WhatsApp template message** ("Your Dukkan courier is at your door…", 09 § 8c), and writes a `whatsapp_sent` event (or `whatsapp_failed`, push only).
3. "Call again" → the phone dialer opens; the app also calls `logContactAttempt({orderId, kind:"call"})`, which increments `doorWait.callAttempts` and writes a `call_attempt` event ("Call 1 · no answer 13:58"). "Message" opens a WhatsApp chat with `https://wa.me/<number>` (no server call).
4. The timer is **5 minutes** from `doorWait.startedAt`. It's computed on the phone ("04:12 wait time left"), and the server re-checks it.
5. When the timer ends → `returnToStore({orderId})`: `now ≥ doorWait.startedAt + 5 min` and `callAttempts ≥ 2`. Moves `arriving → returning`. When the bags are scanned back at the hub (`confirmReturn`, picker or courier), `returning → failed`, `failure.returnedAt`, and the customer gets a push "We couldn't reach you".

The wait is **5 minutes** everywhere (C03.2 and 10.7 "Karim waited 5 min, called twice and sent an SMS"), a constant in the function, `DOOR_WAIT_MS`. Figma's word "SMS" should read "WhatsApp message" (decision: WhatsApp, see 12).

**Worked example, #DK-2051** (2 items · 62.400 TND · cash): arrived 18:08 → called ×2 + WhatsApp 18:10 → wait ended 18:13 (`returnToStore`) → bags back, `failed` at 18:25 → 10.7 offers the next free slot 20:00 – 21:00 or store pickup until 22:00.

---

## 5. Status state machine

```
  cancelled ◄── customer: only while `placed` (10.6 "before packing started")
      ▲         manager/system: any time before delivered/collected
      │
placed ──► picking ──► packed ──► ready_for_pickup ──► courier_assigned ──► picked_up ──► arriving ──► delivered
                                        ▲                                                    │
                                        │ redeliver (slot)                                   │ door wait 5 min, no answer
                                        │                                                    ▼
                                        └───────────────── failed ◄── (bags back at hub) ── returning
                                                             │
                                                             ├── store_pickup ──► awaiting_customer_pickup ──► collected
                                                             └── cancel ──► cancelled
```

| From → To | Who | Callable / trigger | Condition |
|---|---|---|---|
| (new) → `placed` | customer | `placeOrder` | Valid cart, stock reserved, zone OK |
| `placed` → `picking` | picker | `startPicking` | Picker's store == order store |
| `picking` → `packed` | picker | `markPacked` | No `pending` items left; bags created; photo uploaded |
| `packed` → `ready_for_pickup` | picker | `markReady` | Bags on shelf `pickupShelf` |
| `ready_for_pickup` → `courier_assigned` | system | `acceptOffer` / trigger | A courier accepted (see note) |
| `courier_assigned` → `picked_up` | courier | `confirmPickup` | All bags scanned (`scanBag`) |
| `picked_up` → `arriving` | courier | `markArriving` (C02.3 "I've arrived"), or automatic within 300 m | Sets `arrivedAt` |
| `arriving` → `delivered` | courier | `completeDelivery` | Correct delivery code (`verifyDeliveryCode`), cash recorded if cash |
| `arriving` → `returning` | courier | `returnToStore` | Door wait of 5 min elapsed, ≥ 2 call attempts (§ 4c) |
| `returning` → `failed` | picker or courier at the hub | `confirmReturn` | All bags scanned back in; sets `failure.returnedAt`, `failure.resolveBy = now + 24 h` |
| `failed` → `ready_for_pickup` | customer | `resolveFailedDelivery({choice:"redeliver", slotStart})` | `slotStart` is one of the slots from `getRedeliverySlots` (1 h, from opening hours, ≥ 60 min ahead, today or tomorrow) and the slot still has capacity: re-checked and booked (`stores/{storeId}/slotLoad/{date_HH}` +1) in the same transaction. `delivery.slot = scheduled`; dispatch starts 30 min before `slotStart`. Free |
| `failed` → `awaiting_customer_pickup` | customer | `resolveFailedDelivery({choice:"store_pickup"})` | Store open now; `fulfilment = store_pickup`, `resolution.readyUntil` = today's closing time ("open until 22:00") |
| `awaiting_customer_pickup` → `collected` | picker | `confirmCustomerPickup` | Customer shows the delivery code at the counter (same `private/delivery` code). Cash collected here if COD |
| `failed` → `cancelled` | customer | `resolveFailedDelivery({choice:"cancel"})` | COD: nothing to pay. Prepaid: refund to wallet |
| `failed` → `cancelled` | system | `expireFailedOrders` (scheduled) | No choice by `failure.resolveBy` |
| `placed` → `cancelled` | customer | `cancelOrder` | 10.6 "You cancelled before packing started". Releases stock, refunds wallet/card/points, frees the coupon |
| any before `delivered`/`collected` → `cancelled` | manager / system | `cancelOrder` | |

**Why the customer can cancel only while `placed`:** once a picker has started, the items are off the shelf and maybe swapped; cancelling then costs the store work. Figma 10.6 states the same rule. After `placed`, the "Cancel order" button on 10.2 is hidden (the client reads `status`, the function re-checks).

**Why `returning` is its own status:** between "no answer" and "back at the hub" the bags are still with the courier. The customer must not be offered "Pick up at Dukkan Lac 2 · Ready now" before the bags are actually there. `failed` means "bags are in the store, waiting for your choice".

**Courier assignment in parallel with packing.** Dispatch starts when the order is `packed` (09). A courier may accept **before** the order is ready (F02.2 shows "Karim B. · arriving in 4 min" while the picker is still at the shelf). In that case `acceptOffer` sets `courierId`/`courier` immediately but leaves `status`; when `markReady` runs and a courier is already set, it moves straight to `courier_assigned`. Rule: **`courier_assigned` means "ready AND a courier is coming".** The functions that move status always check both.

**`statusGroup`** is written in the same update:

| status | statusGroup | My Orders tab |
|---|---|---|
| placed … arriving, `returning`, `awaiting_customer_pickup` | `active` | Active |
| delivered, collected | `delivered` | Delivered |
| cancelled | `cancelled` | Cancelled |
| failed | `failed` | Delivery failed (10.5 "Choose what to do") |

`returning` stays `active` because the customer can't act yet; `failed` gets its own group so 10.5 can list it with one equality filter.

**Customer step chips** (molecule "Status steps": Placed · Packing · On the way · At door · Done) are a pure mapping on the phone:

| `orders.status` | Customer step | Step index |
|---|---|---|
| `placed` | Placed | 0 |
| `picking`, `packed`, `ready_for_pickup`, `courier_assigned` | Packing | 1 |
| `picked_up` | On the way | 2 |
| `arriving` | At door | 3 |
| `delivered`, `collected` | Done | 4 |
| `returning` | *not a step*: banner "Your courier couldn't reach you · bringing your order back to the store" | – |
| `failed` | *not a step*: the 10.7 "Delivery failed" screen with three choices: **Deliver again today** (first free slot, e.g. 20:00 – 21:00, free), **Pick up at Dukkan Lac 2** (ready now, open until 22:00), **Cancel the order** | – |
| `awaiting_customer_pickup` | *not a step*: banner "Ready at Dukkan Lac 2 · show code 7359" (the delivery code) | – |
| `cancelled` | *not a step*: the 10.6 "Order cancelled" banner with the refund line | – |
| `unknown` (new status from a newer server) | keep the last known step | – |

Kotlin (domain, used by 10.1, 10.2, 10.3, 10.5–10.7 and the Home active-order card). Add the new values to the `OrderStatus` enum in 02 § 5.3.

```kotlin
enum class CustomerOrderStep { Placed, Packing, OnTheWay, AtDoor, Done }

/** What the order screen shows instead of (or on top of) the stepper. */
sealed interface OrderBanner {
    data object Returning : OrderBanner
    data object DeliveryFailed : OrderBanner          // 10.7 choices
    data object AwaitingStorePickup : OrderBanner
    data object Cancelled : OrderBanner               // 10.6
}

/** null = not on the stepper (see [toBanner]). */
fun OrderStatus.toCustomerStep(): CustomerOrderStep? = when (this) {
    OrderStatus.Placed -> CustomerOrderStep.Placed
    OrderStatus.Picking, OrderStatus.Packed,
    OrderStatus.ReadyForPickup, OrderStatus.CourierAssigned -> CustomerOrderStep.Packing
    OrderStatus.PickedUp -> CustomerOrderStep.OnTheWay
    OrderStatus.Arriving -> CustomerOrderStep.AtDoor
    OrderStatus.Delivered, OrderStatus.Collected -> CustomerOrderStep.Done
    OrderStatus.Returning, OrderStatus.Failed, OrderStatus.AwaitingCustomerPickup,
    OrderStatus.Cancelled, OrderStatus.Unknown -> null
}

fun OrderStatus.toBanner(): OrderBanner? = when (this) {
    OrderStatus.Returning -> OrderBanner.Returning
    OrderStatus.Failed -> OrderBanner.DeliveryFailed
    OrderStatus.AwaitingCustomerPickup -> OrderBanner.AwaitingStorePickup
    OrderStatus.Cancelled -> OrderBanner.Cancelled
    else -> null
}
```

### 5.3 Cancel and failure reasons

| Field | Values | Screen |
|---|---|---|
| `cancel.reason` (customer, 10.6) | `ordered_by_mistake`, `changed_mind`, `delivery_too_slow`, `found_cheaper`, `other` | Reason picker before "Cancel order" |
| `cancel.reason` (system/staff) | `delivery_failed`, `no_choice_after_failure`, `out_of_stock`, `store_closed`, `fraud` | Support tools |
| `cancel.refundTo` | `wallet` / `card` / `none` | 10.6 #DK-1990 (cash): `none` → "Nothing to pay · Cash on delivery — no money was taken". A wallet-paid order shows "→ Dukkan wallet · the refund is instant" |
| `cancel.couponRestored` | bool | 10.6 "Coupon FREESHIP is back in My coupons · Valid until 6 Nov" (shown only when `true`; the date is the coupon's `endsAt`) |
| `failure.reason` | `customer_not_answering`, `wrong_address`, `refused`, `damaged` (from the issue type) | 10.7 headline |

**Refund rules (`cancelOrder`, `resolveFailedDelivery` cancel):**
- **Wallet-paid:** credit `wallets/{uid}` instantly with a `refund` transaction, `cancel.refundTo = "wallet"`.
- **Card (after Konnect):** refund through the provider. `payment.status` becomes `refunded` when Konnect confirms.
- **Cash on delivery:** nothing was paid, so `refundTo = "none"` ("Nothing to pay · cash on delivery").
- **In every case:** points used are returned (points ledger `refund`) and stock is returned to `stores/{storeId}/inventory`. Picked items are restocked by the picker when the bags come back.
- **Coupon:** if the coupon is **still valid** (`isActive` and `endsAt` in the future), it is given back in the **same transaction**: `coupons/{code}/redemptions/{uid}.count − 1`, `coupons/{code}.redeemedCount − 1`, and `users/{uid}/coupons/{code}` goes back to `available` (it shows under Available on 07.3). An **expired** coupon is not restored: it would only land in the Expired tab, and the counters stay as they are. The same rule applies to a cancel after a failed delivery and to the 24 h auto-cancel. `cancel.couponRestored` records what happened.

Amounts on these screens are formatted from `Long` millimes with the design system's `formatTnd` helper.

---

## 6. Handover codes: `orders/{orderId}/private/{docId}`

Two short codes protect the two handovers.

| Doc | Field | Who can read | Who checks it | Screen |
|---|---|---|---|---|
| `private/pickup` | `code` (4 digits, e.g. `"4821"`) | the assigned courier and the store's pickers | picker at handover: `confirmPickup` compares | C02.1 "Code 48 21" |
| `private/delivery` | `code` (4 digits, e.g. `"7359"`) | **the customer only** | courier types it; callable `verifyDeliveryCode` compares on the server | C02.4 "Enter delivery code — Amira sees the 4-digit code in her order tracking" |

The two codes are drawn **independently**, so knowing the pickup code tells nothing about the delivery code (Figma: pickup `48 21` on C02.1, delivery `7 3 5 9` on C02.4). For a store pickup after a failed delivery (§ 5), the customer shows the same **delivery** code at the counter and `confirmCustomerPickup` checks it.

Both docs also hold `attempts` (number) and `createdAt`. Written by `placeOrder`. **Never** readable by the courier for the delivery code: if the courier could read it, they could "deliver" without meeting the customer. After 5 wrong attempts `verifyDeliveryCode` locks and asks for support.

**Why a subcollection and not a field on the order:** Firestore rules are per document, not per field. The courier can read `orders/{id}`, so a field there would leak. A separate doc can have its own rule.

---

## 7. `counters/orders`

| Field | Type | Example |
|---|---|---|
| `next` | number | `2049` |

`placeOrder` reads and increments it **inside its transaction**, then writes `number = "DK-" + next`. Firestore transactions retry on contention, so two orders never get the same number. One document handles about one order per second, which is far above what we need; the fix past that is sharded counters (02 § 7).

---

## 8. Live tracking (10.3, C02.x)

The customer's tracking screen uses **two listeners**:

| Listener | Gives | Who writes |
|---|---|---|
| `orders/{orderId}` | status, ETA (`etaFrom`/`etaTo`), courier name, vehicle, plate, rating | functions |
| `courierLocations/{courierId}` | `location`, `heading`, `speedKph`, `updatedAt` | the courier app, every ~5 s while on a run |

Rules let a customer read `courierLocations/{courierId}` **only while their uid is in its `visibleTo` array** (set by `acceptOffer`, cleared by `completeDelivery`, `returnToStore` or `cancelOrder`). See [06 § 4](06_team_collections.md#4-courierlocationscourierid) and [07](07_security_rules.md).

"8 min", "Arriving in 12 min · 12:40", "1 stop before you", "On time": ETA comes from the order; "stops before you" comes from `courierLocations.stopsRemaining` (the courier app sets it from its run list). Distance ticks on the map are computed on the phone from the two points.

"Karim is 2 min away" push: a function triggered by `courierLocations` writes computes the distance to the active order and sends one push when it drops under ~600 m (and sets `orders/{id}.nearPushSentAt` so it's sent once).

---

## 9. Rating and tip (10.4)

Callable `rateDelivery({orderId, stars, tags, comment, tip})`:

- allowed once, by the customer, when `status == "delivered"`;
- writes `rating` and `tip` on the order;
- writes a courier earnings entry for the tip (06) and updates the courier's rating average;
- if `tip > 0` and payment is card/wallet, charges it (cash tips are not supported in-app).

Tags are fixed values: `fast`, `friendly`, `careful`, `polite`.

Product reviews are separate (`products/{id}/reviews/{uid}`, 03 § 6.2).

---

## 10. Kotlin DTOs

```kotlin
package com.example.dukkanapp.features.orders.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class OrderDto(
    @DocumentId val id: String = "",
    val number: String = "",
    val customerId: String = "",
    val storeId: String = "",
    val status: String = "",
    val statusGroup: String = "",
    val itemCount: Int = 0,
    val lineCount: Int = 0,
    val hasFragile: Boolean = false,
    val missingItemRule: String = "",
    val customerNote: String? = null,
    val totals: OrderTotalsDto = OrderTotalsDto(),
    val couponCode: String? = null,
    val pointsUsed: Long = 0,
    val pointsEarned: Long = 0,
    val payment: OrderPaymentDto = OrderPaymentDto(),
    val delivery: OrderDeliveryDto = OrderDeliveryDto(),
    val customer: Map<String, String> = emptyMap(),
    val courierId: String? = null,
    val courier: OrderCourierDto? = null,
    val bagCount: Int = 0,
    val etaFrom: Timestamp? = null,
    val etaTo: Timestamp? = null,
    val tip: Long = 0,
    val fulfilment: String = "delivery",
    val cancel: OrderCancelDto? = null,
    val failure: OrderFailureDto? = null,
    val resolution: Map<String, Any?>? = null,
    val placedAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
)

data class OrderCancelDto(
    val by: String = "",
    val reason: String = "",
    val at: Timestamp? = null,
    val refundMillimes: Long = 0,
    val refundTo: String = "none",
)

data class OrderFailureDto(
    val reason: String = "",
    val returnedAt: Timestamp? = null,
    val resolveBy: Timestamp? = null,
)

data class OrderTotalsDto(
    val subtotal: Long = 0,
    val deliveryFee: Long = 0,
    val couponDiscount: Long = 0,
    val pointsDiscount: Long = 0,
    val refunded: Long = 0,
    val total: Long = 0,
)

data class OrderPaymentDto(
    val method: String = "",
    val status: String = "",
    val cashTendered: Long? = null,
)

data class OrderDeliveryDto(
    val label: String = "",
    val line1: String = "",
    val line2: String = "",
    val area: String = "",
    val city: String = "",
    val location: GeoPoint? = null,
    val dropOff: String = "",
    val riderNote: String? = null,
    val slot: String = "express",
)

data class OrderCourierDto(
    val name: String = "",
    val vehicleType: String = "",
    val plate: String = "",
    val ratingAvg: Double = 0.0,
    val deliveriesCount: Long = 0,
    val photoUrl: String? = null,
)

data class OrderItemDto(
    @DocumentId val id: String = "",
    val productId: String = "",
    val variantId: String = "",
    val name: String = "",
    val brandName: String = "",
    val optionsLabel: Map<String, String> = emptyMap(),
    val imageUrl: String = "",
    val unitPrice: Long = 0,
    val quantity: Int = 0,
    val lineTotal: Long = 0,
    val pickStatus: String = "pending",
)

data class OrderEventDto(
    @DocumentId val id: String = "",
    val type: String = "",
    val status: String? = null,
    val message: Map<String, String> = emptyMap(),
    val at: Timestamp? = null,
)
```

The team app has its own, larger DTOs (aisle, shelf, barcode, bags). The customer app maps only what it shows.

---

## Check

- [ ] The client has **no** write path to `orders` (rules deny; only callables).
- [ ] Every status change also sets `statusGroup`, `updatedAt` and writes an `events` doc.
- [ ] The delivery code doc is unreadable for the courier (emulator test in 07).
- [ ] A customer can cancel only while `placed`; a refund always writes a wallet/points ledger entry.
- [ ] `returnToStore` refuses before 5 minutes; `failed` appears only after bags are back at the hub.
- [ ] Every failed order is resolved (redeliver / store pickup / cancel) or auto-cancelled after 24 h.
- [ ] Money in totals adds up: `total = subtotal + deliveryFee − couponDiscount − pointsDiscount − refunded`.
- [ ] Order numbers are unique under concurrent `placeOrder` calls (emulator load test).
