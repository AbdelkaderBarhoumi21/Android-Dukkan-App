# 08: Queries, Listeners and Indexes

**File:** `firebase/firestore.indexes.json`.

---

## Why

- Firestore can only run queries that have an index. Single-field indexes are automatic; **any query with two or more fields (filter + order, or two filters) needs a composite index**. Without it, the call fails with `FAILED_PRECONDITION` (our `AppException.Firestore.FailedPrecondition`) and a console link to create it.
- We keep the full list in git so emulator and production match.
- **Listener vs one-shot get:** a listener (`addSnapshotListener` / `snapshots()`) costs one read per changed document **and keeps a connection open**. Use it only where the screen must update by itself. Everything else uses `get()` (and the offline cache).

---

## 1. Customer app queries

| Screen | Query (Kotlin, simplified) | Listener? | Index |
|---|---|---|---|
| 01.2 Language | none (DataStore) | – | – |
| 03.1 Saved places | `users/{uid}/addresses` `.orderBy("createdAt", DESC)` | ✅ (list updates after save) | auto |
| 03.2 Zone check | `zones` `.whereEqualTo("isActive", true)` (cache once per session) | ❌ get | auto |
| 04.1 Hero banners | `banners` `.whereEqualTo("placement","home_hero").whereEqualTo("isActive",true).orderBy("sortOrder")` | ❌ | **#1** |
| 04.1 Categories row | `categories` `.whereEqualTo("level",0).whereEqualTo("isActive",true).orderBy("sortOrder")` | ❌ | **#2** |
| 04.1 Active order card "Arriving in 12 min" | `orders` `.whereEqualTo("customerId",uid).whereEqualTo("statusGroup","active").orderBy("placedAt",DESC).limit(1)` | ✅ | **#3** |
| 04.1 Flash deals | `collections` `.whereEqualTo("kind","flash_sale").whereEqualTo("isActive",true).orderBy("endsAt").limit(1)` then products by id | ❌ | **#4** |
| 04.2 Top brands | `brands` `.whereEqualTo("isFeatured",true).whereEqualTo("isActive",true).orderBy("sortOrder")` | ❌ | **#5** |
| 04.2 Collections | `collections` `.whereEqualTo("kind","editorial").whereEqualTo("isFeatured",true).orderBy("sortOrder")` | ❌ | **#6** |
| 04.2 Coupons for you | `users/{uid}/coupons` `.whereEqualTo("status","available")` + `coupons` public `.whereEqualTo("isPublic",true).whereEqualTo("isActive",true)` | ❌ | auto, **#7** |
| 04.2 Recommended | `products` `.whereArrayContains("categoryPath", topCat).whereEqualTo("isActive",true).orderBy("popularityScore",DESC).limit(10)` | ❌ | **#8** |
| 05.2 Category products "Popular" | `products` `.whereArrayContains("categoryPath", catId).whereEqualTo("isActive",true).orderBy("popularityScore",DESC)` paginated | ❌ | **#8** |
| 05.2 "On sale" | same `.whereGreaterThan("discountPercent",0).orderBy("discountPercent",DESC)` | ❌ | **#9** |
| 05.2 "Under 200" | same `.whereLessThan("minPrice",200000).orderBy("minPrice")` | ❌ | **#10** |
| 05.3 Brand "Best sellers" | `products` `.whereEqualTo("brandId",id).whereEqualTo("isActive",true).orderBy("popularityScore",DESC)` | ❌ | **#11** |
| 05.3 Brand "New" | `products` `.whereEqualTo("brandId",id).whereEqualTo("isActive",true).orderBy("publishedAt",DESC)` | ❌ | **#12** |
| 06.1 Product + variants | `products/{id}` + `products/{id}/variants.orderBy("sortOrder")` | ❌ | auto |
| 06.2 Size sheet stock | `stores/{storeId}/inventory` `.whereEqualTo("productId",id)` | ✅ while sheet is open (stock can hit 0) | auto |
| 06.x Reviews | `products/{id}/reviews.orderBy("createdAt",DESC).limit(20)` | ❌ | auto |
| 07.2 Collection grid | `products.whereIn(FieldPath.documentId(), chunkOf30)` | ❌ | auto |
| 08.1 Cart | `carts/{uid}` + `carts/{uid}/items.orderBy("addedAt")` | ✅ (bottom bar total everywhere) | auto |
| 08.3 Buy it again | `orders` `.whereEqualTo("customerId",uid).whereEqualTo("statusGroup","delivered").orderBy("placedAt",DESC).limit(5)` | ❌ | **#3** |
| 10.1 My orders, All | `orders` `.whereEqualTo("customerId",uid).orderBy("placedAt",DESC).limit(20)` | ✅ first page only | **#13** |
| 10.1 / 10.5 tabs Active / Delivered / Cancelled / Delivery failed | add `.whereEqualTo("statusGroup", tab)` (`active`, `delivered`, `cancelled`, `failed`) | ✅ Active and Delivery failed, ❌ others | **#3** |
| 10.1 "October 2026 · 4 orders · 1,296.200 TND" | client-side grouping of the loaded page | – | – |
| 10.2 / 10.6 / 10.7 Order details, cancelled, failed | `orders/{id}` + `items` + `events.orderBy("at")`; 10.6 also `users/{uid}/coupons/{code}` by id ("Valid until"); 10.7 slots come from the `getRedeliverySlots` callable (no client query) | ✅ order + events | auto |
| 10.3 Live tracking | `orders/{id}` + `courierLocations/{courierId}` + `orders/{id}/private/delivery` (get once) | ✅ ✅ ❌ | auto |
| 05b Search, trending, suggestions | **Algolia**, not Firestore ([14](14_search_algolia.md)). "Recently viewed" products: `products.whereIn(documentId(), idsFromDataStore)` | ❌ | auto |
| 07.3 My coupons, Available | `users/{uid}/coupons.whereEqualTo("status","available").orderBy("endsAt")` then `coupons/{code}` by id (chunks of 30) | ❌ | **#23** |
| 07.3 Used | `.whereEqualTo("status","used").orderBy("usedAt",DESC)` | ❌ | **#24** |
| 07.3 Expired | `.whereEqualTo("status","expired").orderBy("endsAt",DESC)` | ❌ | **#25** |
| 07.3 tab counts "Available · 3" | `count()` per status | ❌ | auto |
| 07.4 All brands | `brands.whereEqualTo("isActive",true).orderBy("sortName")` (all 86, cached); "86 official stores" = `count()` | ❌ | **#26** |
| 11.1 Account counts | `count()` on `favorites`, `coupons` (`status == available`), orders | ❌ | auto / #3 |
| 11.1 / 11.6 Wallet | `wallets/{uid}` | ✅ | auto |
| 11.6 History | `wallets/{uid}/transactions.orderBy("createdAt",DESC).limit(20)` | ❌ | auto |
| 11.5 Wishlist | `users/{uid}/favorites.orderBy("addedAt",DESC)` + products by id (chunks of 30). "On sale" / "Back in stock" filtered on the phone (04 § 4) | ✅ list | auto |
| 11.7 Help topics | `helpTopics.whereEqualTo("isActive",true).orderBy("sortOrder")` | ❌ | **#27** |
| 11.7 Topic articles | `helpArticles.whereEqualTo("topicId",id).whereEqualTo("isActive",true).orderBy("sortOrder")` | ❌ | **#28** |
| 11.7 Latest order | same as 04.1 active order card | ✅ | #3 |
| 11.8 Invite friends | `referrals/{users.referralCode}` (get) + `invites.orderBy("joinedAt",DESC)` | ❌ | auto |
| 14.x Empty states | the normal list query returning 0 docs; no extra query | – | – |
| 11.4 Inbox, All | `users/{uid}/notifications.orderBy("createdAt",DESC).limit(30)` | ✅ | auto |
| 11.4 Inbox tab | `.whereEqualTo("category",tab).orderBy("createdAt",DESC)` | ✅ | **#14** |
| 11.4 "2 unread" | `count()` `.whereEqualTo("readAt", null)` | ❌ (refresh on resume) | auto |

## 2. Team app queries

| Screen | Query | Listener? | Index |
|---|---|---|---|
| F01.1 Picking queue | `orders` `.whereEqualTo("storeId",store).whereIn("status",["placed","picking"]).orderBy("dueAt")` | ✅ | **#15** |
| F01.1 "6 To pick" | `count()` of the same | via listener size | #15 |
| F01.2 Order overview | `orders/{id}` + `items` (group by `aisle` on the phone) | ✅ | auto |
| F02.2 Ready for pickup | `orders/{id}` + `bags` | ✅ | auto |
| F02.3 My shift | `staff/{uid}/dailyStats/{today}` | ✅ | auto |
| C01.1 Busy zones | `zones/{id}/live/current` for the courier's zone + neighbours | ✅ | auto |
| C01.2/C01.3 Offers | `dispatchOffers` `.whereEqualTo("courierId",uid).whereEqualTo("status","pending").orderBy("expiresAt")` | ✅ | **#16** |
| C01.4 Earnings week | `couriers/{uid}/dailyStats.whereIn(documentId(), last7DayKeys)` | ❌ | auto |
| C01.4 Payout | `couriers/{uid}/payouts.whereEqualTo("status","scheduled").orderBy("scheduledFor").limit(1)` | ❌ | **#17** |
| C03.4 Payout history | `couriers/{uid}/payouts.orderBy("periodStart",DESC).limit(12)` + `couriers/{uid}` (`cashInHand`, `cashDueBy`) | ❌ / ✅ | auto |
| C02.x Active run | `orders` `.whereEqualTo("courierId",uid).whereEqualTo("statusGroup","active").orderBy("placedAt")` | ✅ | **#18** |
| C03.2 Customer not answering | `orders/{id}` (`doorWait`) + `events.orderBy("at")` | ✅ | auto |
| Shifts | `shifts` `.whereEqualTo("staffId",uid).orderBy("startsAt",DESC).limit(14)` | ❌ | **#19** |
| S02.1 Profile | `staff/{uid}` + `staff/{uid}/dailyStats/{today}` + active shift | ❌ | auto, #19 |
| S02.2 "2 orders are assigned to you" | `count()` on `orders.whereArrayContains("staffIds",uid).whereEqualTo("statusGroup","active")` | ❌ | **#29** |
| Manager: cash still due | `cashDeposits.whereEqualTo("storeId",store).orderBy("createdAt",DESC)` | ❌ | **#30** |

## 3. Functions-only queries

| Function | Query | Index |
|---|---|---|
| `onVariantPriceDrop` | collection group `favorites` `.where("productId","==",id)` | **field override** (collection group) |
| `requeueAbandonedOrders` | `orders` `.where("status","==","picking").where("pickingStartedAt","<",cutoff)` (then checks the latest item `pickedAt`) | **#31** |
| `expireFailedOrders` | `orders` `.where("status","==","failed").where("failure.resolveBy","<",now)` | **#32** |
| `expireUserCoupons` | collection group `coupons` `.where("status","==","available").where("endsAt","<",now)` | **#33** (collection group) |
| `dispatchScheduled` | `orders` `.where("status","==","ready_for_pickup").where("delivery.slotStart","<=",now+30min)` | **#34** |
| `cashDueAlerts` | `couriers` `.where("cashDueBy","<",now)` | auto |
| `nightlyTiers` | `orders` `.where("statusGroup","==","delivered").where("deliveredAt",">=",12 months ago)` (`select` customerId + totals) | **#35** |
| `nightlyTiers` (downgrade sweep) | `users` `.where("tier","in",["silver","gold"])` | auto (single field) |
| `getRedeliverySlots`, `resolveFailedDelivery` | `stores/{storeId}/slotLoad/{date_HH}` read **by id** (`getAll` / `tx.get`), plus `stores/{storeId}` | **none**: direct document reads never need an index |
| `cleanupSlotLoad` | collection group `slotLoad` `.where("slotStart","<",2 days ago)` | auto (single field, collection group exemption below) |
| `onInventoryRestock` | collection group `stockAlerts` `.where("variantId","==",id).where("storeId","==",store)` | **#20** (collection group) |
| `dispatchOrder` | `couriers` `.where("zoneId","==",z).where("status","==","online")` + geohash range on `courierLocations` | **#21**, geohash single field auto |
| `expireOffers` | `dispatchOffers` `.where("status","==","pending").where("expiresAt","<",now)` | **#22** |
| `cleanupDevices` | collection group `devices` `.where("updatedAt","<",cutoff)` | field override |

---

## 4. Pagination

Use cursors, never `offset` (Firestore bills the skipped docs).

```kotlin
suspend fun loadProducts(categoryId: String, after: DocumentSnapshot?, pageSize: Long = 20): Pair<List<ProductDto>, DocumentSnapshot?> =
    guardException {
        var query = firestore.collection("products")
            .whereArrayContains("categoryPath", categoryId)
            .whereEqualTo("isActive", true)
            .orderBy("popularityScore", Query.Direction.DESCENDING)
            .limit(pageSize)
        if (after != null) query = query.startAfter(after)

        val snapshot = query.get().await()
        snapshot.toObjects(ProductDto::class.java) to snapshot.documents.lastOrNull()
    }
```

The ViewModel keeps the last `DocumentSnapshot` **inside the data layer** (or an opaque `PageKey` wrapper) so the domain never sees Firestore types. When the result has fewer than `pageSize` items, the end is reached.

---

## 5. `firebase/firestore.indexes.json`

Index numbers match the tables above.

```json
{
  "indexes": [
    { "collectionGroup": "banners", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "placement", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "categories", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "level", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "customerId", "order": "ASCENDING" },
      { "fieldPath": "statusGroup", "order": "ASCENDING" },
      { "fieldPath": "placedAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "collections", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "kind", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "endsAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "brands", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "isFeatured", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "collections", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "kind", "order": "ASCENDING" },
      { "fieldPath": "isFeatured", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "coupons", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "isPublic", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "endsAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "products", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "categoryPath", "arrayConfig": "CONTAINS" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "popularityScore", "order": "DESCENDING" } ] },

    { "collectionGroup": "products", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "categoryPath", "arrayConfig": "CONTAINS" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "discountPercent", "order": "DESCENDING" } ] },

    { "collectionGroup": "products", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "categoryPath", "arrayConfig": "CONTAINS" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "minPrice", "order": "ASCENDING" } ] },

    { "collectionGroup": "products", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "brandId", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "popularityScore", "order": "DESCENDING" } ] },

    { "collectionGroup": "products", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "brandId", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "publishedAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "customerId", "order": "ASCENDING" },
      { "fieldPath": "placedAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "notifications", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "category", "order": "ASCENDING" },
      { "fieldPath": "createdAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "storeId", "order": "ASCENDING" },
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "dueAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "dispatchOffers", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "courierId", "order": "ASCENDING" },
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "expiresAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "payouts", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "scheduledFor", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "courierId", "order": "ASCENDING" },
      { "fieldPath": "statusGroup", "order": "ASCENDING" },
      { "fieldPath": "placedAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "shifts", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "staffId", "order": "ASCENDING" },
      { "fieldPath": "startsAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "stockAlerts", "queryScope": "COLLECTION_GROUP", "fields": [
      { "fieldPath": "variantId", "order": "ASCENDING" },
      { "fieldPath": "storeId", "order": "ASCENDING" } ] },

    { "collectionGroup": "couriers", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "zoneId", "order": "ASCENDING" },
      { "fieldPath": "status", "order": "ASCENDING" } ] },

    { "collectionGroup": "dispatchOffers", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "expiresAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "coupons", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "endsAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "coupons", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "usedAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "coupons", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "endsAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "brands", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortName", "order": "ASCENDING" } ] },

    { "collectionGroup": "helpTopics", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "helpArticles", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "topicId", "order": "ASCENDING" },
      { "fieldPath": "isActive", "order": "ASCENDING" },
      { "fieldPath": "sortOrder", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "staffIds", "arrayConfig": "CONTAINS" },
      { "fieldPath": "statusGroup", "order": "ASCENDING" } ] },

    { "collectionGroup": "cashDeposits", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "storeId", "order": "ASCENDING" },
      { "fieldPath": "createdAt", "order": "DESCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "pickingStartedAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "failure.resolveBy", "order": "ASCENDING" } ] },

    { "collectionGroup": "coupons", "queryScope": "COLLECTION_GROUP", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "endsAt", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "status", "order": "ASCENDING" },
      { "fieldPath": "delivery.slotStart", "order": "ASCENDING" } ] },

    { "collectionGroup": "orders", "queryScope": "COLLECTION", "fields": [
      { "fieldPath": "statusGroup", "order": "ASCENDING" },
      { "fieldPath": "deliveredAt", "order": "ASCENDING" } ] }
  ],
  "fieldOverrides": [
    { "collectionGroup": "slotLoad", "fieldPath": "slotStart", "indexes": [
      { "order": "ASCENDING", "queryScope": "COLLECTION_GROUP" } ] },
    { "collectionGroup": "favorites", "fieldPath": "productId", "indexes": [
      { "order": "ASCENDING", "queryScope": "COLLECTION" },
      { "order": "ASCENDING", "queryScope": "COLLECTION_GROUP" } ] },

    { "collectionGroup": "devices", "fieldPath": "updatedAt", "indexes": [
      { "order": "ASCENDING", "queryScope": "COLLECTION" },
      { "order": "ASCENDING", "queryScope": "COLLECTION_GROUP" } ] },

    { "collectionGroup": "products", "fieldPath": "description", "indexes": [] },
    { "collectionGroup": "products", "fieldPath": "images", "indexes": [] },
    { "collectionGroup": "zones", "fieldPath": "polygon", "indexes": [] },
    { "collectionGroup": "courierLocations", "fieldPath": "location", "indexes": [] }
  ]
}
```

**Why the empty `indexes: []` overrides:** text maps, image lists and polygons are never queried. Indexing them costs storage and slows writes. `courierLocations.location` is written every 5 seconds and queried by `geohash`, not by `location`.

**Why the `coupons` indexes #23–#25 also apply to the top-level `coupons`:** an index is attached to a collection **id**, and both `coupons/{code}` and `users/{uid}/coupons/{code}` have the id `coupons`. That's harmless: the top-level collection simply gets three indexes it doesn't use. #33 is the collection-group version used by the expiry job.

**Why `isActive` sits in the middle of index #8–#12:** Firestore requires the equality filters first, then the field you sort on. The array filter (`CONTAINS`) counts as an equality filter.

---

## 6. Notes

- **Offline:** every `get()` falls back to the cache when offline (`Source.DEFAULT`). For screens that must never show stale data (checkout totals), use `get(Source.SERVER)` and show the offline state (12.4) on failure.
- **Listeners and lifecycle:** collect them with `repeatOnLifecycle(STARTED)` (we already do in `AppCollectEffect` / state collection) so they stop in the background. A forgotten listener keeps billing reads.
- **Search** ("Search products, brands…") is not a Firestore query; see [14](14_search_algolia.md).

---

## Check

- [ ] `firebase deploy --only firestore:indexes` succeeds.
- [ ] Every list screen in the app runs once on the emulator without `FAILED_PRECONDITION`.
- [ ] Listeners are only on the screens marked ✅.
- [ ] Pagination uses `startAfter`, never offsets.
