# 07: Security Rules (Firestore + Storage) and How to Test Them

**Files:** `firebase/firestore.rules`, `firebase/storage.rules`, `firebase/tests/rules.test.ts`.
**Replaces:** the rules in section 3.2 of the auth guide. Same `users` and `devices` behavior, more fields and collections.

---

## Why

- **Rules are the only thing between a hacked APK and your database.** The API key ships in every APK. Assume an attacker can send any request the rules accept.
- **Default deny.** A collection that isn't matched below is closed. New collections are closed until you write their rule.
- **Field whitelists (`hasOnly`)** stop clients from adding fields like `walletBalance` or `role`.
- **Claims, not reads.** Staff checks use `request.auth.token.role` / `storeId` (free). We use `get()` only where we must (order subcollections, reviews), because each `get()` counts as a read and adds latency.
- **Rules are not filters.** A query must itself be limited to what the rules allow (`where("customerId", "==", uid)`), otherwise Firestore rejects the whole query.

---

## 1. `firebase/firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ───────────────────────── Helpers ─────────────────────────

    function signedIn() { return request.auth != null; }
    function uid() { return request.auth.uid; }
    function isOwner(userId) { return signedIn() && uid() == userId; }

    // Staff claims, set only by the onStaffWritten function.
    function role() { return signedIn() ? request.auth.token.get('role', '') : ''; }
    function claimStore() { return request.auth.token.get('storeId', ''); }
    function isPicker() { return role() == 'picker'; }
    function isCourier() { return role() == 'courier'; }
    function isManager() { return role() == 'manager' || role() == 'admin'; }
    function worksAt(storeId) { return (isPicker() || isManager()) && claimStore() == storeId; }

    // A customer is a signed-in user with no staff role.
    function isCustomer() { return signedIn() && role() == ''; }

    function emailMatchesToken() {
      return request.resource.data.email == request.auth.token.email;
    }

    // Field changes on update.
    function changed() { return request.resource.data.diff(resource.data).affectedKeys(); }

    function isServerTime(field) { return request.resource.data[field] == request.time; }

    function optString(data, field, max) {
      return !(field in data) || data[field] == null
        || (data[field] is string && data[field].size() <= max);
    }

    function orderDoc(orderId) {
      return get(/databases/$(database)/documents/orders/$(orderId)).data;
    }

    // Who can see an order (used by the order doc and its subcollections).
    function canReadOrder(order) {
      return signedIn() && (
        order.customerId == uid()
        || (isCourier() && order.courierId == uid())
        || worksAt(order.storeId)
      );
    }

    // ───────────────────────── Customers ─────────────────────────

    match /users/{userId} {
      function clientUserFields() {
        return ['email', 'displayName', 'firstName', 'lastName', 'photoUrl', 'phoneNumber',
                'dateOfBirth', 'authProviders', 'languageCode', 'marketingNotificationsEnabled',
                'pushPrefs', 'whatsappOptIn', 'defaultAddressId', 'defaultPaymentMethod',
                'createdAt', 'updatedAt', 'lastLoginAt'];
        // NOT here (function-only): tier, spend12mMillimes, nextTier, toNextTierMillimes,
        // tierUpdatedAt, ordersCount, freeDeliveriesLeft, referralCode, referredBy.
      }
      function validUser(d) {
        return optString(d, 'firstName', 50) && optString(d, 'lastName', 50)
          && optString(d, 'displayName', 101)
          && (!('phoneNumber' in d) || d.phoneNumber == null
              || d.phoneNumber.matches('[+][1-9][0-9]{7,14}'))   // E.164 (WhatsApp + courier calls)
          && (!('whatsappOptIn' in d) || d.whatsappOptIn is bool)
          && optString(d, 'dateOfBirth', 10) && optString(d, 'defaultAddressId', 64)
          && (!('pushPrefs' in d) || d.pushPrefs is map)
          && (!('defaultPaymentMethod' in d)
              || d.defaultPaymentMethod in ['cash', 'wallet']
              || d.defaultPaymentMethod.matches('card:[A-Za-z0-9]{1,40}'))
          && (!('languageCode' in d) || d.languageCode in ['ar', 'en', 'fr']);
      }

      allow read: if isOwner(userId);

      allow create: if isOwner(userId)
        && emailMatchesToken()
        && request.resource.data.keys().hasOnly(clientUserFields())
        && validUser(request.resource.data)
        && isServerTime('createdAt');

      allow update: if isOwner(userId)
        && emailMatchesToken()
        && changed().hasOnly(clientUserFields().removeAll(['createdAt']))
        && validUser(request.resource.data);

      allow delete: if false; // deleteAccount function

      match /devices/{installationId} {
        allow read, delete: if isOwner(userId);
        allow create, update: if isOwner(userId)
          && request.resource.data.keys().hasOnly([
               'platform', 'deviceName', 'manufacturer', 'model', 'osVersion', 'sdkInt',
               'appVersionName', 'appVersionCode', 'languageCode', 'timeZone',
               'fcmToken', 'updatedAt']);
      }

      match /addresses/{addressId} {
        function validAddress(d) {
          return d.keys().hasOnly(['label', 'customLabel', 'street', 'area', 'city', 'postalCode',
                   'location', 'zoneId', 'buildingType', 'building', 'floor', 'apartment',
                   'riderNote', 'dropOff', 'voiceNotePath', 'recipientName', 'recipientPhone',
                   'createdAt', 'updatedAt'])
            && d.label in ['home', 'work', 'family', 'other']
            && d.buildingType in ['apartment', 'house', 'office']
            && d.dropOff in ['ring_bell', 'leave_at_door', 'call_me']
            && d.location is latlng
            && d.street is string && d.street.size() <= 120
            && optString(d, 'riderNote', 200)
            && optString(d, 'customLabel', 30);
        }
        allow read, delete: if isOwner(userId);
        allow create: if isOwner(userId) && validAddress(request.resource.data) && isServerTime('createdAt');
        allow update: if isOwner(userId) && validAddress(request.resource.data)
          && request.resource.data.createdAt == resource.data.createdAt;
      }

      // Wishlist (11.5). savedPrice / savedInStock are frozen at save: create only, no update.
      match /favorites/{productId} {
        allow read, delete: if isOwner(userId);
        allow create: if isOwner(userId)
          && request.resource.data.keys().hasOnly(['productId', 'savedPrice', 'savedInStock', 'addedAt'])
          && request.resource.data.productId == productId
          && request.resource.data.savedPrice is int && request.resource.data.savedPrice >= 0
          && request.resource.data.savedInStock is bool
          && isServerTime('addedAt');
      }

      match /followedBrands/{brandId} {
        allow read, delete: if isOwner(userId);
        allow create: if isOwner(userId)
          && request.resource.data.keys().hasOnly(['brandId', 'followedAt'])
          && request.resource.data.brandId == brandId
          && isServerTime('followedAt');
      }

      match /stockAlerts/{variantId} {
        allow read, delete: if isOwner(userId);
        allow create: if isOwner(userId)
          && request.resource.data.keys().hasOnly(['productId', 'variantId', 'storeId', 'createdAt'])
          && request.resource.data.variantId == variantId
          && isServerTime('createdAt');
      }

      match /notifications/{notificationId} {
        allow read, delete: if isOwner(userId);
        allow update: if isOwner(userId) && changed().hasOnly(['readAt']) && isServerTime('readAt');
      }

      // Read-only for the owner. Written by functions (Admin SDK bypasses rules).
      match /paymentMethods/{methodId} { allow read: if isOwner(userId); }
      match /coupons/{code} { allow read: if isOwner(userId); }
      // users/{uid}/private/* : no client access (not matched → denied)
    }

    // Money and points (04 § 7). Functions only; the owner reads.
    match /wallets/{userId} {
      allow read: if isOwner(userId);
      match /transactions/{txId} { allow read: if isOwner(userId); }
      match /pointsLedger/{entryId} { allow read: if isOwner(userId); }
    }

    // Referrals (04 § 10). Functions only; the owner reads their own code and invites.
    match /referrals/{code} {
      allow read: if signedIn() && resource.data.ownerUid == uid();
      match /invites/{inviteeUid} {
        allow read: if signedIn()
          && get(/databases/$(database)/documents/referrals/$(code)).data.ownerUid == uid();
      }
    }

    match /carts/{userId} {
      allow read, delete: if isOwner(userId);
      allow create, update: if isOwner(userId) && isCustomer()
        && request.resource.data.keys().hasOnly(['storeId', 'addressId', 'couponCode', 'pointsToUse', 'updatedAt'])
        && request.resource.data.storeId is string
        && optString(request.resource.data, 'couponCode', 20)
        && (!('pointsToUse' in request.resource.data)
            || (request.resource.data.pointsToUse is int
                && request.resource.data.pointsToUse >= 0
                && request.resource.data.pointsToUse <= 1000000))
        && isServerTime('updatedAt');

      match /items/{variantId} {
        allow read, delete: if isOwner(userId);
        allow create, update: if isOwner(userId)
          && request.resource.data.keys().hasOnly(['productId', 'variantId', 'quantity', 'addedAt'])
          && request.resource.data.variantId == variantId
          && request.resource.data.quantity is int
          && request.resource.data.quantity >= 1 && request.resource.data.quantity <= 20;
      }
    }

    match /zoneWaitlist/{entryId} {
      allow create: if signedIn()
        && request.resource.data.keys().hasOnly(['uid', 'location', 'nearestZoneId', 'createdAt'])
        && request.resource.data.uid == uid()
        && request.resource.data.location is latlng
        && isServerTime('createdAt');
    }

    // ───────────────────────── Catalog (signed-in read) ─────────────────────────
    // Decided: no guest browsing. Splash, language and onboarding read nothing from
    // Firestore, and Home opens only after sign-in, so every catalog read can require auth.
    // App Check still blocks scrapers that have an account.

    match /zones/{zoneId} {
      allow read: if signedIn();
      match /live/{docId} { allow read: if signedIn(); }
    }
    match /stores/{storeId} {
      allow read: if signedIn();
      match /inventory/{variantId} { allow read: if signedIn(); }
      // slotLoad (redelivery capacity) has no rule: server-only, the app asks getRedeliverySlots.
    }

    // App-wide settings, e.g. config/tiers (tier thresholds for the "x TND to Gold" hint).
    match /config/{docId} { allow read: if signedIn(); }
    match /categories/{id} { allow read: if signedIn(); }
    match /brands/{id} { allow read: if signedIn(); }
    match /banners/{id} { allow read: if signedIn(); }
    match /collections/{id} { allow read: if signedIn(); }

    match /products/{productId} {
      allow read: if signedIn();
      match /variants/{variantId} { allow read: if signedIn(); }

      match /reviews/{reviewerId} {
        function boughtIt(d) {
          let order = orderDoc(d.orderId);
          return order.customerId == uid()
            && order.status == 'delivered'
            && productId in order.productIds;
        }
        function validReview(d) {
          return d.keys().hasOnly(['rating', 'comment', 'authorName', 'orderId', 'createdAt', 'updatedAt'])
            && d.rating is int && d.rating >= 1 && d.rating <= 5
            && optString(d, 'comment', 500)
            && d.authorName is string && d.authorName.size() <= 40;
        }
        allow read: if signedIn();
        allow create: if isOwner(reviewerId) && validReview(request.resource.data)
          && boughtIt(request.resource.data) && isServerTime('createdAt');
        allow update: if isOwner(reviewerId) && validReview(request.resource.data)
          && request.resource.data.orderId == resource.data.orderId
          && request.resource.data.createdAt == resource.data.createdAt
          && isServerTime('updatedAt');
        allow delete: if isOwner(reviewerId);
      }
    }

    match /coupons/{code} {
      allow read: if signedIn();
      // redemptions: functions only
    }

    // Help center (11.7): read-only content.
    match /helpTopics/{id} { allow read: if signedIn(); }
    match /helpArticles/{id} { allow read: if signedIn(); }

    // ───────────────────────── Orders ─────────────────────────
    // No client create/update/delete on orders: callables only.

    match /orders/{orderId} {
      allow read: if canReadOrder(resource.data);

      match /items/{itemId} {
        allow read: if canReadOrder(orderDoc(orderId));
        // Picker scan results only. Swaps and removals go through resolveMissingItem.
        allow update: if isPicker()
          && orderDoc(orderId).storeId == claimStore()
          && orderDoc(orderId).status == 'picking'
          && changed().hasOnly(['pickStatus', 'pickedQuantity', 'pickedAt', 'pickMethod'])
          && request.resource.data.pickStatus in ['pending', 'picked']
          && (!('pickMethod' in request.resource.data)
              || request.resource.data.pickMethod in ['scanned', 'typed'])
          && request.resource.data.pickedQuantity is int
          && request.resource.data.pickedQuantity >= 0
          && request.resource.data.pickedQuantity <= resource.data.quantity;
      }

      match /events/{eventId} { allow read: if canReadOrder(orderDoc(orderId)); }
      match /bags/{bagId} { allow read: if canReadOrder(orderDoc(orderId)); }
      // Problem reports (C03.3): written by the reportIssue callable only.
      match /issues/{issueId} { allow read: if canReadOrder(orderDoc(orderId)); }
      // orders/{id}/requests/* (idempotency keys): no client access

      match /private/pickup {
        allow read: if (isCourier() && orderDoc(orderId).courierId == uid())
          || worksAt(orderDoc(orderId).storeId);
      }
      match /private/delivery {
        // The customer only. Never the courier.
        allow read: if signedIn() && orderDoc(orderId).customerId == uid();
      }
    }

    // ───────────────────────── Team ─────────────────────────

    match /staff/{staffId} {
      allow read: if isOwner(staffId) || (isManager() && resource.data.storeId == claimStore());
      match /dailyStats/{dayKey} {
        allow read: if isOwner(staffId) || isManager();
      }
    }

    match /couriers/{courierId} {
      allow read: if isOwner(courierId) || isManager();
      allow update: if isOwner(courierId) && isCourier()
        && changed().hasOnly(['status', 'onlineSince', 'vehicleType', 'plate', 'zoneId', 'updatedAt'])
        && request.resource.data.status in ['offline', 'online']
        && resource.data.status != 'on_run'              // can't go offline mid-run
        && request.resource.data.vehicleType in ['scooter', 'bike', 'car']
        && isServerTime('updatedAt');

      match /earnings/{entryId} { allow read: if isOwner(courierId) || isManager(); }
      match /dailyStats/{dayKey} { allow read: if isOwner(courierId) || isManager(); }
      match /payouts/{payoutId} { allow read: if isOwner(courierId) || isManager(); }
    }

    match /courierLocations/{courierId} {
      function locationFields() {
        return ['location', 'geohash', 'heading', 'speedKph', 'stopsRemaining', 'updatedAt'];
      }
      function validLocation(d) {
        return d.location is latlng && d.geohash is string
          && d.heading is number && d.speedKph is number
          && d.stopsRemaining is int;
      }
      allow read: if isOwner(courierId)
        || isManager()
        || (signedIn() && uid() in resource.data.get('visibleTo', []));
      allow create: if isOwner(courierId) && isCourier()
        && request.resource.data.keys().hasOnly(locationFields())
        && validLocation(request.resource.data) && isServerTime('updatedAt');
      allow update: if isOwner(courierId) && isCourier()
        && changed().hasOnly(locationFields())             // visibleTo is function-only
        && validLocation(request.resource.data) && isServerTime('updatedAt');
    }

    match /dispatchOffers/{offerId} {
      allow read: if isCourier() && resource.data.courierId == uid();
    }

    match /shifts/{shiftId} {
      allow read: if (signedIn() && resource.data.staffId == uid())
        || (isManager() && resource.data.storeId == claimStore());
    }

    // Cash returned at the hub (06). Written by recordCashDeposit (manager) only.
    match /cashDeposits/{depositId} {
      allow read: if (isCourier() && resource.data.courierId == uid())
        || (isManager() && resource.data.storeId == claimStore());
    }

    // counters/* : no client access
  }
}
```

### Notes on tricky lines

| Line | Why |
|---|---|
| `request.auth.token.get('role', '')` | Customers have no `role` claim. `get` with a default avoids an error on a missing key |
| `changed().hasOnly(clientUserFields().removeAll(['createdAt']))` | `createdAt` can never change after creation |
| `isServerTime('createdAt')` | The client must send `FieldValue.serverTimestamp()`. A faked date is rejected |
| `uid() in resource.data.get('visibleTo', [])` | A brand-new location doc has no `visibleTo` yet |
| `resource.data.status != 'on_run'` | A courier can't flip offline with a bag in hand. `completeDelivery` sets them back to `online` |
| `orderDoc(orderId)` called several times | Firestore caches the same `get()` within one request, so it counts as **one** read |
| Picker `pickStatus in ['pending','picked']` | `swapped` / `missing` / `removed` change money, so only the function can set them |
| `wallets/{uid}` has no write rule | Balances and ledgers move together in a function transaction. Before this update the balances sat in `users/{uid}` and every profile rule had to exclude them; now the profile whitelist only lists what the client may edit |
| `favorites` has `create` but no `update` | `savedPrice`/`savedInStock` are frozen history. To "save again", delete and create |
| `referrals/{code}/invites` read uses `get()` on the parent | The invite doc doesn't hold `ownerUid`; one cached read of the parent decides |
| Catalog `allow read: if signedIn()` | Decided: browsing requires an account. Splash, language and onboarding are local only. A signed-out user is sent to Welcome (02.1) before Home, so no screen reads the catalog as a guest |

---

## 2. `firebase/storage.rules`

Paths are explained in [11_storage.md](11_storage.md).

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    function signedIn() { return request.auth != null; }
    function role() { return signedIn() ? request.auth.token.get('role', '') : ''; }
    function isImage() { return request.resource.contentType.matches('image/(jpeg|png|webp)'); }
    function under(mb) { return request.resource.size < mb * 1024 * 1024; }

    // Avatars: public read (shown to couriers in the future), owner write.
    match /users/{userId}/avatar/{fileName} {
      allow read: if true;
      allow write: if signedIn() && request.auth.uid == userId && isImage() && under(2);
    }

    // Address voice notes: the owner, and staff working on an order (functions grant signed URLs).
    match /users/{userId}/voice/{fileName} {
      allow read: if signedIn() && request.auth.uid == userId;
      allow write: if signedIn() && request.auth.uid == userId
        && request.resource.contentType.matches('audio/(mp4|m4a|aac|mpeg)') && under(1);
    }

    // Catalog images: signed-in read, admin tools upload with the Admin SDK.
    // Note: a download URL (with ?token=) bypasses these rules. That's fine for product
    // photos; never put private files behind a download URL.
    match /catalog/{allPaths=**} {
      allow read: if signedIn();
    }

    // Packing photo (picker) and door photo (courier).
    match /orders/{orderId}/packing/{fileName} {
      allow read: if role() in ['picker', 'manager', 'admin'];
      allow write: if role() == 'picker' && isImage() && under(3);
    }
    match /orders/{orderId}/door/{fileName} {
      allow read: if role() in ['courier', 'manager', 'admin'];
      allow write: if role() == 'courier' && isImage() && under(3);
    }
    // Problem report photo (C03.3 "Package damaged"). The reportIssue callable checks the path.
    match /orders/{orderId}/issues/{fileName} {
      allow read: if role() in ['courier', 'picker', 'manager', 'admin'];
      allow write: if role() in ['courier', 'picker'] && isImage() && under(3);
    }
  }
}
```

**Why order photos check only the role and not the order:** Storage rules can read Firestore (`firestore.get(...)`), but each check is billed and slower. The function that marks the order packed/delivered verifies that the uploaded path belongs to that order and that the uploader is its picker/courier. If you want strict checks in rules too, add `firestore.get(/databases/(default)/documents/orders/$(orderId)).data.courierId == request.auth.uid`.

---

## 3. Testing rules with the emulator

### 3.1 Setup

```bash
cd firebase
mkdir tests && cd tests
npm init -y
npm i -D @firebase/rules-unit-testing firebase vitest typescript
```

`firebase/tests/rules.test.ts`:

```typescript
import { readFileSync } from "node:fs";
import {
  assertFails, assertSucceeds, initializeTestEnvironment, RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc, serverTimestamp } from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";

let env: RulesTestEnvironment;

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "dukkan-test",
    firestore: { rules: readFileSync("../firestore.rules", "utf8"), host: "127.0.0.1", port: 8080 },
  });
});
afterAll(() => env.cleanup());
beforeEach(() => env.clearFirestore());

const amira = () => env.authenticatedContext("amira", { email: "amira@x.tn" }).firestore();
const karim = () => env.authenticatedContext("karim", { role: "courier", storeId: "lac2" }).firestore();

async function seedOrder() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "orders/o1"), { customerId: "amira", courierId: "karim", storeId: "lac2", status: "arriving" });
    await setDoc(doc(db, "orders/o1/private/delivery"), { code: "7359" });
    await setDoc(doc(db, "orders/o1/private/pickup"), { code: "4821" });
  });
}

describe("users and wallets", () => {
  it("a customer cannot give themselves wallet money", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "wallets/amira"), { walletBalance: 0, pointsBalance: 0 }));
    await assertFails(updateDoc(doc(amira(), "wallets/amira"), { walletBalance: 999000 }));
  });

  it("a customer cannot add a balance field to their profile", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "users/amira"), { email: "amira@x.tn" }));
    await assertFails(updateDoc(doc(amira(), "users/amira"), { walletBalance: 999000 }));
  });

  it("a customer cannot promote themselves to Gold", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "users/amira"), { email: "amira@x.tn", tier: "member" }));
    await assertFails(updateDoc(doc(amira(), "users/amira"), { tier: "gold" }));
    await assertFails(updateDoc(doc(amira(), "users/amira"), { spend12mMillimes: 9_000_000 }));
  });

  it("a customer can turn WhatsApp messages off", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "users/amira"), { email: "amira@x.tn", whatsappOptIn: true }));
    await assertSucceeds(updateDoc(doc(amira(), "users/amira"), { whatsappOptIn: false, updatedAt: serverTimestamp() }));
  });

  it("a customer can edit their first name", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "users/amira"), { email: "amira@x.tn" }));
    await assertSucceeds(updateDoc(doc(amira(), "users/amira"), { firstName: "Amira", updatedAt: serverTimestamp() }));
  });
});

describe("handover codes", () => {
  it("the courier cannot read the delivery code", async () => {
    await seedOrder();
    await assertFails(getDoc(doc(karim(), "orders/o1/private/delivery")));
  });

  it("the customer can read the delivery code", async () => {
    await seedOrder();
    await assertSucceeds(getDoc(doc(amira(), "orders/o1/private/delivery")));
  });
});

describe("orders", () => {
  it("a customer cannot change the order status", async () => {
    await seedOrder();
    await assertFails(updateDoc(doc(amira(), "orders/o1"), { status: "delivered" }));
  });
});
```

Run: `firebase emulators:exec --only firestore "npx vitest run" ` from `firebase/`.

### 3.2 Minimum test list

| # | Who | Action | Expect |
|---|---|---|---|
| 1 | customer | update own `firstName` | ✅ |
| 2 | customer | update own `tier` / `spend12mMillimes` / `toNextTierMillimes`, or add `walletBalance` / `pointsBalance` to `users/{uid}` | ❌ |
| 2h | customer | set own `whatsappOptIn` true/false | ✅ |
| 2i | customer | set `phoneNumber: "22 345 678"` (not E.164) | ❌ |
| 2j | customer | read `config/tiers` | ✅ (write ❌) |
| 2k | customer | read or write `stores/lac2/slotLoad/*` | ❌ |
| 2b | customer | write anything in `wallets/{uid}` or its ledgers | ❌ |
| 2c | customer | read own `wallets/{uid}` | ✅ |
| 2d | customer | update a `favorites` doc (change `savedPrice`) | ❌ |
| 2e | customer | set cart `pointsToUse: -5` | ❌ |
| 2f | customer A | read `referrals/{code}` owned by B, or B's invites | ❌ |
| 2g | customer | write `users/{uid}/coupons/*` or `orders/{id}/issues/*` | ❌ |
| 3 | customer | read another user's doc | ❌ |
| 4 | customer | cart item quantity 0 or 21 | ❌ |
| 5 | customer | write any field on `orders/*` | ❌ |
| 6 | customer | read own `orders/{id}/private/delivery` | ✅ |
| 7 | courier | read `private/delivery` of their order | ❌ |
| 8 | courier | read `private/pickup` of their order | ✅ |
| 9 | courier | write `visibleTo` on own location | ❌ |
| 10 | customer A | read courier location while in `visibleTo` | ✅ |
| 11 | customer B | read the same location | ❌ |
| 12 | picker (lac2) | set `pickStatus: picked` on a lac2 order in `picking` | ✅ |
| 13 | picker (lac2) | set `pickStatus: removed` | ❌ |
| 13b | picker (lac2) | set `pickStatus: picked, pickMethod: "typed"` | ✅ |
| 13c | courier | read `cashDeposits` of another courier | ❌ |
| 14 | picker (menzah) | update a lac2 order item | ❌ |
| 15 | courier | set own status `online` while `on_run` | ❌ |
| 16 | customer | review a product never ordered | ❌ |
| 17 | guest (signed out) | read `products/*` | ❌ (no guest browsing) |
| 18 | guest | read `coupons/*` | ❌ |
| 19 | customer (signed in) | read `products/*` | ✅ |

---

## Check

- [ ] `firestore.rules` deployed with `firebase deploy --only firestore:rules`.
- [ ] All 18 tests pass on the emulator.
- [ ] The auth feature (sign-up sync, device registration) still works against the new rules.
- [ ] `storage.rules` deployed.
- [ ] No rule uses `allow write: if true` anywhere.
