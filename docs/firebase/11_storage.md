# 11: Cloud Storage

Avatars (11.2 "Change photo"), product and catalog images, address voice notes (03.3), packing photos (F02.1), door photos (C02.4), problem-report photos (C03.3). In the team app, photos taken offline wait in the outbox and are uploaded before their callable is replayed (13 § 7b).

---

## Why

- **Files go to Storage, the path goes to Firestore.** Firestore documents are limited to 1 MiB and are re-downloaded by every listener. A photo is stored once in Storage; Firestore keeps only its path (`photoUrl`, `packingPhotoPath`, …).
- **Store paths, not download URLs, for private files.** A download URL with a token works forever for anyone who has it. Private files (voice notes, door photos) are stored by path and read through the SDK, which checks `storage.rules`.
- **Public files store the download URL.** Catalog images and avatars are public, and Coil can load a URL directly with caching.
- **Compress on the phone.** A 12 MP photo is 4–6 MB. We upload a 1600 px JPEG at 80% quality (≈ 300 KB). This keeps courier uploads fast on mobile data and keeps rules limits realistic.

---

## 1. Paths

| Path | Who writes | Who reads | Max | Type | Stored in Firestore as |
|---|---|---|---|---|---|
| `users/{uid}/avatar/{timestamp}.jpg` | the user | everyone | 2 MB | jpeg/png/webp | `users/{uid}.photoUrl` (download URL) |
| `users/{uid}/voice/{addressId}.m4a` | the user | the user (+ functions give the picker/courier a short signed URL) | 1 MB (≈ 60 s) | audio/mp4 | `addresses/{id}.voiceNotePath` (path), copied into `orders/{id}.delivery.voiceNotePath` |
| `catalog/products/{productId}/{n}.webp` | admin (Admin SDK) | everyone | – | webp | `products/{id}.images[]` (URLs) |
| `catalog/brands/{brandId}/logo.webp`, `cover.webp` | admin | everyone | – | webp | `brands/{id}.logoUrl`, `coverUrl` |
| `catalog/categories/{categoryId}.webp` | admin | everyone | – | webp | `categories/{id}.imageUrl` |
| `catalog/banners/{bannerId}.webp` | admin | everyone | – | webp | `banners/{id}.imageUrl` |
| `orders/{orderId}/packing/{timestamp}.jpg` | picker | pickers, managers | 3 MB | jpeg | `orders/{id}.packingPhotoPath` (path) |
| `orders/{orderId}/door/{timestamp}.jpg` | courier | couriers, managers | 3 MB | jpeg | `orders/{id}.doorPhotoPath` (path) |
| `orders/{orderId}/issues/{issueId}.jpg` | courier / picker | staff, managers | 3 MB | jpeg | `orders/{id}/issues/{issueId}.photoPath` (C03.3 "Package damaged") |

**Why a timestamp in avatar file names:** a new file name busts image caches. Overwriting `avatar.jpg` would keep showing the old photo from Coil's cache. The old file is deleted after the new one is saved.

Rules: [07 § 2](07_security_rules.md#2-firebasestoragerules).

---

## 2. Upload code (Android)

`core/storage/ImageCompressor.kt`, shrink before upload:

```kotlin
package com.example.dukkanapp.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class ImageCompressor @Inject constructor(@ApplicationContext private val context: Context) {

    /** Decodes [uri], scales the long edge to [maxSide] px, and encodes JPEG at [quality]. Runs off the main thread. */
    suspend fun compress(uri: Uri, maxSide: Int = 1600, quality: Int = 80): ByteArray = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (w, h) = info.size.width to info.size.height
            val scale = maxSide.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }
}
```

> `ImageDecoder` needs API 28. Our `minSdk` is 24: on 24–27 fall back to `BitmapFactory.decodeStream` with `inSampleSize`. This is the one place in the app where we choose a dispatcher: decoding is CPU and disk work, and Firebase calls themselves are already non-blocking.

`features/account/data/datasource/AvatarRemoteDataSource.kt`:

```kotlin
class AvatarRemoteDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) {
    /** Uploads the avatar and returns its public download URL. */
    suspend fun upload(uid: String, jpeg: ByteArray): String = guardException {
        val ref = storage.reference.child("users/$uid/avatar/${System.currentTimeMillis()}.jpg")
        val metadata = storageMetadata { contentType = "image/jpeg" }
        ref.putBytes(jpeg, metadata).await()
        ref.downloadUrl.await().toString()
    }

    /** Best effort: deleting the previous avatar never fails the profile update. */
    suspend fun deleteByUrl(url: String) {
        runCatching { storage.getReferenceFromUrl(url).delete().await() }
    }
}
```

**Edit profile flow (11.2):** compress → upload → `users/{uid}.photoUrl = url` (Firestore) → delete old file. If the Firestore write fails, delete the new file so nothing is orphaned.

Team app photo uploads (F02.1, C02.4) use `putFile` with the compressed bytes and report progress with `addOnProgressListener` for a progress ring, then pass the **path** (not URL) to `markPacked` / `completeDelivery`.

---

## 3. Map `StorageException`

Add to `AppFirebaseExceptionMapper`:

| `StorageException.errorCode` | `AppException` |
|---|---|
| `ERROR_NOT_AUTHENTICATED` | `Auth.NoCurrentUser` |
| `ERROR_NOT_AUTHORIZED` | `Firestore.PermissionDenied` (rename later to a shared `PermissionDenied` if you prefer) |
| `ERROR_RETRY_LIMIT_EXCEEDED` | `Network.NoConnection` |
| `ERROR_QUOTA_EXCEEDED` | `Network.TooManyRequests` |
| `ERROR_OBJECT_NOT_FOUND` | `Firestore.NotFound` |
| `ERROR_CANCELED` | rethrow as `CancellationException` |
| else | `Unexpected` |

---

## 4. Images in the UI

Load with **Coil 3** (`io.coil-kt.coil3:coil-compose` + `coil-network-okhttp`). For private paths (door photo shown to a manager), resolve `ref.downloadUrl` at view time, or let a function return a signed URL valid for 15 minutes.

Catalog images are produced by the admin pipeline in **WebP**, at widths 400 (cards) and 1080 (details): `…/1_400.webp`, `…/1_1080.webp`. Cards load the small one. This is the biggest data saver in the app.

---

## Check

- [ ] Uploading a 10 MB photo results in < 500 KB in Storage.
- [ ] A customer cannot upload into another user's folder (rules test).
- [ ] A customer cannot read `orders/*/door/*`.
- [ ] Changing the avatar shows the new photo immediately (new file name) and deletes the old one.
