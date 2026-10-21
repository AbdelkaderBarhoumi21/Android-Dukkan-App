# Auth Feature Implementation Guide

Email/password **sign up** and **login** with Firebase Auth, an **email verification** screen, **Remember Me**, and a Firestore **user + device** schema ready for push notifications. Google and Apple sign-in are planned as TODOs.

Everything follows the project's existing building blocks:

| Building block | File | Used for |
|---|---|---|
| `AppViewModel<S, I, E>` | `core/common/mvi/AppViewModel.kt` | every ViewModel |
| `AppCollectEffect` | `core/common/mvi/AppCollectEffect.kt` | every screen's one-shot effects |
| `AppUiText` | `core/common/text/AppUiText.kt` | every message shown to the user |
| `AppResult` / `EmptyResult` | `core/error/AppResult.kt` | every repository return value |
| `guardException { }` | `core/error/guard/guardException.kt` | every data-source call |
| `failureMapper.guardFailure { }` | `core/error/guard/guardFailure.kt` | every repository call |
| `AppFailure` → `toUiText()` | `core/error/ui/AppFailureUiText.kt` | error messages |
| `ValidationError` → `toUiText()` | `core/error/ui/ValidationErrorUiText.kt` | form errors |

> **Companion doc:** [Auth Packages, Instances and Flows](auth_packages_and_flow_guide.md) explains every dependency, every Firebase instance we create, and how each flow runs step by step. Read it alongside this guide.
>
> This guide **replaces** [firebase_auth_integration_guide.md](firebase_auth_integration_guide.md). You can delete the old one once you have moved over.

```
                ┌──────────── SignUp ────────────┐        ┌──────────── Login ─────────────┐
                │ Firebase Auth: create account  │        │ Firebase Auth: sign in         │
                │ save Remember Me               │        │ save Remember Me               │
                │ sync users/{uid} + device      │        │ sync users/{uid} + device      │
                │ send verification email        │        │                                │
                └───────────────┬────────────────┘        └──────┬─────────────────┬───────┘
                                │                         not verified         verified
                                ▼                                │                 │
                     ┌─────────────────────┐ ◄───────────────────┘                 │
                     │ Email Verification  │                                       │
                     │ re-checks on resume │── verified ──────────────────────────►│
                     └─────────────────────┘                                       ▼
                                                                                 Home
App start: language? → onboarding? → session?
           SignedOut → AuthOptions   EmailNotVerified → EmailVerification   SignedIn → Home
```

---

## Table of Contents

0. [Decisions and Why](#0-decisions-and-why)
1. [Fix the Existing Code First](#1-fix-the-existing-code-first)
2. [Gradle Setup](#2-gradle-setup)
3. [Firebase Console and Security Rules](#3-firebase-console-and-security-rules)
4. [Firestore Schema: users and devices](#4-firestore-schema-users-and-devices)
5. [Core Additions](#5-core-additions)
6. [Domain Layer](#6-domain-layer)
7. [Data Layer](#7-data-layer)
8. [Dependency Injection](#8-dependency-injection)
9. [Sign Up (MVI)](#9-sign-up-mvi)
10. [Login (MVI)](#10-login-mvi)
11. [Email Verification (MVI)](#11-email-verification-mvi)
12. [Home Placeholder (MVI)](#12-home-placeholder-mvi)
13. [Navigation and App Start](#13-navigation-and-app-start)
14. [String Resources](#14-string-resources)
15. [TODO: Google and Apple Sign-In](#15-todo-google-and-apple-sign-in)
16. [Folder Structure](#16-folder-structure)
17. [Implementation Checklist](#17-implementation-checklist)
18. [Manual Test Scenarios](#18-manual-test-scenarios)

---

## 0. Decisions and Why

### 0.1 Remember Me

Firebase Auth **already persists the session on disk**. After a successful sign-in, `FirebaseAuth.currentUser` is non-null on every launch until `signOut()` is called. So "stay logged in" is free; what we actually build is the opposite: **forgetting** the session when the user did *not* tick Remember Me.

| Remember Me | What happens on the next real app launch |
|---|---|
| ✅ ticked | Session kept → start on **Home** (or Email Verification if not verified yet) |
| ⬜ not ticked | Session ended (`SignOutUseCase`) → start on **AuthOptions** |

- The flag is a `Boolean` in DataStore (`AppPreferencesKeys.REMEMBER_ME`). Missing key = `false`, the safe default.
- **"Real launch" matters.** Android kills apps in the background all the time, for example while the user is in the mail app tapping the verification link. When they come back, Android restores the app. That is *not* a restart, and logging them out there would break verification. We tell the two apart with `SavedStateHandle` in `AppStartViewModel` (see [13.3](#step-133-appstartviewmodel)):
  - Fresh launch (icon tap after the app was closed or swiped away): saved state is empty → Remember Me is enforced.
  - Restore after process death: saved state exists → session kept, user returns to the screen they left.

### 0.2 Email verification is required before Home

Email/password accounts can be created with any address. Verifying proves the customer owns it, which matters for order emails, password reset and fraud. The verified flag lives in **Firebase Auth** (`FirebaseUser.isEmailVerified`), never in Firestore: one source of truth.

The cached flag never updates on its own. The verification screen calls `reload()` on **every resume** (user returns from the mail app) and on a manual button. No polling loop: it would run in the background, drain battery and spend quota.

### 0.3 Two Firestore documents, two repositories

```
users/{uid}                       one per customer: profile + preferences
users/{uid}/devices/{installId}   one per installed app: device info + FCM token (later)
```

- **Why a `devices` subcollection instead of an `fcmTokens` array in the user document?** A customer can be signed in on a phone and a tablet. Each install has its own push token, which changes over time and must be **deleted on sign-out** so a signed-out phone stops receiving that customer's order notifications. A document per install makes add/refresh/delete a single write with no array juggling, and keeps the device data (name, OS, app version) next to its token.
- **Why `installationId` as the document id?** It comes from Firebase Installations: stable for the life of the install, not copied by Android backup to a new phone (two phones would otherwise share one document), and the same id FCM uses internally.
- `UserRepository` handles the profile and `DeviceRepository` the device (Interface Segregation: sign-out needs only the device part).

### 0.4 Profile sync is best effort

Once Firebase Auth says "account created" or "signed in", the user **is** signed in. If the Firestore write then fails (flaky network), failing the whole sign-up would be worse: a retry would answer "email already in use". So:

- Auth success = sign-up/sign-in success.
- The profile and device are synced after **every** sign-in, so a failed write heals itself next time.
- Unexpected failures are still reported through `ErrorReporter` by `DefaultFailureMapper`.

### 0.5 Dispatchers: not needed (yet)

You asked whether we need dispatcher injection. **No, not for this feature:**

| Work | Thread | Why it's safe on Main |
|---|---|---|
| Firebase Auth / Firestore / Installations | Firebase's own threads | They return a `Task`; `await()` **suspends** without blocking. |
| DataStore | DataStore's own IO dispatcher | `data` and `edit` are main-safe by design. |
| Device info (`Build.*`, `BuildConfig`, `TimeZone`) | Main | Static fields, no IO. `Settings.Global.getString` is a tiny cached lookup. |

Adding `withContext(Dispatchers.IO)` around these would only add a thread switch. The companion doc explains when you **would** add a `@IoDispatcher` (file IO, Room raw queries, image compression, heavy JSON).

One dispatcher fix is still needed: `AppCollectEffect` says `Main.immediate` in its comment but uses `Dispatchers.Main` (see [1.2](#12-appcollecteffect-must-use-mainimmediate)).

### 0.6 SOLID and patterns used

| Principle / pattern | Where |
|---|---|
| **S**ingle Responsibility | `AuthRepository` (identity), `SessionRepository` (Remember Me), `UserRepository` (profile), `DeviceRepository` (device). One use case = one user action. |
| **O**pen/Closed | Adding Google sign-in adds one method and one use case; sign-up/login code doesn't change. |
| **L**iskov | Every `*RepositoryImpl` is replaceable by a fake in tests. |
| **I**nterface Segregation | Small repositories; `SignOutUseCase` doesn't see profile methods. |
| **D**ependency Inversion | Domain declares interfaces; data implements them; Hilt binds them. Domain never imports Firebase. |
| Repository | `*RepositoryImpl` hides Firebase/DataStore behind domain interfaces. |
| Use Case (Interactor) | `SignInWithEmailUseCase`, `SyncSignedInUserUseCase`, ... |
| Mapper | `FirebaseUser.toAuthUser()`, `AuthUser.toNewUserDto()`, `DeviceInfo.toFirestoreFields()` |
| Adapter | `AndroidDeviceInfoProvider` adapts Android APIs to `DeviceInfoProvider`. |
| Facade | `SignUpWithEmailUseCase` orchestrates auth + session + profile + email in one call. |
| State / Intent / Effect (MVI) | every screen |

---

## 1. Fix the Existing Code First

These bugs are in the current code. The full files later in this guide already include the fixes; this list tells you **why** each line changes.

### 1.1 `AppUiText.asString()` ignores its arguments

`stringResource(id)` drops `args`, so "Password must be at least %1$d characters" shows `%1$d` literally.

**File:** `core/common/text/AppUiText.kt`

```kotlin
package com.example.dukkanapp.core.common.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface AppUiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : AppUiText
    data class Raw(val value: String) : AppUiText
}

@Composable
fun AppUiText.asString(): String = when (this) {
    is AppUiText.Res -> stringResource(id, *args.toTypedArray())
    is AppUiText.Raw -> value
}

fun AppUiText.asString(context: Context): String = when (this) {
    is AppUiText.Res -> context.getString(id, *args.toTypedArray())
    is AppUiText.Raw -> value
}
```

### 1.2 `AppCollectEffect` must use `Main.immediate`

With plain `Main`, an effect can be dispatched one frame late, and a navigation can be lost if the screen leaves composition in between. `Main.immediate` runs the handler right away when already on the main thread.

**File:** `core/common/mvi/AppCollectEffect.kt`

```kotlin
package com.example.dukkanapp.core.common.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <E> AppCollectEffect(effect: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifeCycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifeCycleOwner) {
        lifeCycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate: an effect is never lost between emission and a recomposition.
            withContext(Dispatchers.Main.immediate) {
                effect.collect { currentOnEffect(it) }
            }
        }
    }
}
```

### 1.3 `SignUpViewModel` bugs

| Line today | Problem |
|---|---|
| `AuthFormValidator.validateEmail(password)` | Validates the password **as an email**. |
| `\|\| confirmPassword != null` | `confirmPassword` is a `String`, never null → sign-up **never** proceeds. Should be `confirmPasswordError`. |
| `current.password.trim()` | Spaces are valid password characters. Trimming means the user's real password differs from the one Firebase stores. |

### 1.4 `SignUpUiState` inverted checks

`isValidEmail = emailError != null && ...` shows the green "success" border only when there **is** an error. All three must be `== null`.

### 1.5 `AuthFormValidator` trims passwords

Same issue as 1.3: `password.trim().isValidPassword()` measures a different string than the one sent to Firebase. `"1234567 "` (8 characters, last one a space) is rejected as too short, although Firebase would accept it. Passwords are validated exactly as typed. See [6.3](#step-63-authformvalidator).

### 1.6 Package names don't match folders

| File | Package today | Should be |
|---|---|---|
| `logic/login/LoginEffect.kt`, `LoginIntent.kt` | `...logic.Login` | `...logic.login` |
| `components/signup/SignUpScreenContent.kt` | `...components.SignUp` | `...components.signup` |
| `components/authoptions/AuthButton.kt` | *(no package)* | `...components.authoptions` |

Windows ignores case in folder names but git and Kotlin do not. Your git status shows a half-done rename (`logic/Login/`). Finish it with git so the change is recorded:

```bash
git mv app/src/main/java/com/example/dukkanapp/features/auth/presentation/logic/Login app/src/main/java/com/example/dukkanapp/features/auth/presentation/logic/login_tmp
git mv app/src/main/java/com/example/dukkanapp/features/auth/presentation/logic/login_tmp app/src/main/java/com/example/dukkanapp/features/auth/presentation/logic/login
```

After adding `package com.example.dukkanapp.features.auth.presentation.components.authoptions` at the top of `AuthButton.kt`, remove the `import AuthButton` line from `AuthOptionsSection.kt`: both files are now in the same package.

### 1.7 `LoginScreen` and `SignUpScreen` routes aren't `AppRoute`

`data object LoginScreen` and `data object SignUpScreen` are missing `: AppRoute`. The full `AppRoute.kt` in [13.1](#step-131-approute) fixes it.

### 1.8 Unused Android import in the validator path

`core/utils/extension/StringExtension.kt` still imports `android.util.Patterns` although it uses a Kotlin `Regex`. The import drags Android into code the domain validator uses. Remove it (full file in [5.2](#step-52-stringextension)).

---

## 2. Gradle Setup

Your project already has the Firebase BoM, `firebase-auth`, `firebase-firestore`, `kotlinx-coroutines-play-services` and the Google Services plugin. Two additions are needed.

### Step 2.1: `gradle/libs.versions.toml`

Add under `[libraries]`, next to the other Firebase entries:

```toml
firebase-installations = { group = "com.google.firebase", name = "firebase-installations" }
```

No version: the BoM (`firebase-boom`) picks a compatible one.

### Step 2.2: `app/build.gradle.kts`

Enable `BuildConfig` (we store the app version per device) and add the dependency:

```kotlin
android {
    // ... existing config ...
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.VERSION_NAME / VERSION_CODE for the devices document
    }
}

dependencies {
    // ... existing dependencies ...

    // Firebase
    implementation(platform(libs.firebase.boom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.installations) // stable per-install id for users/{uid}/devices/{id}
}
```

### Step 2.3: `google-services.json`

1. [Firebase Console](https://console.firebase.google.com/) → Project settings → add an Android app with package `com.example.dukkanapp`.
2. Download `google-services.json` into `app/`.
3. If the repository is public, add `app/google-services.json` to `.gitignore`.

---

## 3. Firebase Console and Security Rules

### Step 3.1: Authentication

1. **Authentication → Sign-in method:** enable **Email/Password** (leave "Email link (passwordless)" off).
2. **Authentication → Settings → User actions:** keep **Email enumeration protection** on (default). Wrong password and unknown email both return `ERROR_INVALID_CREDENTIAL`, which your mapper turns into `AppFailure.Auth.InvalidCredentials` ("Incorrect email or password"). Attackers can't probe which emails have accounts.
3. **Authentication → Templates → Email address verification:** set the sender name to "Dukkan" and review the text. Firebase translates it automatically for the language we pass (`ar`, `en`, `fr`), see `AuthRemoteDataSource.sendEmailVerification`.

### Step 3.2: Firestore

Create the database in production mode in the region closest to your customers, then paste these rules (**Firestore → Rules**):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }

    // The profile email must always equal the email Firebase Auth vouches for.
    function emailMatchesToken() {
      return request.resource.data.email == request.auth.token.email;
    }

    match /users/{userId} {
      allow read: if isOwner(userId);

      allow create: if isOwner(userId)
        && emailMatchesToken()
        && request.resource.data.keys().hasOnly([
             'email', 'displayName', 'photoUrl', 'phoneNumber', 'authProviders',
             'languageCode', 'marketingNotificationsEnabled',
             'createdAt', 'updatedAt', 'lastLoginAt'])
        && request.resource.data.createdAt == request.time;

      allow update: if isOwner(userId)
        && emailMatchesToken()
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly([
             'email', 'displayName', 'photoUrl', 'phoneNumber', 'authProviders',
             'languageCode', 'marketingNotificationsEnabled', 'updatedAt', 'lastLoginAt']);

      // Account deletion goes through a Cloud Function (deletes Auth user + data together).
      allow delete: if false;

      match /devices/{installationId} {
        allow read, delete: if isOwner(userId);

        allow create, update: if isOwner(userId)
          && request.resource.data.keys().hasOnly([
               'platform', 'deviceName', 'manufacturer', 'model', 'osVersion', 'sdkInt',
               'appVersionName', 'appVersionCode', 'languageCode', 'timeZone',
               'fcmToken', 'updatedAt']);
      }
    }
  }
}
```

**Why each rule:**
- `isOwner`: a signed-in customer can only touch **their own** documents.
- `hasOnly([...])`: a modified app can't add fields like `role: "admin"` or `walletBalance`.
- `createdAt == request.time`: the client can't fake its sign-up date; it must send a server timestamp.
- `createdAt` missing from the update list: it can never be changed after creation.
- `allow delete: if false` on users: a customer deleting their document from a hacked client would orphan orders.

---

## 4. Firestore Schema: users and devices

### 4.1 `users/{uid}`

Document id = Firebase Auth `uid`.

| Field | Type | Written by | Why we need it |
|---|---|---|---|
| `email` | string | sync on every sign-in | Order emails, support lookup. Synced so a future "change email" flow stays consistent. |
| `displayName` | string? | first sign-in (Google gives one), later profile edit | Greeting, order labels |
| `photoUrl` | string? | first sign-in (Google), later profile edit | Avatar |
| `phoneNumber` | string? | later (checkout / profile) | Delivery contact, SMS |
| `authProviders` | string[] | sync on every sign-in | `["password"]`, `["google.com"]`... Support can tell how the customer logs in; the app can hide "change password" for Google-only accounts. |
| `languageCode` | string | sync on every sign-in | **Push notifications in the customer's language.** The server reads it to pick the `ar`/`en`/`fr` template. |
| `marketingNotificationsEnabled` | bool | first sign-in = `false`, later settings screen | Consent for promotional pushes/emails. Default `false` (opt-in, GDPR-friendly). Order/delivery pushes don't need it. |
| `createdAt` | timestamp | server, once | Customer since; analytics |
| `updatedAt` | timestamp | server, on profile edits | Cache invalidation, auditing |
| `lastLoginAt` | timestamp | server, every sign-in | Inactive-customer campaigns, security review |

**Not stored on purpose:**
- `isEmailVerified`: Firebase Auth is the source of truth; a copy goes stale.
- Password or tokens: never.
- Push tokens: per device, in the subcollection below.
- `role` / `isAdmin`: if you need roles later, use **Firebase Auth custom claims** set by a Cloud Function, never a client-writable field.

Example:

```json
{
  "email": "customer@example.com",
  "displayName": null,
  "photoUrl": null,
  "phoneNumber": null,
  "authProviders": ["password"],
  "languageCode": "ar",
  "marketingNotificationsEnabled": false,
  "createdAt": "2026-10-21T09:12:44Z",
  "updatedAt": "2026-10-21T09:12:44Z",
  "lastLoginAt": "2026-10-21T09:12:44Z"
}
```

### 4.2 `users/{uid}/devices/{installationId}`

Document id = Firebase Installations id (one per app install).

| Field | Type | Example | Why we need it |
|---|---|---|---|
| `platform` | string | `"android"` | Send the right payload (Android vs iOS) once there's an iOS app |
| `deviceName` | string | `"My Pixel"` / `"samsung SM-S918B"` | "Signed-in devices" screen; security emails ("new sign-in on ...") |
| `manufacturer` | string | `"samsung"` | Debugging device-specific push issues (some OEMs kill background apps) |
| `model` | string | `"SM-S918B"` | Same |
| `osVersion` | string | `"15"` | Android 13+ needs the notification permission; target campaigns by OS |
| `sdkInt` | number | `35` | Same, machine-friendly |
| `appVersionName` | string | `"1.0"` | Don't send a push that opens a screen older versions lack; force-update prompts |
| `appVersionCode` | number | `1` | Same, comparable |
| `languageCode` | string | `"fr"` | Per-device language (one person, two phones, two languages) |
| `timeZone` | string | `"Africa/Tunis"` | Send pushes at a sensible local hour |
| `fcmToken` | string? | *(written later)* | **The push address.** Written by the push-notification feature on `onNewToken`. |
| `updatedAt` | timestamp | server | Find stale devices (no activity for 60 days → delete) |

**Lifecycle of a device document:**

```
sign in / sign up ──► set(merge) device info        (fcmToken untouched)
FCM onNewToken    ──► update fcmToken               (push feature, later)
sign out          ──► delete document               (this phone stops getting this customer's pushes)
uninstall         ──► document stays; the server deletes it when FCM answers "UNREGISTERED"
```

`deviceName` comes from the name the user gave their phone in Settings (Android 7.1+), falling back to manufacturer + model. It can be personal ("Sam's phone"): show it only to the account owner.

---

## 5. Core Additions

### Step 5.1: AppPreferencesKeys

**File:** `core/utils/constants/AppPreferencesKeys.kt`

```kotlin
package com.example.dukkanapp.core.utils.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferencesKeys {
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    val REMEMBER_ME = booleanPreferencesKey("remember_me")
}
```

### Step 5.2: StringExtension

**File:** `core/utils/extension/StringExtension.kt`

```kotlin
package com.example.dukkanapp.core.utils.extension

import com.example.dukkanapp.core.utils.constants.AppValidationConstants.MIN_PASSWORD_LENGTH

/**
 * Pure Kotlin on purpose: `android.util.Patterns` would make the validators that use these
 * functions untestable on the JVM and drag Android into the domain.
 */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun String.isValidEmail(): Boolean = isNotEmpty() && EMAIL_REGEX.matches(this)

fun String.isValidPassword(): Boolean = length >= MIN_PASSWORD_LENGTH
```

### Step 5.3: FirebaseModule

Firebase SDK entry points are singletons owned by the SDK. We provide them through Hilt so classes receive them in their constructor (testable, swappable) instead of calling `getInstance()` everywhere.

**File:** `core/di/FirebaseModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseInstallations(): FirebaseInstallations = FirebaseInstallations.getInstance()
}
```

### Step 5.4: AppMessageBanner

Shared by Login, Sign Up and Email Verification for messages that don't belong to a single field ("No internet connection", "Verification email sent").

**File:** `core/common/components/banner/AppMessageBanner.kt`

```kotlin
package com.example.dukkanapp.core.common.components.banner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppMessageBanner(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.radiusMd),
        color = if (isError) scheme.errorContainer else scheme.surfaceVariant,
        contentColor = if (isError) scheme.onErrorContainer else scheme.onSurfaceVariant,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(AppDimens.spaceMd),
        )
    }
}
```

---

## 6. Domain Layer

Pure Kotlin. No Firebase, no Android, no DataStore imports. This is what makes it testable on the JVM and what lets you swap Firebase later without touching ViewModels.

### Step 6.1: Models

**File:** `features/auth/domain/model/AuthUser.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.model

/** The signed-in identity. Source of truth: Firebase Auth (never copied to Firestore). */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    /** How the user signs in: "password", "google.com", "apple.com". */
    val providerIds: List<String>,
)
```

**File:** `features/auth/domain/model/SessionState.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.model

/** What the app should do with the stored session at launch. */
sealed interface SessionState {
    data object SignedOut : SessionState
    data object EmailNotVerified : SessionState
    data object SignedIn : SessionState
}
```

### Step 6.2: Repository interfaces

**File:** `features/auth/domain/repository/AuthRepository.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser

/** Identity only: who is signed in and how. Profile data lives in UserRepository. */
interface AuthRepository {

    /** Cached on disk by Firebase: no network call, safe to read at app start. */
    val currentUser: AuthUser?

    suspend fun signUpWithEmail(email: String, password: String): AppResult<AuthUser>

    suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser>

    // TODO(social-login): suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser>
    // TODO(social-login): suspend fun signInWithApple(): AppResult<AuthUser>

    /** [languageCode] picks the language of Firebase's email template ("ar", "en", "fr"). */
    suspend fun sendEmailVerification(languageCode: String): EmptyResult

    /** Fetches the user from the server: the cached isEmailVerified never changes on its own. */
    suspend fun reloadCurrentUser(): AppResult<AuthUser>

    fun signOut()
}
```

**File:** `features/auth/domain/repository/SessionRepository.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.repository

/** Local session policy. Firebase keeps the session; this decides whether we keep it. */
interface SessionRepository {
    suspend fun setRememberMe(enabled: Boolean)
    suspend fun isRememberMeEnabled(): Boolean
    suspend fun clear()
}
```

**File:** `features/user/domain/repository/UserRepository.kt`

```kotlin
package com.example.dukkanapp.features.user.domain.repository

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser

interface UserRepository {
    /**
     * First sign-in creates users/{uid}. Later sign-ins refresh only what can change
     * (email, providers, language, lastLoginAt). createdAt is never touched.
     */
    suspend fun syncProfile(user: AuthUser, languageCode: String): EmptyResult
}
```

**File:** `features/user/domain/repository/DeviceRepository.kt`

```kotlin
package com.example.dukkanapp.features.user.domain.repository

import com.example.dukkanapp.core.error.EmptyResult

interface DeviceRepository {
    /** Creates or refreshes users/{uid}/devices/{installationId} for this install. */
    suspend fun registerCurrentDevice(uid: String, languageCode: String): EmptyResult

    /** Deletes this install's device document so it stops receiving the user's pushes. */
    suspend fun unregisterCurrentDevice(uid: String): EmptyResult

    // TODO(push): suspend fun updatePushToken(uid: String, token: String): EmptyResult
}
```

### Step 6.3: AuthFormValidator

Updated so passwords are validated exactly as typed.

**File:** `features/auth/domain/validation/AuthFormValidator.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.validation

import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.core.utils.constants.AppValidationConstants
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

/** Shared by Login and Sign Up so each rule is written once. Pure Kotlin: JVM-testable. */
object AuthFormValidator {

    fun validateEmail(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EmailEmpty
        !email.trim().isValidEmail() -> ValidationError.EmailInvalid
        else -> null
    }

    /** Sign up: enforce the password policy. Never trimmed: a space is a valid password character. */
    fun validateNewPassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.PasswordEmpty
        !password.isValidPassword() -> ValidationError.PasswordTooShort(
            AppValidationConstants.MIN_PASSWORD_LENGTH
        )

        else -> null
    }

    /** Login: only require a value. Older accounts may predate the current policy. */
    fun validateExistingPassword(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.PasswordEmpty else null

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationError? =
        when {
            confirmPassword.isEmpty() -> ValidationError.ConfirmPasswordEmpty
            password != confirmPassword -> ValidationError.PasswordsMismatch
            else -> null
        }
}
```

### Step 6.4: User use case (shared by sign-up and sign-in)

**File:** `features/user/domain/usecase/SyncSignedInUserUseCase.kt`

```kotlin
package com.example.dukkanapp.features.user.domain.usecase

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import com.example.dukkanapp.features.user.domain.repository.DeviceRepository
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncSignedInUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val languageRepository: LanguageRepository,
) {
    /**
     * Best effort: never fails the sign-in. Unexpected failures are already reported by the
     * repositories, and the next sign-in syncs again. Profile and device are independent
     * documents, so both writes run in parallel.
     */
    suspend operator fun invoke(user: AuthUser) {
        val languageCode = languageRepository.selectedLanguageCode.first()
        coroutineScope {
            launch { userRepository.syncProfile(user, languageCode) }
            launch { deviceRepository.registerCurrentDevice(user.uid, languageCode) }
        }
    }
}
```

### Step 6.5: Auth use cases

**File:** `features/auth/domain/usecase/SendEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SendEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val languageRepository: LanguageRepository,
) {
    /** The email arrives in the app's language: Firebase has templates for ar, en and fr. */
    suspend operator fun invoke(): EmptyResult =
        authRepository.sendEmailVerification(
            languageCode = languageRepository.selectedLanguageCode.first()
        )
}
```

**File:** `features/auth/domain/usecase/SignUpWithEmailUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import com.example.dukkanapp.features.user.domain.usecase.SyncSignedInUserUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val syncSignedInUser: SyncSignedInUserUseCase,
    private val sendEmailVerification: SendEmailVerificationUseCase,
) {
    /**
     * Once the Auth account exists, sign-up has succeeded. The follow-up steps are best effort:
     * the profile is re-synced on every sign-in, and the email can be resent from the
     * verification screen. Returning a failure here would leave the user unable to retry
     * ("email already in use").
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): AppResult<AuthUser> =
        authRepository.signUpWithEmail(email, password)
            .onSuccess { user ->
                sessionRepository.setRememberMe(rememberMe)
                coroutineScope {
                    launch { syncSignedInUser(user) }
                    launch { sendEmailVerification() }
                }
            }
}
```

**File:** `features/auth/domain/usecase/SignInWithEmailUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import com.example.dukkanapp.features.user.domain.usecase.SyncSignedInUserUseCase
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val syncSignedInUser: SyncSignedInUserUseCase,
) {
    /** Returns the user so the caller can route verified users to Home, others to verification. */
    suspend operator fun invoke(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): AppResult<AuthUser> =
        authRepository.signInWithEmail(email, password)
            .onSuccess { user ->
                sessionRepository.setRememberMe(rememberMe)
                syncSignedInUser(user) // also self-heals a profile write that failed at sign-up
            }
}
```

**File:** `features/auth/domain/usecase/CheckEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.map
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class CheckEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Boolean> =
        authRepository.reloadCurrentUser().map { it.isEmailVerified }
}
```

**File:** `features/auth/domain/usecase/GetCurrentUserUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): AuthUser? = authRepository.currentUser
}
```

**File:** `features/auth/domain/usecase/SignOutUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import com.example.dukkanapp.features.user.domain.repository.DeviceRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() {
        authRepository.currentUser?.let { user ->
            // Must run BEFORE signOut(): the security rules need the user's auth to delete it.
            // Bounded: offline, Firestore would wait for the server forever. If it doesn't
            // finish, the server cleans the device up when FCM reports its token as dead.
            withTimeoutOrNull(UNREGISTER_DEVICE_TIMEOUT_MS) {
                deviceRepository.unregisterCurrentDevice(user.uid)
            }
        }
        authRepository.signOut()
        sessionRepository.clear()
    }

    private companion object {
        const val UNREGISTER_DEVICE_TIMEOUT_MS = 3_000L
    }
}
```

**File:** `features/auth/domain/usecase/GetSessionStateUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.features.auth.domain.model.SessionState
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import javax.inject.Inject

class GetSessionStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val signOut: SignOutUseCase,
) {
    /**
     * [isFreshLaunch] is false when Android restores the app after killing it in the background
     * (e.g. while the user was in the mail app). That is not a restart, so Remember Me is not
     * enforced and the user returns to the screen they left.
     */
    suspend operator fun invoke(isFreshLaunch: Boolean): SessionState {
        val user = authRepository.currentUser ?: return SessionState.SignedOut

        if (isFreshLaunch && !sessionRepository.isRememberMeEnabled()) {
            signOut()
            return SessionState.SignedOut
        }

        return if (user.isEmailVerified) SessionState.SignedIn else SessionState.EmailNotVerified
    }
}
```

---

## 7. Data Layer

Two translations, one per layer (from the [Error Handling guide](../core/error_handling_architecture.md)):

- **Data sources** wrap every SDK call in `guardException { }` → they throw only `AppException`.
- **Repositories** wrap every call in `failureMapper.guardFailure { }` → they return `AppResult` and never throw.

### Step 7.1: Auth mapper

**File:** `features/auth/data/mapper/AuthUserMapper.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.mapper

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.google.firebase.auth.FirebaseUser

/** Firebase lists itself as a provider of every user; it says nothing about how they sign in. */
private const val FIREBASE_PROVIDER_ID = "firebase"

fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    email = email.orEmpty(),
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified,
    providerIds = providerData
        .map { it.providerId }
        .filter { it != FIREBASE_PROVIDER_ID },
)
```

### Step 7.2: AuthRemoteDataSource

**File:** `features/auth/data/datasource/AuthRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.guard.guardException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * The only class that talks to FirebaseAuth. Contract: throws only [AppException].
 * Every suspend call goes through [guardException], which maps Firebase SDK exceptions.
 */
class AuthRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser = guardException {
        auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AppException.Unexpected()
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser = guardException {
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw AppException.Unexpected()
    }

    suspend fun sendEmailVerification(languageCode: String): Unit = guardException {
        auth.setLanguageCode(languageCode)
        requireUser().sendEmailVerification().await()
    }

    suspend fun reloadCurrentUser(): FirebaseUser = guardException {
        requireUser().reload().await()
        requireUser() // reload() updates the same object; read it again for the fresh flags
    }

    fun signOut() = auth.signOut()

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw AppException.Auth.NoCurrentUser()
}
```

### Step 7.3: SessionPreferenceLocalDataSource

Same shape as `OnboardingPreferenceLocalDataSource`.

**File:** `features/auth/data/datasource/SessionPreferenceLocalDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dukkanapp.core.utils.constants.AppPreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionPreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** Missing key = false: an unknown preference never keeps a session alive. */
    val rememberMe: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[AppPreferencesKeys.REMEMBER_ME] ?: false }

    suspend fun setRememberMe(enabled: Boolean) {
        dataStore.edit { it[AppPreferencesKeys.REMEMBER_ME] = enabled }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(AppPreferencesKeys.REMEMBER_ME) }
    }
}
```

### Step 7.4: AuthRepositoryImpl

**File:** `features/auth/data/repository/AuthRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.core.error.guard.guardFailure
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.features.auth.data.datasource.AuthRemoteDataSource
import com.example.dukkanapp.features.auth.data.mapper.toAuthUser
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val failureMapper: AppFailureMapper,
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = remote.currentUser?.toAuthUser()

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<AuthUser> =
        failureMapper.guardFailure { remote.signUpWithEmail(email, password).toAuthUser() }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser> =
        failureMapper.guardFailure { remote.signInWithEmail(email, password).toAuthUser() }

    override suspend fun sendEmailVerification(languageCode: String): EmptyResult =
        failureMapper.guardFailure { remote.sendEmailVerification(languageCode) }

    override suspend fun reloadCurrentUser(): AppResult<AuthUser> =
        failureMapper.guardFailure { remote.reloadCurrentUser().toAuthUser() }

    override fun signOut() = remote.signOut()
}
```

### Step 7.5: SessionRepositoryImpl

Local DataStore, no expected failures: no `AppResult` (same as `OnboardingRepositoryImpl`).

**File:** `features/auth/data/repository/SessionRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.repository

import com.example.dukkanapp.features.auth.data.datasource.SessionPreferenceLocalDataSource
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val localDataSource: SessionPreferenceLocalDataSource,
) : SessionRepository {

    override suspend fun setRememberMe(enabled: Boolean) {
        localDataSource.setRememberMe(enabled)
    }

    override suspend fun isRememberMeEnabled(): Boolean = localDataSource.rememberMe.first()

    override suspend fun clear() {
        localDataSource.clear()
    }
}
```

### Step 7.6: Firestore models

**File:** `features/user/data/model/UserDto.kt`

```kotlin
package com.example.dukkanapp.features.user.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore shape of users/{uid}, used to CREATE the document.
 * Updates write only the changed fields (see UserRemoteDataSource) so createdAt is never touched.
 * The uid is the document id, so it isn't a field.
 */
data class UserDto(
    val email: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val authProviders: List<String> = emptyList(),
    val languageCode: String = "",
    val marketingNotificationsEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
) {
    companion object {
        const val COLLECTION = "users"
        const val FIELD_EMAIL = "email"
        const val FIELD_AUTH_PROVIDERS = "authProviders"
        const val FIELD_LANGUAGE_CODE = "languageCode"
        const val FIELD_LAST_LOGIN_AT = "lastLoginAt"
    }
}
```

**File:** `features/user/data/model/DeviceDocument.kt`

```kotlin
package com.example.dukkanapp.features.user.data.model

/**
 * Field names of users/{uid}/devices/{installationId}.
 * Written as a map with merge, so registering a device never erases the fcmToken
 * that the push-notification feature writes.
 */
object DeviceDocument {
    const val COLLECTION = "devices"

    const val PLATFORM = "platform"
    const val PLATFORM_ANDROID = "android"
    const val DEVICE_NAME = "deviceName"
    const val MANUFACTURER = "manufacturer"
    const val MODEL = "model"
    const val OS_VERSION = "osVersion"
    const val SDK_INT = "sdkInt"
    const val APP_VERSION_NAME = "appVersionName"
    const val APP_VERSION_CODE = "appVersionCode"
    const val LANGUAGE_CODE = "languageCode"
    const val TIME_ZONE = "timeZone"
    const val FCM_TOKEN = "fcmToken" // TODO(push): written by the push-notification feature
    const val UPDATED_AT = "updatedAt"
}
```

### Step 7.7: Device info (Adapter over Android APIs)

The interface lets `DeviceRepositoryImpl` be unit-tested on the JVM with a fake, where `android.os.Build` doesn't exist.

**File:** `features/user/data/device/DeviceInfo.kt`

```kotlin
package com.example.dukkanapp.features.user.data.device

data class DeviceInfo(
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val sdkInt: Int,
    val appVersionName: String,
    val appVersionCode: Long,
    val timeZone: String,
)
```

**File:** `features/user/data/device/DeviceInfoProvider.kt`

```kotlin
package com.example.dukkanapp.features.user.data.device

fun interface DeviceInfoProvider {
    fun current(): DeviceInfo
}
```

**File:** `features/user/data/device/AndroidDeviceInfoProvider.kt`

```kotlin
package com.example.dukkanapp.features.user.data.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.dukkanapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject

class AndroidDeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceInfoProvider {

    override fun current(): DeviceInfo = DeviceInfo(
        deviceName = userVisibleDeviceName() ?: "${Build.MANUFACTURER} ${Build.MODEL}",
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        osVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        appVersionName = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE.toLong(),
        timeZone = TimeZone.getDefault().id,
    )

    /** The name the user gave the phone in Settings ("My Pixel"). Available since Android 7.1. */
    private fun userVisibleDeviceName(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?.takeIf { it.isNotBlank() }
        } else {
            null
        }
}
```

### Step 7.8: User mappers

**File:** `features/user/data/mapper/UserMapper.kt`

```kotlin
package com.example.dukkanapp.features.user.data.mapper

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.user.data.model.UserDto

fun AuthUser.toNewUserDto(languageCode: String): UserDto = UserDto(
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    authProviders = providerIds,
    languageCode = languageCode,
)
```

**File:** `features/user/data/mapper/DeviceMapper.kt`

```kotlin
package com.example.dukkanapp.features.user.data.mapper

import com.example.dukkanapp.features.user.data.device.DeviceInfo
import com.example.dukkanapp.features.user.data.model.DeviceDocument
import com.google.firebase.firestore.FieldValue

fun DeviceInfo.toFirestoreFields(languageCode: String): Map<String, Any> = mapOf(
    DeviceDocument.PLATFORM to DeviceDocument.PLATFORM_ANDROID,
    DeviceDocument.DEVICE_NAME to deviceName,
    DeviceDocument.MANUFACTURER to manufacturer,
    DeviceDocument.MODEL to model,
    DeviceDocument.OS_VERSION to osVersion,
    DeviceDocument.SDK_INT to sdkInt,
    DeviceDocument.APP_VERSION_NAME to appVersionName,
    DeviceDocument.APP_VERSION_CODE to appVersionCode,
    DeviceDocument.LANGUAGE_CODE to languageCode,
    DeviceDocument.TIME_ZONE to timeZone,
    DeviceDocument.UPDATED_AT to FieldValue.serverTimestamp(),
)
```

### Step 7.9: UserRemoteDataSource

**File:** `features/user/data/datasource/UserRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.user.data.datasource

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.guard.guardException
import com.example.dukkanapp.features.user.data.model.UserDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Contract: throws only [AppException]. */
class UserRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun userDoc(uid: String) = firestore.collection(UserDto.COLLECTION).document(uid)

    /**
     * A transaction makes "create if absent, otherwise update" atomic: two devices signing in
     * at the same moment can't both create the document and overwrite createdAt.
     */
    suspend fun syncProfile(uid: String, newProfile: UserDto): Unit = guardException {
        val ref = userDoc(uid)
        firestore.runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                transaction.update(
                    ref,
                    mapOf(
                        UserDto.FIELD_EMAIL to newProfile.email,
                        UserDto.FIELD_AUTH_PROVIDERS to newProfile.authProviders,
                        UserDto.FIELD_LANGUAGE_CODE to newProfile.languageCode,
                        UserDto.FIELD_LAST_LOGIN_AT to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                transaction.set(ref, newProfile)
            }
            null
        }.await()
    }
}
```

### Step 7.10: DeviceRemoteDataSource

**File:** `features/user/data/datasource/DeviceRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.user.data.datasource

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.guard.guardException
import com.example.dukkanapp.features.user.data.model.DeviceDocument
import com.example.dukkanapp.features.user.data.model.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Contract: throws only [AppException]. */
class DeviceRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val installations: FirebaseInstallations,
) {
    /**
     * Stable for the life of this install, not restored from backup onto another phone,
     * and the same id FCM uses. Cached by Firebase after the first call.
     */
    suspend fun installationId(): String = guardException {
        installations.id.await()
    }

    private fun deviceDoc(uid: String, installationId: String) =
        firestore.collection(UserDto.COLLECTION).document(uid)
            .collection(DeviceDocument.COLLECTION).document(installationId)

    /** Merge: keeps fields written by other flows (fcmToken). */
    suspend fun upsertDevice(uid: String, installationId: String, fields: Map<String, Any>): Unit =
        guardException {
            deviceDoc(uid, installationId).set(fields, SetOptions.merge()).await()
        }

    suspend fun deleteDevice(uid: String, installationId: String): Unit = guardException {
        deviceDoc(uid, installationId).delete().await()
    }
}
```

### Step 7.11: User repositories

**File:** `features/user/data/repository/UserRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.user.data.repository

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.core.error.guard.guardFailure
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.user.data.datasource.UserRemoteDataSource
import com.example.dukkanapp.features.user.data.mapper.toNewUserDto
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val failureMapper: AppFailureMapper,
) : UserRepository {

    override suspend fun syncProfile(user: AuthUser, languageCode: String): EmptyResult =
        failureMapper.guardFailure {
            remote.syncProfile(uid = user.uid, newProfile = user.toNewUserDto(languageCode))
        }
}
```

**File:** `features/user/data/repository/DeviceRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.user.data.repository

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.core.error.guard.guardFailure
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.features.user.data.datasource.DeviceRemoteDataSource
import com.example.dukkanapp.features.user.data.device.DeviceInfoProvider
import com.example.dukkanapp.features.user.data.mapper.toFirestoreFields
import com.example.dukkanapp.features.user.domain.repository.DeviceRepository
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val remote: DeviceRemoteDataSource,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val failureMapper: AppFailureMapper,
) : DeviceRepository {

    override suspend fun registerCurrentDevice(uid: String, languageCode: String): EmptyResult =
        failureMapper.guardFailure {
            val fields = deviceInfoProvider.current().toFirestoreFields(languageCode)
            remote.upsertDevice(uid = uid, installationId = remote.installationId(), fields = fields)
        }

    override suspend fun unregisterCurrentDevice(uid: String): EmptyResult =
        failureMapper.guardFailure {
            remote.deleteDevice(uid = uid, installationId = remote.installationId())
        }
}
```

---

## 8. Dependency Injection

**Rule of thumb:** a class with an `@Inject constructor` needs **no module** (data sources, use cases, `AndroidDeviceInfoProvider` itself). A module is needed only to:
- tell Hilt **which implementation** to use for an interface (`@Binds`), or
- create something you can't annotate, like a Firebase SDK object (`@Provides`, see `FirebaseModule` in [5.3](#step-53-firebasemodule)).

Already present and reused: `DataStoreModule` (`DataStore<Preferences>`), `ErrorModule` (`AppFailureMapper`, `ErrorReporter`), `LanguageModule` (`LanguageRepository`).

**File:** `features/auth/di/AuthModule.kt`

```kotlin
package com.example.dukkanapp.features.auth.di

import com.example.dukkanapp.features.auth.data.repository.AuthRepositoryImpl
import com.example.dukkanapp.features.auth.data.repository.SessionRepositoryImpl
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
```

**File:** `features/user/di/UserModule.kt`

```kotlin
package com.example.dukkanapp.features.user.di

import com.example.dukkanapp.features.user.data.device.AndroidDeviceInfoProvider
import com.example.dukkanapp.features.user.data.device.DeviceInfoProvider
import com.example.dukkanapp.features.user.data.repository.DeviceRepositoryImpl
import com.example.dukkanapp.features.user.data.repository.UserRepositoryImpl
import com.example.dukkanapp.features.user.domain.repository.DeviceRepository
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(impl: AndroidDeviceInfoProvider): DeviceInfoProvider
}
```

**No dispatcher module.** See [0.5](#05-dispatchers-not-needed-yet).

---

## 9. Sign Up (MVI)

### Step 9.1: SignUpUiState

**File:** `features/auth/presentation/logic/signup/SignUpUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = false,
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val confirmPasswordError: AppUiText? = null,
    /** Errors that belong to no single field: no connection, rate limit, unknown. */
    val generalError: AppUiText? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean
        get() = confirmPasswordError == null && confirmPassword.isNotEmpty() && password == confirmPassword
}
```

### Step 9.2: SignUpIntent

**File:** `features/auth/presentation/logic/signup/SignUpIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data class RememberMeChanged(val checked: Boolean) : SignUpIntent

    data object SignUpClicked : SignUpIntent
    data object LoginClicked : SignUpIntent
    data object BackClicked : SignUpIntent
}
```

### Step 9.3: SignUpEffect

**File:** `features/auth/presentation/logic/signup/SignUpEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpEffect {
    /** A new account is never verified, so sign-up always continues to verification. */
    data object NavigateToEmailVerification : SignUpEffect
    data object NavigateToLogin : SignUpEffect
    data object NavigateBack : SignUpEffect
}
```

### Step 9.4: SignUpViewModel

**File:** `features/auth/presentation/logic/signup/SignUpViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.features.auth.domain.usecase.SignUpWithEmailUseCase
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpWithEmail: SignUpWithEmailUseCase,
) : AppViewModel<SignUpUiState, SignUpIntent, SignUpEffect>(
    initialState = SignUpUiState()
) {
    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> setState {
                copy(email = intent.email, emailError = null, generalError = null)
            }

            is SignUpIntent.PasswordChanged -> setState {
                copy(
                    password = intent.password,
                    passwordError = null,
                    confirmPasswordError = liveMismatchError(intent.password, confirmPassword),
                    generalError = null,
                )
            }

            is SignUpIntent.ConfirmPasswordChanged -> setState {
                copy(
                    confirmPassword = intent.confirmPassword,
                    confirmPasswordError = liveMismatchError(password, intent.confirmPassword),
                    generalError = null,
                )
            }

            is SignUpIntent.RememberMeChanged -> setState { copy(rememberMe = intent.checked) }

            SignUpIntent.SignUpClicked -> onSignUpClicked()
            SignUpIntent.LoginClicked -> sendEffect(SignUpEffect.NavigateToLogin)
            SignUpIntent.BackClicked -> sendEffect(SignUpEffect.NavigateBack)
        }
    }

    private fun onSignUpClicked() {
        val current = currentState
        if (current.isLoading) return

        val email = current.email.trim()
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateNewPassword(current.password)
        val confirmPasswordError =
            AuthFormValidator.validateConfirmPassword(current.password, current.confirmPassword)

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            setState {
                copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText(),
                    confirmPasswordError = confirmPasswordError?.toUiText(),
                )
            }
            return
        }

        // Set synchronously, before launch, so a double tap can't start two sign-ups.
        setState { copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            signUpWithEmail(email, current.password, current.rememberMe)
                .onSuccess { sendEffect(SignUpEffect.NavigateToEmailVerification) }
                .onFailure(::showFailure)
            setState { copy(isLoading = false) }
        }
    }

    /** A failure about one field is shown under that field; anything else in the banner. */
    private fun showFailure(failure: AppFailure) {
        val message = failure.toUiText()
        setState {
            when (failure) {
                AppFailure.Auth.InvalidEmail,
                AppFailure.Auth.EmailAlreadyInUse -> copy(emailError = message)

                AppFailure.Auth.WeakPassword -> copy(passwordError = message)
                else -> copy(generalError = message)
            }
        }
    }

    private fun liveMismatchError(password: String, confirmPassword: String): AppUiText? =
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            ValidationError.PasswordsMismatch.toUiText()
        } else {
            null
        }
}
```

### Step 9.5: SignUpScreen

**File:** `features/auth/presentation/screens/SignUpScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.signup.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpEffect
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpViewModel

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SignUpEffect.NavigateToEmailVerification -> onNavigateToEmailVerification()
            SignUpEffect.NavigateToLogin -> onNavigateToLogin()
            SignUpEffect.NavigateBack -> onNavigationBack()
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

### Step 9.6: SignUpScreenContent

Changes: package fixed, general-error banner added after the header.

**File:** `features/auth/presentation/components/signup/SignUpScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.banner.AppMessageBanner
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.checkbox.AppCheckBox
import com.example.dukkanapp.core.common.components.dividers.AppDivider
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpIntent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        onNavigateBack = {
            onIntent(SignUpIntent.BackClicked)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            AppAuthHeader(
                title = stringResource(R.string.signup_title),
                subTitle = stringResource(R.string.signup_subtitle),
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            state.generalError?.let { error ->
                AppMessageBanner(message = error.asString())
                Spacer(Modifier.height(AppDimens.sizeXs))
            }

            AppTextFiled(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                placeHolder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailError?.asString(),
                isSuccess = state.isValidEmail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPasswordTextField(
                value = state.password,
                onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                placeHolder = stringResource(R.string.auth_password_placeholder),
                errorText = state.passwordError?.asString(),
                isSuccess = state.isValidPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPasswordTextField(
                value = state.confirmPassword,
                onValueChange = { onIntent(SignUpIntent.ConfirmPasswordChanged(it)) },
                placeHolder = stringResource(R.string.signup_confirm_password_placeholder),
                errorText = state.confirmPasswordError?.asString(),
                isSuccess = state.isValidConfirmPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(SignUpIntent.SignUpClicked)
                    }
                )
            )
            Spacer(Modifier.height(AppDimens.spaceSm))
            AppCheckBox(
                checked = state.rememberMe,
                onCheckChanged = { onIntent(SignUpIntent.RememberMeChanged(it)) },
                label = stringResource(R.string.login_remember_me),
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppPrimaryButton(
                onClick = { onIntent(SignUpIntent.SignUpClicked) },
                text = stringResource(R.string.signup_button),
                isLoading = state.isLoading
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppDivider()
            Spacer(Modifier.height(AppDimens.size5Xs))
            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = { onIntent(SignUpIntent.LoginClicked) },
            )
        }
    }
}
```

---

## 10. Login (MVI)

### Step 10.1: LoginUiState

**File:** `features/auth/presentation/logic/login/LoginUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    /** "Incorrect email or password", no connection...: not tied to one field. */
    val generalError: AppUiText? = null,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
}
```

### Step 10.2: LoginIntent

**File:** `features/auth/presentation/logic/login/LoginIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data class RememberMeChanged(val checked: Boolean) : LoginIntent
    data object LoginClicked : LoginIntent
    data object ForgotPasswordClicked : LoginIntent
    data object SignUpClicked : LoginIntent
    data object BackClicked : LoginIntent
}
```

### Step 10.3: LoginEffect

**File:** `features/auth/presentation/logic/login/LoginEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    /** Correct password, but the email was never verified. */
    data object NavigateToEmailVerification : LoginEffect
    data object NavigateToSignUp : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
    data object NavigateBack : LoginEffect
}
```

### Step 10.4: LoginViewModel

**File:** `features/auth/presentation/logic/login/LoginViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.auth.domain.usecase.SignInWithEmailUseCase
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
) : AppViewModel<LoginUiState, LoginIntent, LoginEffect>(
    initialState = LoginUiState()
) {
    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> setState {
                copy(email = intent.email, emailError = null, generalError = null)
            }

            is LoginIntent.PasswordChanged -> setState {
                copy(password = intent.password, passwordError = null, generalError = null)
            }

            is LoginIntent.RememberMeChanged -> setState { copy(rememberMe = intent.checked) }
            LoginIntent.LoginClicked -> onLoginClicked()
            LoginIntent.SignUpClicked -> sendEffect(LoginEffect.NavigateToSignUp)
            LoginIntent.ForgotPasswordClicked -> sendEffect(LoginEffect.NavigateToForgotPassword)
            LoginIntent.BackClicked -> sendEffect(LoginEffect.NavigateBack)
        }
    }

    private fun onLoginClicked() {
        val current = currentState
        if (current.isLoading) return

        val email = current.email.trim()
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateExistingPassword(current.password)
        if (emailError != null || passwordError != null) {
            setState {
                copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText()
                )
            }
            return
        }

        // Set synchronously, before launch, so a double tap can't start two logins.
        setState { copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            signInWithEmail(email, current.password, current.rememberMe)
                .onSuccess { user ->
                    sendEffect(
                        if (user.isEmailVerified) LoginEffect.NavigateToHome
                        else LoginEffect.NavigateToEmailVerification
                    )
                }
                .onFailure(::showFailure)
            setState { copy(isLoading = false) }
        }
    }

    /**
     * InvalidCredentials goes to the banner, not to a field: with email enumeration protection
     * Firebase doesn't say whether the email or the password was wrong, and neither should we.
     */
    private fun showFailure(failure: AppFailure) {
        val message = failure.toUiText()
        setState {
            when (failure) {
                AppFailure.Auth.InvalidEmail -> copy(emailError = message)
                else -> copy(generalError = message)
            }
        }
    }
}
```

### Step 10.5: LoginScreen

**File:** `features/auth/presentation/screens/LoginScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.login.LoginScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginEffect
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    onNavigateToForgetPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            LoginEffect.NavigateToHome -> onNavigateToHome()
            LoginEffect.NavigateToEmailVerification -> onNavigateToEmailVerification()
            LoginEffect.NavigateToForgotPassword -> onNavigateToForgetPassword()
            LoginEffect.NavigateToSignUp -> onNavigateToSignUp()
            LoginEffect.NavigateBack -> onNavigateBack()
        }
    }

    LoginScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

### Step 10.6: LoginScreenContent

Changes: imports use the lowercase `login` package, general-error banner added, login button shows `isLoading`.

**File:** `features/auth/presentation/components/login/LoginScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.banner.AppMessageBanner
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.checkbox.AppCheckBox
import com.example.dukkanapp.core.common.components.dividers.AppDivider
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginIntent
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        onNavigateBack = {
            onIntent(LoginIntent.BackClicked)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            AppAuthHeader(
                title = stringResource(R.string.login_title),
                subTitle = stringResource(R.string.login_subtitle),
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            state.generalError?.let { error ->
                AppMessageBanner(message = error.asString())
                Spacer(Modifier.height(AppDimens.sizeXs))
            }

            AppTextFiled(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                placeHolder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailError?.asString(),
                isSuccess = state.isValidEmail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(AppDimens.sizeXs))
            AppPasswordTextField(
                value = state.password,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                placeHolder = stringResource(R.string.auth_password_placeholder),
                errorText = state.passwordError?.asString(),
                isSuccess = state.isValidPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(LoginIntent.LoginClicked)
                    }
                )
            )
            Spacer(Modifier.height(AppDimens.spaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppCheckBox(
                    checked = state.rememberMe,
                    onCheckChanged = { onIntent(LoginIntent.RememberMeChanged(it)) },
                    label = stringResource(R.string.login_remember_me),
                )
                AppTextButton(
                    onClick = { onIntent(LoginIntent.ForgotPasswordClicked) },
                    text = stringResource(R.string.login_forgot_password),
                )
            }
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppPrimaryButton(
                onClick = { onIntent(LoginIntent.LoginClicked) },
                text = stringResource(R.string.action_log_in),
                isLoading = state.isLoading,
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppDivider()
            Spacer(Modifier.height(AppDimens.size5Xs))
            AppAuthFooterLink(
                promptText = stringResource(R.string.login_no_account),
                actionText = stringResource(R.string.login_sign_up),
                onActionClick = { onIntent(LoginIntent.SignUpClicked) }
            )
        }
    }
}
```

---

## 11. Email Verification (MVI)

**How the screen works:**

```
open screen ──► [emailJustSent?] start 60 s resend cooldown
     │
every ON_RESUME (first open, back from mail app) ──► silent reload()
     │                                                   ├─ verified       → Home
     │                                                   ├─ not yet        → nothing (silent)
     │                                                   └─ account gone   → sign out → AuthOptions
"I've verified"  ──► reload() with spinner ──► same, but shows "not verified yet" / errors
"Resend email"   ──► send (in app language) ──► success message + 60 s cooldown
"Open email app" ──► launcher for the default mail app (message if none installed)
"Use a different account" ──► sign out ──► AuthOptions
```

### Step 11.1: EmailVerificationUiState

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import com.example.dukkanapp.core.common.text.AppUiText

data class EmailVerificationUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val isSigningOut: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val message: Message? = null,
) {
    /** Text and tone together, so an error tone can never exist without its text. */
    data class Message(val text: AppUiText, val isError: Boolean)

    val canResend: Boolean get() = !isResending && resendCooldownSeconds == 0
}
```

### Step 11.2: EmailVerificationIntent

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationIntent {
    /** Sent on every ON_RESUME, e.g. when the user comes back from their mail app. */
    data object ScreenResumed : EmailVerificationIntent
    data object CheckClicked : EmailVerificationIntent
    data object ResendClicked : EmailVerificationIntent
    data object OpenEmailAppClicked : EmailVerificationIntent
    /** The screen tried to open a mail app and the device has none. */
    data object EmailAppUnavailable : EmailVerificationIntent
    data object UseAnotherAccountClicked : EmailVerificationIntent
}
```

### Step 11.3: EmailVerificationEffect

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationEffect {
    data object NavigateToHome : EmailVerificationEffect
    data object NavigateToAuthOptions : EmailVerificationEffect
    /** Needs a Context, so the screen does it, not the ViewModel. */
    data object OpenEmailApp : EmailVerificationEffect
}
```

### Step 11.4: EmailVerificationViewModel

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.features.auth.domain.usecase.CheckEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SendEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SignOutUseCase
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationUiState.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUser: GetCurrentUserUseCase,
    private val checkEmailVerification: CheckEmailVerificationUseCase,
    private val sendEmailVerification: SendEmailVerificationUseCase,
    private val signOut: SignOutUseCase,
) : AppViewModel<EmailVerificationUiState, EmailVerificationIntent, EmailVerificationEffect>(
    initialState = EmailVerificationUiState(email = getCurrentUser()?.email.orEmpty())
) {
    private var checkJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        // Sign-up has just sent the email: resending right away would hit Firebase's rate limit.
        if (savedStateHandle.toRoute<AppRoute.EmailVerificationScreen>().emailJustSent) {
            startResendCooldown()
        }
    }

    override fun onIntent(intent: EmailVerificationIntent) {
        when (intent) {
            EmailVerificationIntent.ScreenResumed -> checkVerification(silent = true)
            EmailVerificationIntent.CheckClicked -> checkVerification(silent = false)
            EmailVerificationIntent.ResendClicked -> resend()
            EmailVerificationIntent.OpenEmailAppClicked -> sendEffect(EmailVerificationEffect.OpenEmailApp)
            EmailVerificationIntent.EmailAppUnavailable ->
                showMessage(R.string.email_verification_no_email_app, isError = true)

            EmailVerificationIntent.UseAnotherAccountClicked -> signOutAndLeave()
        }
    }

    /**
     * [silent] = automatic check on resume: only navigates, never shows a message.
     * One check at a time: a resume during a manual check doesn't start a second one.
     */
    private fun checkVerification(silent: Boolean) {
        if (checkJob?.isActive == true) return
        if (!silent) setState { copy(isChecking = true, message = null) }

        checkJob = viewModelScope.launch {
            checkEmailVerification()
                .onSuccess { isVerified ->
                    when {
                        isVerified -> sendEffect(EmailVerificationEffect.NavigateToHome)
                        !silent -> showMessage(R.string.email_verification_not_verified_yet, isError = true)
                    }
                }
                .onFailure { failure -> handleFailure(failure, silent) }
            setState { copy(isChecking = false) }
        }
    }

    private fun resend() {
        if (!currentState.canResend) return
        setState { copy(isResending = true, message = null) }

        viewModelScope.launch {
            sendEmailVerification()
                .onSuccess {
                    showMessage(R.string.email_verification_resent_success, isError = false)
                    startResendCooldown()
                }
                .onFailure { failure -> handleFailure(failure, silent = false) }
            setState { copy(isResending = false) }
        }
    }

    private fun handleFailure(failure: AppFailure, silent: Boolean) {
        when {
            failure.meansAccountIsGone -> signOutAndLeave()
            !silent -> setState { copy(message = Message(failure.toUiText(), isError = true)) }
        }
    }

    private fun signOutAndLeave() {
        if (currentState.isSigningOut) return
        setState { copy(isSigningOut = true) }

        viewModelScope.launch {
            signOut()
            sendEffect(EmailVerificationEffect.NavigateToAuthOptions)
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                setState { copy(resendCooldownSeconds = seconds) }
                delay(ONE_SECOND_MS)
            }
            setState { copy(resendCooldownSeconds = 0) }
        }
    }

    private fun showMessage(@StringRes textRes: Int, isError: Boolean) =
        setState { copy(message = Message(AppUiText.Res(textRes), isError)) }

    /**
     * reload() on a deleted account fails with USER_NOT_FOUND, which the mapper reports as
     * InvalidCredentials. On this screen (no credentials typed) all three mean "account gone":
     * staying here could never succeed.
     */
    private val AppFailure.meansAccountIsGone: Boolean
        get() = this == AppFailure.Auth.SessionExpired ||
                this == AppFailure.Auth.UserDisabled ||
                this == AppFailure.Auth.InvalidCredentials

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MS = 1_000L
    }
}
```

### Step 11.5: EmailVerificationScreen

**File:** `features/auth/presentation/screens/EmailVerificationScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.emailverification.EmailVerificationScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationEffect
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationIntent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationViewModel

@Composable
fun EmailVerificationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAuthOptions: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check whenever the user comes back to the app (e.g. after tapping the link in the mail app).
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(EmailVerificationIntent.ScreenResumed)
        onPauseOrDispose { }
    }

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            EmailVerificationEffect.NavigateToHome -> onNavigateToHome()
            EmailVerificationEffect.NavigateToAuthOptions -> onNavigateToAuthOptions()
            EmailVerificationEffect.OpenEmailApp -> {
                val intent = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_EMAIL
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    viewModel.onIntent(EmailVerificationIntent.EmailAppUnavailable)
                }
            }
        }
    }

    EmailVerificationScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

### Step 11.6: EmailVerificationScreenContent

No back arrow on purpose: the account exists, so "back" to the sign-up form makes no sense. The way out is "Use a different account".

**File:** `features/auth/presentation/components/emailverification/EmailVerificationScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.banner.AppMessageBanner
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationIntent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun EmailVerificationScreenContent(
    state: EmailVerificationUiState,
    onIntent: (EmailVerificationIntent) -> Unit,
) {
    AppScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Iconsax.Linear.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimens.icon3Xl)
            )
            Spacer(Modifier.height(AppDimens.spaceXl))

            Text(
                text = stringResource(R.string.email_verification_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppDimens.spaceSm))

            Text(
                text = stringResource(R.string.email_verification_subtitle, state.email),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            state.message?.let { message ->
                AppMessageBanner(
                    message = message.text.asString(),
                    isError = message.isError,
                )
                Spacer(Modifier.height(AppDimens.sizeXs))
            }

            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.OpenEmailAppClicked) },
                text = stringResource(R.string.email_verification_open_email_app),
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.CheckClicked) },
                text = stringResource(R.string.email_verification_check_status),
                isLoading = state.isChecking,
            )
            Spacer(Modifier.height(AppDimens.size3Xs))

            AppTextButton(
                onClick = { onIntent(EmailVerificationIntent.ResendClicked) },
                enabled = state.canResend,
                text = when {
                    state.isResending -> stringResource(R.string.email_verification_resending)
                    state.resendCooldownSeconds > 0 -> stringResource(
                        R.string.email_verification_resend_in,
                        state.resendCooldownSeconds
                    )

                    else -> stringResource(R.string.email_verification_resend)
                },
            )

            AppTextButton(
                onClick = { onIntent(EmailVerificationIntent.UseAnotherAccountClicked) },
                enabled = !state.isSigningOut,
                text = stringResource(R.string.email_verification_use_another_account),
            )
        }
    }
}
```

---

## 12. Home Placeholder (MVI)

Your `HomeScreen` route is empty today. This minimal screen lets you test Remember Me and sign-out end to end. Replace its content when you build the real Home; keep the sign-out logic (it moves to Profile/Settings later).

### Step 12.1: State, Intent, Effect

**File:** `features/home/presentation/logic/HomeUiState.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.logic

data class HomeUiState(
    val email: String = "",
    val isSigningOut: Boolean = false,
)
```

**File:** `features/home/presentation/logic/HomeIntent.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.logic

sealed interface HomeIntent {
    data object SignOutClicked : HomeIntent
}
```

**File:** `features/home/presentation/logic/HomeEffect.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.logic

sealed interface HomeEffect {
    data object NavigateToAuthOptions : HomeEffect
}
```

### Step 12.2: HomeViewModel

**File:** `features/home/presentation/logic/HomeViewModel.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val signOut: SignOutUseCase,
) : AppViewModel<HomeUiState, HomeIntent, HomeEffect>(
    initialState = HomeUiState(email = getCurrentUser()?.email.orEmpty())
) {
    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.SignOutClicked -> onSignOutClicked()
        }
    }

    private fun onSignOutClicked() {
        if (currentState.isSigningOut) return
        setState { copy(isSigningOut = true) }

        viewModelScope.launch {
            signOut()
            sendEffect(HomeEffect.NavigateToAuthOptions)
        }
    }
}
```

### Step 12.3: HomeScreen and HomeScreenContent

**File:** `features/home/presentation/screens/HomeScreen.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.home.presentation.components.HomeScreenContent
import com.example.dukkanapp.features.home.presentation.logic.HomeEffect
import com.example.dukkanapp.features.home.presentation.logic.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToAuthOptions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            HomeEffect.NavigateToAuthOptions -> onNavigateToAuthOptions()
        }
    }

    HomeScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

**File:** `features/home/presentation/components/HomeScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.home.presentation.logic.HomeIntent
import com.example.dukkanapp.features.home.presentation.logic.HomeUiState

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
) {
    AppScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.home_signed_in_as, state.email),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppDimens.sizeMd))
            AppPrimaryButton(
                onClick = { onIntent(HomeIntent.SignOutClicked) },
                text = stringResource(R.string.action_sign_out),
                isLoading = state.isSigningOut,
            )
        }
    }
}
```

---

## 13. Navigation and App Start

### Step 13.1: AppRoute

**File:** `core/navigation/AppRoute.kt`

```kotlin
package com.example.dukkanapp.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object LanguageSelectionScreen : AppRoute

    @Serializable
    data object OnboardingScreen : AppRoute

    @Serializable
    data object AuthOptionsScreen : AppRoute

    @Serializable
    data object LoginScreen : AppRoute

    @Serializable
    data object SignUpScreen : AppRoute

    /** [emailJustSent]: true right after sign-up, so Resend starts in cooldown (rate limit). */
    @Serializable
    data class EmailVerificationScreen(val emailJustSent: Boolean = false) : AppRoute

    @Serializable
    data object HomeScreen : AppRoute
}
```

### Step 13.2: AppNavHost

**Root navigation:** after sign-in, sign-up, verification and sign-out, the target screen becomes the **only** screen on the back stack (`navigateAsRoot`). Pressing back from Home exits the app instead of returning to Login, and pressing back from AuthOptions after sign-out doesn't return to Home.

**File:** `core/navigation/AppNavHost.kt`

```kotlin
package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.AuthOptionsScreen
import com.example.dukkanapp.features.auth.presentation.screens.EmailVerificationScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.auth.presentation.screens.SignUpScreen
import com.example.dukkanapp.features.home.presentation.screens.HomeScreen
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

/*
   AppNavHost                -> HOW to navigate (popUpTo, launchSingleTop). Knows routes.
   XxxScreen                 -> glue: collects state + effects, maps effects to the lambdas below
   XxxViewModel              -> WHEN and WHERE to navigate (sends an Effect). Never sees NavController.
   XxxScreenContent          -> stateless UI: state + onIntent
 */
@Composable
fun AppNavHost(startDestination: AppRoute) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        //  Language Selection Screen
        composable<AppRoute.LanguageSelectionScreen> {
            LanguageSelectionScreen(
                onNavigationToOnboarding = {
                    navController.navigate(AppRoute.OnboardingScreen) {
                        popUpTo(AppRoute.LanguageSelectionScreen) { inclusive = true }
                    }
                }
            )
        }

        //  Onboarding Screen
        composable<AppRoute.OnboardingScreen> {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                },
                onNavigateToAuthOptions = {
                    navController.navigate(AppRoute.AuthOptionsScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                }
            )
        }

        // Auth options screen
        composable<AppRoute.AuthOptionsScreen> {
            AuthOptionsScreen(
                onEmailClick = {
                    navController.navigate(AppRoute.SignUpScreen)
                },
                onGoogleClick = { /* TODO(social-login): see auth guide section 15 */ },
                onAppleClick = { /* TODO(social-login): see auth guide section 15 */ },
                onAlreadyHaveAccountClick = {
                    navController.navigate(AppRoute.LoginScreen)
                }
            )
        }

        // Login screen
        composable<AppRoute.LoginScreen> {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateAsRoot(AppRoute.HomeScreen) },
                onNavigateToEmailVerification = {
                    navController.navigateAsRoot(AppRoute.EmailVerificationScreen(emailJustSent = false))
                },
                onNavigateToSignUp = {
                    navController.navigate(AppRoute.SignUpScreen) {
                        popUpTo(AppRoute.LoginScreen) { inclusive = true }
                        launchSingleTop = true // no second copy if it's already on top
                    }
                },
                onNavigateToForgetPassword = { /* TODO: ForgotPassword route */ },
            )
        }

        // Sign up screen
        composable<AppRoute.SignUpScreen> {
            SignUpScreen(
                onNavigationBack = { navController.popBackStack() },
                onNavigateToEmailVerification = {
                    navController.navigateAsRoot(AppRoute.EmailVerificationScreen(emailJustSent = true))
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.SignUpScreen) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // Email verification screen
        composable<AppRoute.EmailVerificationScreen> {
            EmailVerificationScreen(
                onNavigateToHome = { navController.navigateAsRoot(AppRoute.HomeScreen) },
                onNavigateToAuthOptions = { navController.navigateAsRoot(AppRoute.AuthOptionsScreen) },
            )
        }

        // Home screen
        composable<AppRoute.HomeScreen> {
            HomeScreen(
                onNavigateToAuthOptions = { navController.navigateAsRoot(AppRoute.AuthOptionsScreen) },
            )
        }
    }
}

/**
 * [route] becomes the only screen on the back stack: "back" from it exits the app
 * instead of returning to a screen that no longer makes sense (Login after sign-in, Home after sign-out).
 */
private fun NavController.navigateAsRoot(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
```

### Step 13.3: AppStartViewModel

`SavedStateHandle` is how we tell a **fresh launch** from a **restore after process death** (see [0.1](#01-remember-me)). The activity-scoped ViewModel's saved state survives process death and is discarded when the user closes the app (back from the root screen, or swipe away from recents).

**File:** `core/navigation/startup/AppStartViewModel.kt`

```kotlin
package com.example.dukkanapp.core.navigation.startup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resolveStartDestinationUseCase: ResolveStartDestinationUseCase
) : ViewModel() {
    private val _startDestination = MutableStateFlow<AppRoute?>(null)
    val startDestination: StateFlow<AppRoute?> = _startDestination.asStateFlow()

    init {
        // Empty on a real launch; restored when Android recreates the app after killing it in the background.
        val isFreshLaunch = savedStateHandle.get<Boolean>(KEY_LAUNCHED) == null
        savedStateHandle[KEY_LAUNCHED] = true

        viewModelScope.launch {
            _startDestination.value = resolveStartDestinationUseCase(isFreshLaunch)
        }
    }

    private companion object {
        const val KEY_LAUNCHED = "launched"
    }
}
```

`MainActivity` needs no change: the splash screen stays until `startDestination` is non-null.

### Step 13.4: ResolveStartDestinationUseCase

The session check runs **after** language and onboarding, not in parallel with them: it may sign the user out, which shouldn't happen for someone who will see onboarding first anyway.

**File:** `core/navigation/startup/ResolveStartDestinationUseCase.kt`

```kotlin
package com.example.dukkanapp.core.navigation.startup

import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.features.auth.domain.model.SessionState
import com.example.dukkanapp.features.auth.domain.usecase.GetSessionStateUseCase
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val getSessionState: GetSessionStateUseCase,
) {
    suspend operator fun invoke(isFreshLaunch: Boolean): AppRoute = coroutineScope {
        // Both are local DataStore reads: run them concurrently to keep the splash short.
        val hasLanguageDeferred = async { languageRepository.hasSelectedLanguage() }
        val hasOnboardingDeferred = async { onboardingRepository.hasSeenOnboarding() }

        when {
            !hasLanguageDeferred.await() -> AppRoute.LanguageSelectionScreen
            !hasOnboardingDeferred.await() -> AppRoute.OnboardingScreen
            else -> when (getSessionState(isFreshLaunch)) {
                SessionState.SignedOut -> AppRoute.AuthOptionsScreen
                SessionState.EmailNotVerified -> AppRoute.EmailVerificationScreen(emailJustSent = false)
                SessionState.SignedIn -> AppRoute.HomeScreen
            }
        }
    }
}
```

---

## 14. String Resources

Error and validation strings already exist (`error_*`, `validation_*`). Add these.

### `values/strings.xml`

```xml
<!-- Email Verification -->
<string name="email_verification_title">Verify your email</string>
<string name="email_verification_subtitle">We\'ve sent a verification link to %1$s. Tap the link, then come back to the app.</string>
<string name="email_verification_open_email_app">Open email app</string>
<string name="email_verification_check_status">I\'ve verified my email</string>
<string name="email_verification_resend">Resend email</string>
<string name="email_verification_resend_in">Resend in %1$ds</string>
<string name="email_verification_resending">Sending…</string>
<string name="email_verification_resent_success">A new verification email is on its way.</string>
<string name="email_verification_not_verified_yet">Your email isn\'t verified yet. Please check your inbox and spam folder.</string>
<string name="email_verification_no_email_app">No email app found on this device.</string>
<string name="email_verification_use_another_account">Use a different account</string>

<!-- Home -->
<string name="home_signed_in_as">Signed in as %1$s</string>
<string name="action_sign_out">Sign out</string>
```

### `values-ar/strings.xml`

```xml
<!-- تأكيد البريد الإلكتروني -->
<string name="email_verification_title">تأكيد بريدك الإلكتروني</string>
<string name="email_verification_subtitle">أرسلنا رابط التأكيد إلى %1$s. اضغط على الرابط ثم عد إلى التطبيق.</string>
<string name="email_verification_open_email_app">فتح تطبيق البريد</string>
<string name="email_verification_check_status">لقد أكدت بريدي الإلكتروني</string>
<string name="email_verification_resend">إعادة إرسال البريد</string>
<string name="email_verification_resend_in">إعادة الإرسال خلال %1$d ث</string>
<string name="email_verification_resending">جارٍ الإرسال…</string>
<string name="email_verification_resent_success">تم إرسال رسالة تأكيد جديدة.</string>
<string name="email_verification_not_verified_yet">لم يتم تأكيد بريدك الإلكتروني بعد. يرجى التحقق من صندوق الوارد ومجلد الرسائل غير المرغوب فيها.</string>
<string name="email_verification_no_email_app">لا يوجد تطبيق بريد على هذا الجهاز.</string>
<string name="email_verification_use_another_account">استخدام حساب آخر</string>

<!-- الرئيسية -->
<string name="home_signed_in_as">تم تسجيل الدخول باسم %1$s</string>
<string name="action_sign_out">تسجيل الخروج</string>
```

### `values-fr/strings.xml`

```xml
<!-- Vérification de l'e-mail -->
<string name="email_verification_title">Vérifiez votre e-mail</string>
<string name="email_verification_subtitle">Nous avons envoyé un lien de vérification à %1$s. Appuyez sur le lien, puis revenez dans l\'application.</string>
<string name="email_verification_open_email_app">Ouvrir l\'application e-mail</string>
<string name="email_verification_check_status">J\'ai vérifié mon e-mail</string>
<string name="email_verification_resend">Renvoyer l\'e-mail</string>
<string name="email_verification_resend_in">Renvoyer dans %1$d s</string>
<string name="email_verification_resending">Envoi en cours…</string>
<string name="email_verification_resent_success">Un nouvel e-mail de vérification est en route.</string>
<string name="email_verification_not_verified_yet">Votre e-mail n\'est pas encore vérifié. Consultez votre boîte de réception et vos spams.</string>
<string name="email_verification_no_email_app">Aucune application e-mail trouvée sur cet appareil.</string>
<string name="email_verification_use_another_account">Utiliser un autre compte</string>

<!-- Accueil -->
<string name="home_signed_in_as">Connecté en tant que %1$s</string>
<string name="action_sign_out">Se déconnecter</string>
```

### Firebase error → where it shows (reference)

| Firebase | `AppFailure` | Shown | String |
|---|---|---|---|
| `ERROR_INVALID_EMAIL` | `Auth.InvalidEmail` | email field | `validation_email_invalid` |
| `ERROR_EMAIL_ALREADY_IN_USE` | `Auth.EmailAlreadyInUse` | email field (sign-up) | `error_email_already_in_use` |
| `ERROR_WEAK_PASSWORD` | `Auth.WeakPassword` | password field (sign-up) | `error_weak_password` |
| `ERROR_INVALID_CREDENTIAL` / `WRONG_PASSWORD` / `USER_NOT_FOUND` | `Auth.InvalidCredentials` | banner (login) | `error_invalid_credentials` |
| `ERROR_USER_DISABLED` | `Auth.UserDisabled` | banner | `error_user_disabled` |
| `FirebaseTooManyRequestsException` | `Network.RateLimited` | banner | `error_too_many_requests` |
| `FirebaseNetworkException` | `Network.NoConnection` | banner | `error_no_connection` |
| `FirebaseAuthEmailException` | `Network.ServiceUnavailable` | banner (verification) | `error_service_unavailable` |

---

## 15. TODO: Google and Apple Sign-In

Not implemented now. The architecture is ready: each provider adds **one** repository method, **one** use case and **one** intent. Sign-up/login code doesn't change (Open/Closed).

### Google (Android Credential Manager)

**Dependencies (later):** `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`.

**Console:** enable **Google** in Authentication → Sign-in method; add your debug and release **SHA-1** in Project settings; download the updated `google-services.json`; copy the **Web client ID**.

**Plan:**

```
AuthOptionsScreen ── GoogleClicked ──► AuthOptionsViewModel ── effect LaunchGoogleSignIn
      │  (screen owns the Activity)
      ▼
CredentialManager.getCredential(GetGoogleIdOption(serverClientId = WEB_CLIENT_ID))
      │  GoogleIdTokenCredential.createFrom(result.credential.data).idToken
      ▼
AuthOptionsIntent.GoogleTokenReceived(idToken) ──► SignInWithGoogleUseCase(idToken)
      │
      ▼
AuthRepository.signInWithGoogle(idToken)
  └─ AuthRemoteDataSource: auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
      │
      ▼
sessionRepository.setRememberMe(true)   // social login: always remembered, there's no checkbox
syncSignedInUser(user)                  // displayName + photoUrl come from Google
      │
      ▼
Home (Google accounts arrive with isEmailVerified = true: no verification screen)
```

Signatures to add then:

```kotlin
// AuthRepository
suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser>

// AuthRemoteDataSource
suspend fun signInWithGoogle(idToken: String): FirebaseUser = guardException {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential).await().user ?: throw AppException.Unexpected()
}
```

**Edge case to handle then:** the email already has a password account → Firebase links automatically when "One account per email address" is on (default) and the Google email is verified. `AppFailure.Auth.AccountExistsWithDifferentCredential` already exists for the other providers.

### Apple

**Console:** enable **Apple**; requires an Apple Developer account (Service ID, key, team ID).

**Plan:** `OAuthProvider.newBuilder("apple.com")` + `auth.startActivityForSignInWithProvider(activity, provider)`. It needs an `Activity`, which must not reach the domain: pass it from the screen through a small `ActivityProvider` in the data layer, or start the flow in the screen and hand the resulting `AuthResult` to the ViewModel. Apple can hide the email ("private relay"), so `email` may be a relay address; the profile sync handles it like any other.

---

## 16. Folder Structure

```
core/
├── common/
│   ├── components/banner/AppMessageBanner.kt            NEW
│   ├── mvi/AppCollectEffect.kt                          FIXED (Main.immediate)
│   └── text/AppUiText.kt                                FIXED (args)
├── di/FirebaseModule.kt                                 NEW
├── navigation/
│   ├── AppRoute.kt                                      UPDATED
│   ├── AppNavHost.kt                                    UPDATED
│   └── startup/
│       ├── AppStartViewModel.kt                         UPDATED (fresh launch)
│       └── ResolveStartDestinationUseCase.kt            UPDATED (session state)
└── utils/
    ├── constants/AppPreferencesKeys.kt                  UPDATED (REMEMBER_ME)
    └── extension/StringExtension.kt                     CLEANED

features/auth/
├── data/
│   ├── datasource/
│   │   ├── AuthRemoteDataSource.kt                      NEW
│   │   └── SessionPreferenceLocalDataSource.kt          NEW
│   ├── mapper/AuthUserMapper.kt                         NEW
│   └── repository/
│       ├── AuthRepositoryImpl.kt                        REWRITTEN
│       └── SessionRepositoryImpl.kt                     NEW
├── di/AuthModule.kt                                     UPDATED
├── domain/
│   ├── model/AuthUser.kt, SessionState.kt               NEW
│   ├── repository/AuthRepository.kt                     REWRITTEN
│   ├── repository/SessionRepository.kt                  NEW
│   ├── usecase/
│   │   ├── SignUpWithEmailUseCase.kt                    NEW
│   │   ├── SignInWithEmailUseCase.kt                    NEW
│   │   ├── SendEmailVerificationUseCase.kt              NEW
│   │   ├── CheckEmailVerificationUseCase.kt             NEW
│   │   ├── GetCurrentUserUseCase.kt                     NEW
│   │   ├── SignOutUseCase.kt                            NEW
│   │   └── GetSessionStateUseCase.kt                    NEW
│   └── validation/AuthFormValidator.kt                  FIXED (no trim)
└── presentation/
    ├── components/
    │   ├── authoptions/...                              (AuthButton: add package)
    │   ├── emailverification/EmailVerificationScreenContent.kt   NEW
    │   ├── login/LoginScreenContent.kt                  UPDATED
    │   └── signup/SignUpScreenContent.kt                UPDATED (package)
    ├── logic/
    │   ├── emailverification/  UiState, Intent, Effect, ViewModel   NEW
    │   ├── login/              UiState, Intent, Effect, ViewModel   UPDATED (package)
    │   └── signup/             UiState, Intent, Effect, ViewModel   FIXED
    └── screens/
        ├── EmailVerificationScreen.kt                   NEW
        ├── LoginScreen.kt                               UPDATED
        └── SignUpScreen.kt                              UPDATED

features/user/                                            NEW FEATURE
├── data/
│   ├── datasource/UserRemoteDataSource.kt, DeviceRemoteDataSource.kt
│   ├── device/DeviceInfo.kt, DeviceInfoProvider.kt, AndroidDeviceInfoProvider.kt
│   ├── mapper/UserMapper.kt, DeviceMapper.kt
│   ├── model/UserDto.kt, DeviceDocument.kt
│   └── repository/UserRepositoryImpl.kt, DeviceRepositoryImpl.kt
├── di/UserModule.kt
└── domain/
    ├── repository/UserRepository.kt, DeviceRepository.kt
    └── usecase/SyncSignedInUserUseCase.kt

features/home/                                            NEW (placeholder)
└── presentation/
    ├── components/HomeScreenContent.kt
    ├── logic/HomeUiState.kt, HomeIntent.kt, HomeEffect.kt, HomeViewModel.kt
    └── screens/HomeScreen.kt
```

---

## 17. Implementation Checklist

Build after each block; it should compile before you start the next.

**1. Fixes**
- [ ] `AppUiText.asString()` passes args
- [ ] `AppCollectEffect` uses `Main.immediate`
- [ ] Rename `logic/Login` → `logic/login` (git mv), fix `components.SignUp` package, add package to `AuthButton.kt`
- [ ] `StringExtension.kt` without `android.util.Patterns`

**2. Setup**
- [ ] `firebase-installations` in TOML + Gradle, `buildConfig = true`
- [ ] `google-services.json` in `app/`
- [ ] Console: Email/Password on, enumeration protection on, email template reviewed
- [ ] Firestore created, rules pasted and published

**3. Core**
- [ ] `REMEMBER_ME` key, `FirebaseModule`, `AppMessageBanner`

**4. Domain**
- [ ] `AuthUser`, `SessionState`
- [ ] `AuthRepository`, `SessionRepository`, `UserRepository`, `DeviceRepository`
- [ ] `AuthFormValidator` (no trim)
- [ ] 7 auth use cases + `SyncSignedInUserUseCase`

**5. Data**
- [ ] Auth: `AuthUserMapper`, `AuthRemoteDataSource`, `SessionPreferenceLocalDataSource`, `AuthRepositoryImpl`, `SessionRepositoryImpl`
- [ ] User: `UserDto`, `DeviceDocument`, `DeviceInfo*`, mappers, data sources, repository impls

**6. DI**
- [ ] `AuthModule` (2 binds), `UserModule` (3 binds)

**7. Presentation**
- [ ] Sign Up (6 files), Login (6 files), Email Verification (6 files), Home (6 files)

**8. Navigation**
- [ ] `AppRoute`, `AppNavHost`, `AppStartViewModel`, `ResolveStartDestinationUseCase`

**9. Strings**
- [ ] EN, AR, FR

**10. Cleanup**
- [ ] Delete `docs/auth/firebase_auth_integration_guide.md` once you're done with it

---

## 18. Manual Test Scenarios

| # | Scenario | Steps | Expected |
|---|---|---|---|
| 1 | Sign up | AuthOptions → Email → valid form, Remember Me ✅ → Create | Verification screen, Resend shows "Resend in 60s". Console: Auth user exists; `users/{uid}` with `createdAt`, `authProviders: ["password"]`, `languageCode`; `devices/{id}` with your phone's name. Email arrives **in the app language**. |
| 2 | Verify by link | Open email app → tap link → return to app | Home **automatically** (resume check) |
| 3 | Manual check | Tap "I've verified" before verifying | "Your email isn't verified yet..." |
| 4 | Resend | Wait for cooldown → Resend | Success message, new 60 s cooldown |
| 5 | Remember Me ✅ | On Home, close app (swipe from recents) → reopen | Home |
| 6 | Remember Me ⬜ | Sign out → Login with box unticked → Home → swipe app away → reopen | AuthOptions; device document deleted |
| 7 | Process death keeps session | Remember Me ⬜ → Home → `adb shell am kill com.example.dukkanapp` while app in background → reopen from recents | Still on Home (restore, not a restart) |
| 8 | Unverified restart | Sign up (Remember Me ✅) → swipe away → reopen | Verification screen |
| 9 | Login unverified | Login with an unverified account | Verification screen, Resend enabled immediately |
| 10 | Wrong password | Login with a wrong password | Banner: "Incorrect email or password" |
| 11 | Email taken | Sign up with an existing email | Error under the **email field** |
| 12 | Short password | 5-char password at sign-up | "Password must be at least 8 characters" (number shown, not `%1$d`) |
| 13 | Offline | Airplane mode → Login | Banner: "No internet connection..." |
| 14 | Self-healing profile | Delete `users/{uid}` in the console → log in again | Document recreated |
| 15 | Account deleted | While on verification screen, delete the user in the Auth console → return to app | Signed out → AuthOptions |
| 16 | Sign out | Home → Sign out | AuthOptions; back press exits app (doesn't return to Home); device document deleted |
| 17 | Rules | Rules Playground: authenticated as user A, write `users/B` | Denied |
| 18 | Rules | Rules Playground: update `users/A` with `createdAt` changed | Denied |
