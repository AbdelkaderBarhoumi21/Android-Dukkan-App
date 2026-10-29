# 12: Screen → Firebase Map

Every Figma screen, what it reads, writes, calls, and which pushes relate to it. Updated for the Figma revision with 02b, 05b, 07b, 10b, 11b, 13, 14 (customer) and S02, F03, C03 (team).

Legend: **L** = real-time listener, **G** = one-shot get (cache-first), **W** = client write, **F** = callable function, **P** = push, **DS** = DataStore (phone only), **A** = Algolia (through the `searchProducts` callable), **—** = nothing.

---

## Customer app

### 01 · Splash & onboarding

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 01.1 Splash | Auth session, DS (onboarding done, remember me) | — | — | Start-destination logic (auth guide § 13) |
| 01.2 Language | DS | DS `language` | — | `users.languageCode` is synced at next sign-in |
| 01.3–01.5 Onboarding | — (static) | DS `onboarding_done` | — | Prices/brands on slides are illustrations, not data |

### 02 · Sign in & sign up

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 02.1 Welcome | — | — | — | Google: Credential Manager → `signInWithCredential`. Facebook: Facebook SDK → `FacebookAuthProvider` |
| 02.2 Log in | Auth | `users/{uid}` sync (merge), `devices/{id}` (W) | — | "Keep me logged in" = Remember Me |
| 02.3 Sign up | Auth | `users/{uid}` create with `firstName`, `lastName`, `displayName`, `marketingNotificationsEnabled` ("Send me deals by email") | trigger `onUserCreated` (creates `wallets/{uid}`, `referrals/{code}`) | "1/2": step 2 is **email verification** (02.6) |
| 02.4 Forgot password | Auth `sendPasswordResetEmail` | — | — | "Resend in 0:42" cooldown on the phone; "expires in 1 hour" is Firebase's default |
| 02.5 All set | `users/{uid}` (G): `freeDeliveriesLeft`, `users/{uid}/coupons/WELCOME15` (G) | — | — | Shows the gift written by `onUserCreated` |

### 02b · Email verification (new)

No Firestore data: this is Firebase Auth only, exactly as in the auth guide (`SendEmailVerificationUseCase`, `CheckEmailVerificationUseCase`).

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 02.6 Verify your email | Auth `currentUser.email` ("Sent to amira.bensalah@gmail.com") | — | Auth `sendEmailVerification()` | "2/2". "The link works for 24 hours" is Firebase's action-link lifetime (set the template text in the console). "Resend in 0:60" = the 60 s cooldown on the phone. "Change" = sign out and go back to 02.3 |
| 02.7 Not verified yet | Auth `user.reload()` → `isEmailVerified == false` | — | — | "I've verified" calls `reload()`; true → 02.5 All set → Home. "Resend link" after the cooldown |

### 03 · Location & addresses

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 03.1 Deliver to | `users/{uid}/addresses` (L), `zones` (G, cached), DS recent searches | `carts/{uid}.addressId` (W) | — | Places Autocomplete for search; fused location for "Use my current location" |
| 03.2 Drop the pin | `zones` polygons (cache) | — | — | Point-in-polygon on the phone: "In delivery zone · Express 25 min" |
| 03.3 Address details | — | `users/{uid}/addresses/{id}` (W), voice note to Storage | — | `zoneId` saved with the address |
| 03.4 My addresses | `addresses` (L), `users/{uid}.defaultAddressId` (L) | — | — | "Address saved · Undo" = delete the doc just created |
| 03.5 Address options | — | `users/{uid}.defaultAddressId` (W), delete address (W batch) | — | "Share location" = Android share sheet with a Maps link |
| 03.6 Outside zone | `zones` (cache) | `zoneWaitlist` create (W) | — | "Notify me when you arrive" |

### 04 · Home

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 04.1 / 04.2 Home | `users/{uid}` (L, greeting), `banners` (G), `categories` level 0 (G), active order (L, limit 1), flash `collections` (G) + products (G), `brands` featured (G), editorial `collections` (G), coupons (G), recommended products (G), `carts/{uid}` + items (L, bottom bar "3 · 39.900 TND") | — | — | "Good morning" from the phone clock. "Arriving in 12 min · Karim is on his way" from the active order |

### 05 · Categories & brands

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 05.1 Categories | `categories` level 0 (G), category `banners` (G) | — | — | "Search 12,000+ products" opens 05b.1 |
| 05.2 Category products | `categories where parentId == id` (G, chips), `products` paginated (G), inventory for "Only N left" (G) | cart add (W) | — | Filters: Popular / On sale / Under 200 (indexes #8–#10) |
| 05.3 Brand store | `brands/{id}` (G), public coupons (G, filtered on the phone by `scope.brandIds`), products (G), `users/{uid}/followedBrands/{id}` (G) | follow / unfollow (W) | trigger `onFollowWritten` | Tabs: Products, Best sellers, New, About |

### 05b · Search (new, Algolia: [14](14_search_algolia.md))

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 05b.1 Empty query | DS recent searches + recently viewed ids, `products` by those ids (G), `categories` level 0 (cache) | DS ("Clear all") | `searchSuggestions` (F → A, Query Suggestions index) for "Trending now" | Nothing personal is stored in Firestore |
| 05b.2 Results | — | DS recent searches (on submit) | `searchProducts` (F → A) | "128 results for “runner”" = `totalHits`. Chips All · Sneakers · Running · Kids · On sale |
| 05b.3 Sort sheet | — | — | `searchProducts` with `sort` | Relevance / Price ↑ / Price ↓ / Newest first (replicas) |
| 05b.4 No results | — | — | `searchProducts` → 0 hits + `suggestion` | "Did you mean air runner?", "Popular right now" chips, "You might like" = `searchProducts` with an empty query in the same store |

### 06 · Product

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 06.1 Product details | `products/{id}` (G), `variants` (G), `reviews` (G, first page), `users/{uid}/favorites/{id}` (G) | favorite add/remove (W); DS recently viewed | — | ETA = zone ETA + `prepMinutes`. Heart saves `savedPrice`/`savedInStock` (04 § 4) |
| 06.2 Choose size | `stores/{storeId}/inventory where productId == id` (L while open) | `stockAlerts/{variantId}` (W) on "Sold out — notify me" | — | |
| 06.3 Added to cart | — | `carts/{uid}/items/{variantId}` (W, increment) | — | Snackbar "Air Runner 2 · EU 42 · View cart" |

### 07 · Offers & collections

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 07.1 Offers | `offers_hero` banner (G), user coupons + public coupons (G), editorial collections (G), cart subtotal (L, for "Spend 120 TND more to unlock") | `carts/{uid}.couponCode` on "Apply" (W) | — | Locked = `minSubtotal > cartSubtotal` (estimate). "Manage" opens 07.3 |
| 07.2 Collection | `collections/{id}` (G), products by id in chunks of 30 (G) | cart add (W) | — | Chips filter on the client |

### 07b · Coupons & brands (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 07.3 My coupons | `users/{uid}/coupons where status == tab orderBy endsAt` (G, indexes #23–#25), `coupons/{code}` for each (G, chunks of 30), `count()` per tab, `users/{uid}.tier` + `toNextTierMillimes`, `wallets/{uid}.pointsBalance` (L) | "Apply" → `carts/{uid}.couponCode` (W) | `claimCoupon` (F) on "Enter a promo code · Add" | Tabs Available · Used · Expired. GOLD20 **Locked** "Reach Gold to unlock" when `TIER_RANK[tier] < TIER_RANK[minTier]`; tier = 12-month spend (Gold from 1,500 TND, 04 § 1). A coupon given back by a cancel (10.6) reappears under Available. Footer "1,324 points · 100 pts = 1 TND" |
| 07.4 All brands | `brands where isActive orderBy sortName` (G, all 86, cached), featured row (G) | — | — | Section headers from `initial`, A–Z index scrolls the list. "Search brands" filters on the phone |

### 08 · Cart & coupons

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 08.1 Cart | `carts/{uid}` + items (L), products + variants + inventory for those lines (G, cached), `zones/{zoneId}` (cache) | stepper: items `quantity` (W) / delete line (W) | `previewCheckout` (F) when coupon or lines change | "4 items" = sum of quantities. "Free delivery unlocked", "Arrives 12:30 – 12:45" |
| 08.2 Coupons sheet | user + public coupons (G) | `carts/{uid}.couponCode` (W) | `previewCheckout` (F) → "Applied" / "Locked"; `claimCoupon` (F) for a typed code | DUKKAN50 "50 TND off over 300 TND" |
| 08.3 Empty cart | delivered orders (G) → items → "Buy it again" products (G) | cart add (W) | — | |

### 09 · Checkout

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 09.1 Checkout | cart (L), selected address (G), `users/{uid}` (phone) (L), `wallets/{uid}` (points, wallet) (L) | `carts/{uid}.addressId`, `carts/{uid}.pointsToUse` ("Use 1,250 points") (W) | `previewCheckout` (F), `placeOrder` (F) on "Slide to place order" | New **Contact** row ("+216 22 345 678 · Amira Ben Salah · Change") edits `users/{uid}.phone` (E.164) and name; the unchecked **"Send me delivery updates on WhatsApp"** checkbox under it writes `users/{uid}.whatsappOptIn` (W). The phone is required here (`PHONE_REQUIRED`) even though it's optional in 11.2. **765.300 TND** = 817.800 − 40.000 − 12.500. Cash: "You'll hand 770 TND · change 4.700" → `cashTendered = 770000` |
| 09.2 Pay with | `users/{uid}/paymentMethods` (G), `wallets/{uid}.walletBalance` (L), `users/{uid}.defaultPaymentMethod` | — | `addPaymentMethod` (F) after the Konnect form (after launch) | "Balance 45.000 TND — not enough" from `walletBalance < total`. Card row is disabled: "Coming soon · via Konnect" |
| 09.3 Order placed | `placeOrder` result, then `orders/{id}` (L) | — | — | P `order_placed`. "+74 points earned · 1,324 total". Show notification pre-prompt (12.1) after this screen the first time |

### 10 · Orders & tracking

The "Status steps" component (Placed · Packing · On the way · At door · Done) on 04.1, 10.1, 10.2 and 10.3 maps `orders.status` like this (same table as [05 § 5](05_order_collections.md#5-status-state-machine)):

| `orders.status` | Step / banner |
|---|---|
| `placed` | Placed |
| `picking`, `packed`, `ready_for_pickup`, `courier_assigned` | Packing |
| `picked_up` | On the way |
| `arriving` | At door |
| `delivered`, `collected` | Done |
| `returning` | No stepper; banner "bringing your order back to the store" |
| `failed` | No stepper; 10.7 "Delivery failed" with three choices |
| `awaiting_customer_pickup` | No stepper; banner "Ready at Dukkan Lac 2 · show code 7359" (the delivery code) |
| `cancelled` | No stepper; 10.6 "Order cancelled" with the refund line |

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 10.1 My orders | `orders where customerId == uid` (+ `statusGroup`) (L first page, G next pages) | — | — | Tabs All · Active · Delivered · Cancelled. "Reorder" = add each item variant to cart (W batch), skipping inactive/out-of-stock. Empty → 14.1 |
| 10.2 Order details | `orders/{id}` (L), `items` (G), `events` (L) | — | `cancelOrder` (F, only while `placed`) | "Total · cash 765.300 TND", "Points · 1,250 −12.500 TND". "Chat with support" = external link (11.7) |
| 10.3 Live tracking | `orders/{id}` (L), `courierLocations/{courierId}` (L), `private/delivery` (G) for the code | — | — | P `order_on_the_way`, `courier_near`. "Cash · 765.300 TND" |
| 10.4 Delivered · rate | `orders/{id}` (G) | — | `rateDelivery` (F) | Product reviews prompt: `products/{p}/reviews/{uid}` (W) |

### 10b · Order problems (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 10.5 My orders · Cancelled / Delivery failed | `orders where customerId == uid && statusGroup == "cancelled"` / `"failed"` (L for failed, G for cancelled), index #3 | — | — | "#DK-2051 · 2 items · 62.400 TND · cash · Choose what to do" (failed); "#DK-1990 · 2 items · 109.800 TND · nothing to pay" (cancelled), the suffix from `cancel.refundTo` (`none` → "nothing to pay", `wallet` → "refunded to wallet") |
| 10.6 Order cancelled | `orders/{id}` (G): `cancel` (by, reason, at, `refundMillimes`, `refundTo`, `couponRestored`), `items` (G), `users/{uid}/coupons/{couponCode}` (G, for "Valid until") | — | (`cancelOrder` happened on 10.2) | #DK-1990: "Reason: ordered by mistake", "Payment · Nothing to pay · Cash on delivery — no money was taken", "Coupon FREESHIP is back in My coupons · Valid until 6 Nov" (only when `couponRestored`). "Reorder these items" = cart batch (W). P `refund_issued` |
| 10.7 Delivery failed | `orders/{id}` (L): `failure`, `doorWait`, `events` (L) for "18:08 Arrived · 18:10 Called ×2 · message · 18:13 Wait ended" | — | `getRedeliverySlots` (F) when the screen opens; `resolveFailedDelivery` (F) on "Confirm" | #DK-2051. Choices: **Deliver again today 20:00 – 21:00 · free** (first free slot from `getRedeliverySlots`; more slots on tap) / **Pick up at Dukkan Lac 2 · Ready now · open until 22:00** (`storePickupOpenUntil`) / Cancel the order ("Nothing to pay · cash on delivery"). An option is hidden when the function returns no slot / null. P `delivery_failed` |

### 11 · Account & settings

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 11.1 Account | `users/{uid}` (L), `wallets/{uid}` (L, "45 TND Wallet"), counts: orders, `favorites`, available coupons (`count()`) | — | — | "Silver member" = `users/{uid}.tier` (12-month spend: Silver ≥ 500, Gold ≥ 1,500 TND; function-only, 09 § 8d). "235 TND to Gold" = `toNextTierMillimes` (235000) + `nextTier`; hide the line for Gold. "Invite friends, get 10 TND" opens 11.8 |
| 11.2 Edit profile | `users/{uid}` (G), Auth `providerData` | `users/{uid}` names/phone/dateOfBirth/photoUrl (W), `whatsappOptIn` from the unchecked "Send me delivery updates on WhatsApp" box under the phone (W), avatar to Storage | — | Email not editable here ("Verified with Google") |
| 11.3 App settings | DS (theme, language), `users/{uid}.pushPrefs`, `whatsappOptIn` (G) | DS, `users/{uid}.languageCode` / `pushPrefs` / `whatsappOptIn` (W) | — | "Clear search history" = DS; "Log out" = auth sign-out + delete device doc |
| 11.4 Notifications | `users/{uid}/notifications` (L), unread `count()` | `readAt` (W) | — | Tabs = `category`. Empty → 14.2 |

### 11b · Account extras (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 11.5 Wishlist | `users/{uid}/favorites orderBy addedAt desc` (L), products by id (G, chunks of 30), cart store id | remove favorite (W), cart add (W) | — | Tabs All · On sale · Back in stock, filtered on the phone. Banner "Cherry Mini Bag dropped to 215.000 TND" = live `minPrice < savedPrice`. P `price_drop` |
| 11.6 Payment methods | `wallets/{uid}` (L), `wallets/{uid}/transactions` (G, "History"), `users/{uid}.defaultPaymentMethod` (L) | `users/{uid}.defaultPaymentMethod` (W) | — | "1,324 points · worth 13.240 TND" computed. Card row "Soon" (Konnect after launch, Remote Config flag) |
| 11.7 Help center | `helpTopics` (G), `helpArticles` (G per topic), latest active order (L) | — | — | "Search help articles" filters on the phone. "Chat with us" = WhatsApp/email intent (v1). "Get help" opens 10.2 |
| 11.8 Invite friends | `users/{uid}.referralCode` (L), `referrals/{code}` (G), `referrals/{code}/invites orderBy joinedAt` (G) | — | (`applyReferralCode` is called by the **invitee** at sign-up) | "AMIRA10 · Copy · Share invite link", "Your invites 2 of 10", "Pending" / "+10 TND". P `referral_reward` |

### 12 · Push & system states

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| 12.1 Push pre-prompt | DS (last dismissed) | DS | — | |
| 12.2 System permission | — | device `fcmToken` (W) when granted | — | |
| 12.3 Lock screen push | — | — | — | Result of 10 § 3 |
| 12.4 Offline | Firestore cache | queued writes (cart) | — | Detect with `ConnectivityManager` + Firestore `Unavailable` errors |

### 13 · Dark mode & Arabic (new)

| Screen | Data |
|---|---|
| 13.1 Home · dark, 13.2 Log in · dark, 13.3 Product details · dark | **Same reads and writes as 04.1, 02.2, 06.1.** Dark mode is a theme (DS `theme`), not data |
| 13.4 Log in · Arabic (RTL) | Same as 02.2. Catalog text comes from the `{ar,en,fr}` maps (02 § 5.4); `users.languageCode = "ar"` makes the functions send Arabic pushes and emails |

### 14 · Empty & loading states (new)

| Screen | Data |
|---|---|
| 14.1 My orders · empty | The 10.1 query returns 0 documents. "Start shopping" → Home |
| 14.2 Notifications · empty | The 11.4 query returns 0 documents. "Notification settings" → 11.3 |
| 14.3 Addresses · empty | The 03.4 query returns 0 documents. "Add a new address" → 03.1 |
| 14.4 Home · loading, 14.5 Product grid · loading | Shown while the first `get()` / listener has no snapshot yet. With the offline cache, a returning user usually skips straight to cached data; the skeleton is for the very first launch and cache misses |

---

## Team app

### S01 · Shared · Welcome & sign in

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| S01.1 Welcome | — | — | — | The stats bubbles are illustrations |
| S01.2 Staff sign in | Auth | `devices` (W) | `lookupInvite` (F) for "Invite found · Dukkan Lac 2" | No sign-up. Forgot password → manager |
| S01.3 Opening Courier | ID token claims, `staff/{uid}` (G), today's `shifts` (G) | — | — | Routes by `role` = `courier` |
| S01.4 Opening Picker | same | — | — | `role` = `picker` |

### S02 · Shared · Profile & offline (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| S02.1 Staff profile | `staff/{uid}` (G), `staff/{uid}/dailyStats/{today}` (L), active shift (G) | DS (language, alerts, scanner beep) | — | "App version 1.0.0 (12)" = `BuildConfig` |
| S02.2 Sign out sheet | `count()` of `orders where staffIds contains uid && statusGroup == active` (#29), active shift | — | `endShift` (F) on "End shift & sign out" | Then Auth sign-out + delete device doc. "Stay signed in" closes the sheet |
| S02.3 Offline | Room outbox (phone), Firestore cache | queued Firestore writes (scans) | outbox replays callables with `requestId` when back online | "Waiting to sync · 3 items" (06 § 8b) |

### F01 · Picker · Picking

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| F01.1 Picking queue | `orders where storeId == claim && status in [placed, picking] orderBy dueAt` (L), `staff/{uid}/dailyStats/{today}` (L), active shift (G) | — | `startPicking` (F), `clockIn` (F) | "Batch of 2": the app groups the two next orders with the same due window; one `startPicking` call per order |
| F01.2 Order overview | `orders/{id}` (L), `items` (L) | — | — | "By aisle · Shortest route" = sort items by aisle on the phone |
| F01.3 Pick item | `items` (L) | `items/{id}` `pickStatus: picked`, `pickedQuantity`, `pickedAt`, `pickMethod: scanned` (W) | — | Barcode compare happens on the phone (ML Kit) against `barcode`; "Undo" sets `pending` |
| F01.4 Can't find it | similar variants: `products where categoryPath contains` + inventory (G) | — | `resolveMissingItem` (F) | P customer `order_item_swapped` |

### F03 · Picker · Manual barcode (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| F03.1 Type barcode | the item's `barcode` (already loaded) | `items/{id}` pick fields with `pickMethod: typed` (W) | — | Match on the phone: "10/13 · Matches the first 10 digits"; confirm only on 13 digits + valid EAN-13 (06 § 8) |

### F02 · Picker · Pack & hand over

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| F02.1 Pack order | `orders/{id}` + `items` (L) | packing photo → Storage | `markPacked` (F) | Creates bags with `shortCode`; starts dispatch |
| F02.2 Ready for pickup | `orders/{id}` (L, courier snapshot + ETA), `bags` (L), `private/pickup` (G) | — | `markReady` (F); `confirmPickup` (F) is called by the courier, the picker sees the result | "#DK-2048 · 2 bags", "Hand over to Karim" |
| F02.3 My shift | `staff/{uid}/dailyStats/{today}` (L), shift (G) | — | `clockOut` (F) | |

### C01 · Courier · Home & offers

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| C01.1 Home · Offline | `couriers/{uid}` (L), `zones/*/live/current` (L), week `dailyStats` (G) | `couriers/{uid}.status = online`, `onlineSince` (W) | `clockIn` (F) | Starts the location foreground service |
| C01.2 Home · Online | `couriers/{uid}` (L), today stats (L), `dispatchOffers` pending (L) | `courierLocations/{uid}` every 5 s (W) | — | "Looking for orders…" |
| C01.3 New order sheet | the pending offer (L) | — | `acceptOffer` / `declineOffer` (F) | P `courier_offer`. "#DK-2048 · 4 items · 2 bags · Cash on delivery", "Drop off · Amira B. · Rue du Lac Biwa 12", countdown from `expiresAt` |
| C01.4 Earnings | `couriers/{uid}/dailyStats` week/month (G), next payout (G), `earnings` breakdown (G) | — | `requestPayout` (F) | "Cash out" |

### C02 · Courier · Run

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| C02.1 Go to store | `orders/{id}` (L), `stores/{storeId}` (G), `private/pickup` (G) "Code 48 21" | location (W) | — | Navigation via Google Maps intent ("Rue du Lac Leman") |
| C02.2 Scan bags | `bags` (L) | — | `scanBag` (F) per QR, `confirmPickup` (F) on "Confirm" | Customer gets P `order_on_the_way` |
| C02.3 To customer | `orders/{id}` (L): `customer`, `delivery`, payment | location (W) | `markArriving` (F) | "Cash on delivery · collect 765.300 TND" = `totals.total` |
| C02.4 Hand over | `orders/{id}` (L) | door photo → Storage | `verifyDeliveryCode` (F), `completeDelivery` (F, `requestId`), `reportIssue` (F) on "Customer not answering? · Report" | "Received 770.000 · Give change 4.700" computed on the phone and checked by the function |
| C02.5 Delivered | `completeDelivery` result, today stats (L) | — | — | "Back online" (fn sets `online` when the run is empty) |

### C03 · Courier · Problems & payouts (new)

| Screen | Reads | Writes | Functions | Notes |
|---|---|---|---|---|
| C03.1 Type bag code | `bags` of the order (already loaded) | — | `scanBag({orderId, shortCode})` (F) | "6 characters printed under the QR", "Looks like bag B-15 · #DK-2048" matched on the phone first |
| C03.2 Customer not answering | `orders/{id}` (L): `doorWait`, `events` (L) | — | `logContactAttempt` (F) on "Call again", `returnToStore` (F) when the 5-min timer ends | "We also sent Amira a push and a WhatsApp message" = `reportIssue` side effects: P `courier_at_door` + **WhatsApp template** `courier_at_door` (09 § 8c); the "WhatsApp sent" attempt row = the `whatsapp_sent` event. The green **WhatsApp** button opens `https://wa.me/<number>` (13, `openWhatsAppChat()`) |
| C03.3 Report a problem | — | issue photo → Storage (optional) | `reportIssue` (F) | Types: customer not answering, wrong or incomplete address, refused, damaged, something else |
| C03.4 Payout history | `couriers/{uid}` (L): `cashInHand`, `cashDueBy`; `couriers/{uid}/payouts orderBy periodStart desc` (G) | — | (manager: `recordCashDeposit`) | "Cash in hand · 765.300 TND · Return it … by 22:00". P `cash_due` |

---

## Figma vs our schema, and remaining inconsistencies

**Fixed in the latest Figma (removed from this list):** order totals (765.300 everywhere), item and bag counts, customer name "Amira B.", drop-off address, DUKKAN50 rule, picker/courier role names, 4-digit delivery code, Google + Facebook sign-in, number formats in the bottom bar, door wait 5 min on both C03.2 and 10.7, #DK-2051 amount (62.400 TND), the cancelled example renamed to #DK-1990, separate pickup (48 21) and delivery (7359) codes, S01 "role from custom claims".

**Paths where we keep our own design** (see the table in [00 § 6](00_README.md#6-figma-firebase-map-vs-our-schema)):

| Figma map says | We use | Why |
|---|---|---|
| `bags/{id}` | `orders/{id}/bags/{bagId}` | Bags never exist without their order; the QR carries the order id |
| `couriers/{id}/location` | `courierLocations/{courierId}` | A path ending in `location` would be a collection; one doc per courier is simpler and the `visibleTo` rule works |
| `offers/{courierId}` | `dispatchOffers/{offerId}` + `courierId` field | One courier receives many offers over time; history is kept |
| `payouts/{courierId}` | `couriers/{uid}/payouts/{weekId}` | One doc per week keeps the history C03.4 shows |
| 11b note "Wishlist (favorites/{uid})" vs cover `users/{uid}/favorites` | `users/{uid}/favorites/{productId}` | The cover's path is the one that fits our "everything under the user" rule |

**Still open in the mockups:** nothing. Round 4 (2026-10-29) fixed the last item: C03.2 and 10.7 now say **WhatsApp**, and the opt-in checkbox, settings switch and tier hint were added.

Notes (not inconsistencies): 03.3 voice note (0:08) is supported (Storage) and needs the `RECORD_AUDIO` permission.
