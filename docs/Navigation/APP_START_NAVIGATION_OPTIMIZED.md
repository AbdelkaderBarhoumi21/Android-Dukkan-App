# App Startup Navigation — Explanation & Optimization Plan

No full files here on purpose — explanations, signatures, and what changes from the uploaded doc, so you write it yourself.

---

## 1. Root cause of your actual bug

Your current `AppNavHost` (from the routing conversation) does this:
```kotlin
NavHost(navController = navController, startDestination = AppRoute.LanguageSelection) { ... }
```
`startDestination` is a **hardcoded literal** — nothing reads DataStore before the `NavHost` is built. So on every cold start, regardless of what the user did last time, the app always opens on `LanguageSelection`. That's the entire bug, in one sentence: **the app never checks "have I been here before?" before deciding where to open.**

---

## 2. The core idea to keep from the uploaded doc — simple version

Instead of saving *which screen* the user is on (a stepper `0/1/2/3` that silently goes stale — e.g. you'd have to remember to reset it on logout), save **independent yes/no facts**:

```
hasSelectedLanguage : Boolean   (persists forever — a language choice doesn't "undo")
hasSeenOnboarding   : Boolean   (persists forever — same reasoning)
isLoggedIn          : Boolean   (NOT persisted by you — derived from whether a valid auth token exists)
```

Then at launch, one `if/else` chain picks the first thing not yet done:
```kotlin
when {
    !hasSelectedLanguage -> AppRoute.LanguageSelection
    !hasSeenOnboarding   -> AppRoute.Onboarding
    !isLoggedIn           -> AppRoute.Login
    else                   -> AppRoute.Home
}
```
Log out → `isLoggedIn` becomes `false` on its own (it was never a stored flag you'd forget to flip) → user lands on `Login` next launch, automatically, no manual "reset the stepper" step anywhere. That's genuinely the right pattern — keep it.

---

## 3. What changes from the uploaded doc, and why

### 3.1 Don't create `LanguagePreferences` — reuse `LanguageRepository`

The uploaded doc adds a **brand new** DataStore-backed class:
```kotlin
class LanguagePreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val hasSelectedLanguage: Flow<Boolean> = dataStore.data.map { it[SELECTED_LANGUAGE] != null }
    ...
}
```
But you already have `LanguageRepository` (domain interface) + `LanguageRepositoryImpl` from the Clean Architecture conversation, backed by the **same** `DataStore<Preferences>`. Two classes both reading/writing language state is exactly the duplication problem the Clean Architecture doc warned about — if one saves under a different key than the other reads, they silently desync.

**Fix**: add one method to the *existing* `LanguageRepository` interface instead of creating a new class:
```kotlin
interface LanguageRepository {
    fun getSupportedLanguages(): List<Language>
    val selectedLanguageCode: Flow<String>
    suspend fun selectLanguage(code: String)
    suspend fun hasSelectedLanguage(): Boolean   // ← new — "has the user ever explicitly chosen one"
}
```
Implemented in `LanguageRepositoryImpl` by checking the *local datasource's* raw saved value is non-null (not `selectedLanguageCode`, which already falls back to `"en"` via your `?:` chain — that fallback would make `hasSelectedLanguage()` always look "true," defeating the whole point):
```kotlin
override suspend fun hasSelectedLanguage(): Boolean =
    localDataSource.savedLanguageCode.first() != null
```

### 3.2 Keep type-safe `AppRoute` — don't reintroduce string routes

The uploaded doc's `StartDestination` enum:
```kotlin
enum class StartDestination(val route: String) {
    LANGUAGE_SELECTION("language_selection"), ...
}
```
...and `composable(StartDestination.LANGUAGE_SELECTION.route)` — that's the **old**, pre-type-safety pattern, and it creates a second, parallel "list of screens" alongside the `AppRoute` sealed interface you already built with `@Serializable`. Two systems describing the same set of screens is a maintenance trap (add a screen, forget to update one of the two lists).

**Fix**: `ResolveStartDestinationUseCase` returns `AppRoute` directly — no separate enum needed:
```kotlin
class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppRoute = when {
        !languageRepository.hasSelectedLanguage()   -> AppRoute.LanguageSelection
        !onboardingRepository.hasSeenOnboarding()    -> AppRoute.Onboarding
        !authRepository.isLoggedInOnce()             -> AppRoute.Login   // add to AppRoute if not already there
        else                                          -> AppRoute.Home
    }
}
```
And `AppNavHost` uses `composable<AppRoute.X>` throughout, exactly as already built — nothing changes there except `startDestination` now comes from this use case instead of a literal.

### 3.3 Where does `ResolveStartDestinationUseCase` actually live? — a real architecture catch

This is the one worth thinking through carefully, not glossing over. The use case needs `LanguageRepository` (lives in `feature:language`) **and** `OnboardingRepository` (lives in `feature:onboarding`) **and** `AuthRepository` (presumably `feature:auth` eventually). Per the Clean Architecture rule already established — *"feature modules never depend on each other; only `app` is allowed to know about every feature"* — putting this use case in `core` would force `core` to depend on 3 feature modules, which **inverts** the dependency direction (features depend on core, never the reverse).

**Fix**: this use case doesn't belong in any feature's `domain/`, and it doesn't belong in `core` either — it's genuinely **app-level orchestration**, so it lives in the `app` module itself:
```
app/
└── startup/
    ├── ResolveStartDestinationUseCase.kt
    └── AppStartViewModel.kt
```
`app` is the one place already allowed to import every feature module (it does so today just to wire `AppNavHost`), so this is the correct, and only architecturally consistent, home for it.

### 3.4 `OnboardingRepository` — new, small, mirrors `LanguageRepository`'s shape

```kotlin
// feature/onboarding/domain/repository/OnboardingRepository.kt
interface OnboardingRepository {
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun setOnboardingCompleted()
}
```
Backed by a `DataStore<Preferences>` boolean key, same pattern as `LanguagePreferenceLocalDataSource` from the localization work — same shape, different feature, nothing new to learn here.

### 3.5 Avoid the blank-screen flash — use the real Splash Screen API instead of a manual null check

The uploaded doc's `AppNavHost`:
```kotlin
if (startDestination == null) {
    // SplashScreen()
    return
}
```
This renders **nothing** (a blank `Composable` returning early) while `ResolveStartDestinationUseCase` runs — on a slow DataStore read, that's a visible flash of blank white/black before content appears, which looks like a bug even though it isn't one.

**Fix**: Android has a dedicated Splash Screen API (`androidx.core:core-splashscreen`) built exactly for this — it keeps the *system's* splash screen on screen until your condition is satisfied, instead of your Compose content rendering an empty frame:
```kotlin
// build.gradle.kts (app)
implementation("androidx.core:core-splashscreen:1.0.1")
```
```kotlin
// MainActivity.onCreate(), BEFORE setContent { }
val splashScreen = installSplashScreen()
splashScreen.setKeepOnScreenCondition { appStartViewModel.startDestination.value == null }
```
Now there's no blank Compose frame at all — the OS's own splash (your launcher icon, typically) stays visible until `startDestination` resolves, then your `NavHost` appears already on the correct screen. `AppNavHost` itself no longer needs the `if (startDestination == null) return` branch — by the time `setContent { }` even composes `AppNavHost`, the splash condition has already been satisfied.

### 3.6 Small duplication in `OnboardingScreen`'s `onEvent` — worth cleaning while you're in this file

The uploaded doc has two separate `when` branches with **identical bodies**:
```kotlin
OnboardingEvent.OnGetStartedClicked -> {
    viewModel.onEvent(event)
    navController.navigate(...) { popUpTo(...) { inclusive = true } }
}
OnboardingEvent.OnLoginClicked -> {
    viewModel.onEvent(event)                                    // ← same code
    navController.navigate(...) { popUpTo(...) { inclusive = true } }  // ← same code
}
```
Note the `OnboardingViewModel` right above it in the same doc already does this correctly — combine on one line:
```kotlin
OnboardingEvent.OnGetStartedClicked,
OnboardingEvent.OnLoginClicked -> {
    viewModel.onEvent(event)
    navController.navigate(AppRoute.Login) { popUpTo(AppRoute.Onboarding) { inclusive = true } }
}
```
One branch, same behavior, no copy-pasted body to keep in sync if the navigation logic ever changes.

### 3.7 Optional, minor — parallelize the 3 startup reads

`ResolveStartDestinationUseCase` currently reads 3 independent sources **sequentially** (`await`s each one before starting the next). They don't depend on each other, so they could run concurrently:
```kotlin
suspend operator fun invoke(): AppRoute = coroutineScope {
    val hasLanguageDeferred = async { languageRepository.hasSelectedLanguage() }
    val hasOnboardingDeferred = async { onboardingRepository.hasSeenOnboarding() }
    val isLoggedInDeferred = async { authRepository.isLoggedInOnce() }

    when {
        !hasLanguageDeferred.await()   -> AppRoute.LanguageSelection
        !hasOnboardingDeferred.await()  -> AppRoute.Onboarding
        !isLoggedInDeferred.await()     -> AppRoute.Login
        else                             -> AppRoute.Home
    }
}
```
This is a genuine "nice to have, not urgent" — DataStore reads are typically fast, so the sequential version won't be noticeably slower in practice. Worth doing only if you're already touching this file; not worth a special trip back for.

---

## 4. Final flow, restated simply

```
Cold start
   ↓
installSplashScreen() keeps system splash up
   ↓
AppStartViewModel.init{} → ResolveStartDestinationUseCase()
   ↓
reads: LanguageRepository.hasSelectedLanguage(), OnboardingRepository.hasSeenOnboarding(), AuthRepository.isLoggedInOnce()
   ↓
returns ONE AppRoute value
   ↓
splash condition satisfied → NavHost composes, startDestination = that AppRoute
   ↓
user sees the CORRECT screen immediately — no flash, no wrong default
```

**Your original bug, traced against this fix**: you select a language → `LanguageRepository.selectLanguage()` saves it (already correct, already built) → you finish onboarding → currently, nothing saves `hasSeenOnboarding` at all, and even if it did, `AppNavHost`'s hardcoded `startDestination` would ignore it on restart. Both halves of the fix — `OnboardingRepository.setOnboardingCompleted()` being called, **and** `startDestination` coming from `ResolveStartDestinationUseCase` instead of a literal — are required together; either one alone still leaves the bug.

---

## 5. File checklist

| File | Module | Status |
|---|---|---|
| `LanguageRepository.hasSelectedLanguage()` — new method on existing interface | `feature/language/domain` | add |
| `LanguageRepositoryImpl` — implement the new method | `feature/language/data` | add |
| `OnboardingRepository` (new interface) + impl + DataStore datasource | `feature/onboarding/domain` + `data` | new |
| `AppRoute.Login` (if not already present) | `core/navigation` | confirm/add |
| `ResolveStartDestinationUseCase` | `app/startup/` (not `core`, not a feature — see 3.3) | new |
| `AppStartViewModel` | `app/startup/` | new |
| `AppNavHost` — `startDestination` now comes from the ViewModel, not a literal | `app` | update |
| `androidx.core:core-splashscreen` dependency + `installSplashScreen()` wiring | `app/build.gradle.kts` + `MainActivity.kt` | new |
| `OnboardingScreen.onEvent` — merge the two duplicated branches | `feature/onboarding/presentation` | cleanup |
