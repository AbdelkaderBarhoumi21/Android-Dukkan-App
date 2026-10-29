# 01: Project Setup (Console, Gradle, CLI, Emulators)

**Changes:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, new root folder `firebase/` (rules, indexes, functions).
**Already done by the auth feature:** Firebase BoM `34.19.0`, `firebase-auth`, `firebase-firestore`, `firebase-installations`, the `google-services` plugin, `google-services.json`.

---

## Why

- **One project, two Android apps.** The customer app and the team app share orders, so they share one Firestore database.
- **One region for everything.** Firestore, Functions and Storage in the same region keep latency low and avoid cross-region egress costs. Tunisia is closest to **`europe-west1` (Belgium)**. A Firestore location **cannot be changed later**, so choose carefully.
- **Rules, indexes and functions live in git.** You deploy them with the CLI, never by pasting in the console. That keeps them reviewed and reproducible.
- **Emulators first.** You can test rules, functions and triggers on your laptop for free, without touching real data.

---

## 1. Console steps

| Step | Where | What |
|---|---|---|
| 1 | Project settings → General | Project already exists (the auth feature uses it). Set the **public-facing name** to "Dukkan" (shown in emails) |
| 2 | Project settings → Your apps | Customer app `com.example.dukkanapp` is registered. Add the **SHA-1 and SHA-256** of your debug and release keys (needed for Google sign-in and Play Integrity). Get them with `./gradlew signingReport` |
| 3 | Same | **Later:** register the team app (e.g. `com.example.dukkanteam`). It gets its own `google-services.json` |
| 4 | Authentication → Sign-in method | Enable **Email/Password** (done), **Google**, **Facebook** (needs a Meta app id + secret; see open question in 00) |
| 5 | Firestore → Create database | **Production mode**, location **`europe-west1`** |
| 6 | Storage → Get started | Same location. Requires the **Blaze** plan for new buckets |
| 7 | Upgrade to **Blaze** (pay as you go) | Required by Cloud Functions and Storage. Set a **budget alert** (e.g. 10 USD) in Google Cloud Billing |
| 8 | App Check → Apps | Register the Android app with **Play Integrity**. Start in **monitor mode**, enforce once your metrics show real traffic is verified |
| 9 | Cloud Messaging | Nothing to enable. FCM v1 API is on by default for new projects |

---

## 2. Gradle

### 2.1 `gradle/libs.versions.toml`

Add to `[libraries]` under `# Firebase` (the BoM picks the versions):

```toml
firebase-storage = { group = "com.google.firebase", name = "firebase-storage" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
firebase-functions = { group = "com.google.firebase", name = "firebase-functions" }
firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity" }
firebase-appcheck-debug = { group = "com.google.firebase", name = "firebase-appcheck-debug" }
```

Optional (add when you need them):

```toml
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }
firebase-config = { group = "com.google.firebase", name = "firebase-config" }
```

Crashlytics also needs its Gradle plugin (`com.google.firebase.crashlytics`). Add it only when you enable Crashlytics.

### 2.2 `app/build.gradle.kts`

```kotlin
    // Firebase
    implementation(platform(libs.firebase.boom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.installations)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
```

**Why `debugImplementation` for the debug provider:** the debug App Check provider prints a token you register in the console, so emulators and your phone can call protected APIs during development. It must never ship in a release build.

### 2.3 App Check install (in `DukkanApplication.onCreate`)

```kotlin
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

override fun onCreate() {
    super.onCreate()
    Firebase.initialize(this)
    Firebase.appCheck.installAppCheckProviderFactory(
        if (BuildConfig.DEBUG) {
            com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
    )
}
```

> The `debug` class only exists in debug builds. If the release build fails to compile, move this `if` into two source sets (`src/debug/.../AppCheckInstaller.kt` and `src/release/.../AppCheckInstaller.kt`) that expose the same function. That is the clean fix, so do it as soon as you add a release build.

---

## 3. Firebase CLI and folder layout

Install once: `npm install -g firebase-tools`, then `firebase login`.

From the repo root:

```bash
firebase init firestore functions storage emulators
```

Choices: TypeScript, ESLint yes, install dependencies yes. Emulators: Auth, Functions, Firestore, Storage, Pub/Sub (for scheduled functions).

Resulting layout (commit all of it except `node_modules` and `lib`):

```
Android-Dukkan-App/
├── app/                         # customer Android app
├── firebase/                    # ← move generated files here to keep the root clean
│   ├── firebase.json
│   ├── .firebaserc              # project alias: default → your project id
│   ├── firestore.rules          # 07_security_rules.md
│   ├── firestore.indexes.json   # 08_queries_and_indexes.md
│   ├── storage.rules            # 11_storage.md
│   ├── functions/
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── src/                 # 09_cloud_functions.md
│   └── tests/                   # rules unit tests (07)
└── docs/firebase/               # these docs
```

### `firebase/firebase.json`

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "storage": {
    "rules": "storage.rules"
  },
  "functions": [
    {
      "source": "functions",
      "codebase": "default",
      "predeploy": ["npm --prefix \"$RESOURCE_DIR\" run build"]
    }
  ],
  "emulators": {
    "auth": { "port": 9099 },
    "functions": { "port": 5001 },
    "firestore": { "port": 8080 },
    "storage": { "port": 9199 },
    "pubsub": { "port": 8085 },
    "ui": { "enabled": true, "port": 4000 },
    "singleProjectMode": true
  }
}
```

### Commands you will use

| Command | When |
|---|---|
| `firebase emulators:start --import=./seed --export-on-exit` | Daily development. Keeps your test data between runs |
| `firebase deploy --only firestore:rules` | After editing rules |
| `firebase deploy --only firestore:indexes` | After editing indexes |
| `firebase deploy --only functions` | After editing functions |
| `firebase deploy --only storage` | After editing storage rules |

---

## 4. Point the Android app at the emulators (debug only)

```kotlin
// core/di/FirebaseModule.kt, inside the providers, debug builds only
private const val EMULATOR_HOST = "10.0.2.2" // the host machine as seen from the Android emulator

if (BuildConfig.USE_FIREBASE_EMULATORS) {
    auth.useEmulator(EMULATOR_HOST, 9099)
    firestore.useEmulator(EMULATOR_HOST, 8080)
    functions.useEmulator(EMULATOR_HOST, 5001)
    storage.useEmulator(EMULATOR_HOST, 9199)
}
```

Add the flag in `defaultConfig` / build types:

```kotlin
buildTypes {
    debug { buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false") } // flip to true when you test locally
    release { buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false") }
}
```

**Why a flag and not "always in debug":** you will often want to test a debug build against the real project (push notifications do not go through emulators).

`useEmulator` must be called **before the first use** of each instance, so do it inside the `@Provides` function. See [13_android_integration.md](13_android_integration.md).

On a physical phone, use your computer's LAN IP instead of `10.0.2.2`, and allow cleartext traffic for that IP in a **debug-only** network security config.

---

## 5. Notes

- **Firestore offline cache** is enabled by default on Android. Nothing to configure. Figma 12.4 relies on it.
- **Do not** create collections by hand in the console. Seed them with a script (`firebase/seed/` + the Admin SDK) so the emulator and production have the same shape.
- **API key in `google-services.json` is not a secret.** Security comes from rules + App Check, not from hiding the key.

---

## Check

- [ ] SHA-1 and SHA-256 added for debug and release.
- [ ] Google and Facebook providers enabled.
- [ ] Firestore and Storage created in `europe-west1`.
- [ ] Blaze plan + budget alert.
- [ ] App Check registered (monitor mode).
- [ ] New Gradle dependencies sync.
- [ ] `firebase emulators:start` shows the UI at `http://localhost:4000`.
- [ ] Debug build with `USE_FIREBASE_EMULATORS=true` signs in against the Auth emulator.
