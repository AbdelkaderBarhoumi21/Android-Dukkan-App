# 09: Cloud Functions (2nd gen, TypeScript)

**Folder:** `firebase/functions/src/`. Region `europe-west1`.

---

## Why

- **Trust boundary.** Anything involving money, stock, status, roles, or other users' data is computed on the server. The phone only *asks*.
- **Callable functions** (`onCall`) for user actions: they receive the caller's verified `auth` and App Check token automatically, and return typed errors the app can map.
- **Triggers** (`onDocumentWritten`, …) for consequences: counters, aggregates, pushes. They run after the write, so the user doesn't wait.
- **Scheduled** functions for time: offer expiry, flash sale end, weekly reset, cleanup.
- **Idempotency:** triggers can run more than once. Every trigger must be safe to repeat (check before incrementing, or use the event id).

---

## 1. Function catalog

### Callables (the app calls these)

| Function | Caller | Does | Screens |
|---|---|---|---|
| `placeOrder` | customer | Validates cart, zone, stock, coupon, `pointsToUse`, wallet; computes totals; creates order + items + codes + event; reserves stock; clears cart; returns `{orderId, number, totals, etaFrom, etaTo, pointsEarned}` | 09.1 "Slide to place order", 09.3 |
| `previewCheckout` | customer | Same math as `placeOrder`, **no writes**. Returns totals and coupon status | 08.1 coupon row, 08.2, 09.1 |
| `cancelOrder` | customer / manager | Customer: only while `placed`, with a reason. Releases stock, refunds (wallet instant), returns points, frees the coupon, event, push | 10.2 "Cancel order", 10.6 |
| `getRedeliverySlots` | customer | 1-hour slots from the store's `openingHours` (today, else tomorrow), ≥ 60 min from now, with free capacity; plus "open until" for store pickup | 10.7 "Deliver again today 20:00 – 21:00 · free", "Ready now · open until 22:00" |
| `resolveFailedDelivery` | customer | `failed` → redeliver (slot re-checked + booked in the same transaction) / store pickup / cancel | 10.7 "Confirm" |
| `claimCoupon` | customer | Validates a typed promo code and adds it to `users/{uid}/coupons` | 07.3 "Enter a promo code · Add", 08.2 |
| `applyReferralCode` | customer | Links a new user to a friend's code, gives the invitee coupon | 11.8, sign-up |
| `rateDelivery` | customer | Rating + tip, courier rating avg, tip earning | 10.4 |
| `deleteAccount` | customer | Deletes Auth user and data, anonymizes orders | 11.x (later) |
| `addPaymentMethod` | customer | Exchanges the provider's setup result for a saved card doc | 09.2 "Add a new card" |
| `startPicking` | picker | `placed → picking`, sets `pickerId`, `staffIds` | F01.1 "Start picking" |
| `resolveMissingItem` | picker | swap / remove an item, refund, push customer | F01.4 |
| `markPacked` | picker | `picking → packed`, creates bags, stats, starts dispatch | F02.1 |
| `markReady` | picker | `packed → ready_for_pickup` (or `courier_assigned`) | F02.2 |
| `acceptOffer` / `declineOffer` | courier | Transactional assignment | C01.3 |
| `scanBag` | courier | Marks a bag scanned, by QR **or** typed `shortCode` | C02.2, C03.1 |
| `confirmPickup` | courier | All bags scanned + pickup code → `picked_up` | C02.1, C02.2 |
| `markArriving` | courier | `picked_up → arriving`, sets `arrivedAt` | C02.3 "I've arrived" |
| `verifyDeliveryCode` | courier | Compares the code, counts attempts | C02.4 |
| `completeDelivery` | courier | Code verified, cash recorded → `delivered`, earnings, points, `cashInHand`. **Idempotent** (`requestId`) | C02.4 → C02.5 |
| `reportIssue` | courier / picker | Writes `orders/{id}/issues`, event, support alert; `customer_not_answering` starts the door wait (push + **WhatsApp template** `courier_at_door`, § 8c) | C02.4 "Report", C03.3 |
| `logContactAttempt` | courier | `doorWait.callAttempts += 1`, `call_attempt` event | C03.2 "Call again" |
| `returnToStore` | courier | After the 5-min wait: `arriving → returning` | C03.2 |
| `confirmReturn` | picker / courier | Bags back at the hub: `returning → failed`, push "We couldn't reach you" | hub |
| `confirmCustomerPickup` | picker | Store pickup with the delivery code: `awaiting_customer_pickup → collected` | counter |
| `requestPayout` | courier | Creates an early payout | C01.4 "Cash out" |
| `recordCashDeposit` | manager | Cash returned at the hub: `cashDeposits` + `cashInHand −=` | C03.4 "Return it at Dukkan Hub" |
| `clockIn` / `clockOut` | staff | Shift start/end with location check | F01.1, C01.1 |
| `endShift` | staff | Clock out, go offline, **release held orders** back to the queue | S02.2 "End shift & sign out" |
| `upsertStaff` | manager/admin | Creates/updates staff + Auth user, invite link | admin tool |
| `lookupInvite` | anyone (rate-limited) | "Invite found · Dukkan Lac 2" | S01.2 |

### Triggers

| Function | On | Does |
|---|---|---|
| `onUserCreated` | `users/{uid}` created | Server fields, `wallets/{uid}`, `referrals/{code}` + its private `coupons/REF-{code}` (10 TND off, min 50 TND, first order only), WELCOME15 coupon, welcome inbox entry |
| `onOrderStatusChanged` | `orders/{id}` updated, `status` changed | Writes `events`, inbox entry, push to customer (and staff), sets `nearPushSentAt` reset; on the first `delivered` order calls `rewardReferral` |
| `onCourierLocationWritten` | `courierLocations/{id}` | ETA refresh (throttled), "2 min away" push once |
| `onStaffWritten` | `staff/{uid}` | Sets custom claims, creates `couriers/{uid}`, disables Auth user when inactive |
| `onReviewWritten` | `products/{p}/reviews/{u}` | `ratingAvg` / `ratingCount` on product and brand |
| `onVariantWritten` | `products/{p}/variants/{v}` | `minPrice`, `compareAtPrice`, `discountPercent`; price drop → pushes to users with the product in `favorites` |
| `onInventoryWritten` | `stores/{s}/inventory/{v}` | `inStockStoreIds`; restock → stock alert pushes |
| `onFollowWritten` | `users/{u}/followedBrands/{b}` | `brands/{b}.followersCount` |
| `onProductWritten` | `products/{p}` | `productCount` on brand and categories |

### Scheduled

| Function | Every | Does |
|---|---|---|
| `expireOffers` | 1 min | Pending offers past `expiresAt` → `expired`; re-dispatch |
| `updateZoneLive` | 1 min | `zones/{z}/live/current` (orders waiting, surge) |
| `endFlashSales` | 5 min | Reverts flash prices when a `flash_sale` collection ends |
| `nightlyPopularity` | daily 03:00 | `popularityScore` from last 30 days of orders |
| `resetWeeklyEarnings` | Mon 00:00 | `couriers/*.weekEarnings = 0`, creates `couriers/{uid}/payouts/{weekId}` |
| `cleanupDevices` | daily | Deletes devices not updated for 60 days |
| `requeueAbandonedOrders` | 2 min | Releases orders held by staff silent for 10 min (06 § 8b) |
| `dispatchScheduled` | 5 min | Redelivery slots starting within 30 min → `dispatchOrder` |
| `expireFailedOrders` | 15 min | `failed` orders past `failure.resolveBy` → `cancelled` (`no_choice_after_failure`) |
| `expireUserCoupons` | daily 00:10 | `users/*/coupons` past `endsAt` → `expired`; `coupon_expiring` pushes 48 h before |
| `cashDueAlerts` | 22:05 | Couriers with `cashInHand > 0` past `cashDueBy` → manager alert |
| `nightlyTiers` | daily 02:30 | Recomputes `spend12mMillimes` and `tier` for every customer with an order in the last 13 months; the **only** place a tier goes **down** (§ 8d) |
| `cleanupSlotLoad` | daily 04:00 | Deletes `stores/*/slotLoad/*` docs older than 2 days |

---

## 2. Project skeleton

`firebase/functions/src/index.ts`:

```typescript
import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions/v2";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

export * from "./orders/placeOrder";
export * from "./orders/statusCallables";
export * from "./orders/onOrderStatusChanged";
export * from "./dispatch/dispatch";
export * from "./staff/onStaffWritten";
export * from "./catalog/aggregates";
export * from "./users/onUserCreated";
export * from "./maintenance/scheduled";
export * from "./search/searchProducts"; // doc 14
```

`firebase/functions/src/shared/errors.ts`, one place for the error contract the Android app maps:

```typescript
import { HttpsError } from "firebase-functions/v2/https";

/** Stable reason codes. The Android app maps these to AppFailure.Feature values. Never rename. */
export type Reason =
  | "CART_EMPTY" | "OUTSIDE_ZONE" | "STORE_CLOSED" | "OUT_OF_STOCK" | "PRICE_CHANGED"
  | "COUPON_INVALID" | "COUPON_EXPIRED" | "COUPON_MIN_NOT_MET" | "COUPON_ALREADY_USED"
  | "PHONE_REQUIRED" | "NOT_ENOUGH_POINTS" | "NOT_ENOUGH_WALLET"
  | "INVALID_TRANSITION" | "NOT_YOUR_ORDER" | "OFFER_EXPIRED" | "OFFER_TAKEN" | "RUN_FULL"
  | "WRONG_CODE" | "CODE_LOCKED" | "BAGS_NOT_SCANNED" | "NOT_STAFF"
  // Added with the order-problem, coupon and referral screens:
  | "COUPON_TIER_REQUIRED" | "CANCEL_NOT_ALLOWED" | "WAIT_NOT_OVER" | "SLOT_UNAVAILABLE"
  | "REFERRAL_INVALID" | "REFERRAL_OWN_CODE" | "REFERRAL_ALREADY_USED" | "REFERRAL_FULL";

export function fail(reason: Reason, details: Record<string, unknown> = {}): never {
  throw new HttpsError("failed-precondition", reason, { reason, ...details });
}

export function requireAuth(auth: { uid: string } | undefined): string {
  if (!auth) throw new HttpsError("unauthenticated", "Sign in first");
  return auth.uid;
}
```

`firebase/functions/src/shared/money.ts`:

```typescript
/** All money is integer millimes. Percent discounts round DOWN to the millime (customer-friendly, never over-discount). */
export const percentOf = (amount: number, percent: number): number => Math.floor((amount * percent) / 100);

/** 100 points = 1 TND, so 1 point = 10 millimes (04 § 5). */
export const MILLIMES_PER_POINT = 10;

/** Tier order for coupons with `minTier` (GOLD20). Thresholds live in `config/tiers` (§ 8d). */
export const TIER_RANK: Record<string, number> = { member: 0, silver: 1, gold: 2 };
```

`firebase/functions/src/shared/idempotency.ts`, used by every callable the team app may **replay** from its offline outbox (06 § 8b):

```typescript
import { Timestamp, Transaction } from "firebase-admin/firestore";
import { getFirestore } from "firebase-admin/firestore";

const db = getFirestore();

/**
 * Runs `work` at most once per (orderId, requestId). A replay returns the first result.
 * Call it INSIDE the function's transaction so the marker and the work commit together.
 */
export async function once<T>(
  tx: Transaction, orderId: string, requestId: string | undefined, work: () => Promise<T>,
): Promise<T> {
  if (!requestId) return work(); // old clients: no protection, same as before
  const ref = db.doc(`orders/${orderId}/requests/${requestId}`);
  const seen = await tx.get(ref);
  if (seen.exists) return seen.data()!.result as T;
  const result = await work();
  tx.set(ref, { result, at: Timestamp.now() });
  return result;
}
```

> Firestore transactions require **all reads before any write**. `once` reads the marker first, and `work` must then do its own reads before writing, so call `once` as the very first thing in the transaction.

---

## 3. `placeOrder` (the most important function)

`firebase/functions/src/orders/placeOrder.ts`:

```typescript
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { onCall } from "firebase-functions/v2/https";
import { fail, requireAuth } from "../shared/errors";
import { MILLIMES_PER_POINT, percentOf, TIER_RANK } from "../shared/money";
import { pointInPolygon, randomCode } from "../shared/geo";

const db = getFirestore();

interface PlaceOrderRequest {
  addressId: string;
  paymentMethod: "cash" | "card" | "wallet";
  paymentMethodId?: string;
  cashTendered?: number;          // millimes
  // Points come from carts/{uid}.pointsToUse (04 § 5), not from the request.
  missingItemRule: "swap_similar" | "call_me" | "remove";
  customerNote?: string;
  expectedTotal: number;          // what the customer saw; protects against silent price changes
}

const POINTS_PER_TND = 0.1;       // earn 1 point per 10 TND

export const placeOrder = onCall<PlaceOrderRequest>({ enforceAppCheck: true }, async (request) => {
  const uid = requireAuth(request.auth);
  const input = request.data;

  return db.runTransaction(async (tx) => {
    // ── 1. READS (all reads must happen before any write in a transaction) ──
    const userRef = db.doc(`users/${uid}`);
    const walletRef = db.doc(`wallets/${uid}`);
    const cartRef = db.doc(`carts/${uid}`);
    const [userSnap, walletSnap, cartSnap, cartItemsSnap, addressSnap, counterSnap] = await Promise.all([
      tx.get(userRef),
      tx.get(walletRef),
      tx.get(cartRef),
      tx.get(cartRef.collection("items")),
      tx.get(userRef.collection("addresses").doc(input.addressId)),
      tx.get(db.doc("counters/orders")),
    ]);

    if (cartItemsSnap.empty) fail("CART_EMPTY");
    if (!addressSnap.exists) fail("OUTSIDE_ZONE");
    const user = userSnap.data() ?? {};
    const wallet = walletSnap.data() ?? { walletBalance: 0, pointsBalance: 0 };
    const cart = cartSnap.data() ?? {};
    const address = addressSnap.data()!;

    if (input.paymentMethod === "cash" && !user.phoneNumber) fail("PHONE_REQUIRED");

    const zoneSnap = await tx.get(db.doc(`zones/${address.zoneId}`));
    const zone = zoneSnap.data();
    if (!zone?.isActive || !pointInPolygon(address.location, zone.polygon)) fail("OUTSIDE_ZONE");

    const storeId: string = zone.storeId;
    const storeSnap = await tx.get(db.doc(`stores/${storeId}`));
    if (!storeSnap.data()?.isOpen) fail("STORE_CLOSED");

    // Variants, inventory and products for every cart line.
    const lines = await Promise.all(cartItemsSnap.docs.map(async (itemDoc) => {
      const { productId, quantity } = itemDoc.data();
      const variantId = itemDoc.id;
      const [productSnap, variantSnap, stockSnap] = await Promise.all([
        tx.get(db.doc(`products/${productId}`)),
        tx.get(db.doc(`products/${productId}/variants/${variantId}`)),
        tx.get(db.doc(`stores/${storeId}/inventory/${variantId}`)),
      ]);
      const product = productSnap.data();
      const variant = variantSnap.data();
      const stock = stockSnap.data();
      if (!product?.isActive || !variant?.isActive) fail("OUT_OF_STOCK", { variantId });
      if (!stock || stock.quantity < quantity) fail("OUT_OF_STOCK", { variantId, available: stock?.quantity ?? 0 });
      return { productId, variantId, quantity: quantity as number, product, variant, stock, stockRef: stockSnap.ref };
    }));

    // Coupon
    let couponDiscount = 0;
    let freeDelivery = false;
    const couponCode: string | undefined = cart.couponCode ?? undefined;
    const subtotal = lines.reduce((sum, l) => sum + l.variant.price * l.quantity, 0);
    let redemptionRef: FirebaseFirestore.DocumentReference | undefined;

    if (couponCode) {
      const [couponSnap, redemptionSnap] = await Promise.all([
        tx.get(db.doc(`coupons/${couponCode}`)),
        tx.get(db.doc(`coupons/${couponCode}/redemptions/${uid}`)),
      ]);
      const coupon = couponSnap.data();
      const now = Timestamp.now();
      if (!coupon?.isActive) fail("COUPON_INVALID");
      if (coupon.endsAt.toMillis() < now.toMillis()) fail("COUPON_EXPIRED");
      if (coupon.firstOrderOnly && (user.ordersCount ?? 0) > 0) fail("COUPON_ALREADY_USED");
      if (coupon.minTier && (TIER_RANK[user.tier ?? "member"] ?? 0) < TIER_RANK[coupon.minTier]) {
        fail("COUPON_TIER_REQUIRED", { minTier: coupon.minTier }); // GOLD20 "Reach Gold to unlock"
      }
      if ((redemptionSnap.data()?.count ?? 0) >= coupon.perUserLimit) fail("COUPON_ALREADY_USED");

      const eligible = lines
        .filter((l) => inScope(coupon.scope, l.product))
        .reduce((sum, l) => sum + l.variant.price * l.quantity, 0);
      if (eligible < coupon.minSubtotal) fail("COUPON_MIN_NOT_MET", { missing: coupon.minSubtotal - eligible });

      if (coupon.type === "percent") couponDiscount = percentOf(eligible, coupon.value);
      if (coupon.type === "fixed") couponDiscount = coupon.value;
      if (coupon.type === "free_delivery") freeDelivery = true;
      if (coupon.maxDiscount != null) couponDiscount = Math.min(couponDiscount, coupon.maxDiscount);
      couponDiscount = Math.min(couponDiscount, eligible);
      redemptionRef = redemptionSnap.ref;
    }

    // Delivery fee
    const hasFreeDeliveryGift = (user.freeDeliveriesLeft ?? 0) > 0;
    const deliveryFee = freeDelivery || hasFreeDeliveryGift || subtotal >= zone.freeDeliveryThreshold ? 0 : zone.deliveryFee;

    // Points (cart.pointsToUse, 1 point = 10 millimes). 1,250 points → 12.500 TND.
    const requestedPoints: number = cart.pointsToUse ?? 0;
    if (requestedPoints > (wallet.pointsBalance ?? 0)) fail("NOT_ENOUGH_POINTS");
    const maxByOrder = Math.floor((subtotal - couponDiscount) / MILLIMES_PER_POINT);
    const pointsUsed = Math.min(requestedPoints, maxByOrder);
    const pointsDiscount = pointsUsed * MILLIMES_PER_POINT;

    // #DK-2048: 817800 + 0 − 40000 − 12500 = 765300
    const total = subtotal + deliveryFee - couponDiscount - pointsDiscount;
    if (total !== input.expectedTotal) fail("PRICE_CHANGED", { total });
    if (input.paymentMethod === "wallet" && (wallet.walletBalance ?? 0) < total) fail("NOT_ENOUGH_WALLET");

    const pointsEarned = Math.floor((total / 1000) * POINTS_PER_TND);
    const next: number = counterSnap.data()?.next ?? 1000;
    const number = `DK-${next}`;
    const now = Timestamp.now();
    const etaFrom = Timestamp.fromMillis(now.toMillis() + zone.etaMinMinutes * 60_000);
    const etaTo = Timestamp.fromMillis(now.toMillis() + zone.etaMaxMinutes * 60_000);

    // ── 2. WRITES ──
    const orderRef = db.collection("orders").doc();
    tx.set(db.doc("counters/orders"), { next: next + 1 }, { merge: true });

    tx.set(orderRef, {
      number, customerId: uid, storeId, zoneId: address.zoneId,
      status: "placed", statusGroup: "active",
      itemCount: lines.reduce((n, l) => n + l.quantity, 0),
      lineCount: lines.length,
      productIds: [...new Set(lines.map((l) => l.productId))],
      hasFragile: lines.some((l) => l.product.isFragile),
      missingItemRule: input.missingItemRule,
      customerNote: input.customerNote ?? null,
      totals: { subtotal, deliveryFee, couponDiscount, pointsDiscount, refunded: 0, total },
      couponCode: couponCode ?? null, pointsUsed, pointsEarned,
      payment: {
        method: input.paymentMethod, paymentMethodId: input.paymentMethodId ?? null,
        status: input.paymentMethod === "cash" ? "pending" : input.paymentMethod === "wallet" ? "paid" : "authorized",
        cashTendered: input.cashTendered ?? null,
      },
      delivery: {
        addressId: input.addressId, label: address.customLabel ?? address.label,
        line1: address.street,
        line2: [address.building && `Bldg ${address.building}`, address.floor && `floor ${address.floor}`, address.apartment && `apt ${address.apartment}`].filter(Boolean).join(", "),
        area: address.area, city: address.city, location: address.location,
        dropOff: address.dropOff, riderNote: address.riderNote ?? null, voiceNotePath: address.voiceNotePath ?? null,
        recipientName: address.recipientName ?? null, recipientPhone: address.recipientPhone ?? null,
        slot: "express",
      },
      fulfilment: "delivery",
      // "Amira B.": staff see the first name and one initial only (02 § 4).
      customer: {
        firstName: user.firstName ?? "",
        lastInitial: (user.lastName ?? "").charAt(0).toUpperCase(),
        phone: address.recipientPhone ?? user.phoneNumber ?? "",
      },
      pickerId: null, courierId: null, courier: null, staffIds: [],
      bagCount: 0, tip: 0, rating: null,
      dueAt: etaFrom, etaFrom, etaTo,
      placedAt: now, updatedAt: now, deliveredAt: null,
    });

    for (const l of lines) {
      tx.set(orderRef.collection("items").doc(l.variantId), {
        productId: l.productId, variantId: l.variantId,
        name: l.product.name, brandName: l.product.brandName,
        optionsLabel: l.variant.optionsLabel, imageUrl: l.variant.imageUrl ?? l.product.images[0],
        sku: l.variant.sku, barcode: l.variant.barcode,
        aisle: l.stock.aisle, aisleName: l.stock.aisleName, shelf: l.stock.shelf,
        isFragile: l.product.isFragile,
        unitPrice: l.variant.price, quantity: l.quantity, lineTotal: l.variant.price * l.quantity,
        pickStatus: "pending", pickedQuantity: 0, pickedAt: null, substitute: null, refundAmount: 0,
      });
      tx.update(l.stockRef, { quantity: FieldValue.increment(-l.quantity), updatedAt: now });
      tx.delete(cartRef.collection("items").doc(l.variantId));
    }

    tx.set(orderRef.collection("private").doc("pickup"), { code: randomCode(4), attempts: 0, createdAt: now });
    tx.set(orderRef.collection("private").doc("delivery"), { code: randomCode(4), attempts: 0, createdAt: now });
    tx.set(orderRef.collection("events").doc(), { type: "status_changed", status: "placed", actorRole: "customer", actorId: uid, at: now });

    if (redemptionRef) {
      tx.set(redemptionRef, { orderId: orderRef.id, count: FieldValue.increment(1), lastAt: now }, { merge: true });
      tx.update(db.doc(`coupons/${couponCode}`), { redeemedCount: FieldValue.increment(1) });
      tx.set(userRef.collection("coupons").doc(couponCode!), { status: "used", usedOrderId: orderRef.id, usedAt: now }, { merge: true });
    }

    const userUpdate: Record<string, unknown> = { ordersCount: FieldValue.increment(1), updatedAt: now };
    if (hasFreeDeliveryGift && deliveryFee === 0) userUpdate.freeDeliveriesLeft = FieldValue.increment(-1);
    tx.update(userRef, userUpdate);

    // Wallet doc: points spent and wallet payment, each with its ledger entry (same transaction).
    const walletUpdate: Record<string, unknown> = { updatedAt: now };
    if (pointsUsed > 0) {
      walletUpdate.pointsBalance = FieldValue.increment(-pointsUsed);
      tx.set(walletRef.collection("pointsLedger").doc(), {
        delta: -pointsUsed, reason: "order_redeemed", orderId: orderRef.id,
        balanceAfter: (wallet.pointsBalance ?? 0) - pointsUsed, createdAt: now,
      });
    }
    if (input.paymentMethod === "wallet") {
      walletUpdate.walletBalance = FieldValue.increment(-total);
      tx.set(walletRef.collection("transactions").doc(), {
        amount: -total, reason: "order_payment", orderId: orderRef.id,
        balanceAfter: (wallet.walletBalance ?? 0) - total, createdAt: now,
      });
    }
    tx.set(walletRef, walletUpdate, { merge: true });
    tx.set(cartRef, { couponCode: null, pointsToUse: 0, updatedAt: now }, { merge: true });

    return { orderId: orderRef.id, number, totals: { subtotal, deliveryFee, couponDiscount, pointsDiscount, total }, etaFrom: etaFrom.toMillis(), etaTo: etaTo.toMillis(), pointsEarned };
  });
});

function inScope(scope: { brandIds?: string[]; categoryIds?: string[] } | undefined, product: FirebaseFirestore.DocumentData): boolean {
  if (!scope || (!scope.brandIds && !scope.categoryIds)) return true;
  if (scope.brandIds?.includes(product.brandId)) return true;
  return (scope.categoryIds ?? []).some((c) => product.categoryPath.includes(c));
}
```

**Notes**

- **Points earned are credited on delivery** (`completeDelivery`), not here. A cancelled order must not leave points behind.
- **Card payments:** for `card`, call the payment provider *before* the transaction to authorize the amount, pass its reference in, and **capture** on delivery. If the transaction fails, void the authorization in a `catch`.
- **Transaction size:** ~6 writes per line. 50 lines ≈ 300 writes, under the 500 limit. Cap the cart at 50 lines in the app.
- `pointInPolygon` and `randomCode` live in `shared/geo.ts` (ray-casting test; `crypto.randomInt(0, 10_000)` padded to 4 digits).

---

## 4. Status callables (one transition table)

`firebase/functions/src/orders/statusCallables.ts`:

```typescript
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { onCall } from "firebase-functions/v2/https";
import { fail, requireAuth } from "../shared/errors";

const db = getFirestore();

type Status = "placed" | "picking" | "packed" | "ready_for_pickup" | "courier_assigned"
  | "picked_up" | "arriving" | "delivered" | "returning" | "failed"
  | "awaiting_customer_pickup" | "collected" | "cancelled";

const GROUP: Record<Status, "active" | "delivered" | "cancelled" | "failed"> = {
  placed: "active", picking: "active", packed: "active", ready_for_pickup: "active",
  courier_assigned: "active", picked_up: "active", arriving: "active",
  returning: "active", awaiting_customer_pickup: "active",
  delivered: "delivered", collected: "delivered",
  failed: "failed", cancelled: "cancelled",
};

/** Moves an order from one status to the next, after the caller-specific checks pass. */
async function transition(
  orderId: string, from: Status[], to: Status, actor: { role: string; uid: string },
  extra: (order: FirebaseFirestore.DocumentData) => Record<string, unknown> = () => ({}),
) {
  const ref = db.doc(`orders/${orderId}`);
  return db.runTransaction(async (tx) => {
    const order = (await tx.get(ref)).data();
    if (!order) fail("NOT_YOUR_ORDER");
    if (!from.includes(order.status)) fail("INVALID_TRANSITION", { status: order.status });
    const now = Timestamp.now();
    tx.update(ref, { status: to, statusGroup: GROUP[to], updatedAt: now, ...extra(order) });
    tx.set(ref.collection("events").doc(), { type: "status_changed", status: to, actorRole: actor.role, actorId: actor.uid, at: now });
    return order;
  });
}

export const startPicking = onCall<{ orderId: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const { role, storeId } = req.auth!.token as { role?: string; storeId?: string };
  if (role !== "picker") fail("NOT_STAFF");
  await transition(req.data.orderId, ["placed"], "picking", { role, uid }, (order) => {
    if (order.storeId !== storeId) fail("NOT_YOUR_ORDER");
    return { pickerId: uid, staffIds: [uid], pickingStartedAt: Timestamp.now() };
  });
});

export const markArriving = onCall<{ orderId: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  await transition(req.data.orderId, ["picked_up"], "arriving", { role: "courier", uid }, (order) => {
    if (order.courierId !== uid) fail("NOT_YOUR_ORDER");
    return { arrivedAt: Timestamp.now() };
  });
});
```

`markPacked`, `markReady`, `confirmPickup`, `confirmReturn`, `confirmCustomerPickup` follow the same pattern with their own checks (see [05 § 5](05_order_collections.md#5-status-state-machine)). `markPacked` also writes the bags (with `shortCode`, 05 § 4), updates `staff/{uid}/dailyStats/{today}`, and calls `dispatchOrder(orderId)`. The order-problem callables are in § 9b.

---

## 5. Delivery code + completion

```typescript
export const verifyDeliveryCode = onCall<{ orderId: string; code: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const orderRef = db.doc(`orders/${req.data.orderId}`);
  const codeRef = orderRef.collection("private").doc("delivery");

  return db.runTransaction(async (tx) => {
    const [order, secret] = await Promise.all([tx.get(orderRef), tx.get(codeRef)]);
    if (order.data()?.courierId !== uid) fail("NOT_YOUR_ORDER");
    const attempts = secret.data()?.attempts ?? 0;
    if (attempts >= 5) fail("CODE_LOCKED");
    if (secret.data()?.code !== req.data.code) {
      tx.update(codeRef, { attempts: attempts + 1 });
      return { ok: false, attemptsLeft: 4 - attempts }; // return, don't throw: the attempt must be saved
    }
    tx.update(codeRef, { verifiedAt: Timestamp.now() });
    return { ok: true, attemptsLeft: 5 - attempts };
  });
});
```

> **Why return instead of `fail()` for a wrong code:** a throw inside `runTransaction` rolls back every write in it, so the attempt counter would never increase and the 5-attempt lock would never trigger. The app shows "Wrong code, N tries left" from `attemptsLeft`.

`completeDelivery({orderId, requestId, cashCollected?, doorPhotoPath?})`, all in one transaction, wrapped in `once(tx, orderId, requestId, …)` so an outbox replay after a timeout returns the first result instead of paying twice:

1. `private/delivery.verifiedAt` exists (or `doorPhotoPath` when the customer is away, then support review), `arriving → delivered`, `deliveredAt`.
2. Payment: cash → `payment.status = paid`, `cashCollected` (770000), `changeGiven` (4700); card → capture.
3. Courier earnings entries (`delivery_fee`, `boost`), `dailyStats`, `todayEarnings`, `weekEarnings`, `deliveriesCount`; remove the order from `activeOrderIds`. **Multi-stop:** set `status: online` **only if `activeOrderIds` is now empty**. Otherwise the courier stays `on_run` and goes on to the next stop.
4. **Cash orders:** `couriers/{uid}.cashInHand += totals.total` (765300), and `cashDueBy` = today 22:00 if it was empty (C03.4).
5. Customer: `wallets/{uid}` points ledger `+pointsEarned`, `pointsBalance`.
5b. Customer tier: `users/{uid}.spend12mMillimes += totals.total`, then `applyTierUpgrade()` (§ 8d). **Upgrades only**: a delivery can lift a customer to Silver or Gold right away, but never lowers them. Read `config/tiers` and `users/{uid}` at the start of the transaction, with the other reads.
6. `courierLocations/{uid}.visibleTo`: `FieldValue.arrayRemove(customerId)`, so only this customer is removed and the next customer on the run keeps tracking.
7. Return `{fee, tip, boost, onTime}` for C02.5.

The same `requestId` protection applies to `scanBag`, `confirmPickup`, `markPacked`, `markArriving`, `returnToStore` and `reportIssue`: every callable the team app may replay from its outbox.

---

## 6. Dispatch

**Multi-stop runs (decided: yes).** A courier can carry up to `MAX_ACTIVE_ORDERS = 2` orders on one run, which is what the customer sees as "1 stop before you". A courier who is already `on_run` gets a second offer only if **both orders come from the same store** and the courier **hasn't picked up the first one yet**. They collect both bags in one stop and drop them off one after the other. This keeps the customer's ETA honest. Couriers who are `online` are always preferred.

```typescript
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";

const MAX_ACTIVE_ORDERS = 2;

/** Can this on-run courier take one more order from `storeId`? */
async function canStack(courier: FirebaseFirestore.DocumentData, storeId: string): Promise<boolean> {
  const active: string[] = courier.activeOrderIds ?? [];
  if (active.length === 0 || active.length >= MAX_ACTIVE_ORDERS) return false;
  const first = (await db.doc(`orders/${active[0]}`).get()).data();
  return first?.storeId === storeId && first?.status !== "picked_up" && first?.status !== "arriving";
}

export async function dispatchOrder(orderId: string): Promise<void> {
  const order = (await db.doc(`orders/${orderId}`).get()).data()!;
  const store = (await db.doc(`stores/${order.storeId}`).get()).data()!;

  // 1. Candidates in the zone: online couriers, plus on-run couriers who can stack this order.
  const couriers = await db.collection("couriers")
    .where("zoneId", "==", order.zoneId).where("status", "in", ["online", "on_run"]).get();
  const alreadyOffered = new Set((await db.collection("dispatchOffers").where("orderId", "==", orderId).get())
    .docs.map((d) => d.data().courierId));

  const free: string[] = [];
  const stackable: string[] = [];
  for (const d of couriers.docs) {
    if (alreadyOffered.has(d.id)) continue;
    const c = d.data();
    if (c.status === "online") free.push(d.id);
    else if (await canStack(c, order.storeId)) stackable.push(d.id);
  }

  // 2. Rank by distance to the store (courierLocations), take the closest. Free couriers first.
  //    rankByDistance drops couriers whose location updatedAt is older than 60 s: no recent
  //    GPS = no connection, so an offer would never reach them (06 § 8b, S02.3).
  let ranked = await rankByDistance(free, store.location);
  if (ranked.length === 0) ranked = await rankByDistance(stackable, store.location);
  if (ranked.length === 0) return; // expireOffers retries every minute

  const live = (await db.doc(`zones/${order.zoneId}/live/current`).get()).data();
  const boost = live?.surgeMultiplier ?? 1;
  const baseFee = 5000;
  const fee = Math.round(baseFee * boost);

  await db.collection("dispatchOffers").add({
    orderId, orderNumber: order.number, courierId: ranked[0].courierId, status: "pending",
    fee, boostMultiplier: boost, distanceMeters: ranked[0].meters + order.delivery.distanceMeters,
    etaMinutes: Math.ceil((ranked[0].meters + order.delivery.distanceMeters) / 250), // ~15 km/h city average
    itemCount: order.itemCount, bagCount: order.bagCount,
    paymentLabel: order.payment.method === "cash" ? "cash" : "paid_online",
    pickup: { name: store.hubName, address: store.address, distanceMeters: ranked[0].meters },
    dropoff: { name: `${order.customer.firstName} ${order.customer.lastName.charAt(0)}.`, area: order.delivery.area, distanceMeters: order.delivery.distanceMeters },
    expiresAt: Timestamp.fromMillis(Date.now() + 30_000),
    createdAt: Timestamp.now(),
  });
  // onDocumentCreated(dispatchOffers) sends the high-priority push to that courier (10).
}

export const acceptOffer = onCall<{ offerId: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const offerRef = db.doc(`dispatchOffers/${req.data.offerId}`);

  await db.runTransaction(async (tx) => {
    const offer = (await tx.get(offerRef)).data();
    if (!offer || offer.courierId !== uid) fail("NOT_YOUR_ORDER");
    if (offer.status !== "pending" || offer.expiresAt.toMillis() < Date.now()) fail("OFFER_EXPIRED");

    const orderRef = db.doc(`orders/${offer.orderId}`);
    const [order, courier, staff] = await Promise.all([
      tx.get(orderRef), tx.get(db.doc(`couriers/${uid}`)), tx.get(db.doc(`staff/${uid}`)),
    ]);
    if (order.data()?.courierId) fail("OFFER_TAKEN");

    const c = courier.data()!;
    if ((c.activeOrderIds ?? []).length >= MAX_ACTIVE_ORDERS) fail("RUN_FULL");
    const s = staff.data()!;
    const ready = order.data()!.status === "ready_for_pickup";
    tx.update(orderRef, {
      courierId: uid,
      staffIds: [...(order.data()!.staffIds ?? []), uid],
      courier: { name: `${s.firstName} ${s.lastName.charAt(0)}.`, vehicleType: c.vehicleType, plate: c.plate ?? "",
                 ratingAvg: c.ratingAvg, deliveriesCount: c.deliveriesCount, photoUrl: s.photoUrl ?? null },
      ...(ready ? { status: "courier_assigned", statusGroup: "active" } : {}),
      updatedAt: Timestamp.now(),
    });
    tx.update(offerRef, { status: "accepted" });
    tx.update(db.doc(`couriers/${uid}`), { status: "on_run", activeOrderIds: FieldValue.arrayUnion(offer.orderId) });
    // arrayUnion, not a new array: on a multi-stop run the first customer must keep seeing the courier.
    tx.set(db.doc(`courierLocations/${uid}`), { visibleTo: FieldValue.arrayUnion(order.data()!.customerId) }, { merge: true });
  });
});
```

---

## 7. Staff claims

```typescript
import { getAuth } from "firebase-admin/auth";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

export const onStaffWritten = onDocumentWritten("staff/{uid}", async (event) => {
  const uid = event.params.uid;
  const after = event.data?.after.data();

  if (!after || after.isActive === false) {
    await getAuth().setCustomUserClaims(uid, null);
    await getAuth().updateUser(uid, { disabled: true });
    await getAuth().revokeRefreshTokens(uid); // signs them out within the hour
    return;
  }

  await getAuth().setCustomUserClaims(uid, { role: after.role, storeId: after.storeId });

  if (after.role === "courier") {
    await getFirestore().doc(`couriers/${uid}`).set({
      status: "offline", vehicleType: "scooter", zoneId: "", activeOrderIds: [],
      ratingAvg: 0, ratingCount: 0, deliveriesCount: 0, acceptRate: 1,
      weekEarnings: 0, todayEarnings: 0, updatedAt: Timestamp.now(),
    }, { merge: true }); // merge keeps existing stats on role edits
  }
});
```

**Why `merge` and why it is idempotent:** if the trigger runs twice, `setCustomUserClaims` with the same values and a merged `couriers` doc give the same result. Careful: the merge above resets `status` and stats on **every** staff edit. In the real code, write the defaults only when `before` didn't exist or wasn't a courier.

---

## 8. Aggregates (example: product rating)

```typescript
export const onReviewWritten = onDocumentWritten("products/{productId}/reviews/{uid}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  const productRef = getFirestore().doc(`products/${event.params.productId}`);

  await getFirestore().runTransaction(async (tx) => {
    const p = (await tx.get(productRef)).data()!;
    let sum = (p.ratingAvg ?? 0) * (p.ratingCount ?? 0);
    let count = p.ratingCount ?? 0;
    if (before) { sum -= before.rating; count -= 1; }
    if (after) { sum += after.rating; count += 1; }
    tx.update(productRef, { ratingCount: count, ratingAvg: count === 0 ? 0 : Math.round((sum / count) * 10) / 10 });
  });
});
```

Triggers can be delivered twice. For counters where double counting matters (followers), store the processed `event.id` in a small `processedEvents/{eventId}` doc inside the same transaction and skip if it exists.

---

## 8b. Order problems, coupons, referrals, shifts

`firebase/functions/src/orders/problems.ts`, `orders/slots.ts`, `coupons/claimCoupon.ts`, `referrals/referrals.ts`, `staff/endShift.ts`, `notify/whatsapp.ts`, `users/tiers.ts`. Export them from `index.ts`. They reuse `transition`, `fail`, `requireAuth`, `once` and the money constants. `problems.ts` also imports `buildSlots`, `slotKey`, `openUntil`, `DEFAULT_SLOT_CAPACITY` from `./slots` and `FieldValue` from `firebase-admin/firestore`.

### Shared helper: refund an order (cancel and failed-cancel)

```typescript
import { FieldValue, Timestamp, Transaction } from "firebase-admin/firestore";

/** Writes every "undo" of an order inside the caller's transaction. Reads happen in the caller. */
export function refundWrites(
  tx: Transaction,
  orderRef: FirebaseFirestore.DocumentReference,
  order: FirebaseFirestore.DocumentData,
  wallet: FirebaseFirestore.DocumentData,
  items: FirebaseFirestore.QueryDocumentSnapshot[],
  coupon: FirebaseFirestore.DocumentData | undefined, // coupons/{order.couponCode}, read by the caller
  reason: string,
): { refundMillimes: number; refundTo: "wallet" | "card" | "none"; couponRestored: boolean } {
  const now = Timestamp.now();
  const walletRef = db.doc(`wallets/${order.customerId}`);
  let refundMillimes = 0;
  let refundTo: "wallet" | "card" | "none" = "none";

  // 1. Money: wallet-paid orders are refunded instantly (10.6 "the refund is instant").
  if (order.payment.method === "wallet" && order.payment.status === "paid") {
    refundMillimes = order.totals.total;
    refundTo = "wallet";
    tx.set(walletRef, { walletBalance: FieldValue.increment(refundMillimes), updatedAt: now }, { merge: true });
    tx.set(walletRef.collection("transactions").doc(), {
      amount: refundMillimes, reason: "refund", orderId: orderRef.id,
      balanceAfter: (wallet.walletBalance ?? 0) + refundMillimes, createdAt: now,
    });
  }
  if (order.payment.method === "card") refundTo = "card"; // Konnect refund API, after launch

  // 2. Points back.
  if (order.pointsUsed > 0) {
    tx.set(walletRef, { pointsBalance: FieldValue.increment(order.pointsUsed), updatedAt: now }, { merge: true });
    tx.set(walletRef.collection("pointsLedger").doc(), {
      delta: order.pointsUsed, reason: "refund", orderId: orderRef.id,
      balanceAfter: (wallet.pointsBalance ?? 0) + order.pointsUsed, createdAt: now,
    });
  }

  // 3. Coupon back in the pocket (10.6 "Coupon FREESHIP is back in My coupons · Valid until 6 Nov").
  //    Restored only if it is still valid: an expired coupon stays "used" (it would only move to Expired anyway).
  let couponRestored = false;
  if (order.couponCode && coupon && coupon.isActive && coupon.endsAt.toMillis() > Date.now()) {
    couponRestored = true;
    tx.set(db.doc(`coupons/${order.couponCode}/redemptions/${order.customerId}`), { count: FieldValue.increment(-1) }, { merge: true });
    tx.update(db.doc(`coupons/${order.couponCode}`), { redeemedCount: FieldValue.increment(-1) });
    tx.set(db.doc(`users/${order.customerId}/coupons/${order.couponCode}`),
      { status: "available", usedOrderId: null, usedAt: null, endsAt: coupon.endsAt }, { merge: true });
  }

  // 4. Stock back on the shelf (items never picked; picked items are restocked by hand at the hub).
  for (const item of items) {
    const it = item.data();
    if (it.pickStatus === "pending") {
      tx.update(db.doc(`stores/${order.storeId}/inventory/${it.variantId}`), { quantity: FieldValue.increment(it.quantity) });
    }
  }

  tx.set(orderRef.collection("events").doc(), { type: "refund_issued", actorRole: "system", at: now, message: { en: reason } });
  return { refundMillimes, refundTo, couponRestored };
}
```

Cash-on-delivery orders cancelled before delivery: `refundTo = "none"`, `refundMillimes = 0`. That's the "Payment · Nothing to pay · Cash on delivery — no money was taken" card on 10.6 (#DK-1990).

```typescript
/** Reads the order's coupon (or nothing). Call it with the other reads, before any write. */
export async function readOrderCoupon(tx: Transaction, order: FirebaseFirestore.DocumentData) {
  return order.couponCode ? (await tx.get(db.doc(`coupons/${order.couponCode}`))).data() : undefined;
}
```

### `cancelOrder` (10.2 → 10.6)

```typescript
const CUSTOMER_REASONS = ["ordered_by_mistake", "changed_mind", "delivery_too_slow", "found_cheaper", "other"];

export const cancelOrder = onCall<{ orderId: string; reason: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const role = (req.auth!.token.role as string | undefined) ?? "customer";
  const orderRef = db.doc(`orders/${req.data.orderId}`);

  return db.runTransaction(async (tx) => {
    const orderSnap = await tx.get(orderRef);
    const order = orderSnap.data();
    if (!order) fail("NOT_YOUR_ORDER");

    if (role === "customer") {
      if (order.customerId !== uid) fail("NOT_YOUR_ORDER");
      if (order.status !== "placed") fail("CANCEL_NOT_ALLOWED", { status: order.status }); // "before packing started"
      if (!CUSTOMER_REASONS.includes(req.data.reason)) fail("CANCEL_NOT_ALLOWED");
    } else if (role !== "manager" && role !== "admin") {
      fail("NOT_STAFF");
    } else if (["delivered", "collected", "cancelled"].includes(order.status)) {
      fail("INVALID_TRANSITION", { status: order.status });
    }

    const [walletSnap, itemsSnap, coupon] = await Promise.all([
      tx.get(db.doc(`wallets/${order.customerId}`)),
      tx.get(orderRef.collection("items")),
      readOrderCoupon(tx, order),
    ]);

    // ── writes ──
    const { refundMillimes, refundTo, couponRestored } =
      refundWrites(tx, orderRef, order, walletSnap.data() ?? {}, itemsSnap.docs, coupon, req.data.reason);
    const now = Timestamp.now();
    tx.update(orderRef, {
      status: "cancelled", statusGroup: "cancelled", updatedAt: now,
      cancel: { by: role, reason: req.data.reason, at: now, refundMillimes, refundTo, couponRestored },
    });
    tx.set(orderRef.collection("events").doc(), { type: "status_changed", status: "cancelled", actorRole: role, actorId: uid, at: now });
    return { refundMillimes, refundTo, couponRestored };
  });
});
```

`onOrderStatusChanged` then sends the push and the `refund_issued` inbox entry. For #DK-1990 (cash, 109.800 TND, FREESHIP) the result is `{refundMillimes: 0, refundTo: "none", couponRestored: true}`, which 10.6 shows as "Nothing to pay" and "Coupon FREESHIP is back in My coupons · Valid until 6 Nov".

### Door wait: `logContactAttempt`, `returnToStore`, `confirmReturn` (C03.2, 10.7)

```typescript
const DOOR_WAIT_MS = 5 * 60_000; // Figma C03.2 "Wait at the door for 5 min"

export const returnToStore = onCall<{ orderId: string; requestId?: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const ref = db.doc(`orders/${req.data.orderId}`);
  return db.runTransaction((tx) => once(tx, req.data.orderId, req.data.requestId, async () => {
    const order = (await tx.get(ref)).data();
    if (!order || order.courierId !== uid) fail("NOT_YOUR_ORDER");
    if (order.status !== "arriving") fail("INVALID_TRANSITION", { status: order.status });
    const started = order.doorWait?.startedAt?.toMillis();
    if (!started || Date.now() < started + DOOR_WAIT_MS || (order.doorWait.callAttempts ?? 0) < 2) {
      fail("WAIT_NOT_OVER", { waitUntil: (started ?? Date.now()) + DOOR_WAIT_MS });
    }
    const now = Timestamp.now();
    tx.update(ref, {
      status: "returning", statusGroup: "active", updatedAt: now,
      failure: { reason: "customer_not_answering", returnedAt: null, resolveBy: null },
    });
    tx.set(ref.collection("events").doc(), { type: "status_changed", status: "returning", actorRole: "courier", actorId: uid, at: now });
    // The courier stops being visible to this customer.
    tx.set(db.doc(`courierLocations/${uid}`), { visibleTo: FieldValue.arrayRemove(order.customerId) }, { merge: true });
    return { ok: true };
  }));
});
```

`logContactAttempt({orderId, kind})` increments `doorWait.callAttempts` and writes a `call_attempt` event. `confirmReturn` (picker or courier at the hub, all bags scanned back) does `returning → failed` with `failure.returnedAt = now` and `failure.resolveBy = now + 24 h`, removes the order from the courier's `activeOrderIds`, and the status trigger pushes "We couldn't reach you".

### `resolveFailedDelivery` (10.7 "What would you like to do?")

```typescript
type Choice =
  | { choice: "redeliver"; slotStart: number }  // epoch ms, one of the starts returned by getRedeliverySlots
  | { choice: "store_pickup" }
  | { choice: "cancel" };

export const resolveFailedDelivery = onCall<{ orderId: string } & Choice>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const orderRef = db.doc(`orders/${req.data.orderId}`);

  return db.runTransaction(async (tx) => {
    // ── reads (all of them, before any write) ──
    const order = (await tx.get(orderRef)).data();
    if (!order || order.customerId !== uid) fail("NOT_YOUR_ORDER");
    if (order.status !== "failed") fail("INVALID_TRANSITION", { status: order.status });
    const store = (await tx.get(db.doc(`stores/${order.storeId}`))).data()!;
    const now = Timestamp.now();

    if (req.data.choice === "redeliver") {
      // Re-check the slot the customer picked: someone else may have taken the last place.
      const slotStart = req.data.slotStart;
      const slot = buildSlots(store, Date.now()).find((s) => s.start === slotStart);
      if (!slot) fail("SLOT_UNAVAILABLE");
      const loadRef = db.doc(`stores/${order.storeId}/slotLoad/${slotKey(slotStart)}`);
      const load = (await tx.get(loadRef)).data()?.count ?? 0;
      if (load >= (store.redeliverySlotCapacity ?? DEFAULT_SLOT_CAPACITY)) fail("SLOT_UNAVAILABLE");
      const slotEnd = slot.end;

      // ── writes ──
      tx.set(loadRef, { count: FieldValue.increment(1), slotStart: Timestamp.fromMillis(slotStart) }, { merge: true });
      tx.update(orderRef, {
        status: "ready_for_pickup", statusGroup: "active", updatedAt: now,
        courierId: null, courier: null,
        "delivery.slot": "scheduled",
        "delivery.slotStart": Timestamp.fromMillis(slotStart),
        "delivery.slotEnd": Timestamp.fromMillis(slotEnd),
        resolution: { choice: "redeliver", slotStart: Timestamp.fromMillis(slotStart), slotEnd: Timestamp.fromMillis(slotEnd), at: now },
      });
      // dispatchScheduled starts dispatch 30 min before slotStart. Free: no new delivery fee.
    } else if (req.data.choice === "store_pickup") {
      const readyUntil = openUntil(store, Date.now());
      if (!store.isOpen || readyUntil === null) fail("STORE_CLOSED");
      tx.update(orderRef, {
        status: "awaiting_customer_pickup", statusGroup: "active", fulfilment: "store_pickup", updatedAt: now,
        resolution: { choice: "store_pickup", readyUntil: Timestamp.fromMillis(readyUntil), at: now },
      });
    } else {
      const [walletSnap, itemsSnap, coupon] = await Promise.all([
        tx.get(db.doc(`wallets/${uid}`)), tx.get(orderRef.collection("items")), readOrderCoupon(tx, order),
      ]);
      const { refundMillimes, refundTo, couponRestored } =
        refundWrites(tx, orderRef, order, walletSnap.data() ?? {}, itemsSnap.docs, coupon, "delivery_failed");
      tx.update(orderRef, {
        status: "cancelled", statusGroup: "cancelled", updatedAt: now,
        cancel: { by: "customer", reason: "delivery_failed", at: now, refundMillimes, refundTo, couponRestored },
        resolution: { choice: "cancel", at: now },
      });
    }
    tx.set(orderRef.collection("events").doc(), { type: "note", actorRole: "customer", actorId: uid, at: now, message: { en: `Customer chose ${req.data.choice}` } });
    return { ok: true };
  });
});
```

> Each branch does its extra reads (slot load, wallet, items, coupon) after the store read but **before** its first write. That's allowed: Firestore only requires that no read comes **after** a write. `expireFailedOrders` uses the same `refundWrites` + `readOrderCoupon`, so an auto-cancel also gives the coupon back.

### Redelivery slots: `getRedeliverySlots` + shared helpers (10.7)

**Decided (recommended way):** slots are **1 hour** long, built from the store's `openingHours`, start **at least 60 minutes from now** (time to dispatch a courier), and each slot has a **capacity** (6 redeliveries per store per hour). If nothing is left today, tomorrow's slots are offered. Redelivery stays **free**.

**Launch values (decided):** capacity **6** and lead time **60 min** are the defaults. Each store can override them with `stores/{storeId}.redeliverySlotCapacity` and `redeliveryLeadMinutes` (doc 03), so you tune a busy store from the console without a deploy. Why 6: a quick-commerce hub runs roughly 10–20 couriers at peak, and failed deliveries are a few percent of orders, so 6 redeliveries an hour keeps them from crowding out new orders. Why 60 min: dispatch + pickup + ride is about 30–40 min, and the extra margin keeps the promised window honest.

`firebase/functions/src/orders/slots.ts`:

```typescript
import { getFirestore } from "firebase-admin/firestore";
import { onCall } from "firebase-functions/v2/https";
import { fail, requireAuth } from "../shared/errors";

const db = getFirestore();
export const DEFAULT_SLOT_CAPACITY = 6;
export const DEFAULT_LEAD_MINUTES = 60;
const SLOT_MS = 60 * 60_000;
const TZ_OFFSET_MS = 60 * 60_000; // Africa/Tunis is UTC+1 all year (no DST)
const DAYS = ["sun", "mon", "tue", "wed", "thu", "fri", "sat"];

/** "2026-10-29_20": one load counter per store and local hour. */
export function slotKey(startMs: number): string {
  const local = new Date(startMs + TZ_OFFSET_MS);
  return `${local.toISOString().slice(0, 10)}_${String(local.getUTCHours()).padStart(2, "0")}`;
}

/** Opening window of a local day in epoch ms, from `openingHours.{mon..sun}` = "08:00-23:00". */
function windowFor(store: FirebaseFirestore.DocumentData, dayStartLocalMs: number): [number, number] | null {
  const day = DAYS[new Date(dayStartLocalMs).getUTCDay()];
  const hours: string | undefined = store.openingHours?.[day];
  if (!hours) return null;
  const [open, close] = hours.split("-").map((hm) => {
    const [h, m] = hm.split(":").map(Number);
    return dayStartLocalMs - TZ_OFFSET_MS + (h * 60 + m) * 60_000;
  });
  return [open, close];
}

function localDayStart(nowMs: number, plusDays: number): number {
  const local = new Date(nowMs + TZ_OFFSET_MS);
  return Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate() + plusDays);
}

/** All 1-hour slots for today (or tomorrow if today has none left), starting ≥ now + the store's lead time. */
export function buildSlots(store: FirebaseFirestore.DocumentData, nowMs: number): { start: number; end: number }[] {
  const LEAD_MS = (store.redeliveryLeadMinutes ?? DEFAULT_LEAD_MINUTES) * 60_000;
  for (const plusDays of [0, 1]) {
    const w = windowFor(store, localDayStart(nowMs, plusDays));
    if (!w) continue;
    const slots: { start: number; end: number }[] = [];
    // Slots begin on the hour.
    let start = Math.ceil(Math.max(w[0], nowMs + LEAD_MS) / SLOT_MS) * SLOT_MS;
    for (; start + SLOT_MS <= w[1]; start += SLOT_MS) slots.push({ start, end: start + SLOT_MS });
    if (slots.length) return slots;
  }
  return [];
}

/** "Ready now · open until 22:00": today's closing time if the store is open now, else null. */
export function openUntil(store: FirebaseFirestore.DocumentData, nowMs: number): number | null {
  const w = windowFor(store, localDayStart(nowMs, 0));
  return w && nowMs >= w[0] && nowMs < w[1] ? w[1] : null;
}

export const getRedeliverySlots = onCall<{ orderId: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const order = (await db.doc(`orders/${req.data.orderId}`).get()).data();
  if (!order || order.customerId !== uid) fail("NOT_YOUR_ORDER");
  const store = (await db.doc(`stores/${order.storeId}`).get()).data()!;
  const capacity = store.redeliverySlotCapacity ?? DEFAULT_SLOT_CAPACITY;
  const now = Date.now();

  const slots = buildSlots(store, now);
  // Direct document reads by id: no query, so no index is needed.
  const loads = await db.getAll(...slots.map((s) => db.doc(`stores/${order.storeId}/slotLoad/${slotKey(s.start)}`)));
  return {
    slots: slots
      .map((s, i) => ({ start: s.start, end: s.end, free: capacity - (loads[i].data()?.count ?? 0) }))
      .filter((s) => s.free > 0),
    storePickupOpenUntil: store.isOpen ? openUntil(store, now) : null, // epoch ms or null
  };
});
```

**Why a load counter doc per hour:** `resolveFailedDelivery` can read and increment it inside its transaction, so two customers can't both take the last place. Reading 1–14 docs by id is cheap and needs **no index**. `cleanupSlotLoad` deletes old counters.

The app shows the **first** free slot as "Deliver again today · 20:00 – 21:00 · free" (10.7, #DK-2051 failed at 18:25, so 19:25 is the earliest start, rounded up to 20:00) and lists the others when tapped. If `slots` is empty, the redeliver choice is hidden. If `storePickupOpenUntil` is null, "Pick up at Dukkan Lac 2" is hidden.

### 8c. WhatsApp "courier at your door" message (C03.2)

**Decided:** the extra message for "customer not answering" goes through **WhatsApp**, not SMS. In Tunisia almost every customer has WhatsApp, a template message is cheaper than an international SMS, and it shows the Dukkan name and logo. We use Meta's **WhatsApp Business Platform (Cloud API)**. It is not a Firebase product; a Cloud Function calls it over HTTPS.

**One-time setup**

| Step | Where | Result |
|---|---|---|
| 1. Meta Business account (verified) | business.facebook.com | Owner of the WhatsApp assets |
| 2. WhatsApp Business account + phone number | Meta developer app → WhatsApp | A **phone number id** (not the number itself) |
| 3. Permanent token | System user with `whatsapp_business_messaging` permission | Stored as the Functions secret `WHATSAPP_TOKEN` |
| 4. Template `courier_at_door` (category **Utility**) in `ar`, `fr`, `en` | WhatsApp Manager → Templates | Approved by Meta (usually minutes to a day) |
| 5. Phone number id | Functions param `WHATSAPP_PHONE_NUMBER_ID` in `.env` | |

Template body (en), two parameters: `Hi {{1}}, your Dukkan courier is at your door with order {{2}}. Please open or call them back within 5 minutes.`

**Consent.** WhatsApp requires the customer to have **opted in** to receive messages from us. We store it explicitly as `users/{uid}.whatsappOptIn` (bool, default `false`; it becomes `true` only when the customer ticks the **unchecked** box "Send me delivery updates on WhatsApp" under the phone field at checkout or in Edit profile, or turns on the switch in 11.3 App settings → Notifications → "WhatsApp delivery messages"). A separate flag (instead of reusing "Order updates") lets a customer keep push but stop WhatsApp. Order/delivery messages are *utility* messages, so they don't need `marketingNotificationsEnabled`.

**Phone format.** The number must be **E.164** (`+21622345678`). `placeOrder` already fails with `PHONE_REQUIRED` when there is no phone; it also normalizes `22 345 678` → `+21622345678` before copying it into `orders/{id}.customer.phone`.

**Pricing (checked October 2026).** Meta charges **per delivered template message** (since 1 July 2025), by category and the recipient's country code. Tunisia falls in Meta's **"Rest of Africa"** price group, where a **utility** template costs about **0.004 USD** (marketing ≈ 0.0225 USD). Since **1 October 2026**, utility templates are charged even inside the 24-hour customer-service window. We send one message per failed door wait, so even 1,000 a month costs about 4 USD. Re-check Meta's rate card before launch; prices change every quarter.

**Graph API version (checked October 2026).** The current version is **v26.0** (July 2026). v25.0 stays supported until July 2028. We pin the version in a param, so upgrading is a config change, not a code change.

`firebase/functions/src/notify/whatsapp.ts`:

```typescript
import { defineSecret, defineString } from "firebase-functions/params";
import { logger } from "firebase-functions";

export const WHATSAPP_TOKEN = defineSecret("WHATSAPP_TOKEN");
const WHATSAPP_PHONE_NUMBER_ID = defineString("WHATSAPP_PHONE_NUMBER_ID");
// Pinned Graph API version. Override in functions/.env (WHATSAPP_GRAPH_VERSION=v27.0) when Meta ships a new one.
const GRAPH_VERSION_PARAM = defineString("WHATSAPP_GRAPH_VERSION", { default: "v26.0" });

/**
 * Sends an approved template message. Returns the WhatsApp message id, or null on failure.
 * Never throws: the caller falls back to push only.
 */
export async function sendWhatsAppTemplate(
  toE164: string, template: string, language: "ar" | "fr" | "en", params: string[],
): Promise<string | null> {
  try {
    const res = await fetch(`https://graph.facebook.com/${GRAPH_VERSION_PARAM.value()}/${WHATSAPP_PHONE_NUMBER_ID.value()}/messages`, {
      method: "POST",
      headers: { Authorization: `Bearer ${WHATSAPP_TOKEN.value()}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        to: toE164.replace("+", ""),
        type: "template",
        template: {
          name: template,
          language: { code: language },
          components: [{ type: "body", parameters: params.map((text) => ({ type: "text", text })) }],
        },
      }),
    });
    const body = await res.json();
    if (!res.ok) {
      logger.warn("WhatsApp send failed", { status: res.status, error: body?.error?.message });
      return null;
    }
    return body?.messages?.[0]?.id ?? null;
  } catch (e) {
    logger.warn("WhatsApp send threw", e);
    return null;
  }
}
```

Used by `reportIssue` when `type == "customer_not_answering"` (declare `secrets: [WHATSAPP_TOKEN]` on that callable). The send runs **after** the transaction that starts the door wait, because a network call must never sit inside a transaction (it could be retried):

```typescript
// inside reportIssue, after the transaction that set doorWait.startedAt and wrote the issue
const customer = (await db.doc(`users/${order.customerId}`).get()).data() ?? {};
await sendPush(order.customerId, { type: "courier_at_door", orderId }); // always (10)

const lang = (customer.languageCode ?? "fr") as "ar" | "fr" | "en";
const messageId = customer.whatsappOptIn && order.customer.phone
  ? await sendWhatsAppTemplate(order.customer.phone, "courier_at_door", lang, [order.customer.firstName, order.number])
  : null;

await db.doc(`orders/${orderId}`).update({ "doorWait.whatsappSentAt": messageId ? Timestamp.now() : null });
await db.collection(`orders/${orderId}/events`).add({
  type: messageId ? "whatsapp_sent" : "whatsapp_failed",
  actorRole: "system", at: Timestamp.now(), ...(messageId ? { whatsappMessageId: messageId } : {}),
});
```

**Fallback:** if the customer has no opt-in, no phone, or Meta returns an error, only the push goes out and a `whatsapp_failed` event is logged (support can see it). The door-wait timer runs either way.

**The courier's "Message" button (C03.2)** opens a normal WhatsApp chat with `https://wa.me/21622345678` (number without `+`) via an `ACTION_VIEW` intent. Privacy trade-off: the courier already sees the customer's phone in the order snapshot (`orders/{id}.customer.phone`) for calling, so the deep link reveals nothing new; it does put the chat in the courier's personal WhatsApp history. If that becomes a problem later, replace it with a masked relay. For v1 the simple link is enough.

### 8d. Customer tiers (11.1 "Gold member", 07.3 "Reach Gold to unlock")

**Decided:** the tier comes from **total spend**:

- **Spend** = sum of `totals.total` of **delivered** (or `collected`) orders in the **last 12 months**. Cancelled and failed orders don't count, and refunds are subtracted.
- **Tiers:** `member` below 500 TND, `silver` from 500 TND, `gold` from 1,500 TND.
- **Upgrades are immediate**, in `completeDelivery`. **Downgrades happen only in `nightlyTiers`**, so a customer never loses Gold in the middle of an order.

Thresholds live in `config/tiers` (read-only for clients) so marketing can change them without an app update:

```json
{ "silverMillimes": 500000, "goldMillimes": 1500000, "windowMonths": 12 }
```

`firebase/functions/src/users/tiers.ts`:

```typescript
import { Timestamp, Transaction } from "firebase-admin/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { getFirestore } from "firebase-admin/firestore";
import { TIER_RANK } from "../shared/money";

const db = getFirestore();
type Tier = "member" | "silver" | "gold";
interface TierConfig { silverMillimes: number; goldMillimes: number; windowMonths: number }

export function tierFor(spend: number, c: TierConfig): Tier {
  return spend >= c.goldMillimes ? "gold" : spend >= c.silverMillimes ? "silver" : "member";
}

/** The "x TND to Gold" hint on 11.1. */
export function nextTierInfo(spend: number, c: TierConfig) {
  if (spend >= c.goldMillimes) return { nextTier: null, toNextTierMillimes: 0 };
  if (spend >= c.silverMillimes) return { nextTier: "gold", toNextTierMillimes: c.goldMillimes - spend };
  return { nextTier: "silver", toNextTierMillimes: c.silverMillimes - spend };
}

/** In completeDelivery: `user` and `config` were read at the start of the transaction. Upgrade only. */
export function applyTierUpgrade(
  tx: Transaction, uid: string, user: FirebaseFirestore.DocumentData, config: TierConfig, orderTotal: number,
): void {
  const spend = (user.spend12mMillimes ?? 0) + orderTotal;
  const computed = tierFor(spend, config);
  const current: Tier = user.tier ?? "member";
  const tier = TIER_RANK[computed] > TIER_RANK[current] ? computed : current;
  tx.update(db.doc(`users/${uid}`), {
    spend12mMillimes: spend,
    tier,
    ...nextTierInfo(spend, config),
    ...(tier !== current ? { tierUpdatedAt: Timestamp.now() } : {}),
  });
}

/** Exact recompute (and the only downgrade path). */
export const nightlyTiers = onSchedule({ schedule: "30 2 * * *", timeZone: "Africa/Tunis" }, async () => {
  const config = (await db.doc("config/tiers").get()).data() as TierConfig;
  const since = new Date();
  since.setMonth(since.getMonth() - config.windowMonths);

  const delivered = await db.collection("orders")
    .where("statusGroup", "==", "delivered")
    .where("deliveredAt", ">=", Timestamp.fromDate(since))
    .select("customerId", "totals.total", "refundedMillimes").get();

  const spend = new Map<string, number>();
  delivered.forEach((d) => {
    const o = d.data();
    spend.set(o.customerId, (spend.get(o.customerId) ?? 0) + o.totals.total - (o.refundedMillimes ?? 0));
  });

  // Customers who had spend before but none in the window must drop to member too.
  const tiered = await db.collection("users").where("tier", "in", ["silver", "gold"]).select("tier").get();
  tiered.forEach((d) => { if (!spend.has(d.id)) spend.set(d.id, 0); });

  const writer = db.bulkWriter();
  for (const [uid, s] of spend) {
    const tier = tierFor(s, config);
    writer.set(db.doc(`users/${uid}`), {
      spend12mMillimes: s, tier, ...nextTierInfo(s, config), tierUpdatedAt: Timestamp.now(),
    }, { merge: true });
  }
  await writer.close();
});
```

> `confirmCustomerPickup` sets `deliveredAt` too (status `collected`, group `delivered`), so store pickups count as spend. `refundedMillimes` is set on an order when a missing item is refunded after delivery (picker swap/remove refunds happen before delivery and already lower `totals.total`). The query needs index **#35** (`statusGroup` + `deliveredAt`, see 08).

### `claimCoupon` (07.3 "Enter a promo code · Add")

```typescript
export const claimCoupon = onCall<{ code: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const code = (req.data.code ?? "").trim().toUpperCase().slice(0, 20);

  return db.runTransaction(async (tx) => {
    const [couponSnap, userSnap, mineSnap, redemptionSnap] = await Promise.all([
      tx.get(db.doc(`coupons/${code}`)),
      tx.get(db.doc(`users/${uid}`)),
      tx.get(db.doc(`users/${uid}/coupons/${code}`)),
      tx.get(db.doc(`coupons/${code}/redemptions/${uid}`)),
    ]);
    const coupon = couponSnap.data();
    const user = userSnap.data() ?? {};
    if (!coupon?.isActive || !coupon.isPublic) fail("COUPON_INVALID");          // private codes are given, not typed
    if (coupon.endsAt.toMillis() < Date.now()) fail("COUPON_EXPIRED");
    if (coupon.firstOrderOnly && (user.ordersCount ?? 0) > 0) fail("COUPON_ALREADY_USED");
    if ((redemptionSnap.data()?.count ?? 0) >= coupon.perUserLimit) fail("COUPON_ALREADY_USED");
    if (mineSnap.exists) return { code, status: mineSnap.data()!.status };      // already in the pocket: no error
    // Tier is NOT checked here: a member (or silver) user may keep GOLD20 and see it "Locked" (07.3).
    tx.set(mineSnap.ref, { code, source: "claimed", status: "available", endsAt: coupon.endsAt, assignedAt: Timestamp.now() });
    return { code, status: "available" };
  });
});
```

### Referrals: `applyReferralCode` and `rewardReferral` (11.8)

```typescript
export const applyReferralCode = onCall<{ code: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const code = (req.data.code ?? "").trim().toUpperCase().slice(0, 20);
  const refRef = db.doc(`referrals/${code}`);
  const userRef = db.doc(`users/${uid}`);

  return db.runTransaction(async (tx) => {
    const [refSnap, userSnap] = await Promise.all([tx.get(refRef), tx.get(userRef)]);
    const referral = refSnap.data();
    const user = userSnap.data() ?? {};
    if (!referral) fail("REFERRAL_INVALID");
    if (referral.ownerUid === uid) fail("REFERRAL_OWN_CODE");
    if (user.referredBy || (user.ordersCount ?? 0) > 0) fail("REFERRAL_ALREADY_USED");
    if (referral.invitesCount >= referral.maxInvites) fail("REFERRAL_FULL");

    const now = Timestamp.now();
    tx.update(userRef, { referredBy: code, updatedAt: now });
    tx.set(refRef.collection("invites").doc(uid), {
      inviteeName: `${user.firstName ?? ""} ${(user.lastName ?? "").charAt(0)}.`.trim(),
      status: "pending", joinedAt: now, firstOrderId: null, rewardedAt: null,
    });
    tx.update(refRef, { invitesCount: FieldValue.increment(1) });
    // The invitee's 10 TND: a private one-time coupon (the coupon doc REF-AMIRA10 is created once per code by onUserCreated).
    tx.set(db.doc(`users/${uid}/coupons/REF-${code}`), {
      code: `REF-${code}`, source: "referral", status: "available",
      endsAt: Timestamp.fromMillis(Date.now() + 30 * 86_400_000), assignedAt: now,
    });
    return { ok: true };
  });
});

/** Called by onOrderStatusChanged when an order reaches `delivered` and it is the customer's first delivered order. */
export async function rewardReferral(orderId: string, order: FirebaseFirestore.DocumentData): Promise<void> {
  const user = (await db.doc(`users/${order.customerId}`).get()).data();
  const code: string | undefined = user?.referredBy;
  if (!code) return;
  const refRef = db.doc(`referrals/${code}`);
  const inviteRef = refRef.collection("invites").doc(order.customerId);

  await db.runTransaction(async (tx) => {
    const [refSnap, inviteSnap] = await Promise.all([tx.get(refRef), tx.get(inviteRef)]);
    const referral = refSnap.data()!;
    if (inviteSnap.data()?.status !== "pending") return;                    // idempotent: rewarded once
    if (order.totals.subtotal < referral.minFirstOrderMillimes) return;     // "first order over 50 TND"
    if (referral.earnedTotal + referral.rewardMillimes > referral.maxInvites * referral.rewardMillimes) return; // 100 TND cap
    const walletRef = db.doc(`wallets/${referral.ownerUid}`);
    const wallet = (await tx.get(walletRef)).data() ?? {};
    const now = Timestamp.now();
    tx.set(walletRef, { walletBalance: FieldValue.increment(referral.rewardMillimes), updatedAt: now }, { merge: true });
    tx.set(walletRef.collection("transactions").doc(), {
      amount: referral.rewardMillimes, reason: "referral_reward", referralCode: code,
      balanceAfter: (wallet.walletBalance ?? 0) + referral.rewardMillimes, createdAt: now,
    });
    tx.update(inviteRef, { status: "rewarded", firstOrderId: orderId, rewardedAt: now });
    tx.update(refRef, { rewardedCount: FieldValue.increment(1), earnedTotal: FieldValue.increment(referral.rewardMillimes) });
  });
  // then: push + inbox "Youssef ordered · +10 TND" (type referral_reward)
}
```

### `endShift` (S02.2 "End shift & sign out")

```typescript
export const endShift = onCall<{ requestId?: string }>({ enforceAppCheck: true }, async (req) => {
  const uid = requireAuth(req.auth);
  const role = req.auth!.token.role as string | undefined;
  if (role !== "picker" && role !== "courier") fail("NOT_STAFF");

  const held = await db.collection("orders")
    .where("staffIds", "array-contains", uid).where("statusGroup", "==", "active").get();

  const batch = db.batch();
  const now = Timestamp.now();
  for (const doc of held.docs) {
    const o = doc.data();
    if (role === "picker" && o.status === "picking" && o.pickerId === uid) {
      batch.update(doc.ref, { status: "placed", pickerId: null, pickingStartedAt: null, updatedAt: now }); // back to the queue
      batch.set(doc.ref.collection("events").doc(), { type: "note", actorRole: "system", at: now, message: { en: "Picker ended shift, back in queue" } });
    }
    if (role === "courier" && o.courierId === uid && ["ready_for_pickup", "courier_assigned"].includes(o.status)) {
      batch.update(doc.ref, { status: "ready_for_pickup", courierId: null, courier: null, updatedAt: now });
    }
  }
  const shifts = await db.collection("shifts").where("staffId", "==", uid).where("status", "==", "active").get();
  shifts.forEach((s) => batch.update(s.ref, { status: "done", clockOutAt: now }));
  if (role === "courier") batch.set(db.doc(`couriers/${uid}`), { status: "offline", activeOrderIds: [], updatedAt: now }, { merge: true });
  await batch.commit();

  // Released courier orders go back into dispatch.
  for (const doc of held.docs) if (role === "courier" && doc.data().courierId === uid) await dispatchOrder(doc.id);
  return { released: held.size };
});
```

> A courier who already **picked up** bags can't end the shift with them: `endShift` leaves those orders alone, and the S02.2 sheet says "Hand over picked bags first". Ending a shift is a batch, not a transaction, because nothing here moves money; `requeueAbandonedOrders` covers a crash halfway through.

---

## 9. Calling a function from Android

```kotlin
suspend fun placeOrder(request: PlaceOrderRequestDto): PlaceOrderResultDto = guardException {
    val result = functions.getHttpsCallable("placeOrder")
        .call(request.toMap())
        .await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as Map<String, Any?>).toPlaceOrderResult()
}
```

Add the **generic** `FirebaseFunctionsException` codes to the shared `AppFirebaseExceptionMapper` (it must not know about checkout):

```kotlin
is FirebaseFunctionsException -> when (e.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppException.Auth.NoCurrentUser()
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppException.Network.NoConnection(e)
    FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppException.Network.Timeout(e)
    FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> AppException.Network.TooManyRequests(e)
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppException.Firestore.PermissionDenied(e)
    FirebaseFunctionsException.Code.NOT_FOUND -> AppException.Firestore.NotFound(e)
    else -> AppException.Unexpected(e)
}
```

`FAILED_PRECONDITION` (our business `Reason` codes) is caught **in the feature's data source** and turned into a feature failure such as `CheckoutFailure.OutOfStock(variantId)`. Full code in [13 § 6](13_android_integration.md#6-callables-and-feature-failures).

---

## Check

- [ ] `firebase emulators:start` loads all functions without errors.
- [ ] `placeOrder` rejects: empty cart, outside zone, stock < quantity, expired coupon, wrong `expectedTotal`.
- [ ] Two parallel `placeOrder` calls never share an order number.
- [ ] `acceptOffer` by two couriers on the same order: one gets `OFFER_TAKEN`.
- [ ] Changing a staff role updates claims.
- [ ] Every callable has `enforceAppCheck: true` (after the debug token is registered).
- [ ] `cancelOrder` by a customer fails with `CANCEL_NOT_ALLOWED` once the order is `picking`; a wallet-paid cancel credits the wallet instantly.
- [ ] `returnToStore` fails with `WAIT_NOT_OVER` before 5 minutes or with fewer than 2 calls.
- [ ] `resolveFailedDelivery` handles redeliver / store pickup / cancel; unresolved orders are cancelled after 24 h.
- [ ] `completeDelivery` called twice with the same `requestId` pays the courier once.
- [ ] `rewardReferral` credits once per invite and stops at 100 TND.
- [ ] `placeOrder` for #DK-2048 with `pointsToUse = 1250` returns `total = 765300`.
- [ ] Cancelling #DK-1990 (cash, FREESHIP still valid) returns `{refundMillimes: 0, refundTo: "none", couponRestored: true}`; FREESHIP is back under Available, and `coupons/FREESHIP.redeemedCount` went down by 1. An expired coupon is **not** restored.
- [ ] `getRedeliverySlots` at 18:25 (store open 08:00–22:00) returns 20:00–21:00 and 21:00–22:00, and `storePickupOpenUntil` = 22:00. A full slot is not returned, and `resolveFailedDelivery` on a slot that just filled fails with `SLOT_UNAVAILABLE`.
- [ ] `reportIssue(customer_not_answering)` sends the push; with `whatsappOptIn` and a phone it writes a `whatsapp_sent` event; with a bad token it writes `whatsapp_failed` and the door wait still starts.
- [ ] A delivery that takes `spend12mMillimes` past 1,500 TND sets `tier: "gold"` immediately; `nightlyTiers` lowers a customer whose 12-month spend dropped, and `completeDelivery` never lowers a tier.
