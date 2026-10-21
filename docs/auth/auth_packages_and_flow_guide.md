# Auth Packages, Instances and Flows

The companion to the [Auth Feature Implementation Guide](auth_feature_implementation_guide.md). That guide is **what to type**. This one is **why it works**:

1. what each dependency does and where we use it,
2. which objects (instances) we create, who owns them and how they reach our classes,
3. how every auth flow runs, call by call, through the layers.

---

## Table of Contents

1. [The Big Picture](#1-the-big-picture)
2. [Packages: What Each Dependency Does](#2-packages-what-each-dependency-does)
3. [Instances: What We Create and Who Owns It](#3-instances-what-we-create-and-who-owns-it)
4. [Example: The FirebaseAuth Instance Up Close](#4-example-the-firebaseauth-instance-up-close)
5. [How One Tap Travels Through the Layers](#5-how-one-tap-travels-through-the-layers)
6. [Flow: Sign Up](#6-flow-sign-up)
7. [Flow: Email Verification](#7-flow-email-verification)
8. [Flow: Login](#8-flow-login)
9. [Flow: App Start and Remember Me](#9-flow-app-start-and-remember-me)
10. [Flow: Sign Out](#10-flow-sign-out)
11. [Threads and Dispatchers](#11-threads-and-dispatchers)
12. [Firestore Writes and Offline Behavior](#12-firestore-writes-and-offline-behavior)
13. [FAQ](#13-faq)

---

## 1. The Big Picture

```
┌──────────────────────────────── presentation ────────────────────────────────┐
│ LoginScreen ──intent──► LoginViewModel ──state──► LoginScreenContent        │
│     ▲                         │                                              │
│     └────────effect───────────┘  (NavigateToHome, NavigateToEmailVerification)│
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │ calls use cases
┌──────────────────────────────── domain (pure Kotlin) ───────────────────────┐
│ SignInWithEmailUseCase ─► AuthRepository     SessionRepository               │
│                        └► SyncSignedInUserUseCase ─► UserRepository          │
│                                                   └► DeviceRepository        │
│                         (interfaces only: no Firebase, no Android)           │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │ implemented by
┌──────────────────────────────── data ────────────────────────────────────────┐
│ AuthRepositoryImpl ─► AuthRemoteDataSource ─────────► FirebaseAuth            │
│ SessionRepositoryImpl ─► SessionPreferenceLocalDS ──► DataStore<Preferences>  │
│ UserRepositoryImpl ─► UserRemoteDataSource ─────────► FirebaseFirestore       │
│ DeviceRepositoryImpl ─► DeviceRemoteDataSource ─────► FirebaseFirestore       │
│                      │                          └───► FirebaseInstallations   │
│                      └► AndroidDeviceInfoProvider ──► Build, Settings         │
└──────────────────────────────────────────────────────────────────────────────┘
                     Hilt creates and connects every box above
```

**Error path, in the opposite direction:**

```
Firebase throws FirebaseAuthException("ERROR_INVALID_CREDENTIAL")
  └─ guardException (data source)      → AppException.Auth.InvalidCredential
     └─ guardFailure (repository)      → AppResult.Failure(AppFailure.Auth.InvalidCredentials)
        └─ ViewModel .onFailure        → state.generalError = AppUiText.Res(R.string.error_invalid_credentials)
           └─ Content                  → AppMessageBanner("Incorrect email or password")
```

No layer above data ever sees a Firebase class. That is what lets you test ViewModels and use cases with fakes, and swap Firebase for another backend by rewriting only `data/`.

---

## 2. Packages: What Each Dependency Does

### 2.1 At a glance

| Dependency | Gradle alias | Status | Used by |
|---|---|---|---|
| Firebase BoM | `libs.firebase.boom` | already added | Gradle (versions only) |
| Firebase Auth | `libs.firebase.auth` | already added | `AuthRemoteDataSource`, `AuthUserMapper`, `AppFirebaseExceptionMapper` |
| Cloud Firestore | `libs.firebase.firestore` | already added | `UserRemoteDataSource`, `DeviceRemoteDataSource`, `UserDto`, `DeviceMapper` |
| Firebase Installations | `libs.firebase.installations` | **new** | `DeviceRemoteDataSource` |
| Coroutines Play Services | `libs.kotlinx.coroutines.play.services` | already added | every `.await()` |
| Google Services plugin | `libs.plugins.google.services` | already added | Gradle (reads `google-services.json`) |
| DataStore Preferences | `libs.androidx.datastore.preferences` | already added | `SessionPreferenceLocalDataSource` |
| Hilt | `libs.hilt.android`, `libs.hilt.compiler`, `libs.hilt.navigation.compose` | already added | every `@Inject`, `@HiltViewModel`, module |
| Navigation Compose + Serialization | `libs.androidx.navigation.compose`, `libs.kotlinx.serialization.json` | already added | `AppRoute`, `AppNavHost`, `toRoute()` |
| Lifecycle Compose | comes with navigation/hilt compose | already available | `collectAsStateWithLifecycle`, `LifecycleResumeEffect` |
| BuildConfig | `buildFeatures.buildConfig = true` | **new flag** | `AndroidDeviceInfoProvider` |

### 2.2 Firebase BoM (`firebase-bom`)

**What:** a "Bill of Materials": a list of Firebase library versions that are tested together. You declare the BoM version once; every Firebase library without a version gets its version from it.

**Why:** mixing `firebase-auth` from one release with `firebase-firestore` from another can crash at runtime (they share internal libraries like `firebase-common`). The BoM rules that out.

```kotlin
implementation(platform(libs.firebase.boom))   // version lives here only
implementation(libs.firebase.auth)             // no version
implementation(libs.firebase.firestore)        // no version
implementation(libs.firebase.installations)    // no version
```

**Since BoM 34**, the `-ktx` artifacts are gone: the Kotlin extensions are inside the main artifacts. Never add `firebase-auth-ktx`.

### 2.3 Google Services plugin + `google-services.json`

**What:** a Gradle plugin that reads `app/google-services.json` at build time and generates Android resources (`google_app_id`, `project_id`, API key...).

**Why:** `FirebaseAuth.getInstance()` needs to know **which Firebase project** to talk to. Firebase initializes itself automatically at app start (through a `ContentProvider` it adds to your manifest) using those generated resources. That's why we never call `FirebaseApp.initializeApp()` ourselves.

**If missing:** build error "File google-services.json is missing", or at runtime `IllegalStateException: Default FirebaseApp is not initialized`.

### 2.4 Firebase Auth (`firebase-auth`)

**What:** the client for Firebase Authentication: creating accounts, signing in, the session, ID tokens, verification emails.

**Where we use it:**

| API | Our code | What it does |
|---|---|---|
| `createUserWithEmailAndPassword` | `AuthRemoteDataSource.signUpWithEmail` | Creates the account **and signs it in** |
| `signInWithEmailAndPassword` | `AuthRemoteDataSource.signInWithEmail` | Signs in, persists the session |
| `currentUser` | `AuthRemoteDataSource.currentUser` | Cached user, no network |
| `setLanguageCode` | `AuthRemoteDataSource.sendEmailVerification` | Language of Firebase's emails |
| `FirebaseUser.sendEmailVerification` | same | Sends the link |
| `FirebaseUser.reload` | `AuthRemoteDataSource.reloadCurrentUser` | Fetches fresh `isEmailVerified` |
| `signOut` | `AuthRemoteDataSource.signOut` | Deletes the local session |
| `FirebaseAuthException.errorCode` | `AppFirebaseExceptionMapper` | Error codes → `AppException` |

Details in [section 4](#4-example-the-firebaseauth-instance-up-close).

### 2.5 Cloud Firestore (`firebase-firestore`)

**What:** a NoSQL document database. Collections contain documents; documents contain fields and can contain subcollections.

**Where we use it:**

| API | Our code | Why this API |
|---|---|---|
| `collection("users").document(uid)` | both remote data sources | Address of a document; no network by itself |
| `runTransaction { get → set/update }` | `UserRemoteDataSource.syncProfile` | Atomic "create if absent": two devices can't both create and overwrite `createdAt` |
| `set(map, SetOptions.merge())` | `DeviceRemoteDataSource.upsertDevice` | Creates or updates **only the given fields**: `fcmToken` survives |
| `delete()` | `DeviceRemoteDataSource.deleteDevice` | Removes the device on sign-out |
| `FieldValue.serverTimestamp()` | update maps | The **server** writes the time: the phone's clock can be wrong |
| `@ServerTimestamp` | `UserDto` | Same, when writing a whole object |
| `FirebaseFirestoreException.code` | `AppFirebaseExceptionMapper` | Error codes → `AppException` |

**Built in:** an offline cache. See [section 12](#12-firestore-writes-and-offline-behavior).

### 2.6 Firebase Installations (`firebase-installations`) — new

**What:** gives each **app install** a Firebase Installation ID (FID), e.g. `"eF3kLp..."`.

**Why we use it** as the `devices/{installationId}` document id:

| Alternative | Problem |
|---|---|
| `Settings.Secure.ANDROID_ID` | Shared by all apps of the same signing key; privacy-sensitive; Play policy discourages it |
| Our own UUID in DataStore | `android:allowBackup="true"` restores DataStore onto a **new phone** → two phones share one id → one overwrites the other's push token |
| **FID** | Per install, **not** backed up, cached after first call, and the same id FCM uses internally |

**Where:** `DeviceRemoteDataSource.installationId()` → `installations.id.await()`.

### 2.7 Coroutines Play Services (`kotlinx-coroutines-play-services`)

**What:** one extension function, `Task<T>.await()`.

**Why:** every Firebase call returns a Google Play `Task<T>`: a callback-based "future". Without this library you write:

```kotlin
auth.signInWithEmailAndPassword(email, password)
    .addOnSuccessListener { result -> /* ... */ }
    .addOnFailureListener { e -> /* ... */ }
```

With it:

```kotlin
val result = auth.signInWithEmailAndPassword(email, password).await()
```

`await()` **suspends** the coroutine (the thread is free meanwhile), resumes with the result on success, and **throws** the task's exception on failure. That throw is what `guardException` catches. It also cancels cleanly: if the ViewModel is cleared, the coroutine stops waiting.

### 2.8 DataStore Preferences

**What:** asynchronous, transactional key-value storage (the replacement for `SharedPreferences`).

**Where:** `SessionPreferenceLocalDataSource` stores `REMEMBER_ME`. The single `DataStore<Preferences>` instance comes from your existing `DataStoreModule` and is shared with the language and onboarding data sources (one file: `app_preferences`).

**Why not in Firestore?** Remember Me is a decision about **this device**. It must be readable before sign-in, offline, in milliseconds, at the splash screen.

### 2.9 Hilt (`hilt-android`, `hilt-compiler`, `hilt-navigation-compose`)

**What:** compile-time dependency injection. You declare what a class needs in its constructor; Hilt generates the code that builds it.

| Annotation | Meaning | Example |
|---|---|---|
| `@HiltAndroidApp` | Root of the object graph | `DukkanApplication` |
| `@AndroidEntryPoint` | Activity can receive injected objects | `MainActivity` |
| `@HiltViewModel` + `@Inject constructor` | ViewModel built by Hilt | `LoginViewModel` |
| `@Inject constructor` | "Hilt, you may build this class" | data sources, use cases |
| `@Binds` | "When someone asks for this interface, give this implementation" | `AuthRepository` → `AuthRepositoryImpl` |
| `@Provides` | "Build it with this function" (for classes you can't annotate) | `FirebaseAuth.getInstance()` |
| `@Singleton` | One instance for the whole app | repositories, Firebase |
| `@ApplicationContext` | The application `Context` (never an Activity: no leaks) | `AndroidDeviceInfoProvider` |

`hiltViewModel()` in a screen creates the ViewModel **scoped to its navigation destination**, and fills its `SavedStateHandle` with the route's arguments (that's how `toRoute()` reads `emailJustSent`).

### 2.10 Navigation Compose + Kotlin Serialization

**What:** type-safe routes. A route is a `@Serializable` object or data class; arguments are its properties.

```kotlin
@Serializable
data class EmailVerificationScreen(val emailJustSent: Boolean = false) : AppRoute

navController.navigate(AppRoute.EmailVerificationScreen(emailJustSent = true))   // send

savedStateHandle.toRoute<AppRoute.EmailVerificationScreen>().emailJustSent        // receive (ViewModel)
```

No string keys, no `Bundle`: a typo is a compile error.

### 2.11 Lifecycle Compose

| API | Where | Why |
|---|---|---|
| `collectAsStateWithLifecycle()` | every screen | Stops collecting state while the app is in the background |
| `LifecycleResumeEffect` | `EmailVerificationScreen` | Runs on **every** `ON_RESUME`: first display and every return from the mail app |
| `repeatOnLifecycle(STARTED)` | `AppCollectEffect` | Effects (navigation) are only handled while visible, never lost, never run in the background |

### 2.12 BuildConfig — new flag

**What:** a class generated at build time with constants from `build.gradle.kts`: `BuildConfig.VERSION_NAME = "1.0"`, `VERSION_CODE = 1`, `DEBUG`...

**Why:** the devices document stores the app version. Reading it from `BuildConfig` is a constant: no `PackageManager` call (an IPC to the system). AGP no longer generates it by default, hence `buildConfig = true`.

### 2.13 Android APIs used without a dependency

| API | Where | Note |
|---|---|---|
| `Build.MANUFACTURER`, `MODEL`, `VERSION.RELEASE`, `VERSION.SDK_INT` | `AndroidDeviceInfoProvider` | Static fields |
| `Settings.Global.DEVICE_NAME` | same | The name the user gave the phone. API 25+, hence the version check |
| `TimeZone.getDefault().id` | same | `"Africa/Tunis"`; for sending pushes at a sensible local time |
| `Intent.makeMainSelectorActivity(ACTION_MAIN, CATEGORY_APP_EMAIL)` | `EmailVerificationScreen` | Opens the default mail app's **inbox**. `ActivityNotFoundException` if none |

---

## 3. Instances: What We Create and Who Owns It

### 3.1 The instance table

| Instance | Created by | Lifetime | Why that lifetime |
|---|---|---|---|
| `FirebaseAuth` | `FirebaseModule.provideFirebaseAuth()` → `FirebaseAuth.getInstance()` | App (`@Singleton`) | The SDK keeps **one** session per app; a second instance would be the same object anyway |
| `FirebaseFirestore` | `FirebaseModule.provideFirebaseFirestore()` | App | Owns the local cache, the network connection and the pending-write queue |
| `FirebaseInstallations` | `FirebaseModule.provideFirebaseInstallations()` | App | Caches the FID |
| `DataStore<Preferences>` | `DataStoreModule.provideDataStore()` | App | **Must** be one instance per file: two instances on the same file corrupt it |
| `AppFailureMapper` / `ErrorReporter` | `ErrorModule` | App | Stateless, shared |
| `AuthRepositoryImpl`, `SessionRepositoryImpl`, `UserRepositoryImpl`, `DeviceRepositoryImpl` | `@Binds @Singleton` | App | Stateless facades over singletons; one is enough |
| `AndroidDeviceInfoProvider` | `@Binds @Singleton` | App | Holds the application `Context` only |
| Data sources (`AuthRemoteDataSource`...) | `@Inject constructor` (unscoped) | New each time it's injected | Stateless and tiny; scoping adds nothing |
| Use cases (`SignInWithEmailUseCase`...) | `@Inject constructor` (unscoped) | New per ViewModel | Stateless; no reason to keep them |
| ViewModels | `hiltViewModel()` | Navigation destination | Survives rotation; cleared when the screen leaves the back stack |
| `AppStartViewModel` | `by viewModels()` in `MainActivity` | Activity | Decides the first screen once |

### 3.2 Why inject instead of calling `getInstance()` everywhere?

```kotlin
// ❌ Hidden dependency: untestable, the class decides where Firebase comes from
class AuthRemoteDataSource {
    private val auth = FirebaseAuth.getInstance()
}

// ✅ Declared dependency: a test passes a mock, the class doesn't care where it comes from
class AuthRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
)
```

This is **Dependency Inversion**: classes state what they need; the composition root (`FirebaseModule`) decides how it's built. It also means `getInstance()` is written exactly once.

### 3.3 The graph Hilt builds for `LoginViewModel`

```
LoginViewModel
└── SignInWithEmailUseCase
    ├── AuthRepository  ──@Binds──►  AuthRepositoryImpl                (singleton)
    │                                 ├── AuthRemoteDataSource
    │                                 │   └── FirebaseAuth               (singleton, @Provides)
    │                                 └── AppFailureMapper ─► DefaultFailureMapper
    │                                                         └── ErrorReporter ─► LogCatErrorReporter
    ├── SessionRepository ──@Binds──► SessionRepositoryImpl             (singleton)
    │                                 └── SessionPreferenceLocalDataSource
    │                                     └── DataStore<Preferences>     (singleton, @Provides)
    └── SyncSignedInUserUseCase
        ├── UserRepository ──@Binds──► UserRepositoryImpl               (singleton)
        │                              └── UserRemoteDataSource
        │                                  └── FirebaseFirestore         (singleton, @Provides)
        ├── DeviceRepository ──@Binds──► DeviceRepositoryImpl           (singleton)
        │                                ├── DeviceRemoteDataSource
        │                                │   ├── FirebaseFirestore       (same instance)
        │                                │   └── FirebaseInstallations   (singleton, @Provides)
        │                                └── DeviceInfoProvider ─► AndroidDeviceInfoProvider
        │                                                          └── @ApplicationContext Context
        └── LanguageRepository ──@Binds──► LanguageRepositoryImpl       (existing)
```

You never write this wiring. If one arrow is missing (forgot a `@Binds`), the build fails with `[Dagger/MissingBinding] AuthRepository cannot be provided...`: a compile error, not a runtime crash.

### 3.4 When do I need a module?

```
Does the class have an @Inject constructor and is it a concrete class?
├── yes → no module (data sources, use cases)
└── no
    ├── It's an interface → @Binds in a module      (AuthRepository, DeviceInfoProvider)
    └── It's a third-party class → @Provides        (FirebaseAuth, DataStore)
```

---

## 4. Example: The FirebaseAuth Instance Up Close

### 4.1 What it holds

```
FirebaseAuth (one per app)
├── currentUser: FirebaseUser?        ← restored from disk at startup, synchronously
│   ├── uid, email, displayName, photoUrl
│   ├── isEmailVerified               ← a CACHED copy; changes only after reload() or sign-in
│   ├── providerData                  ← [firebase, password] / [firebase, google.com]
│   └── ID token (1 h) + refresh token
├── languageCode                      ← language of emails Firebase sends
└── listeners                         ← AuthStateListener / IdTokenListener (not used yet)
```

### 4.2 Lifetime of the session

```
signInWithEmailAndPassword ──► tokens written to app-private storage
         │
         ▼
app killed, phone rebooted ──► still signed in: currentUser restored on next launch
         │
         ▼
every ~1 h ──► ID token refreshed automatically in the background
         │
         ▼
signOut() ──► local tokens deleted; currentUser = null
```

The session ends only on `signOut()`, uninstall / clear data, or when the server revokes it (account deleted, disabled, password changed elsewhere). That's why Remember Me is implemented as **"sign out at launch if not remembered"**: staying signed in is the default.

### 4.3 The calls we make, with their results

| Call | Returns | Typical failure → our `AppFailure` |
|---|---|---|
| `createUserWithEmailAndPassword(e, p).await()` | `AuthResult` → `.user` | `ERROR_EMAIL_ALREADY_IN_USE` → `EmailAlreadyInUse`; `FirebaseAuthWeakPasswordException` → `WeakPassword` |
| `signInWithEmailAndPassword(e, p).await()` | `AuthResult` → `.user` | `ERROR_INVALID_CREDENTIAL` → `InvalidCredentials`; `ERROR_USER_DISABLED` → `UserDisabled` |
| `user.sendEmailVerification().await()` | `Void` | `FirebaseTooManyRequestsException` → `RateLimited` |
| `user.reload().await()` | `Void` (updates `user` in place) | `ERROR_USER_NOT_FOUND` (deleted) → `InvalidCredentials`; `ERROR_USER_TOKEN_EXPIRED` → `SessionExpired` |
| `signOut()` | `Unit`, synchronous | never fails |
| any call, offline | — | `FirebaseNetworkException` → `NoConnection` |

### 4.4 Why `reload()` then `requireUser()` again

```kotlin
suspend fun reloadCurrentUser(): FirebaseUser = guardException {
    requireUser().reload().await()
    requireUser()
}
```

`reload()` returns `Task<Void>`: no user. It refreshes the existing `FirebaseUser` object's fields. Reading `auth.currentUser` again afterwards gives the fresh `isEmailVerified`, and also catches the case where the reload revealed the user is gone (`currentUser` became null → `NoCurrentUser` → `SessionExpired`).

---

## 5. How One Tap Travels Through the Layers

The user taps **Log In** with a correct password and an unverified email.

| # | Layer | Code | What happens |
|---|---|---|---|
| 1 | UI | `LoginScreenContent` → `onIntent(LoginIntent.LoginClicked)` | Content only reports the tap |
| 2 | ViewModel | `LoginViewModel.onLoginClicked()` | Validates email/password with `AuthFormValidator` (pure) |
| 3 | ViewModel | `setState { copy(isLoading = true) }` **before** `launch` | Button shows spinner; a second tap returns early (`if (current.isLoading) return`) |
| 4 | ViewModel | `viewModelScope.launch { signInWithEmail(...) }` | Coroutine on Main; suspends, doesn't block |
| 5 | Use case | `authRepository.signInWithEmail(email, password)` | |
| 6 | Repository | `failureMapper.guardFailure { remote.signInWithEmail(...).toAuthUser() }` | Any exception becomes `AppResult.Failure` |
| 7 | Data source | `guardException { auth.signInWithEmailAndPassword(...).await().user }` | Any SDK exception becomes `AppException` |
| 8 | Firebase | network call on Firebase's threads | Main thread free, UI animates |
| 9 | Repository | `FirebaseUser.toAuthUser()` → `AppResult.Success(AuthUser(isEmailVerified = false))` | Firebase types stop here |
| 10 | Use case | `.onSuccess { sessionRepository.setRememberMe(...); syncSignedInUser(user) }` | DataStore write; profile transaction and device write **in parallel** |
| 11 | ViewModel | `.onSuccess { user -> sendEffect(NavigateToEmailVerification) }` | Effect into the `Channel` |
| 12 | ViewModel | `setState { copy(isLoading = false) }` | |
| 13 | Screen | `AppCollectEffect` → `onNavigateToEmailVerification()` | Only while STARTED |
| 14 | NavHost | `navigateAsRoot(EmailVerificationScreen(emailJustSent = false))` | Verification becomes the only screen |

Same tap, wrong password: step 7 throws `AppException.Auth.InvalidCredential`, step 6 returns `Failure(InvalidCredentials)`, step 10 is skipped (`onSuccess` doesn't run), the ViewModel's `onFailure` sets `generalError`, and the banner shows "Incorrect email or password". Nothing crashed, nothing was caught in the ViewModel.

---

## 6. Flow: Sign Up

```
SignUpViewModel          SignUpWithEmailUseCase         Firebase / DataStore
     │ validate (local)          │                              │
     │ isLoading = true          │                              │
     │──invoke(email,pw,rm)─────►│                              │
     │                           │──createUserWithEmail───────► Auth   (account created + signed in)
     │                           │◄─────────── AuthUser ────────│
     │                           │──setRememberMe(rm)─────────► DataStore
     │                           │  ┌ parallel ────────────────────────────────────────┐
     │                           │  │ syncSignedInUser:                               │
     │                           │  │   ├─ runTransaction: users/{uid} absent → set ─► Firestore
     │                           │  │   └─ set(merge) devices/{fid} ──────────────────► Firestore
     │                           │  │ sendEmailVerification(languageCode) ────────────► Auth (email sent)
     │                           │  └─────────────────────────────────────────────────┘
     │◄────── Success(user) ─────│   (follow-up failures ignored: best effort)
     │ effect NavigateToEmailVerification
     ▼
navigateAsRoot(EmailVerificationScreen(emailJustSent = true))
```

**Why the order:**
- Remember Me is saved first: it's local and instant, and the start-up logic needs it even if everything after fails.
- Profile, device and email are independent, so they run in parallel (`coroutineScope { launch; launch }`). `coroutineScope` waits for both, so the spinner stays until all writes finished and nothing is cancelled by navigation.
- `emailJustSent = true` starts the Resend cooldown: Firebase rate-limits verification emails, and the user just got one.

**What if `createUser` succeeds but the profile write fails?** Sign-up still succeeds. The next sign-in runs the same transaction and creates the document ([guide 0.4](auth_feature_implementation_guide.md#04-profile-sync-is-best-effort)).

---

## 7. Flow: Email Verification

```
            ┌──────────── EmailVerificationScreen ─────────────┐
            │                                                   │
 ON_RESUME ─┤ ScreenResumed → checkVerification(silent = true)  │
            │   reload() ──► verified?    yes → NavigateToHome  │
            │                             no  → (nothing)       │
            │                   account gone  → sign out        │
            │                                   → AuthOptions   │
            │                                                   │
 tap check ─┤ CheckClicked → checkVerification(silent = false)  │
            │   same, plus "not verified yet" / error banner    │
            │                                                   │
 tap resend ┤ ResendClicked → canResend? → send → 60 s cooldown │
            │                                                   │
 tap open  ─┤ OpenEmailAppClicked → effect OpenEmailApp         │
            │   screen: startActivity(mail app inbox)           │
            │   no mail app → EmailAppUnavailable → banner      │
            └───────────────────────────────────────────────────┘
```

**The real-world sequence:**

1. User taps **Open email app** → our app goes to the background (`ON_PAUSE`).
2. User taps the link → the browser opens Firebase's page "Your email has been verified". **Our app is not notified**: the link is handled by Firebase's servers.
3. User switches back to Dukkan → `ON_RESUME` → `LifecycleResumeEffect` → silent `reload()` → `isEmailVerified = true` → Home.

**Guards:**
- `checkJob?.isActive == true` → return: a resume during a manual check doesn't run two reloads, and can't navigate twice.
- Silent checks never show messages: the user didn't ask, so "not verified yet" on every resume would be noise.
- `meansAccountIsGone`: if the account was deleted or disabled, the screen could never succeed; we sign out and leave.

---

## 8. Flow: Login

```
LoginViewModel ──► SignInWithEmailUseCase
                     ├─ signInWithEmail ──► Auth
                     ├─ setRememberMe(rm) ──► DataStore
                     └─ syncSignedInUser ──► Firestore (users + device, parallel)
                ◄── Success(AuthUser)
     user.isEmailVerified ?
       ├── true  → NavigateToHome                         → navigateAsRoot(Home)
       └── false → NavigateToEmailVerification            → navigateAsRoot(EmailVerification(false))
```

- The routing decision is in the **ViewModel** (it knows *where*), the back-stack handling in the **NavHost** (it knows *how*).
- `emailJustSent = false`: an unverified user logging in may need a fresh link (old ones expire), so Resend is enabled immediately.
- Sync runs on every login: updates `lastLoginAt`, `languageCode` (the user may have switched language), re-registers this device, and heals a missing profile.

---

## 9. Flow: App Start and Remember Me

### 9.1 The decision

```
MainActivity.onCreate
  ├─ installSplashScreen; keep it while startDestination == null
  └─ AppStartViewModel.init
       ├─ isFreshLaunch = savedStateHandle["launched"] == null ; then set it
       └─ ResolveStartDestinationUseCase(isFreshLaunch)
            ├─ language chosen?      (DataStore) ─ no → LanguageSelection
            ├─ onboarding seen?      (DataStore) ─ no → Onboarding
            └─ GetSessionStateUseCase(isFreshLaunch)
                 ├─ currentUser == null                       → SignedOut        → AuthOptions
                 ├─ isFreshLaunch && !rememberMe → SignOutUseCase → SignedOut    → AuthOptions
                 ├─ !isEmailVerified                          → EmailNotVerified → EmailVerification(false)
                 └─ else                                      → SignedIn         → Home
```

Everything above reads **local** data (DataStore + Firebase's cached user): no network, so the splash is short and the app starts offline. The one exception: a Remember-Me sign-out deletes the device document, bounded at 3 s.

### 9.2 Fresh launch vs restore

| What the user did | `savedInstanceState` | `isFreshLaunch` | Remember Me ⬜ result |
|---|---|---|---|
| Tapped the icon, app wasn't running | none | `true` | Signed out → AuthOptions |
| Swiped the app away in recents, reopened | none | `true` | Signed out → AuthOptions |
| Pressed back on the root screen, reopened | none | `true` | Signed out → AuthOptions |
| Rotated the phone | ViewModel survives; `init` doesn't run again | — | Nothing changes |
| Went to the mail app; Android killed Dukkan; came back | restored | `false` | **Session kept**, back on the screen they left |
| Phone rebooted, tapped the icon | none | `true` | Signed out → AuthOptions |

Without the `isFreshLaunch` check, row 5 would sign the user out in the middle of email verification, which is exactly when Android is most likely to kill the app.

### 9.3 Why not just call `signOut()` in `onDestroy` when Remember Me is off?

`onDestroy` isn't guaranteed to run (Android kills processes without callbacks), and it also runs on rotation. "Check at the next launch" is the only reliable moment.

---

## 10. Flow: Sign Out

```
HomeViewModel / EmailVerificationViewModel / GetSessionStateUseCase
  └─ SignOutUseCase
       ├─ currentUser? ─► withTimeoutOrNull(3 s) { deviceRepository.unregisterCurrentDevice(uid) }
       │                    └─ delete users/{uid}/devices/{fid}     ← needs auth: BEFORE signOut
       ├─ authRepository.signOut()                                   ← local tokens deleted
       └─ sessionRepository.clear()                                  ← REMEMBER_ME removed
  ► effect NavigateToAuthOptions ► navigateAsRoot(AuthOptions)       ← back press exits app
```

**Why this order:** Firestore rules check `request.auth.uid == userId`. After `signOut()` there is no auth, so the delete would be denied. The device must go first.

**Why the timeout:** offline, `delete().await()` waits for the server forever, and the user would stare at a spinner. After 3 s we sign out anyway. A device document left behind is harmless: when the server sends a push to its token and FCM answers "UNREGISTERED" (or the token belongs to another account now), the server deletes it.

---

## 11. Threads and Dispatchers

### 11.1 Where each piece of work runs

| Code | Thread | Blocking? |
|---|---|---|
| `onIntent`, `setState`, `sendEffect` | Main | No (in-memory) |
| `viewModelScope.launch { }` | `Dispatchers.Main.immediate` | — |
| Firebase `Task` work | Firebase's internal thread pool | Not on our threads |
| `.await()` | suspends the Main coroutine | **No**: Main is free while waiting |
| DataStore `data.first()` / `edit {}` | DataStore's internal IO dispatcher | No |
| `AndroidDeviceInfoProvider.current()` | Main | Microseconds |
| `AuthFormValidator` | Main | Microseconds |

### 11.2 So we don't inject dispatchers

The rule "do IO off the main thread" is about **blocking** calls. Every call in this feature is either asynchronous (Firebase, DataStore) or trivial. Wrapping them in `withContext(Dispatchers.IO)` would add two thread switches and protect against nothing.

### 11.3 When you *would* add one

When you first write a **blocking** call: reading/writing a `File`, `Bitmap.compress`, parsing a large JSON by hand, `PackageManager` queries in a loop, Room `@RawQuery` without `suspend`. Then:

```kotlin
// core/di/DispatchersModule.kt — NOT needed now; for when the first blocking call appears.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

// Usage, in the data source that blocks:
class ImageCompressor @Inject constructor(
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun compress(bitmap: Bitmap): ByteArray = withContext(io) { /* blocking work */ }
}
```

Injected (not `Dispatchers.IO` directly) so tests can pass a `StandardTestDispatcher` and control time.

### 11.4 The one dispatcher we did fix

`AppCollectEffect` now uses `Dispatchers.Main.immediate`. With plain `Main`, an effect emitted while already on the main thread is **re-posted** to the next frame. If the screen leaves composition in between, a navigation effect can be dropped. `immediate` runs it right away.

---

## 12. Firestore Writes and Offline Behavior

Firestore keeps a local cache and a queue of pending writes. What `await()` waits for depends on the call:

| Call | Online | Offline |
|---|---|---|
| `runTransaction { }` (profile sync) | Reads + writes on the server, retries on contention | **Fails fast** with `UNAVAILABLE` → `AppException.Firestore.Unavailable` → ignored (best effort), retried next sign-in |
| `set(merge)` (device) | Resumes when the server confirms | Applied to cache immediately, queued; `await()` resumes **only when back online** |
| `delete()` (device) | Resumes when the server confirms | Same: queued, `await()` waits → that's why sign-out has a timeout |

**Sign-in and sign-up need the network anyway** (Firebase Auth is online-only), so the device write in `SyncSignedInUserUseCase` normally completes in a few hundred ms. If the connection drops in between, the spinner waits until it's back; the write is never lost.

**Why server timestamps:** `createdAt`, `updatedAt`, `lastLoginAt` use `@ServerTimestamp` / `FieldValue.serverTimestamp()`. The phone's clock can be wrong or deliberately changed; the server's can't. The security rule `createdAt == request.time` enforces it.

---

## 13. FAQ

**Q: Firebase already keeps me signed in. Why do we need a `SessionRepository` at all?**
To implement the *opposite*: forgetting the session when Remember Me is off. Firebase has no such option on Android.

**Q: Why isn't `isEmailVerified` stored in `users/{uid}`?**
It would be a copy of a value Firebase Auth owns. The copy goes stale the moment the user taps the link (Firestore doesn't know). Servers that need it read it from the ID token (`request.auth.token.email_verified` in rules) or the Admin SDK.

**Q: Can I read the user's profile now?**
Not needed yet, so `UserRepository` has only `syncProfile`. When a Profile screen needs it, add `observeProfile(uid): Flow<AppResult<UserProfile>>` using `snapshots()` + your existing `Flow.guardException()` / `Flow.guardFailure()` helpers.

**Q: Where does the FCM token come from later?**
The push feature adds `firebase-messaging` and a `FirebaseMessagingService`. In `onNewToken(token)`, it calls `DeviceRepository.updatePushToken(uid, token)` (the TODO in the interface), which does `update(DeviceDocument.FCM_TOKEN, token)` on the same document. Because device registration uses `merge`, signing in again never erases it.

**Q: Why is `SyncSignedInUserUseCase` in `features/user` and not in `features/auth`?**
It writes user data, which belongs to the user feature. Auth use cases call it through its **domain** API, which the project rules allow (a feature may import another feature's domain, never its data or presentation).

**Q: Why does `SignUpWithEmailUseCase` return success when the verification email failed to send?**
The account exists; the user can resend from the next screen. A failure there would show "Something went wrong" while the account was in fact created, and retrying would say "email already registered".

**Q: What happens if the user signs up on two phones with the same email at the same time?**
Firebase Auth allows only one `createUser` per email; the second gets `EmailAlreadyInUse`. Both phones can then sign in; the transaction guarantees one `users/{uid}` creation, and each phone gets its own `devices/{fid}`.

**Q: Is `deviceName` personal data?**
It can be ("Sam's phone"). It is readable only by the account owner (rules), used for a "your devices" list and security emails. Mention it in your privacy policy along with the other device fields.
