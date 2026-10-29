# 13: Android Integration (Clean Architecture + MVI)

How the collections in 03–06 become Kotlin code in this repo, following the patterns the auth feature already uses: `guardException` in data sources, `AppFailureMapper.guardFailure` in repositories, `AppResult` in the domain, MVI ViewModels (`AppViewModel<S, I, E>`).

---

## Why

- **Firestore stays in the data layer.** No `DocumentSnapshot`, `Timestamp`, `GeoPoint` or `FirebaseFirestore` import above `data/`. The domain uses plain Kotlin models (`Millimes`, `Instant`, `LatLng`-like value classes), so ViewModels and use cases are testable without Firebase.
- **One data source per collection family, one repository per domain concept.** Small classes, single responsibility, easy fakes.
- **Real-time screens get `Flow`s, the rest get `suspend` functions.** Matches the listener/get decisions in [08](08_queries_and_indexes.md).

---

## 1. Features and packages

```
features/
├── auth/            (exists)
├── location/        zones, addresses, "Deliver to", pin, outside-zone
├── catalog/         categories, brands, products, variants, inventory, banners, collections, reviews
├── cart/            cart + items, coupon selection, previewCheckout
├── checkout/        placeOrder, payment methods
├── orders/          my orders, details, tracking, rating, cancel
├── account/         profile, avatar, settings (pushPrefs), points, wallet, coupons wallet
├── notifications/   inbox, permission prompt
core/
├── push/            DukkanMessagingService, NotificationChannels
├── storage/         ImageCompressor
├── utils/money/     Millimes
└── di/              FirebaseModule, CoroutineScopeModule
```

Each feature: `data/{datasource, model, mapper, repository}`, `domain/{model, repository, usecase}`, `presentation/{logic, screens, components}`, `di/`. Same as the auth feature.

---

## 2. `FirebaseModule` (replace the auth guide's version)

`core/di/FirebaseModule.kt`:

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.installations.installations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    private const val REGION = "europe-west1"
    private const val EMULATOR_HOST = "10.0.2.2"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth.apply {
        if (BuildConfig.USE_FIREBASE_EMULATORS) useEmulator(EMULATOR_HOST, 9099)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        if (BuildConfig.USE_FIREBASE_EMULATORS) useEmulator(EMULATOR_HOST, 8080)
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = Firebase.functions(REGION).apply {
        if (BuildConfig.USE_FIREBASE_EMULATORS) useEmulator(EMULATOR_HOST, 5001)
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage.apply {
        if (BuildConfig.USE_FIREBASE_EMULATORS) useEmulator(EMULATOR_HOST, 9199)
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = Firebase.messaging

    @Provides
    @Singleton
    fun provideFirebaseInstallations(): FirebaseInstallations = Firebase.installations
}
```

**Why `Firebase.functions(REGION)`:** without the region the SDK calls `us-central1`, and every call fails with `NOT_FOUND` because our functions run in `europe-west1`.

**Why `@Singleton`:** `useEmulator` must run exactly once, before first use. Singleton providers guarantee that.

---

## 3. Data sources per collection

| Data source (data layer) | Collections | Style |
|---|---|---|
| `ZoneRemoteDataSource` | `zones`, `zones/*/live` | get (cached) |
| `AddressRemoteDataSource` | `users/{uid}/addresses` | Flow + suspend writes |
| `CatalogRemoteDataSource` | `categories`, `brands`, `banners`, `collections` | get |
| `ProductRemoteDataSource` | `products`, `variants`, `reviews` | get, paginated |
| `InventoryRemoteDataSource` | `stores/{id}/inventory` | Flow (size sheet) + get |
| `CartRemoteDataSource` | `carts/{uid}`, `items` | Flow + suspend writes |
| `CheckoutRemoteDataSource` | callables `previewCheckout`, `placeOrder`, `addPaymentMethod`; `paymentMethods` | suspend |
| `OrderRemoteDataSource` | `orders`, `items`, `events`, `private/delivery`, `courierLocations`; callables `cancelOrder`, `getRedeliverySlots`, `resolveFailedDelivery`, `rateDelivery` | Flow + suspend |
| `UserRemoteDataSource` (extend auth's) | `users/{uid}` (incl. `defaultPaymentMethod`) | Flow + suspend |
| `WalletRemoteDataSource` | `wallets/{uid}`, `transactions`, `pointsLedger` (read only) | Flow + get |
| `CouponRemoteDataSource` | `users/{uid}/coupons`, `coupons/{code}`; callable `claimCoupon` | get + suspend |
| `ReferralRemoteDataSource` | `referrals/{code}`, `invites`; callable `applyReferralCode` | get + suspend |
| `FavoritesRemoteDataSource` | `users/{uid}/favorites`, `followedBrands`, `stockAlerts` | Flow + suspend |
| `HelpRemoteDataSource` | `helpTopics`, `helpArticles` | get (cached) |
| `SearchRemoteDataSource` | callables `searchProducts`, `searchSuggestions` (14) | suspend |
| `InboxRemoteDataSource` | `notifications` | Flow + suspend |
| `DeviceRemoteDataSource` (exists) | `devices` + FCM token | suspend |
| `AvatarRemoteDataSource` | Storage | suspend |

**Customer features** (one folder each under `features/`, each with `data/`, `domain/`, `presentation/`, `di/`):

| Feature | Screens | Data sources |
|---|---|---|
| `favorites` | 11.5, heart on cards | `FavoritesRemoteDataSource` |
| `coupons` | 07.3, 08.2 | `CouponRemoteDataSource` |
| `brands` | 05.3, 07.4 | `CatalogRemoteDataSource` |
| `wallet` | 11.6 (+ balances on 09.1, 11.1) | `WalletRemoteDataSource`, `UserRemoteDataSource` |
| `referrals` | 11.8, code entry at sign-up | `ReferralRemoteDataSource` |
| `orders` (extended) | 10.5–10.7 cancel / delivery failed | `OrderRemoteDataSource`. 10.7 calls `getRedeliverySlots` when it opens and shows the first slot ("20:00 – 21:00 · free") and `storePickupOpenUntil`; a `SLOT_UNAVAILABLE` from `resolveFailedDelivery` reloads the slots |
| `account` (tier) | 11.1 badge, 07.3 lock | `UserRemoteDataSource` (`tier`, `toNextTierMillimes`, read-only), `config/tiers` (G, cached) for the threshold labels |
| `help` | 11.7 | `HelpRemoteDataSource` + WhatsApp/email intent |
| `search` | 05b.x | `SearchRemoteDataSource` + DataStore history |

**Team app only:** `PickerRemoteDataSource`, `CourierRemoteDataSource` (incl. `reportIssue`, `logContactAttempt`, `returnToStore`, `endShift`, payouts), and the **outbox** (§ 7b).

**WhatsApp on the phone (no SDK).** The WhatsApp message itself is sent by the server (09 § 8c). The apps only open WhatsApp with an intent:

```kotlin
/** Opens a WhatsApp chat (C03.2 "Message", 11.7 "Chat with us"). `e164` like "+21622345678". */
fun Context.openWhatsAppChat(e164: String, text: String? = null) {
    val uri = Uri.parse("https://wa.me/${e164.removePrefix("+")}" + (text?.let { "?text=${Uri.encode(it)}" } ?: ""))
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$e164"))) } // no WhatsApp: fall back to a call
}
```

The customer's consent switch (11.3 "WhatsApp delivery messages") writes `users/{uid}.whatsappOptIn` through `UserRemoteDataSource`; checkout normalizes the phone to E.164 before saving it.

---

## 4. A real-time listener (pattern)

The Firestore KTX `snapshots()` extension already gives a `Flow`. We wrap it with the project's `Flow.guardException()` (exists in `core/error/guard/guardException.kt`):

```kotlin
package com.example.dukkanapp.features.orders.data.datasource

import com.example.dukkanapp.core.error.guard.guardException
import com.example.dukkanapp.features.orders.data.model.OrderDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OrderRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    /** Emits the order every time it changes; null if it doesn't exist (or was never readable). */
    fun observeOrder(orderId: String): Flow<OrderDto?> =
        firestore.collection(ORDERS).document(orderId)
            .snapshots()
            .map { it.toObject<OrderDto>() }
            .guardException()

    private companion object {
        const val ORDERS = "orders"
    }
}
```

The same thing written by hand with `callbackFlow`, so you know what `snapshots()` does:

```kotlin
fun observeOrderManual(orderId: String): Flow<OrderDto?> = callbackFlow {
    val registration = firestore.collection("orders").document(orderId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)                     // ends the flow; guardException() maps it
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject<OrderDto>())
        }
    awaitClose { registration.remove() }         // stops billing reads when the collector leaves
}.guardException()
```

**`awaitClose` is mandatory.** Without it the listener keeps running after the screen is gone.

Repository:

```kotlin
class OrderRepositoryImpl @Inject constructor(
    private val remote: OrderRemoteDataSource,
    private val failureMapper: AppFailureMapper,
) : OrderRepository {

    override fun observeOrder(orderId: String): Flow<AppResult<Order>> =
        remote.observeOrder(orderId)
            .map<OrderDto?, AppResult<Order>> { dto ->
                dto?.toDomain()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppFailure.Data.NotFound)
            }
            .catch { e -> emit(AppResult.Failure(failureMapper.map(e))) }
}
```

A missing document (or one the rules hide) becomes `AppFailure.Data.NotFound`, which already exists in `core/error/AppFailure.kt`.

ViewModel (MVI):

```kotlin
@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeOrderTracking: ObserveOrderTrackingUseCase,
) : AppViewModel<OrderTrackingUiState, OrderTrackingIntent, OrderTrackingEffect>(OrderTrackingUiState()) {

    private val orderId = savedStateHandle.toRoute<AppRoute.OrderTracking>().orderId

    init {
        observeOrderTracking(orderId)
            .onEach { result ->
                when (result) {
                    is AppResult.Success -> setState { copy(tracking = result.data, isLoading = false, error = null) }
                    is AppResult.Failure -> setState { copy(isLoading = false, error = result.failure.toUiText()) }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: OrderTrackingIntent) { /* call courier, help… */ }
}
```

**Lifecycle:** the screen collects `state` with `collectAsStateWithLifecycle()`. The Firestore flow lives in `viewModelScope`, so it survives rotation and stops when the user leaves the screen. To also stop it while the app is in the background, expose it with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)` instead of `launchIn`.

`ObserveOrderTrackingUseCase` combines two flows: the order, and, when it has a `courierId`, the courier location (`flatMapLatest`).

---

## 5. DTO ↔ domain mappers

```kotlin
// features/orders/data/mapper/OrderMapper.kt
fun OrderDto.toDomain(): Order = Order(
    id = id,
    number = number,
    status = OrderStatus.fromWire(status),
    step = OrderStatus.fromWire(status).toCustomerStep(),
    itemCount = itemCount,
    totals = OrderTotals(
        subtotal = Millimes(totals.subtotal),
        deliveryFee = Millimes(totals.deliveryFee),
        couponDiscount = Millimes(totals.couponDiscount),
        pointsDiscount = Millimes(totals.pointsDiscount),
        total = Millimes(totals.total),
    ),
    paymentMethod = PaymentMethod.fromWire(payment.method),
    deliveryLabel = delivery.label,
    courier = courier?.let { CourierSummary(it.name, it.vehicleType, it.plate, it.ratingAvg) },
    etaFrom = etaFrom?.toInstant(),
    etaTo = etaTo?.toInstant(),
    placedAt = placedAt?.toInstant(),
)

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
```

Rules for mappers:

- Wire enums go through `fromWire()` with an `Unknown` fallback (02 § 5.3).
- Translated maps are resolved **in the presentation layer** with the current language, not in the mapper. The domain keeps `LocalizedText(Map<String, String>)` so a language switch doesn't need a reload.
- Money becomes `Millimes` right here. Raw `Long` never leaves the data layer.

---

## 6. Callables and feature failures

```kotlin
// features/checkout/domain/model/CheckoutFailure.kt
sealed interface CheckoutFailure : AppFailure.Feature {
    data object CartEmpty : CheckoutFailure
    data object OutsideZone : CheckoutFailure
    data object StoreClosed : CheckoutFailure
    data class OutOfStock(val variantId: String?) : CheckoutFailure
    data class PriceChanged(val newTotal: Millimes?) : CheckoutFailure
    data object CouponInvalid : CheckoutFailure
    data object CouponExpired : CheckoutFailure
    data class CouponMinNotMet(val missing: Millimes?) : CheckoutFailure
    data object CouponAlreadyUsed : CheckoutFailure
    data object PhoneRequired : CheckoutFailure
    data object NotEnoughWallet : CheckoutFailure
    data object Unknown : CheckoutFailure

    companion object {
        /** Maps the function's stable reason code (09 § 2). Unknown codes never crash old apps. */
        fun fromReason(reason: String?, details: Map<*, *>?): CheckoutFailure = when (reason) {
            "CART_EMPTY" -> CartEmpty
            "OUTSIDE_ZONE" -> OutsideZone
            "STORE_CLOSED" -> StoreClosed
            "OUT_OF_STOCK" -> OutOfStock(details?.get("variantId") as? String)
            "PRICE_CHANGED" -> PriceChanged((details?.get("total") as? Number)?.let { Millimes(it.toLong()) })
            "COUPON_INVALID" -> CouponInvalid
            "COUPON_EXPIRED" -> CouponExpired
            "COUPON_MIN_NOT_MET" -> CouponMinNotMet((details?.get("missing") as? Number)?.let { Millimes(it.toLong()) })
            "COUPON_ALREADY_USED" -> CouponAlreadyUsed
            "PHONE_REQUIRED" -> PhoneRequired
            "NOT_ENOUGH_WALLET" -> NotEnoughWallet
            else -> Unknown
        }
    }
}
```

**Where the mapping happens:** `AppFirebaseExceptionMapper` is shared and must not know about checkout. Two clean options:

1. The data source catches `FirebaseFunctionsException` with `FAILED_PRECONDITION` itself and throws `AppException.Feature(CheckoutFailure.fromReason(...))`, then lets `guardException` handle the rest. **Recommended**: each feature owns its reasons.
2. The shared mapper produces a generic `AppException.Feature(FunctionRejected(reason, details))` and each repository translates it.

Option 1 in code:

```kotlin
suspend fun placeOrder(request: Map<String, Any?>): Map<String, Any?> = guardException {
    try {
        @Suppress("UNCHECKED_CAST")
        functions.getHttpsCallable("placeOrder").call(request).await().getData() as Map<String, Any?>
    } catch (e: FirebaseFunctionsException) {
        if (e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
            val details = e.details as? Map<*, *>
            throw AppException.Feature(CheckoutFailure.fromReason(details?.get("reason") as? String, details), e)
        }
        throw e
    }
}
```

`guardException` must let `AppException` pass through untouched. Check that `AppFirebaseExceptionMapper.map(e)` returns `e` when `e is AppException` (add that line first if it's missing), and add the `FirebaseFunctionsException` and `StorageException` cases from [09 § 9](09_cloud_functions.md#9-calling-a-function-from-android) and [11 § 3](11_storage.md#3-map-storageexception).

UI text per failure: a `CheckoutFailure.toUiText()` in `features/checkout/presentation/`, e.g. `PriceChanged` → "Prices changed. New total: %1$s TND".

---

## 7. Offline behavior

| Situation | What happens | What the UI does |
|---|---|---|
| Browse while offline | `get()` returns cached docs (if seen before) | Normal UI; show the 12.4 banner if a get fails with `Unavailable` |
| Add to cart offline | Write is queued locally; listener emits the new state at once | Normal ("Your cart is saved on this phone") |
| Place order offline | Callables need the network → `Network.NoConnection` | Block the slider, show 12.4 |
| Listener while offline | Emits from cache with `metadata.isFromCache == true` | Optional "last updated" hint on tracking |

Do **not** disable persistence. If you ever want a bigger cache: `firestoreSettings { setLocalCacheSettings(persistentCacheSettings { setSizeBytes(100L * 1024 * 1024) }) }`.

---

## 7b. Team app outbox (offline callables, S02.3)

Firestore queues **direct writes** offline, but **callables are HTTPS requests** and fail at once without a network. The team app keeps working offline (S02.3 "scans and photos are saved on this phone and sync when you're back"), so it needs a small queue of its own. The customer app does **not** need one: placing an order offline must fail visibly (12.4).

`core/sync/PendingActionEntity.kt` (Room):

```kotlin
package com.example.dukkanteam.core.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val requestId: String,   // UUID created when the action happened; sent to the function
    val functionName: String,            // "scanBag", "completeDelivery", "returnToStore", …
    val payloadJson: String,             // the callable's data, without requestId
    val label: String,                   // "Bag B-15 scanned", "Delivered · #DK-2048" (S02.3 list)
    val localFilePath: String?,          // a photo to upload first (door/packing/issue)
    val storagePath: String?,            // where to upload it
    val createdAt: Long,
    val attempts: Int = 0,
)
```

`core/sync/OutboxWorker.kt` (WorkManager):

```kotlin
package com.example.dukkanteam.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File

@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingActionDao,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        for (action in dao.oldestFirst()) {            // keep the order: scan before complete
            try {
                if (action.localFilePath != null && action.storagePath != null) {
                    storage.reference.child(action.storagePath).putFile(android.net.Uri.fromFile(File(action.localFilePath))).await()
                }
                val data = JSONObject(action.payloadJson).toMap() + ("requestId" to action.requestId)
                functions.getHttpsCallable(action.functionName).call(data).await()
                dao.delete(action.requestId)
            } catch (e: FirebaseFunctionsException) {
                if (e.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
                    dao.delete(action.requestId)       // business refusal: replaying won't help; show it to the user
                    continue
                }
                dao.incrementAttempts(action.requestId)
                return Result.retry()                  // network/server problem: stop and retry the whole queue later
            } catch (e: Exception) {
                dao.incrementAttempts(action.requestId)
                return Result.retry()
            }
        }
        return Result.success()
    }
}

private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { get(it) }
```

Enqueue it as unique work with a `NetworkType.CONNECTED` constraint and exponential backoff, each time an action is added and on app start. The data sources **always** go through the outbox for the replayable callables (`scanBag`, `confirmPickup`, `markPacked`, `markArriving`, `returnToStore`, `reportIssue`, `completeDelivery`). Online, the worker runs within a second; offline, the UI shows the row in "Waiting to sync".

**Why `requestId` matters:** a call can succeed on the server while the response is lost (timeout). The worker then retries the same row. The function sees the same `requestId` and returns the first result (09 § 2 `once`), so the courier isn't paid twice and the order doesn't move twice.

Dependencies to add (team app only): `androidx.room` (runtime, ktx, compiler via KSP), `androidx.work:work-runtime-ktx`, `androidx.hilt:hilt-work` (+ its KSP compiler).

---

## 8. Build order

Each step ends with something you can run.

| # | Build | Depends on | Figma |
|---|---|---|---|
| 1 | Setup (01): Gradle, CLI, emulators, `FirebaseModule` | – | – |
| 2 | Rules (07) + indexes (08) deployed to the emulator; seed script for zones, stores, categories, brands, 20 products | 1 | – |
| 3 | `onUserCreated` + extended `users` fields; Edit profile; avatar upload | 2 | 02.5, 11.1, 11.2 |
| 4 | Location: zones, addresses, Deliver to, pin, outside zone | 3 | 03.x |
| 5 | Catalog read: Home rows, categories, category products, brand store, product details, size sheet | 2 | 04, 05, 06 |
| 6 | Cart + favorites + follow | 5 | 06.3, 08.1, 08.3, 11.5 |
| 6b | Search (Algolia extension, `searchProducts`, history) | 5 | 05b.x |
| 7 | Wallet doc + functions: `previewCheckout`, `placeOrder` (points from cart), counter; coupons + `claimCoupon`; all brands | 6 | 07, 07.3, 07.4, 08.2, 09, 11.6 |
| 8 | Orders: My orders, details, cancel (+ refund), rating | 7 | 10.1, 10.2, 10.4, 10.5, 10.6 |
| 9 | Push: channels, service, token, pre-prompt, inbox, status trigger | 8 | 11.4, 12.x |
| 9b | Referrals, help center, empty/loading states | 7 | 11.7, 11.8, 14.x |
| 10 | Team app module: staff claims, sign-in routing, profile + `endShift`, outbox | 2 | S01, S02 |
| 11 | Picker flow: queue, pick, manual barcode, missing item, pack, ready | 10, 8 | F01, F02, F03 |
| 12 | Courier flow: online, location, offers, run, handover, earnings, problems, payouts, cash | 11 | C01, C02, C03 |
| 13 | Live tracking + delivery failed / store pickup in the customer app | 12 | 10.3, 10.7 |
| 14 | App Check enforcement, Crashlytics, budget review | all | – |

---

## Check

- [ ] No `com.google.firebase` import in any `domain/` or `presentation/` package (`grep -rn "com.google.firebase" app/src/main/java --include=*.kt | grep -E "/domain/|/presentation/"` returns nothing).
- [ ] Every listener uses `snapshots()` or `callbackFlow` + `awaitClose`.
- [ ] Every callable data source maps `FAILED_PRECONDITION` reasons to a feature failure.
- [ ] Functions instance is created with `europe-west1`.
- [ ] Money is `Millimes` in every domain model; points convert with `MILLIMES_PER_POINT`.
- [ ] Balances are read from `wallets/{uid}`, never from `users/{uid}`.
- [ ] Team app: every replayable callable goes through the outbox with a `requestId`.
- [ ] The app never writes `tier` or `spend12mMillimes`; it only displays them.
- [ ] 10.7 hides an option when `getRedeliverySlots` returns no slot or `storePickupOpenUntil = null`.
- [ ] Phone numbers are saved in E.164 (`+216…`); "Message" opens `wa.me` and falls back to the dialer.
