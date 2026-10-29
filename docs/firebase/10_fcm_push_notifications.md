# 10: Push Notifications (FCM)

Screens: 12.1 Push pre-prompt, 12.2 System permission, 12.3 Lock screen push, 11.3 notification toggles, 11.4 inbox; team app offers (C01.3) and "order ready" (F02.2).

---

## Why

- **One token per install**, stored in `users/{uid}/devices/{installationId}.fcmToken` (auth guide § 4.2). Pushes go to **tokens**, not topics, because every push here is personal (order status, quota of 3 deals per week, wishlist price drop, courier offer).
- **Data-only messages, built on the phone.** The server sends `data` (no `notification` block). The app builds the notification itself, so it controls the channel, the deep link, the icon and the style, both in foreground and background.
  - *Exception:* Android may delay data messages for apps in deep Doze with **normal** priority. Order status and courier offers use **high** priority, which wakes the app.
- **Text rendered on the server in the device's language** (`devices.languageCode`). The same string goes to the inbox (04 § 6), so push and inbox always match.
- **Permission is asked in context.** Android 13+ needs `POST_NOTIFICATIONS`. We show our own pre-prompt (12.1) first, at a moment that makes sense (after the first order is placed), because a "Don't allow" on the system dialog is almost final: after two refusals the system stops asking.

---

## 1. Token lifecycle

```
App install / first sign-in ──► registerCurrentDevice() writes device doc (auth feature)
                                 └─ now also writes fcmToken = FirebaseMessaging.token.await()
FCM rotates the token        ──► onNewToken(token) ──► if signed in: update devices/{id}.fcmToken
Sign out                     ──► delete device doc (auth feature) + FirebaseMessaging.deleteToken()
Server send fails "UNREGISTERED" / "INVALID_ARGUMENT" ──► function deletes that device doc
60 days without updatedAt    ──► cleanupDevices deletes it
```

**Why delete the token on sign-out:** the next person who signs in on this phone must not receive the previous person's order updates.

---

## 2. Notification channels

Channels are created once at app start (`DukkanApplication.onCreate`). Users can mute a channel in system settings, so **one channel per kind of message the user may want to mute separately**.

### Customer app

| Channel id | Name (localized) | Importance | Used for |
|---|---|---|---|
| `order_updates` | "Order updates" | HIGH (heads-up) | placed, packing, on the way, 2 min away, courier at the door, delivered, delivery failed, ready for store pickup, cancelled, item swapped |
| `offers` | "Deals and offers" | DEFAULT | flash sales, coupons expiring, price drops on favorites, back in stock |
| `account` | "Account" | LOW | welcome, points, refund issued, referral reward |

### Team app

| Channel id | Name | Importance | Used for |
|---|---|---|---|
| `courier_offers` | "New delivery offers" | HIGH, custom sound, vibration | C01.3 new offer (30 s to accept) |
| `run_updates` | "Run updates" | HIGH | order ready for pickup, order cancelled mid-run |
| `picking` | "Picking queue" | DEFAULT | new express order in queue, customer replied |
| `shift` | "Shift" | LOW | shift reminders |
| `online_status` | "You're online" | LOW | the foreground-service notification while sharing location |

---

## 3. Push catalog

| Type (`type`) | App | Channel | Trigger (09) | Respects | Deep link |
|---|---|---|---|---|---|
| `order_placed` | customer | order_updates | `onOrderStatusChanged` → placed | `pushPrefs.orderUpdates` | `dukkan://orders/{id}` |
| `order_packing` | customer | order_updates | → picking | same | same |
| `order_item_swapped` | customer | order_updates | `resolveMissingItem` | same | same |
| `order_on_the_way` | customer | order_updates | → picked_up ("Karim is on the way") | same | `dukkan://orders/{id}/track` |
| `courier_near` | customer | order_updates | `onCourierLocationWritten` < 600 m ("Karim is 2 min away") | same | `…/track` |
| `order_delivered` | customer | order_updates | → delivered ("Rate your items and earn points") | same | `dukkan://orders/{id}/rate` |
| `order_cancelled` | customer | order_updates | → cancelled | always | `dukkan://orders/{id}` |
| `courier_at_door` | customer | order_updates | `reportIssue(customer_not_answering)` ("Your courier is at the door · call back") **+ a WhatsApp template message** | always (high priority) | `dukkan://orders/{id}/track` |
| `delivery_failed` | customer | order_updates | → failed ("We couldn't reach you · choose what to do") | always | `dukkan://orders/{id}` (opens 10.7) |
| `ready_for_store_pickup` | customer | order_updates | → awaiting_customer_pickup ("Ready at Dukkan Lac 2 until 22:00") | always | `dukkan://orders/{id}` |
| `refund_issued` | customer | account | `cancelOrder` / `resolveFailedDelivery` with a wallet refund ("62.400 TND → Dukkan wallet") or a restored coupon ("FREESHIP is back in My coupons"). Not sent for a cash order with nothing to refund and no coupon | always | `dukkan://wallet` |
| `referral_reward` | customer (inviter) | account | `rewardReferral` ("Youssef ordered · +10 TND") | always | `dukkan://invite` |
| `coupon_expiring` | customer | offers | `expireUserCoupons` 48 h before `endsAt` ("WELCOME15 ends in 2 days") | `pushPrefs.flashDeals` | `dukkan://coupons` |
| `flash_sale` | customer | offers | admin campaign | `pushPrefs.flashDeals` **and** ≤ 3 per week (`private/pushQuota`) | `dukkan://collections/{id}` |
| `price_drop` | customer | offers | `onVariantWritten` | `pushPrefs.priceDrops` | `dukkan://products/{id}` |
| `back_in_stock` | customer | offers | `onInventoryWritten` | an explicit stock alert (opt-in by tapping) | `dukkan://products/{id}` |
| `welcome` | customer | account | `onUserCreated` | inbox only, no push | `dukkan://offers` |
| `courier_offer` | team | courier_offers | `dispatchOffers` created | courier is online | `dukkanteam://offers/{offerId}` |
| `order_ready` | team (courier) | run_updates | → ready_for_pickup / courier_assigned | – | `dukkanteam://runs/{orderId}` |
| `run_cancelled` | team (courier) | run_updates | → cancelled while assigned | – | `dukkanteam://home` |
| `new_express_order` | team (picker) | picking | → placed, store match | picker clocked in | `dukkanteam://picking` |
| `order_returning` | team (picker) | picking | → returning ("#DK-2048 coming back · scan bags in") | store match | `dukkanteam://returns/{orderId}` |
| `orders_released` | team (picker/courier) | shift | `requeueAbandonedOrders` released your order | – | `dukkanteam://home` |
| `cash_due` | team (courier) | shift | 21:00 if `cashInHand > 0` ("Return 765.300 TND at Dukkan Hub · Lac 2 by 22:00") | – | `dukkanteam://payouts` |

**WhatsApp for "courier at the door" (decided, replaces SMS):** FCM can't reach a phone with notifications off. For this one message we also send a **WhatsApp template message** (`courier_at_door`, Utility category, ar/fr/en) through Meta's WhatsApp Business Platform Cloud API, called from `reportIssue` with the E.164 number in `orders/{id}.customer.phone`, only when `users/{uid}.whatsappOptIn` is true. The token is the Functions secret `WHATSAPP_TOKEN`. Success logs a `whatsapp_sent` order event; any failure logs `whatsapp_failed` and the push alone goes out. Setup, consent, pricing and the TypeScript `sendWhatsAppTemplate()` are in [09 § 8c](09_cloud_functions.md#8c-whatsapp-courier-at-your-door-message-c032). Every other push stays FCM-only.

> Designer note: C03.2 still says "We also sent Amira a push and an **SMS**" and the attempts list "SMS sent"; 10.7 says "called twice and sent an **SMS**". The copy should say "WhatsApp message".

"Order updates can be muted" (11.3 toggle) controls **pushes** only. The inbox entry is always written.

---

## 4. Payload shape (FCM HTTP v1, sent from functions)

```json
{
  "message": {
    "token": "<fcmToken>",
    "android": {
      "priority": "HIGH",
      "ttl": "600s",
      "collapse_key": "order_8Fq2"
    },
    "data": {
      "type": "order_on_the_way",
      "channelId": "order_updates",
      "title": "Karim is on the way",
      "body": "Order #DK-2048 arrives in 12 min.",
      "deepLink": "dukkan://orders/8Fq2/track",
      "orderId": "8Fq2",
      "imageUrl": "",
      "notificationId": "inbox doc id"
    }
  }
}
```

| Key | Why |
|---|---|
| `ttl` | An "on the way" push delivered 2 hours late is noise. Offers use `30s` |
| `collapse_key` / tag = order id | A newer status replaces the older one on the phone instead of stacking 6 notifications |
| All values are strings | FCM `data` only allows string values |
| `notificationId` | Tapping marks that inbox entry read |

Function helper (`firebase/functions/src/push/send.ts`):

```typescript
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { render, TemplateKey } from "./templates";

/** Sends to every device of a user, in each device's language, and removes dead tokens. */
export async function pushToUser(uid: string, key: TemplateKey, vars: Record<string, string>, data: Record<string, string>) {
  const devices = await getFirestore().collection(`users/${uid}/devices`).where("fcmToken", "!=", null).get();

  await Promise.all(devices.docs.map(async (device) => {
    const lang = device.data().languageCode ?? "en";
    const { title, body, channelId } = render(key, lang, vars);
    try {
      await getMessaging().send({
        token: device.data().fcmToken,
        android: { priority: "high", ttl: 600_000, collapseKey: data.orderId },
        data: { ...data, type: key, channelId, title, body },
      });
    } catch (e: any) {
      if (e.code === "messaging/registration-token-not-registered" || e.code === "messaging/invalid-argument") {
        await device.ref.delete();
      }
    }
  }));
}
```

`templates.ts` holds every message in `ar` / `en` / `fr`, e.g.:

```typescript
export const TEMPLATES = {
  order_on_the_way: {
    channelId: "order_updates",
    en: { title: "{courier} is on the way", body: "Order #{number} arrives in {minutes} min." },
    fr: { title: "{courier} est en route", body: "La commande #{number} arrive dans {minutes} min." },
    ar: { title: "{courier} في الطريق", body: "طلبك #{number} يصل خلال {minutes} دقيقة." },
  },
  // …
} as const;
```

The inbox entry uses the **user's** `languageCode` (one language per inbox).

---

## 5. Android: manifest

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <service
        android:name=".core.push.DukkanMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>

    <meta-data
        android:name="com.google.firebase.messaging.default_notification_channel_id"
        android:value="order_updates" />
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_icon"
        android:resource="@drawable/ic_notification" />
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_color"
        android:resource="@color/brand_primary" />

    <activity android:name=".MainActivity" ...>
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="dukkan" />
        </intent-filter>
    </activity>
</application>
```

`ic_notification` must be a **white, single-colour** vector (the Dukkan awning mark). Android draws it as a silhouette.

---

## 6. Android: channels

`core/push/NotificationChannels.kt`:

```kotlin
package com.example.dukkanapp.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.dukkanapp.R

object NotificationChannels {
    const val ORDER_UPDATES = "order_updates"
    const val OFFERS = "offers"
    const val ACCOUNT = "account"

    /** Safe to call on every start: re-creating an existing channel only updates its name. */
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(ORDER_UPDATES, context.getString(R.string.channel_order_updates), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(OFFERS, context.getString(R.string.channel_offers), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(ACCOUNT, context.getString(R.string.channel_account), NotificationManager.IMPORTANCE_LOW),
            )
        )
    }
}
```

Call `NotificationChannels.createAll(this)` in `DukkanApplication.onCreate()`. Channel names come from `strings.xml`, so they follow the app language at the time of creation (re-called after a language change).

---

## 7. Android: the messaging service

`core/push/DukkanMessagingService.kt`:

```kotlin
package com.example.dukkanapp.core.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.dukkanapp.MainActivity
import com.example.dukkanapp.R
import com.example.dukkanapp.core.di.ApplicationScope
import com.example.dukkanapp.features.auth.domain.repository.DeviceRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DukkanMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    /** FCM rotated the token: save it on this device's document (no-op when signed out). */
    override fun onNewToken(token: String) {
        appScope.launch { deviceRepository.updateFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"].orEmpty()
        val channelId = data["channelId"] ?: NotificationChannels.ORDER_UPDATES
        val deepLink = data["deepLink"]
        val tag = data["orderId"] ?: data["type"]

        val intent = Intent(Intent.ACTION_VIEW, deepLink?.toUri(), this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, data["notificationId"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this, tag.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.brand_primary))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) {
            @Suppress("MissingPermission") // checked by areNotificationsEnabled()
            manager.notify(tag, NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        private const val NOTIFICATION_ID = 1 // with a per-order tag, a new status replaces the old one
    }
}
```

**Why `appScope` and not `GlobalScope`:** `onNewToken` returns immediately; the write must outlive it, but still be cancellable and testable. Add the scope once:

```kotlin
// core/di/CoroutineScopeModule.kt
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module @InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

Add to the auth feature's `DeviceRepository`:

```kotlin
/** Saves a rotated FCM token on this install's device doc. Does nothing when signed out. */
suspend fun updateFcmToken(token: String): EmptyResult
```

and in `DeviceRemoteDataSource`:

```kotlin
suspend fun updateFcmToken(uid: String, installationId: String, token: String): Unit = guardException {
    firestore.collection("users").document(uid)
        .collection("devices").document(installationId)
        .set(mapOf("fcmToken" to token, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        .await()
}

suspend fun currentFcmToken(): String = guardException { messaging.token.await() }
suspend fun deleteFcmToken(): Unit = guardException { messaging.deleteToken().await() }
```

`registerCurrentDevice` adds `"fcmToken" to currentFcmToken()` to its fields, and sign-out calls `deleteFcmToken()` after deleting the device doc.

---

## 8. Permission flow (12.1 → 12.2)

**When:** right after the first order is placed (09.3), and from Settings (11.3) if a toggle is turned on while permission is denied. Never at first launch.

**State:** a small `NotificationPermissionViewModel` (MVI) with:

| Intent | Effect |
|---|---|
| `TurnOnClicked` (12.1 "Turn on notifications") | `RequestSystemPermission` → launcher shows 12.2 |
| `NotNowClicked` | save `NOTIF_PROMPT_DISMISSED_AT` in DataStore; don't ask again for 7 days |
| `PermissionResult(granted)` | if denied twice → next time open app settings instead |

Screen side:

```kotlin
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    onIntent(NotificationPromptIntent.PermissionResult(granted))
}

AppCollectEffect(viewModel.effect) { effect ->
    when (effect) {
        NotificationPromptEffect.RequestSystemPermission ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onIntent(NotificationPromptIntent.PermissionResult(granted = true)) // no runtime permission before 13
            }
        NotificationPromptEffect.OpenAppSettings -> context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
        NotificationPromptEffect.Close -> onClose()
    }
}
```

The pre-prompt copy promises what we really do: "Order status in real time · Price drops on saved items · Flash deals — max 3 a week". The `pushQuota` doc enforces that promise.

---

## 9. Deep links

`MainActivity` passes `intent.data` to the NavHost. With type-safe navigation, add `deepLinks` to destinations:

```kotlin
composable<AppRoute.OrderTracking>(
    deepLinks = listOf(navDeepLink<AppRoute.OrderTracking>(basePath = "dukkan://orders/{orderId}/track")),
) { /* … */ }
```

Customer deep links used by the catalog in § 3:

| Link | Screen |
|---|---|
| `dukkan://orders/{id}` | 10.2 (or 10.6 / 10.7 when `status` is cancelled / failed) |
| `dukkan://orders/{id}/track` | 10.3 |
| `dukkan://orders/{id}/rate` | 10.4 |
| `dukkan://products/{id}`, `dukkan://collections/{id}` | 06.1, 07.2 |
| `dukkan://coupons` | 07.3 My coupons |
| `dukkan://wallet` | 11.6 Payment methods (wallet history) |
| `dukkan://invite` | 11.8 Invite friends |
| `dukkan://offers` | 07.1 |

If the user is signed out, the start-destination logic (auth guide § 13) runs first; keep the pending deep link in `AppStartViewModel` and open it after Home.

Tapping also marks the inbox entry read: `EXTRA_NOTIFICATION_ID` → `markRead`.

---

## 10. Topics (only for broadcast)

| Topic | Who subscribes | Use |
|---|---|---|
| `all_customers_{lang}` | every customer install, by app language | app-wide announcements, e.g. service outage. Not for deals (quota can't be enforced per user on topics) |
| `couriers_{zoneId}` | team app couriers, by zone | "Demand ×1.5 near you" nudges |

---

## Check

- [ ] Channels exist on Android 8+ (Settings → Apps → Dukkan → Notifications shows 3 categories).
- [ ] `fcmToken` appears on the device doc after sign-in, and changes after reinstall.
- [ ] Sign out → the device doc is gone and no more pushes arrive.
- [ ] Send a test data message from the emulator / console: notification appears in background **and** foreground.
- [ ] Tapping opens the right screen and marks the inbox entry read.
- [ ] Android 13: pre-prompt → system dialog → "Don't allow" twice → next time opens app settings.
- [ ] A 4th flash-deal push in the same week is not sent.
