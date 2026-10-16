# Project Structure Guide

Dukkan App (`com.example.dukkanapp`), single `app` module. Where every file lives, why it lives there, and the exact moves that get the current project into that shape.

Related guides:
- [MVI Migration Guide](mvi_migration_guide.md): how each screen is built (State, Intent, Effect, ViewModel).
- [Error Handling Architecture](error_handling_architecture.md): what lives in `core/error/`.

**Do this guide first, then the MVI migration.** The moves here are mechanical: the app behaves the same before and after, so you can commit and check that it still builds before you start changing screens.

---

## Table of Contents

1. [The Dependency Rule](#1-the-dependency-rule)
2. [Where Does This File Go?](#2-where-does-this-file-go)
3. [Target Tree: Every File in Its Place](#3-target-tree-every-file-in-its-place)
4. [App Root Rules](#4-app-root-rules)
5. [Core Rules](#5-core-rules)
6. [Feature Rules](#6-feature-rules)
7. [Naming Rules](#7-naming-rules)
8. [Migration: Step by Step, With Code](#8-migration-step-by-step-with-code)
9. [Verification](#9-verification)
10. [Adding a New Feature](#10-adding-a-new-feature)

---

## 1. The Dependency Rule

One rule decides almost every placement question:

```
┌────────────────────────────────────────────┐
│ app root                                   │   MainActivity, DukkanApplication,
│ navigation/   startup/                     │   NavHost, start destination
└───────────────────┬────────────────────────┘
                    │ imports
                    ▼
┌────────────────────────────────────────────┐
│ features/auth   features/language   ...    │   one folder per feature
└───────────────────┬────────────────────────┘
                    │ imports
                    ▼
┌────────────────────────────────────────────┐
│ core/                                      │   shared by every feature,
│                                            │   knows no feature
└────────────────────────────────────────────┘
```

| Who | May import | Must never import |
|---|---|---|
| **app root** | features, core | (nothing forbidden) |
| **a feature** | core, its own files | another feature's `data/` or `presentation/` |
| **core** | core only | anything in `features/`, `navigation/`, `startup/` |

**Why it matters:**
- **Core stays reusable.** You can add a `cart` feature without touching core, and delete `onboarding` without touching core.
- **Changes stay local.** A change inside a feature can't break core, because core doesn't know the feature exists.
- **Later, splitting into Gradle modules is a move, not a rewrite.** Each folder above is already a module boundary (`:core`, `:feature:auth`, `:app`).

**Today, five core files break the rule** (they import `features.*`):

| File | Imports |
|---|---|
| `core/di/AuthModule.kt` | `features.auth.data`, `features.auth.domain` |
| `core/di/LanguageModule.kt` | `features.language.data`, `features.language.domain` |
| `core/di/OnboardingModule.kt` | `features.onboarding.data`, `features.onboarding.domain` |
| `core/navigation/AppNavHost.kt` | every feature's screen |
| `core/navigation/startup/ResolveStartDestinationUseCase.kt` | three features' repositories |

Section 8 moves them out.

---

## 2. Where Does This File Go?

Ask the questions in order. Stop at the first "yes".

```
1. Does it wire the whole app together?             ──yes──► app root
   (NavHost, routes, start destination, Activity,          (navigation/, startup/)
    Application)
          │ no
          ▼
2. Is it used by ONE feature only?                  ──yes──► features/<that feature>/
          │ no                                              (see §6 for the layer)
          ▼
3. Is it used by two or more features, or is it     ──yes──► core/
   app-wide plumbing (errors, theme, MVI base,              (see §5 for the folder)
   DataStore, DI for core types)?
          │ no
          ▼
4. Nobody uses it yet ──► put it in the feature that will use it first.
                          Move it to core when a second feature needs it.
```

**The delete test:** imagine deleting `features/onboarding/`. Every file you'd also have to delete or edit belongs to onboarding (or to the app root, if it's the NavHost or startup). If nothing breaks, the file is truly core.

**Don't move to core "just in case".** A file used by one feature lives in that feature. Moving it to core later takes a minute; a core folder full of single-use files is hard to clean up.

---

## 3. Target Tree: Every File in Its Place

Every current file, in its final place. Files marked `←` move or change in §8.

```
com.example.dukkanapp/
│
├── DukkanApplication.kt
├── MainActivity.kt                                    ← imports change (§8.9)
│
├── navigation/                                        ← new: from core/navigation/ (§8.7)
│   ├── AppNavHost.kt
│   └── AppRoute.kt                                    ← renamed from AppRoutes.kt
│
├── startup/                                           ← new: from core/navigation/startup/ (§8.8)
│   ├── AppStartViewModel.kt
│   └── ResolveStartDestinationUseCase.kt
│
├── core/
│   ├── common/
│   │   ├── components/
│   │   │   ├── buttons/
│   │   │   │   ├── AppPrimaryButton.kt
│   │   │   │   └── AppTextButton.kt                   (AuthButton.kt moves to auth, §8.5)
│   │   │   ├── checkbox/                              ← was CheckBox/ (§8.1)
│   │   │   │   └── AppCheckBox.kt
│   │   │   ├── dividers/
│   │   │   │   └── AppDivider.kt
│   │   │   ├── headers/
│   │   │   │   └── AppAuthHeader.kt
│   │   │   ├── pager/                                 ← was core/common/pager/ (§8.1)
│   │   │   │   ├── AppHorizontalPager.kt
│   │   │   │   └── AppPagerIndicator.kt
│   │   │   ├── scaffold/
│   │   │   │   └── AppScaffold.kt
│   │   │   ├── textfield/                             ← was TextField/ (§8.1)
│   │   │   │   ├── AppPasswordTextField.kt
│   │   │   │   └── AppTextField.kt
│   │   │   └── texts/
│   │   │       └── AppAuthFooterLink.kt               ← renamed from AppAlreadyHaveAccount.kt
│   │   ├── mvi/                                       ← was core/common/logic/ (§8.3)
│   │   │   ├── AppViewModel.kt
│   │   │   └── AppCollectEffect.kt
│   │   └── text/                                      ← new (§8.4)
│   │       └── AppUiText.kt                           ← from components/texts/
│   │
│   ├── config/
│   │   └── theme/
│   │       ├── AppTheme.kt
│   │       ├── AppTypography.kt
│   │       └── ThemePreview.kt
│   │
│   ├── di/
│   │   ├── DataStoreModule.kt                         ← renamed from LocalizationModule.kt (§8.6)
│   │   └── ErrorModule.kt                             ← finished (§8.6)
│   │
│   ├── error/
│   │   ├── AppException.kt
│   │   ├── AppFailure.kt
│   │   ├── AppResult.kt
│   │   ├── guard/
│   │   │   ├── guardException.kt
│   │   │   └── guardFailure.kt
│   │   ├── mapper/
│   │   │   ├── AppFailureMapper.kt
│   │   │   └── AppFirebaseExceptionMapper.kt
│   │   ├── reporter/
│   │   │   └── ErrorReporter.kt
│   │   ├── ui/
│   │   │   ├── AppFailureUiText.kt
│   │   │   └── ValidationErrorUiText.kt
│   │   └── validation/
│   │       └── ValidationError.kt
│   │
│   └── utils/
│       ├── constants/
│       │   ├── AppColors.kt
│       │   ├── AppDimens.kt
│       │   ├── AppExtendedColors.kt
│       │   ├── AppPreferencesKeys.kt
│       │   ├── AppShapes.kt
│       │   └── AppValidationConstants.kt
│       └── extension/
│           ├── DataStoreExtension.kt
│           ├── ModifierExtension.kt
│           └── StringExtension.kt
│
└── features/
    ├── auth/
    │   ├── data/
    │   │   └── repository/
    │   │       └── AuthRepositoryImpl.kt
    │   ├── di/                                        ← new (§8.6)
    │   │   └── AuthModule.kt                          ← from core/di/
    │   ├── domain/
    │   │   ├── repository/
    │   │   │   └── AuthRepository.kt
    │   │   └── validation/
    │   │       └── AuthFormValidator.kt               (from the MVI guide, Step 2.4)
    │   └── presentation/
    │       ├── components/
    │       │   ├── authoptions/
    │       │   │   ├── AuthButton.kt                  ← from core/common/components/buttons/ (§8.5)
    │       │   │   ├── AuthOptionsScreenContent.kt
    │       │   │   └── AuthOptionsSection.kt
    │       │   ├── login/
    │       │   │   └── LoginScreenContent.kt
    │       │   └── signup/
    │       │       └── SignUpScreenContent.kt         ← package fix (§8.1)
    │       ├── logic/
    │       │   ├── login/
    │       │   │   ├── LoginEffect.kt                 (MVI guide)
    │       │   │   ├── LoginIntent.kt                 (MVI guide)
    │       │   │   ├── LoginUiState.kt
    │       │   │   └── LoginViewModel.kt
    │       │   └── signup/
    │       │       ├── SignUpEffect.kt
    │       │       ├── SignUpIntent.kt
    │       │       ├── SignUpUiState.kt
    │       │       └── SignUpViewModel.kt
    │       └── screens/
    │           ├── AuthOptionsScreen.kt
    │           ├── LoginScreen.kt
    │           └── SignUpScreen.kt
    │
    ├── language/
    │   ├── data/
    │   │   ├── datasource/
    │   │   │   ├── LanguagePreferenceLocalDataSource.kt
    │   │   │   └── PlatformLocaleDataSource.kt
    │   │   └── repository/
    │   │       └── LanguageRepositoryImpl.kt
    │   ├── di/                                        ← new (§8.6)
    │   │   └── LanguageModule.kt                      ← from core/di/
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── LanguageModel.kt
    │   │   ├── repository/
    │   │   │   └── LanguageRepository.kt
    │   │   └── usecase/
    │   │       ├── GetSupportedLanguagesUseCase.kt
    │   │       ├── ObserveSelectedLanguageUseCase.kt
    │   │       └── SelectLanguageUseCase.kt
    │   └── presentation/
    │       ├── components/
    │       │   ├── LanguageItemCard.kt
    │       │   ├── LanguageSearchField.kt
    │       │   ├── LanguageSelectionIndicator.kt
    │       │   └── LanguageSelectionScreenContent.kt
    │       ├── logic/
    │       │   ├── LanguageSelectionEffect.kt         (MVI guide)
    │       │   ├── LanguageSelectionIntent.kt         (MVI guide, replaces ...Event.kt)
    │       │   ├── LanguageSelectionUiState.kt
    │       │   └── LanguageSelectionViewModel.kt
    │       ├── model/
    │       │   └── LanguageUiModel.kt
    │       └── screens/
    │           └── LanguageSelectionScreen.kt
    │
    └── onboarding/
        ├── data/
        │   ├── datasource/
        │   │   └── OnboardingPreferenceLocalDataSource.kt
        │   └── repository/
        │       └── OnboardingRepositoryImpl.kt
        ├── di/                                        ← new (§8.6)
        │   └── OnboardingModule.kt                    ← from core/di/
        ├── domain/
        │   ├── model/
        │   │   └── OnboardingPageModel.kt
        │   ├── repository/
        │   │   └── OnboardingRepository.kt
        │   └── usecase/                               ← was usecases/ (§8.2)
        │       ├── GetOnboardingPagesUseCase.kt
        │       └── SetOnboardingCompletedUseCase.kt
        └── presentation/
            ├── components/
            │   ├── OnboardingIllustration.kt
            │   └── OnboardingScreenContent.kt
            ├── logic/
            │   ├── OnboardingEffect.kt                (MVI guide)
            │   ├── OnboardingIntent.kt                (MVI guide, replaces ...Event.kt)
            │   ├── OnboardingUiState.kt
            │   └── OnboardingViewModel.kt
            ├── model/
            │   └── OnboardingPageUiModel.kt
            └── screens/
                └── OnboardingScreen.kt
```

### Test folders mirror the main folders

A test sits in the same package as the class it tests, under `src/test/` (JVM) or `src/androidTest/` (device):

```
app/src/test/java/com/example/dukkanapp/
├── core/error/mapper/DefaultFailureMapperTest.kt
└── features/auth/domain/validation/AuthFormValidatorTest.kt
```

---

## 4. App Root Rules

The root package `com.example.dukkanapp` holds what assembles the app. It's the only place allowed to know about every feature.

| Folder / file | Holds | Belongs here because |
|---|---|---|
| `DukkanApplication.kt` | `@HiltAndroidApp` class | Android entry point |
| `MainActivity.kt` | Splash, theme, `AppNavHost` | Android entry point |
| `navigation/` | `AppNavHost`, `AppRoute` | Imports every feature's screen |
| `startup/` | `AppStartViewModel`, `ResolveStartDestinationUseCase` | Reads the language, onboarding and auth repositories to pick the first screen |

**Keep it thin.** No UI components, no business rules. If the root starts growing, the code probably belongs in a feature.

**Why routes live with the NavHost and not in core:** routes describe *this app's* screens. A route like `LoginScreen` is not reusable infrastructure. Features don't import routes either; they expose `onNavigateToX` lambdas, and the NavHost maps those to routes.

---

## 5. Core Rules

`core/` is a toolbox every feature can use. **It never names a feature.** No `Login`, `Language`, `Onboarding` in a core class name, import or string.

> `AppAuthHeader` and `AppAuthFooterLink` have "Auth" in the name but are used by Onboarding and Auth, so they're shared UI. The rule is about *imports and knowledge of a feature*, not about the word.

### What each core folder holds

| Folder | Holds | A file belongs here if... | Examples |
|---|---|---|---|
| `common/components/<kind>/` | Reusable composables, grouped by kind | Two or more features show it, and it takes plain parameters (no feature types) | `AppPrimaryButton`, `AppTextField`, `AppScaffold`, `AppHorizontalPager` |
| `common/mvi/` | The MVI base | Every screen's ViewModel or Screen uses it | `AppViewModel`, `AppCollectEffect` |
| `common/text/` | Text models (no UI) | It carries text from a ViewModel to the UI | `AppUiText` |
| `config/theme/` | Material theme setup | It configures `MaterialTheme` | `AppTheme`, `AppTypography` |
| `di/` | Hilt modules for **core** types only | It binds or provides a type declared in core, or an app-wide singleton | `ErrorModule`, `DataStoreModule` |
| `error/` | Error model and translation | See the [Error Handling guide](error_handling_architecture.md#3-folder-structure) | `AppFailure`, `AppResult`, `AppFailureUiText` |
| `utils/constants/` | App-wide constants and design tokens | Two or more features read it | `AppDimens`, `AppPreferencesKeys` |
| `utils/extension/` | Kotlin extension functions | Pure helper, no feature knowledge | `isValidEmail()`, `clearFocusOnTap()` |

### Core rules in short

1. **No `features.*` imports.** Ever.
2. **Components take primitives and lambdas** (`String`, `Boolean`, `() -> Unit`), never a feature's `UiState` or model.
3. **Components don't call `stringResource` for feature text.** The feature passes the text in. (A neutral default like `AppDivider`'s "OR" is fine.)
4. **Core DI binds core types.** A feature's repository is bound in that feature's `di/` folder.
5. **`AppPreferencesKeys` stays in one file**, even though each key belongs to a feature. One file means two features can't pick the same key name by accident.
6. **Core public names start with `App`** (`AppViewModel`, `AppTextField`). In a feature you can tell at a glance what comes from the toolbox.

---

## 6. Feature Rules

Each feature has the same four folders. Leave out a folder the feature doesn't need (`auth` has no `model/` yet).

```
features/<feature>/
├── data/            how data is stored or fetched
│   ├── datasource/  one class per source: DataStore, Firebase, platform APIs
│   ├── dto/         wire/storage formats + toDomain() mappers (when needed)
│   └── repository/  <X>RepositoryImpl: implements the domain interface
├── di/              <Feature>Module: binds the repository (and data sources if needed)
├── domain/          what the feature does: pure Kotlin
│   ├── model/       <X>Model: the feature's business types
│   ├── repository/  <X>Repository: interfaces only
│   ├── usecase/     <Verb><Noun>UseCase: one action each
│   └── validation/  form and business rules (AuthFormValidator)
└── presentation/    what the user sees
    ├── components/  composables only this feature uses, incl. <X>ScreenContent
    ├── logic/       MVI per screen: <X>UiState, <X>Intent, <X>Effect, <X>ViewModel
    ├── model/       <X>UiModel + toUiModel() mappers
    └── screens/     <X>Screen: glue between ViewModel and ScreenContent
```

### What each layer may import

| Layer | May import | Must not import |
|---|---|---|
| `domain/` | Kotlin, coroutines, `javax.inject`, `core/error/*` (not `ui/`), `core/utils/*` (pure Kotlin parts) | `android.*`, Compose, Firebase, `data/`, `presentation/`, `R` |
| `data/` | Its own `domain/`, `core/error/*`, `core/utils/*`, Android, DataStore, Firebase | `presentation/`, Compose |
| `presentation/` | Its own `domain/`, `core/common/*`, `core/error/ui/*`, `core/config/theme/*`, Android, Compose, `R` | Its own `data/` (no `RepositoryImpl`, no `DataSource`) |
| `di/` | Its own `data/` and `domain/`, Hilt | `presentation/` |

**Why domain has no Android:** it runs in plain JVM unit tests (fast, no emulator), and the business rules don't change when the UI or the storage changes.

**Why presentation never sees data:** the ViewModel asks a use case. If you swap DataStore for Firestore, only `data/` and `di/` change.

### When two features need the same thing

| They share... | Do this |
|---|---|
| A composable | Move it to `core/common/components/` with plain parameters |
| A model or rule that's really app-wide (e.g. `ValidationError`) | Move it to `core/` |
| A feature's data (e.g. startup needs `LanguageRepository`) | The consumer uses the other feature's **domain interface** only. Today only the app root (`startup/`) does this, which is allowed |
| A feature's screen | Never import it. Navigate to it through the NavHost |

### Per-screen MVI files

Screens with one screen per feature (language, onboarding) keep their MVI files directly in `logic/`. Features with several screens (auth) get a sub-folder per screen: `logic/login/`, `logic/signup/`. Components follow the same split: `components/login/`, `components/signup/`.

---

## 7. Naming Rules

### Packages and folders
- **Lowercase, no underscores:** `textfield`, `checkbox`, `signup`, `authoptions`. Never `TextField`, `CheckBox`, `SignUp`.
- **Folder path = package name**, always. Kotlin allows a mismatch; don't use it.
- **Singular for kinds of things:** `usecase/`, `repository/`, `model/`, `datasource/`. Plural only for "a bag of": `components/`, `screens/`, `constants/`.

### Files
- **One main declaration per file, file named after it:** `AppRoute.kt` holds `sealed interface AppRoute`; `AppAuthFooterLink.kt` holds `fun AppAuthFooterLink`.
- **A file of extension functions or top-level helpers** is named after what they extend or do: `StringExtension.kt`, `guardException.kt`.
- A mapper extension lives in the file of the type it produces (`LanguageModel.toUiModel()` sits in `LanguageUiModel.kt`).

### Class suffixes

| Suffix | Layer | Example |
|---|---|---|
| `Model` | domain | `LanguageModel`, `OnboardingPageModel` |
| `Repository` / `RepositoryImpl` | domain / data | `LanguageRepository` / `LanguageRepositoryImpl` |
| `LocalDataSource` / `RemoteDataSource` | data | `LanguagePreferenceLocalDataSource`, `AuthRemoteDataSource` |
| `Dto` | data | `UserProfileDto` |
| `UseCase` | domain | `SelectLanguageUseCase` (verb + noun) |
| `Validator` | domain | `AuthFormValidator` |
| `UiModel` | presentation | `LanguageUiModel` |
| `UiState` / `Intent` / `Effect` / `ViewModel` | presentation | `LoginUiState`, `LoginIntent`, `LoginEffect`, `LoginViewModel` |
| `Screen` | presentation | `LoginScreen`: has a ViewModel, collects state and effects |
| `ScreenContent` | presentation | `LoginScreenContent`: stateless, `state` + `onIntent` |
| `Module` | di | `AuthModule`, `ErrorModule` |

### Composables
- **Composables that emit UI (return `Unit`) start with a capital letter:** `AppCollectEffect`, `LoginScreen`. Lowercase composables are for ones that return a value (`rememberPagerState`).
- **Shared composables start with `App`.** Feature composables start with the feature or screen name (`LanguageItemCard`, `OnboardingIllustration`).

### Resources

| Resource | Pattern | Examples |
|---|---|---|
| String, one screen | `<screen>_<what>` | `login_title`, `signup_button` |
| String, shared within a feature | `<feature>_<what>` | `auth_email_placeholder` |
| String, app-wide action | `action_<verb>` | `action_continue`, `action_log_in` |
| String, content description | `cd_<what>` | `cd_back`, `cd_show_password` |
| String, errors | `validation_<what>` / `error_<what>` | `validation_email_empty`, `error_no_connection` |
| Drawable, icon | `ic_<name>` | `ic_google`, `ic_flag_ar` |
| Drawable, illustration | `img_<screen>_<name>` | `img_onboarding_discover` |

Every string exists in `values/`, `values-ar/` and `values-fr/` with the same keys. A string used in one place is never duplicated under a second key; a shared text gets one shared key.

---

## 8. Migration: Step by Step, With Code

### How to move files safely

**Use Android Studio's refactoring, not the file explorer.** Right-click the file or package → **Refactor → Move** (F6) or **Rename** (Shift+F6). It updates the `package` line and every import in one go. The code below shows the result, so you can check it; you don't have to type the imports by hand.

**Windows: renaming only the letter case** (`TextField` → `textfield`) can silently fail, because Windows treats both names as the same folder. Rename in two steps: `TextField` → `textfield_tmp` → `textfield`. If you rename with git instead:

```bash
git mv app/src/main/java/com/example/dukkanapp/core/common/components/TextField app/src/main/java/com/example/dukkanapp/core/common/components/textfield_tmp
git mv app/src/main/java/com/example/dukkanapp/core/common/components/textfield_tmp app/src/main/java/com/example/dukkanapp/core/common/components/textfield
```

**Build after each step** (Build → Make Project). A broken import is easy to find when you've only moved one thing.

---

### Step 8.1: Fix folder and package names

| Change | Files that update their import |
|---|---|
| Folder `components/TextField/` → `components/textfield/` (package is already `textfield`) | none |
| Folder and package `components/CheckBox/` → `components/checkbox/` | `LoginScreenContent.kt` |
| Folder and package `core/common/pager/` → `core/common/components/pager/` | `OnboardingScreenContent.kt` |
| Package `components.SignUp` → `components.signup` (folder is already `signup`) | `SignUpScreen.kt` |
| File `texts/AppAlreadyHaveAccount.kt` → `texts/AppAuthFooterLink.kt` (function name unchanged) | none |

New first lines:

```kotlin
// core/common/components/checkbox/AppCheckBox.kt
package com.example.dukkanapp.core.common.components.checkbox
```

```kotlin
// core/common/components/pager/AppHorizontalPager.kt and AppPagerIndicator.kt
package com.example.dukkanapp.core.common.components.pager
```

```kotlin
// features/auth/presentation/components/signup/SignUpScreenContent.kt
package com.example.dukkanapp.features.auth.presentation.components.signup
```

Updated imports:

```kotlin
// LoginScreenContent.kt
import com.example.dukkanapp.core.common.components.checkbox.AppCheckBox

// OnboardingScreenContent.kt
import com.example.dukkanapp.core.common.components.pager.AppHorizontalPager
import com.example.dukkanapp.core.common.components.pager.AppPagerIndicator

// SignUpScreen.kt
import com.example.dukkanapp.features.auth.presentation.components.signup.SignUpScreenContent
```

**Optional, same step:** the text field composable is spelled `AppTextFiled`. Rename the function (Shift+F6 on the name) to `AppTextField` so it matches its file. It's used in `LoginScreenContent`, `SignUpScreenContent` and `LanguageSearchField`.

---

### Step 8.2: `usecases/` → `usecase/` in onboarding

Language already uses `usecase/`. Rename the onboarding package.

```kotlin
// features/onboarding/domain/usecase/GetOnboardingPagesUseCase.kt
// features/onboarding/domain/usecase/SetOnboardingCompletedUseCase.kt
package com.example.dukkanapp.features.onboarding.domain.usecase
```

```kotlin
// OnboardingViewModel.kt
import com.example.dukkanapp.features.onboarding.domain.usecase.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.domain.usecase.SetOnboardingCompletedUseCase
```

---

### Step 8.3: `core/common/logic/` → `core/common/mvi/`

Inside a feature, `presentation/logic/` is a clear name. In core, `logic` could mean anything; `mvi` says exactly what's inside. Nothing imports these files yet, so the move is free.

Two fixes on the way:
- `appCollectEffect` → `AppCollectEffect` (a composable that returns `Unit` starts with a capital).
- `Dispatchers.Main` → `Dispatchers.Main.immediate`, as the comment already says.

**File:** `core/common/mvi/AppViewModel.kt`

```kotlin
package com.example.dukkanapp.core.common.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * MVI base: the screen sends [I]ntents, renders [S]tate, and handles one-shot [E]ffects
 * (navigation, snackbars, opening other apps).
 */
abstract class AppViewModel<S, I, E>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UNLIMITED: trySend never drops; receiveAsFlow delivers each effect to exactly one collector.
    private val _effect = Channel<E>(Channel.UNLIMITED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Single entry point: the screen only sends intents. */
    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) = _state.update(reduce)

    protected fun sendEffect(effect: E) {
        _effect.trySend(effect)
    }
}
```

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

/**
 * Collects one-shot effects only while the screen is visible (STARTED), so navigation never
 * fires in the background. Effects sent meanwhile wait in the channel and are delivered on return.
 */
@Composable
fun <E> AppCollectEffect(effect: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate: an effect is never lost between emission and a recomposition.
            withContext(Dispatchers.Main.immediate) {
                effect.collect { currentOnEffect(it) }
            }
        }
    }
}
```

Delete the empty `core/common/logic/` folder.

---

### Step 8.4: `AppUiText` → `core/common/text/`

`AppUiText` is a data model that carries text from a ViewModel to the UI. It draws nothing, so it isn't a component. It also gets the `args` fix from the MVI guide (Step 2.3).

**File:** `core/common/text/AppUiText.kt`

```kotlin
package com.example.dukkanapp.core.common.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text decided in a ViewModel (which has no Context) and resolved in the UI. */
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

Files that update their import:

```kotlin
// core/error/ui/AppFailureUiText.kt
// core/error/ui/ValidationErrorUiText.kt
import com.example.dukkanapp.core.common.text.AppUiText
```

After the MVI migration, ScreenContent files also import `com.example.dukkanapp.core.common.text.asString`.

---

### Step 8.5: `AuthButton` → auth feature

Only `AuthOptionsSection` uses it (the Google and Apple buttons). Move it next to its only caller.

**File:** `features/auth/presentation/components/authoptions/AuthButton.kt`

Only the `package` line changes; the rest of the file stays exactly as it is.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.authoptions
```

In `AuthOptionsSection.kt`, **delete** this import (same package now, no import needed):

```kotlin
import com.example.dukkanapp.core.common.components.buttons.AuthButton
```

> If a "Sign in with Google" button later appears outside auth (e.g. checkout asks guests to sign in), move it back to `core/common/components/buttons/` and rename it `AppSocialButton`.

---

### Step 8.6: Hilt modules

Hilt finds a module anywhere in the source set, so moving a module changes nothing at runtime. Only the location and package change.

**File:** `features/auth/di/AuthModule.kt`

```kotlin
package com.example.dukkanapp.features.auth.di

import com.example.dukkanapp.features.auth.data.repository.AuthRepositoryImpl
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
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
}
```

`@Singleton` is added so all three feature modules behave the same. The Firebase guide adds the auth data sources here later.

**File:** `features/language/di/LanguageModule.kt`

```kotlin
package com.example.dukkanapp.features.language.di

import com.example.dukkanapp.features.language.data.repository.LanguageRepositoryImpl
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageModule {

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository
}
```

**File:** `features/onboarding/di/OnboardingModule.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.di

import com.example.dukkanapp.features.onboarding.data.repository.OnboardingRepositoryImpl
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
```

**File:** `core/di/DataStoreModule.kt` (renamed from `LocalizationModule.kt`)

It provides the app's single `DataStore`, which language *and* onboarding use. "Localization" undersold it.

```kotlin
package com.example.dukkanapp.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.dukkanapp.core.utils.extension.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}
```

**File:** `core/di/ErrorModule.kt` (finish the file you started)

Binds the two error interfaces from `core/error/` to their implementations. Repositories inject `AppFailureMapper` to use `guardFailure { }`.

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.core.error.mapper.DefaultFailureMapper
import com.example.dukkanapp.core.error.reporter.ErrorReporter
import com.example.dukkanapp.core.error.reporter.LogCatErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {

    @Binds
    @Singleton
    abstract fun bindErrorReporter(impl: LogCatErrorReporter): ErrorReporter

    @Binds
    @Singleton
    abstract fun bindFailureMapper(impl: DefaultFailureMapper): AppFailureMapper
}
```

Swap `LogCatErrorReporter` for a Crashlytics one later by changing one line here.

Delete `core/di/AuthModule.kt`, `core/di/LanguageModule.kt`, `core/di/OnboardingModule.kt` after the moves (Refactor → Move does this for you).

---

### Step 8.7: Navigation → root `navigation/`

**File:** `navigation/AppRoute.kt` (renamed from `core/navigation/AppRoutes.kt`, named after the type it holds)

Also fixes `LoginScreen` and `SignUpScreen`, which didn't implement `AppRoute`.

```kotlin
package com.example.dukkanapp.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object LanguageSelectionScreen : AppRoute

    @Serializable
    data object OnboardingScreen : AppRoute

    @Serializable
    data object AuthOptionsScreen : AppRoute

    @Serializable
    data object HomeScreen : AppRoute

    @Serializable
    data object LoginScreen : AppRoute

    @Serializable
    data object SignUpScreen : AppRoute
}
```

**File:** `navigation/AppNavHost.kt`

Only the `package` line changes now; the body stays as it is. (The [MVI guide §7.2](mvi_migration_guide.md#step-72-appnavhost) rewrites the body later; use `package com.example.dukkanapp.navigation` there too.)

```kotlin
package com.example.dukkanapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.AuthOptionsScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.auth.presentation.screens.SignUpScreen
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

// ... body unchanged ...
```

`AppRoute` is in the same package, so it needs no import.

---

### Step 8.8: Startup → root `startup/`

`ResolveStartDestinationUseCase` reads three features' repositories and returns a route. That's app wiring, so it leaves core. `AppStartViewModel` goes with it: only `MainActivity` uses it.

`AppStartViewModel` stays a plain `ViewModel`, not an `AppViewModel`: it has no intents and no effects, only one value.

**File:** `startup/ResolveStartDestinationUseCase.kt`

```kotlin
package com.example.dukkanapp.startup

import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import com.example.dukkanapp.navigation.AppRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/** Picks the first screen. Uses only the features' domain interfaces, never their data classes. */
class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppRoute = coroutineScope {
        // The three reads are independent: run them at the same time.
        val hasLanguage = async { languageRepository.hasSelectedLanguage() }
        val hasSeenOnboarding = async { onboardingRepository.hasSeenOnboarding() }
        val isLoggedIn = async { authRepository.isLoggedIn() }

        when {
            !hasLanguage.await() -> AppRoute.LanguageSelectionScreen
            !hasSeenOnboarding.await() -> AppRoute.OnboardingScreen
            !isLoggedIn.await() -> AppRoute.AuthOptionsScreen
            else -> AppRoute.HomeScreen
        }
    }
}
```

**File:** `startup/AppStartViewModel.kt`

```kotlin
package com.example.dukkanapp.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds the first route. null = still deciding (the splash screen stays up). */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val resolveStartDestination: ResolveStartDestinationUseCase,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<AppRoute?>(null)
    val startDestination: StateFlow<AppRoute?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value = resolveStartDestination()
        }
    }
}
```

Delete the empty `core/navigation/` folder.

---

### Step 8.9: MainActivity

Only imports change, plus removing `GreetingPreview` (it can't render: `LanguageSelectionScreen` needs Hilt).

**File:** `MainActivity.kt`

```kotlin
package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.navigation.AppNavHost
import com.example.dukkanapp.startup.AppStartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Activity-scoped: survives the recreation caused by a language change.
    private val appStartViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen on-screen until the start destination is decided.
        splashScreen.setKeepOnScreenCondition { appStartViewModel.startDestination.value == null }
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val startDestination by appStartViewModel.startDestination.collectAsStateWithLifecycle()
                startDestination?.let { AppNavHost(startDestination = it) }
            }
        }
    }
}
```

---

## 9. Verification

### Search checks

Run each search over `app/src/main/java` (Ctrl+Shift+F in Android Studio). Each should return **no results**:

| Search for | In | Means |
|---|---|---|
| `import com.example.dukkanapp.features` | `core/` | Core knows no feature (§1) |
| `import com.example.dukkanapp.navigation` | `core/`, `features/` | Only the root knows routes |
| `import com.example.dukkanapp.startup` | everywhere except `MainActivity.kt` | Startup is used once |
| `.data.` in an import | `features/*/presentation/` | Presentation never sees data |
| `import android.` or `import androidx.compose` | `features/*/domain/` | Domain is pure Kotlin |
| `components.TextField`, `components.CheckBox`, `components.SignUp`, `usecases` | everywhere | Package names fixed |
| `core.common.logic`, `core.common.pager`, `core.common.components.texts.AppUiText`, `core.navigation` | everywhere | Old packages gone |

From Git Bash, the first check in one line:

```bash
grep -rn "import com.example.dukkanapp.features" app/src/main/java/com/example/dukkanapp/core
```

### Build and run

- [ ] Build → Clean Project, then Make Project: no errors
- [ ] Clear app data, launch: Language → Onboarding → Auth Options works as before
- [ ] Relaunch: the start screen is correct (Hilt modules still bound after the move)
- [ ] Commit: `refactor: reorganize packages by layer and feature`

### Checklist

- [ ] 8.1 `textfield/`, `checkbox/`, `components/pager/`, `signup` package, `AppAuthFooterLink.kt`
- [ ] 8.2 onboarding `usecase/`
- [ ] 8.3 `core/common/mvi/AppViewModel.kt`, `AppCollectEffect.kt`
- [ ] 8.4 `core/common/text/AppUiText.kt`
- [ ] 8.5 `AuthButton` in `authoptions/`
- [ ] 8.6 three feature `di/` modules, `DataStoreModule`, `ErrorModule` complete
- [ ] 8.7 `navigation/AppRoute.kt`, `navigation/AppNavHost.kt`
- [ ] 8.8 `startup/AppStartViewModel.kt`, `startup/ResolveStartDestinationUseCase.kt`
- [ ] 8.9 `MainActivity` imports, `GreetingPreview` removed
- [ ] §9 search checks clean

---

## 10. Adding a New Feature

Example: `cart`.

```
features/cart/
├── data/
│   ├── datasource/CartRemoteDataSource.kt
│   ├── dto/CartItemDto.kt
│   └── repository/CartRepositoryImpl.kt
├── di/CartModule.kt
├── domain/
│   ├── model/CartItemModel.kt
│   ├── model/CartFailure.kt                 (sealed interface : AppFailure.Feature)
│   ├── repository/CartRepository.kt
│   └── usecase/AddToCartUseCase.kt
└── presentation/
    ├── components/CartScreenContent.kt
    ├── logic/
    │   ├── CartEffect.kt
    │   ├── CartIntent.kt
    │   ├── CartUiState.kt
    │   └── CartViewModel.kt                 (extends AppViewModel)
    ├── model/CartItemUiModel.kt
    └── screens/CartScreen.kt
```

- [ ] Domain first: model, repository interface, use cases (pure Kotlin)
- [ ] Data: data source with `guardException { }`, `RepositoryImpl` with `guardFailure { }`
- [ ] `di/CartModule.kt` binds `CartRepository`
- [ ] Presentation: State, Intent, Effect, ViewModel, Screen, ScreenContent (see the [MVI guide](mvi_migration_guide.md#1-the-mvi-contract))
- [ ] Strings in all three `values*/strings.xml`, keys prefixed `cart_`
- [ ] Route in `navigation/AppRoute.kt`, `composable<AppRoute.CartScreen>` in `AppNavHost`
- [ ] Nothing added to `core/` unless a second feature needs it
