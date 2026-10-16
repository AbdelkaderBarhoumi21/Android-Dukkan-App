# MVI Migration Guide: Language, Onboarding, Login, Sign Up

Dukkan App (`com.example.dukkanapp`). Moves the four existing screens onto one MVI pattern, so every screen in the app is built the same way.

This guide follows the **current code**, after the moves in the [Project Structure guide](project_structure_guide.md) (do that one first). Two names differ from older docs:

| Older docs say | Code (and this guide) uses |
|---|---|
| `UiText` in `core/common/text/` | `AppUiText` in `core/common/text/` |
| `R.string.signup_error_*`, `login_error_*` | `R.string.validation_*` (strings cleanup) |

Related guides:
- [Error Handling Architecture](error_handling_architecture.md): `AppResult`, `AppFailure`, `AppUiText`, `ValidationError`.
- [Firebase Auth Integration](../auth/firebase_auth_integration_guide.md): plugs real auth into the Login and Sign Up ViewModels written here.

---

## Table of Contents

0. [Audit: Where Each Feature Stands](#0-audit-where-each-feature-stands)
1. [The MVI Contract](#1-the-mvi-contract)
2. [Core Setup](#2-core-setup)
3. [Language Selection](#3-language-selection)
4. [Onboarding](#4-onboarding)
5. [Login](#5-login)
6. [Sign Up](#6-sign-up)
7. [Navigation](#7-navigation)
8. [Cleanup](#8-cleanup)
9. [Implementation Checklist](#9-implementation-checklist)
10. [Manual Testing](#10-manual-testing)

---

## 0. Audit: Where Each Feature Stands

| Feature | Today | What's missing |
|---|---|---|
| **Language** | `onEvent(LanguageSelectionEvent)`, hand-made `MutableStateFlow` | Intent naming, no Effect: "Continue" bypasses the ViewModel |
| **Onboarding** | `onEvent(OnboardingEvent)`, hand-made `MutableStateFlow` | No Effect: the Screen intercepts events and navigates itself |
| **Login** | Old style: `onEmailChanged()`, `onPasswordChanged()`, ... | Everything: no Intent, no Effect, `@StringRes` errors |
| **Sign Up** | Has `SignUpIntent` + `SignUpEffect` (closest to done) | Not on `AppViewModel`, public `onX()` functions, `@StringRes` errors, navigation lambdas bypass the ViewModel, bugs below |
| **Auth Options** | Stateless screen, no ViewModel | Nothing. No logic to own yet. It gets MVI when Google/Apple sign-in arrives |

### Bugs found while auditing (all fixed by the code in this guide)

| # | Where | Bug | Effect on the user |
|---|---|---|---|
| 1 | `SignUpViewModel` | `if (emailError != null && passwordError != null && confirmPasswordError != null) return`: `&&` should be `||` | Sign up continues while one or two fields are invalid |
| 2 | `SignUpUiState` | `isValidEmail = emailError != null && ...` (same for password, confirm): should be `== null` | The green success tick never shows |
| 3 | `AppUiText.asString()` (Compose version) | Calls `stringResource(id)` and drops `args` | "Password must be at least %1$d characters" shows the raw `%1$d` |
| 4 | `OnboardingScreen` | Calls `onFinished()` right after `viewModel.onEvent(...)`, before `setOnboardingCompleted()` has finished writing | If the app is killed right away, onboarding shows again. A double tap navigates twice |
| 5 | `LanguageSelectionViewModel` | "Continue" never saves the language. It is only saved when a row is tapped | User keeps the default language, taps Continue → the language screen shows again on every launch |
| 6 | `AppNavHost` | Language screen gets `onBack = {}` | A back arrow that does nothing |
| 7 | `AppNavHost` | Onboarding "Already have an account? Log In" goes to Auth Options | The user has to find "log in" a second time |
| 8 | `LoginViewModel` | No `isLoading`, no double-tap guard, enforces the 8-char minimum on login | The button never shows loading. Accounts with an older shorter password can't log in |
| 9 | `OnboardingViewModel` | `coerceIn(0, pages.lastIndex)` with an empty list is `coerceIn(0, -1)` | Crash (`IllegalArgumentException`) |
| 10 | `OnboardingScreenContent` | `LaunchedEffect(pagerState.currentPage)` around a `snapshotFlow` | The collector restarts on every swipe (works, but wasteful) |
| 11 | `AppRoutes` | `LoginScreen` and `SignUpScreen` don't implement `AppRoute` | Not type-safe: they can't be passed where an `AppRoute` is expected |
| 12 | `SignUpScreenContent.kt` | Package `components.SignUp`, folder `components/signup` | Package and folder don't match (Kotlin warning, confusing imports) |
| 13 | `MainActivity` | `GreetingPreview` renders `LanguageSelectionScreen`, which needs Hilt | The preview can't render |

---

## 1. The MVI Contract

```
                 ┌──────────────── state: StateFlow<UiState> ─────────────────┐
                 │                                                             ▼
NavHost ──► XxxScreen ──► XxxScreenContent(state, onIntent)          (renders state only)
   ▲             │                    │
   │             │                    └── user taps ──► XxxIntent ──► ViewModel.onIntent()
   │             │                                                        │
   │             └── AppCollectEffect ◄── effect: Flow<XxxEffect> ◄──────────┘ (one-shot)
   │                       │
   └─── onNavigateToX() ◄──┘
```

### Four files per screen

| File | Holds | Example |
|---|---|---|
| `XxxUiState` | Everything the screen shows. One immutable `data class` | `email`, `emailError`, `isLoading` |
| `XxxIntent` | Everything the user can do. A `sealed interface` | `EmailChanged(email)`, `LoginClicked` |
| `XxxEffect` | Things that happen once (navigation, snackbar). A `sealed interface` | `NavigateToHome` |
| `XxxViewModel` | Turns intents into new state and effects. Extends `AppViewModel` | |

### Rules

1. **The Content composable takes only `state` and `onIntent`.** No other lambdas. That makes it stateless and easy to preview.
2. **Every user action is an Intent**, including "back" and "go to sign up". The ViewModel decides *whether* and *where* to go and sends an Effect.
3. **The NavHost decides *how* to navigate** (`popUpTo`, `launchSingleTop`). The ViewModel never sees a `NavController`.
4. **The Screen composable is glue only**: collect state, collect effects, call Content. No `when` on intents, no logic.
5. **Errors in state are `AppUiText?`**, never `@StringRes Int?` or `String?`. The ViewModel has no `Context`, and `AppUiText` carries format args.
6. **Guard async work in state, synchronously, before `launch`**: `if (currentState.isLoading) return` then `setState { copy(isLoading = true) }`. A double tap can't start the work twice.
7. **Send navigation effects only after the work finishes** (save first, then navigate).

### Naming

| Kind | Style | Examples |
|---|---|---|
| Intent | What the user did, past tense | `EmailChanged`, `LoginClicked`, `BackClicked`, `PageChanged` |
| Effect | What should happen | `NavigateToHome`, `NavigateBack` |
| Screen callback | `onNavigateToX` / `onNavigateBack` | `onNavigateToOnboarding` |
| Package | lowercase | `logic.signup`, `components.signup` |

---

## 2. Core Setup

### Step 2.1: AppViewModel

Same idea as [Error Handling §11](error_handling_architecture.md#11-mvi-base-mviviewmodel-and-collecteffect) (called `MviViewModel` / `CollectEffect` there). Location and names follow the [Project Structure guide, Step 8.3](project_structure_guide.md#step-83-corecommonlogic--corecommonmvi).

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

**Why a `Channel` and not a `SharedFlow`?** A `SharedFlow` with no collector drops the value. If the effect is sent while the screen is in the background, the navigation would be lost. A `Channel` keeps it until the screen comes back.

### Step 2.2: AppCollectEffect

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

It replaces the hand-written `LaunchedEffect` + `rememberUpdatedState` block in today's `SignUpScreen`.

### Step 2.3: Fix `AppUiText.asString()` (bug 3)

The Compose version ignores `args`. Change one line.

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
    is AppUiText.Res -> stringResource(id, *args.toTypedArray()) // was: stringResource(id)
    is AppUiText.Raw -> value
}

fun AppUiText.asString(context: Context): String = when (this) {
    is AppUiText.Res -> context.getString(id, *args.toTypedArray())
    is AppUiText.Raw -> value
}
```

Now `ValidationError.PasswordTooShort(8).toUiText().asString()` gives "Password must be at least 8 characters". The screens no longer pass `AppValidationConstants.MIN_PASSWORD_LENGTH` themselves.

### Step 2.4: AuthFormValidator

Login and Sign Up check the same fields. Write the rules once. This is the same file as [Firebase guide Step 3.4](../auth/firebase_auth_integration_guide.md#step-34-authformvalidator). Write it now; the Firebase guide reuses it.

**File:** `features/auth/domain/validation/AuthFormValidator.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.validation

import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.core.utils.constants.AppValidationConstants.MIN_PASSWORD_LENGTH
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

/** Pure Kotlin (no `android.util.Patterns`), so it runs in plain JVM unit tests. */
object AuthFormValidator {

    fun validateEmail(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EmailEmpty
        !email.trim().isValidEmail() -> ValidationError.EmailInvalid
        else -> null
    }

    /** Sign up: enforce the password policy. */
    fun validateNewPassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.PasswordEmpty
        !password.isValidPassword() -> ValidationError.PasswordTooShort(MIN_PASSWORD_LENGTH)
        else -> null
    }

    /** Login: only require a value. Older accounts may predate the current policy. */
    fun validateExistingPassword(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.PasswordEmpty else null

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationError? = when {
        confirmPassword.isEmpty() -> ValidationError.ConfirmPasswordEmpty
        password != confirmPassword -> ValidationError.PasswordsMismatch
        else -> null
    }
}
```

`ValidationError` and `ValidationError.toUiText()` already exist in `core/error/`. Nothing to change there.

---

## 3. Language Selection

**Changes:** `Event` → `Intent`, new `Effect`, "Continue" goes through the ViewModel and saves first (bug 5), no back arrow on the first screen (bug 6).

```
features/language/presentation/logic/
├── LanguageSelectionUiState.kt     (edit: + isSaving)
├── LanguageSelectionIntent.kt      (new, replaces LanguageSelectionEvent.kt)
├── LanguageSelectionEffect.kt      (new)
└── LanguageSelectionViewModel.kt   (rewrite)
```

### Step 3.1: LanguageSelectionUiState

**File:** `features/language/presentation/logic/LanguageSelectionUiState.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.logic

import com.example.dukkanapp.features.language.presentation.model.LanguageUiModel

data class LanguageSelectionUiState(
    val query: String = "",
    val selectedCode: String = "en",
    val languages: List<LanguageUiModel> = emptyList(),
    val isSaving: Boolean = false,
) {
    val filteredLanguages: List<LanguageUiModel>
        get() = if (query.isBlank()) languages
        else languages.filter { it.displayName.contains(query, ignoreCase = true) }

    val selectedLanguage: LanguageUiModel? get() = languages.firstOrNull { it.code == selectedCode }
}
```

### Step 3.2: LanguageSelectionIntent and LanguageSelectionEffect

**File:** `features/language/presentation/logic/LanguageSelectionIntent.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.logic

sealed interface LanguageSelectionIntent {
    data class QueryChanged(val query: String) : LanguageSelectionIntent
    data class LanguageSelected(val code: String) : LanguageSelectionIntent
    data object ContinueClicked : LanguageSelectionIntent
}
```

**File:** `features/language/presentation/logic/LanguageSelectionEffect.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.logic

sealed interface LanguageSelectionEffect {
    data object NavigateToOnboarding : LanguageSelectionEffect
}
```

Delete `LanguageSelectionEvent.kt`.

### Step 3.3: LanguageSelectionViewModel

**File:** `features/language/presentation/logic/LanguageSelectionViewModel.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.features.language.domain.usecase.GetSupportedLanguagesUseCase
import com.example.dukkanapp.features.language.domain.usecase.ObserveSelectedLanguageUseCase
import com.example.dukkanapp.features.language.domain.usecase.SelectLanguageUseCase
import com.example.dukkanapp.features.language.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    getSupportedLanguages: GetSupportedLanguagesUseCase,
    private val observeSelectedLanguage: ObserveSelectedLanguageUseCase,
    private val selectLanguage: SelectLanguageUseCase,
) : AppViewModel<LanguageSelectionUiState, LanguageSelectionIntent, LanguageSelectionEffect>(
    LanguageSelectionUiState(languages = getSupportedLanguages().map { it.toUiModel() })
) {

    init {
        viewModelScope.launch {
            observeSelectedLanguage().collect { code -> setState { copy(selectedCode = code) } }
        }
    }

    override fun onIntent(intent: LanguageSelectionIntent) {
        when (intent) {
            is LanguageSelectionIntent.QueryChanged -> setState { copy(query = intent.query) }
            is LanguageSelectionIntent.LanguageSelected -> onLanguageSelected(intent.code)
            LanguageSelectionIntent.ContinueClicked -> onContinueClicked()
        }
    }

    private fun onLanguageSelected(code: String) {
        if (code == currentState.selectedCode) return
        setState { copy(selectedCode = code) } // instant feedback, before the locale switch
        viewModelScope.launch { selectLanguage(code) }
    }

    /** Saves the shown language even if the user never tapped a row (the platform default). */
    private fun onContinueClicked() {
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            selectLanguage(currentState.selectedCode)
            setState { copy(isSaving = false) }
            sendEffect(LanguageSelectionEffect.NavigateToOnboarding)
        }
    }
}
```

**Notes:**
- `getSupportedLanguages` has no `private val`: it's only needed to build the initial state.
- Selecting a language calls `AppCompatDelegate.setApplicationLocales`, which recreates the Activity. The ViewModel survives that, so the state and any pending effect survive too.

### Step 3.4: LanguageSelectionScreen

**File:** `features/language/presentation/screens/LanguageSelectionScreen.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.language.presentation.components.LanguageSelectionScreenContent
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionEffect
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionViewModel

@Composable
fun LanguageSelectionScreen(
    onNavigateToOnboarding: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            LanguageSelectionEffect.NavigateToOnboarding -> onNavigateToOnboarding()
        }
    }

    LanguageSelectionScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

### Step 3.5: LanguageSelectionScreenContent

Changes: `onEvent` → `onIntent`, `onBack` / `onContinue` removed, Continue shows loading.

**File:** `features/language/presentation/components/LanguageSelectionScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.language.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionIntent
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionUiState

@Composable
fun LanguageSelectionScreenContent(
    state: LanguageSelectionUiState,
    onIntent: (LanguageSelectionIntent) -> Unit,
) {
    AppScaffold(
        // First screen of the app: no back arrow (onNavigateBack = null).
        bottomBar = {
            AppPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = { onIntent(LanguageSelectionIntent.ContinueClicked) },
                isLoading = state.isSaving,
                modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceLg),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .clearFocusOnTap()
                .padding(horizontal = AppDimens.screenHorizontalPadding),
        ) {
            item {
                Text(
                    text = stringResource(R.string.language_selection_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item { Spacer(Modifier.height(AppDimens.spaceSm)) }
            item {
                Text(
                    text = stringResource(R.string.language_selection_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Spacer(Modifier.height(AppDimens.spaceLg)) }

            item {
                Text(
                    text = stringResource(R.string.language_selection_you_selected),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item { Spacer(Modifier.height(AppDimens.spaceSm)) }
            state.selectedLanguage?.let { selected ->
                item(key = "selected_${selected.code}") {
                    LanguageItemCard(
                        language = selected,
                        isSelected = true,
                        isHighlightedStyle = true,
                        onClick = {},
                    )
                }
            }
            item { Spacer(Modifier.height(AppDimens.spaceLg)) }

            item {
                Text(
                    text = stringResource(R.string.language_selection_all_languages),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item { Spacer(Modifier.height(AppDimens.spaceSm)) }
            item {
                LanguageSearchField(
                    query = state.query,
                    onQueryChange = { onIntent(LanguageSelectionIntent.QueryChanged(it)) },
                )
            }

            items(state.filteredLanguages, key = { it.code }) { language ->
                LanguageItemCard(
                    language = language,
                    isSelected = language.code == state.selectedCode,
                    isHighlightedStyle = false,
                    onClick = { onIntent(LanguageSelectionIntent.LanguageSelected(language.code)) },
                    modifier = Modifier.padding(vertical = AppDimens.space2Xs),
                )
            }
        }
    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "French", locale = "fr")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun LanguageSelectionScreenPreview() {
    AppTheme {
        LanguageSelectionScreenContent(
            state = LanguageSelectionUiState(
                selectedCode = "ar",
                languages = emptyList(), // swap in fake LanguageUiModel list to preview real rows
            ),
            onIntent = {},
        )
    }
}
```

> Later, if Settings opens this screen too, add `val showBack: Boolean` to the state plus a `BackClicked` intent and a `NavigateBack` effect. Don't add a back arrow that does nothing.

---

## 4. Onboarding

**Changes:** `Event` → `Intent`, new `Effect`, save *then* navigate (bug 4), the ViewModel decides Next vs Get Started, "Log In" goes to Login (bug 7), empty-list crash (bug 9), pager collector (bug 10).

```
features/onboarding/presentation/logic/
├── OnboardingUiState.kt       (edit: + isFinishing)
├── OnboardingIntent.kt        (new, replaces OnboardingEvent.kt)
├── OnboardingEffect.kt        (new)
└── OnboardingViewModel.kt     (rewrite)
```

### Step 4.1: OnboardingUiState

**File:** `features/onboarding/presentation/logic/OnboardingUiState.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel

data class OnboardingUiState(
    val currentPage: Int = 0,
    val pages: List<OnboardingPageUiModel> = emptyList(),
    val isFinishing: Boolean = false,
) {
    val isLastPage: Boolean get() = pages.isNotEmpty() && currentPage >= pages.lastIndex

    @get:StringRes
    val primaryActionRes: Int get() = if (isLastPage) R.string.action_get_started else R.string.action_next
}
```

### Step 4.2: OnboardingIntent and OnboardingEffect

One `PrimaryActionClicked` instead of `OnNextClicked` + `OnGetStartedClick`: the button doesn't need to know which page is the last one. The ViewModel does.

**File:** `features/onboarding/presentation/logic/OnboardingIntent.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingIntent {
    data class PageChanged(val page: Int) : OnboardingIntent

    /** "Next" on pages 1..n-1, "Get Started" on the last page. */
    data object PrimaryActionClicked : OnboardingIntent
    data object LoginClicked : OnboardingIntent
}
```

**File:** `features/onboarding/presentation/logic/OnboardingEffect.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingEffect {
    data object NavigateToAuthOptions : OnboardingEffect
    data object NavigateToLogin : OnboardingEffect
}
```

Delete `OnboardingEvent.kt`.

### Step 4.3: OnboardingViewModel

**File:** `features/onboarding/presentation/logic/OnboardingViewModel.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.features.onboarding.domain.usecase.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.domain.usecase.SetOnboardingCompletedUseCase
import com.example.dukkanapp.features.onboarding.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    getOnboardingPages: GetOnboardingPagesUseCase,
    private val setOnboardingCompleted: SetOnboardingCompletedUseCase,
) : AppViewModel<OnboardingUiState, OnboardingIntent, OnboardingEffect>(
    OnboardingUiState(pages = getOnboardingPages().map { it.toUiModel() })
) {

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.PageChanged -> onPageChanged(intent.page)
            OnboardingIntent.PrimaryActionClicked -> onPrimaryActionClicked()
            OnboardingIntent.LoginClicked -> finish(OnboardingEffect.NavigateToLogin)
        }
    }

    private fun onPageChanged(page: Int) {
        if (currentState.pages.isEmpty()) return // coerceIn(0, -1) would throw
        setState { copy(currentPage = page.coerceIn(0, pages.lastIndex)) }
    }

    private fun onPrimaryActionClicked() {
        if (currentState.isLastPage) finish(OnboardingEffect.NavigateToAuthOptions)
        else setState { copy(currentPage = currentPage + 1) }
    }

    /** Save first, navigate after: the "seen" flag is on disk before the screen leaves. */
    private fun finish(destination: OnboardingEffect) {
        if (currentState.isFinishing) return
        setState { copy(isFinishing = true) }
        viewModelScope.launch {
            setOnboardingCompleted()
            sendEffect(destination)
        }
    }
}
```

**Why no `if (current.currentPage == safePage) current` check any more?** `StateFlow` already ignores a value equal to the current one. A `copy` with the same page is an equal `data class`, so nothing recomposes.

### Step 4.4: OnboardingScreen

No more `when` on events. The Screen is glue only.

**File:** `features/onboarding/presentation/screens/OnboardingScreen.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.onboarding.presentation.components.OnboardingScreenContent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingEffect
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onNavigateToAuthOptions: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            OnboardingEffect.NavigateToAuthOptions -> onNavigateToAuthOptions()
            OnboardingEffect.NavigateToLogin -> onNavigateToLogin()
        }
    }

    OnboardingScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

### Step 4.5: OnboardingScreenContent

Changes: `onEvent` → `onIntent`, one button intent, button shows loading while saving, pager collector keyed on `pagerState`.

**File:** `features/onboarding/presentation/components/OnboardingScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.common.components.pager.AppHorizontalPager
import com.example.dukkanapp.core.common.components.pager.AppPagerIndicator
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingIntent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingUiState
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OnboardingScreenContent(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    if (state.pages.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pages.size },
    )
    val currentOnIntent by rememberUpdatedState(onIntent)

    // Direction 1: pager -> ViewModel (user swipes). One collector for the screen's lifetime.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> currentOnIntent(OnboardingIntent.PageChanged(page)) }
    }

    // Direction 2: ViewModel -> pager (Next button tapped)
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.onPrimary),
    ) {
        // ZONE 1: illustration + dots, plain background, swipeable
        Box(modifier = Modifier.weight(1f)) {
            AppHorizontalPager(
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                OnboardingIllustration(page = state.pages[page])
            }

            AppPagerIndicator(
                pageCount = state.pages.size,
                currentPage = state.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.spaceLg),
            )
        }

        // ZONE 2: rounded card, fixed, NOT inside the pager
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .border(
                    width = AppDimens.borderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(topStart = AppDimens.radiusLg, topEnd = AppDimens.radiusLg),
                )
                .navigationBarsPadding()
                .padding(AppDimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(state.pages[state.currentPage].titleRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(AppDimens.spaceSm))
            Text(
                text = stringResource(state.pages[state.currentPage].subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppDimens.spaceLg))
            AppPrimaryButton(
                text = stringResource(state.primaryActionRes),
                onClick = { onIntent(OnboardingIntent.PrimaryActionClicked) },
                isLoading = state.isFinishing,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.spaceMd))
            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = { onIntent(OnboardingIntent.LoginClicked) },
            )
        }
    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "French", locale = "fr")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun OnboardingScreenPreview() {
    AppTheme {
        OnboardingScreenContent(
            state = OnboardingUiState(
                pages = listOf(
                    OnboardingPageUiModel(
                        id = "discover",
                        titleRes = R.string.onboarding_discover_title,
                        subtitleRes = R.string.onboarding_discover_subtitle,
                        imageRes = R.drawable.img_onboarding_discover,
                    ),
                    OnboardingPageUiModel(
                        id = "checkout",
                        titleRes = R.string.onboarding_checkout_title,
                        subtitleRes = R.string.onboarding_checkout_subtitle,
                        imageRes = R.drawable.img_onboarding_checkout,
                    ),
                    OnboardingPageUiModel(
                        id = "shopping",
                        titleRes = R.string.onboarding_shopping_title,
                        subtitleRes = R.string.onboarding_shopping_subtitle,
                        imageRes = R.drawable.img_onboarding_shopping,
                    ),
                ),
                currentPage = 0,
            ),
            onIntent = {},
        )
    }
}
```

**Why `rememberUpdatedState`?** `LaunchedEffect(pagerState)` starts once. Without it, the collector keeps the `onIntent` from the first composition forever. Here that would still work (it's always `viewModel::onIntent`), but the pattern stays correct if a parent ever passes a different lambda.

---

## 5. Login

**Changes:** full migration: Intent + Effect files are new, errors become `AppUiText?`, `isLoading` + double-tap guard, login only requires a password (bug 8), all navigation through effects.

```
features/auth/presentation/logic/login/
├── LoginUiState.kt       (rewrite)
├── LoginIntent.kt        (new)
├── LoginEffect.kt        (new)
└── LoginViewModel.kt     (rewrite)
```

### Step 5.1: LoginUiState

**File:** `features/auth/presentation/logic/login/LoginUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()

    // Login only requires a value (see AuthFormValidator.validateExistingPassword).
    val isValidPassword: Boolean get() = passwordError == null && password.isNotEmpty()
}
```

### Step 5.2: LoginIntent and LoginEffect

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

**File:** `features/auth/presentation/logic/login/LoginEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

sealed interface LoginEffect {
    data object NavigateBack : LoginEffect
    data object NavigateToHome : LoginEffect
    data object NavigateToSignUp : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
}
```

### Step 5.3: LoginViewModel

**File:** `features/auth/presentation/logic/login/LoginViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.login

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    // TODO(firebase): private val signIn: SignInUseCase, see firebase_auth_integration_guide.md §7
) : AppViewModel<LoginUiState, LoginIntent, LoginEffect>(LoginUiState()) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            // Clear the field's error as soon as the user edits it.
            is LoginIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.password, passwordError = null) }
            is LoginIntent.RememberMeChanged -> setState { copy(rememberMe = intent.checked) }
            LoginIntent.LoginClicked -> onLoginClicked()
            LoginIntent.ForgotPasswordClicked -> sendEffect(LoginEffect.NavigateToForgotPassword)
            LoginIntent.SignUpClicked -> sendEffect(LoginEffect.NavigateToSignUp)
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
            setState { copy(emailError = emailError?.toUiText(), passwordError = passwordError?.toUiText()) }
            return
        }

        // Set synchronously, before launch, so a double tap can't start two logins.
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            // TODO(firebase): signIn(email, current.password).onSuccess { ... }.onFailure { ... }
            setState { copy(isLoading = false) }
            sendEffect(LoginEffect.NavigateToHome)
        }
    }
}
```

**Why does `ForgotPasswordClicked` go through the ViewModel when it only navigates?** Rule 2. Today it's one line. Tomorrow you might pass the typed email to the reset screen or block it while `isLoading`, and the screen won't change.

### Step 5.4: LoginScreen

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
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            LoginEffect.NavigateBack -> onNavigateBack()
            LoginEffect.NavigateToHome -> onNavigateToHome()
            LoginEffect.NavigateToSignUp -> onNavigateToSignUp()
            LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
        }
    }

    LoginScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

### Step 5.5: LoginScreenContent

Changes: 8 lambdas → `onIntent`, errors use `asString()` (no more `MIN_PASSWORD_LENGTH` here), button shows `isLoading`, preview added.

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
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.checkbox.AppCheckBox
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.dividers.AppDivider
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginIntent
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        onNavigateBack = { onIntent(LoginIntent.BackClicked) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
        ) {
            AppAuthHeader(
                title = stringResource(R.string.login_title),
                subTitle = stringResource(R.string.login_subtitle),
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            AppTextFiled(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm),
                    )
                },
                placeHolder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailError?.asString(),
                isSuccess = state.isValidEmail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
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
                        modifier = Modifier.size(AppDimens.iconSm),
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(LoginIntent.LoginClicked)
                    },
                ),
            )
            Spacer(Modifier.height(AppDimens.spaceSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
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
                onActionClick = { onIntent(LoginIntent.SignUpClicked) },
            )
        }
    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        LoginScreenContent(state = LoginUiState(), onIntent = {})
    }
}
```

> `checkbox` (lowercase) comes from the [Project Structure guide](project_structure_guide.md#step-81-fix-folder-and-package-names), Step 8.1. Do that guide first.

---

## 6. Sign Up

**Changes:** move to `AppViewModel`, private handlers, `AppUiText?` errors, bugs 1, 2 and 12, all navigation through effects. Intent and Effect files already exist and grow.

```
features/auth/presentation/logic/signup/
├── SignUpUiState.kt       (rewrite)
├── SignUpIntent.kt        (edit: + 3 intents)
├── SignUpEffect.kt        (edit: + 3 effects)
└── SignUpViewModel.kt     (rewrite)
```

### Step 6.1: SignUpUiState

Bug 2 fixed: `== null`, not `!= null`.

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
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val confirmPasswordError: AppUiText? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean
        get() = confirmPasswordError == null && confirmPassword.isNotEmpty() && password == confirmPassword
}
```

### Step 6.2: SignUpIntent and SignUpEffect

**File:** `features/auth/presentation/logic/signup/SignUpIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object ForgotPasswordClicked : SignUpIntent
    data object LoginClicked : SignUpIntent
    data object BackClicked : SignUpIntent
}
```

**File:** `features/auth/presentation/logic/signup/SignUpEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpEffect {
    data object NavigateBack : SignUpEffect

    /** Becomes NavigateToEmailVerification with Firebase (firebase guide §6). */
    data object NavigateToHome : SignUpEffect
    data object NavigateToLogin : SignUpEffect
    data object NavigateToForgotPassword : SignUpEffect
}
```

### Step 6.3: SignUpViewModel

Bug 1 fixed by `||` on one line. Validation moves to `AuthFormValidator`.

**File:** `features/auth/presentation/logic/signup/SignUpViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    // TODO(firebase): private val signUp: SignUpUseCase, see firebase_auth_integration_guide.md §6
) : AppViewModel<SignUpUiState, SignUpIntent, SignUpEffect>(SignUpUiState()) {

    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> setState { copy(email = intent.email, emailError = null) }
            is SignUpIntent.PasswordChanged -> setState {
                copy(
                    password = intent.password,
                    passwordError = null,
                    confirmPasswordError = liveMismatchError(intent.password, confirmPassword),
                )
            }
            is SignUpIntent.ConfirmPasswordChanged -> setState {
                copy(
                    confirmPassword = intent.confirmPassword,
                    confirmPasswordError = liveMismatchError(password, intent.confirmPassword),
                )
            }
            SignUpIntent.SignUpClicked -> onSignUpClicked()
            SignUpIntent.ForgotPasswordClicked -> sendEffect(SignUpEffect.NavigateToForgotPassword)
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
        val confirmError = AuthFormValidator.validateConfirmPassword(current.password, current.confirmPassword)

        if (emailError != null || passwordError != null || confirmError != null) {
            setState {
                copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText(),
                    confirmPasswordError = confirmError?.toUiText(),
                )
            }
            return
        }

        // Set synchronously, before launch, so a double tap can't start two sign-ups.
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            // TODO(firebase): signUp(email, current.password).onSuccess { ... }.onFailure(::showFailure)
            setState { copy(isLoading = false) }
            sendEffect(SignUpEffect.NavigateToHome)
        }
    }

    /** Shows "Passwords do not match" while typing, but only once the confirm field has text. */
    private fun liveMismatchError(password: String, confirmPassword: String): AppUiText? =
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            ValidationError.PasswordsMismatch.toUiText()
        } else {
            null
        }
}
```

The old `delay(2000)` fake request is gone. The loading state now covers only real work, which arrives with the Firebase guide.

### Step 6.4: SignUpScreen

`AppCollectEffect` replaces the manual `LaunchedEffect` + `rememberUpdatedState`.

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
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SignUpEffect.NavigateBack -> onNavigateBack()
            SignUpEffect.NavigateToHome -> onNavigateToHome()
            SignUpEffect.NavigateToLogin -> onNavigateToLogin()
            SignUpEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

### Step 6.5: SignUpScreenContent

Changes: package fixed to `components.signup` (bug 12), 3 navigation lambdas → intents, errors use `asString()`, preview added.

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.dividers.AppDivider
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.config.theme.AppTheme
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
        onNavigateBack = { onIntent(SignUpIntent.BackClicked) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
        ) {
            AppAuthHeader(
                title = stringResource(R.string.signup_title),
                subTitle = stringResource(R.string.signup_subtitle),
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            AppTextFiled(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm),
                    )
                },
                placeHolder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailError?.asString(),
                isSuccess = state.isValidEmail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
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
                        modifier = Modifier.size(AppDimens.iconSm),
                    )
                },
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
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
                        modifier = Modifier.size(AppDimens.iconSm),
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(SignUpIntent.SignUpClicked)
                    },
                ),
            )
            Spacer(Modifier.height(AppDimens.spaceSm))

            AppTextButton(
                onClick = { onIntent(SignUpIntent.ForgotPasswordClicked) },
                text = stringResource(R.string.login_forgot_password),
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(Modifier.height(AppDimens.size3Xs))

            AppPrimaryButton(
                onClick = { onIntent(SignUpIntent.SignUpClicked) },
                text = stringResource(R.string.signup_button),
                isLoading = state.isLoading,
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

@Preview(name = "English", locale = "en")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreenContent(state = SignUpUiState(), onIntent = {})
    }
}
```

> **Design question, not a code bug:** a "Forgot password?" link on the Sign Up screen is unusual (the user has no account yet). If you remove it, delete `ForgotPasswordClicked`, `NavigateToForgotPassword`, and the `onNavigateToForgotPassword` parameter too.

---

## 7. Navigation

### Step 7.1: AppRoutes (bug 11)

`LoginScreen` and `SignUpScreen` now implement `AppRoute`.

**File:** `navigation/AppRoute.kt`

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

### Step 7.2: AppNavHost

The NavHost only maps effects to `navController` calls.

**File:** `navigation/AppNavHost.kt`

```kotlin
package com.example.dukkanapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.AuthOptionsScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.auth.presentation.screens.SignUpScreen
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
        startDestination = startDestination,
    ) {
        composable<AppRoute.LanguageSelectionScreen> {
            LanguageSelectionScreen(
                onNavigateToOnboarding = {
                    navController.navigate(AppRoute.OnboardingScreen) {
                        popUpTo(AppRoute.LanguageSelectionScreen) { inclusive = true }
                    }
                },
            )
        }

        composable<AppRoute.OnboardingScreen> {
            OnboardingScreen(
                onNavigateToAuthOptions = {
                    navController.navigate(AppRoute.AuthOptionsScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    // Auth Options goes under Login, so "back" from Login lands there, not outside the app.
                    navController.navigate(AppRoute.AuthOptionsScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                    navController.navigate(AppRoute.LoginScreen)
                },
            )
        }

        composable<AppRoute.AuthOptionsScreen> {
            AuthOptionsScreen(
                onGoogleClick = {},
                onAppleClick = {},
                onEmailClick = { navController.navigate(AppRoute.SignUpScreen) },
                onAlreadyHaveAccountClick = { navController.navigate(AppRoute.LoginScreen) },
            )
        }

        composable<AppRoute.HomeScreen> {
            // TODO: HomeScreen
        }

        composable<AppRoute.LoginScreen> {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateToHome() },
                onNavigateToSignUp = {
                    navController.navigate(AppRoute.SignUpScreen) {
                        popUpTo(AppRoute.LoginScreen) { inclusive = true }
                        launchSingleTop = true // no second copy if it's already on top
                    }
                },
                onNavigateToForgotPassword = { /* TODO: ForgotPassword route */ },
            )
        }

        composable<AppRoute.SignUpScreen> {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateToHome() },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.SignUpScreen) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToForgotPassword = { /* TODO: ForgotPassword route */ },
            )
        }
    }
}

/** Home becomes the only screen: "back" from Home exits the app instead of returning to Login. */
private fun NavController.navigateToHome() {
    navigate(AppRoute.HomeScreen) {
        popUpTo(graph.id) { inclusive = true }
    }
}
```

**Back stack after each flow:**

| Flow | Back stack | Back from top goes to |
|---|---|---|
| Language → Onboarding → Get Started | `AuthOptions` | exits app |
| Onboarding → "Log In" | `AuthOptions, Login` | Auth Options |
| Auth Options → Email → Sign Up → "Log in" | `AuthOptions, Login` | Auth Options |
| Login → Home | `Home` | exits app |

---

## 8. Cleanup

| Action | File |
|---|---|
| Delete | `features/language/presentation/logic/LanguageSelectionEvent.kt` |
| Delete | `features/onboarding/presentation/logic/OnboardingEvent.kt` |
| Delete `GreetingPreview` (bug 13) and its unused imports | `MainActivity.kt` |
| Fix package line to `...components.signup` | `features/auth/presentation/components/signup/SignUpScreenContent.kt` |

`GreetingPreview` renders `LanguageSelectionScreen`, which calls `hiltViewModel()`. Previews can't create Hilt ViewModels. The preview inside `LanguageSelectionScreenContent.kt` already covers that screen.

Then search the project for leftovers. Each should return **no results**:

```
onEvent(
LanguageSelectionEvent
OnboardingEvent
MutableStateFlow          (only inside AppViewModel and AppStartViewModel)
@StringRes val emailError
components.SignUp
```

> `AppStartViewModel` is not a screen: it has no intents and no effects, only one value. It stays a plain `ViewModel`.

---

## 9. Implementation Checklist

The order keeps the project compiling after every step. **Each feature's screen and its NavHost block change together**, because the screen's parameters change.

**Core**
- [ ] `core/common/mvi/AppViewModel.kt` (Step 2.1)
- [ ] `core/common/mvi/AppCollectEffect.kt` (Step 2.2)
- [ ] `AppUiText.asString()` passes `args` (Step 2.3)
- [ ] `features/auth/domain/validation/AuthFormValidator.kt` (Step 2.4)

**Language**
- [ ] `LanguageSelectionUiState` + `isSaving`
- [ ] `LanguageSelectionIntent`, `LanguageSelectionEffect`
- [ ] `LanguageSelectionViewModel`
- [ ] `LanguageSelectionScreen` + `LanguageSelectionScreenContent`
- [ ] NavHost language block, delete `GreetingPreview`, delete `LanguageSelectionEvent.kt`

**Onboarding**
- [ ] `OnboardingUiState` + `isFinishing`
- [ ] `OnboardingIntent`, `OnboardingEffect`
- [ ] `OnboardingViewModel`
- [ ] `OnboardingScreen` + `OnboardingScreenContent`
- [ ] NavHost onboarding block, delete `OnboardingEvent.kt`

**Login**
- [ ] `LoginUiState`, `LoginIntent`, `LoginEffect`
- [ ] `LoginViewModel`
- [ ] `LoginScreen` + `LoginScreenContent`
- [ ] NavHost login block

**Sign Up**
- [ ] `SignUpUiState`, `SignUpIntent`, `SignUpEffect`
- [ ] `SignUpViewModel`
- [ ] `SignUpScreen` + `SignUpScreenContent` (package fix)
- [ ] NavHost sign-up block

**Finish**
- [ ] `AppRoutes`: `LoginScreen : AppRoute`, `SignUpScreen : AppRoute`
- [ ] `navigateToHome()` helper
- [ ] Leftover search from §8 is clean
- [ ] Build passes, manual tests below pass

---

## 10. Manual Testing

Clear app data before the first-launch tests (Settings → Apps → Dukkan → Storage → Clear data).

### Language
| # | Steps | Expected |
|---|---|---|
| L1 | First launch, tap **Continue** without tapping a language. Kill and reopen the app | Opens on Onboarding, not on Language (bug 5) |
| L2 | Tap Arabic | UI switches to Arabic, RTL, Arabic stays highlighted |
| L3 | Type "fr" in search | Only Français is listed |
| L4 | Look at the top bar | No back arrow (bug 6) |
| L5 | Double-tap Continue fast | Onboarding opens once |

### Onboarding
| # | Steps | Expected |
|---|---|---|
| O1 | Swipe through pages | Dots and text follow the swipe |
| O2 | Tap **Next** twice | Pager animates forward, button turns into **Get Started** |
| O3 | Tap **Get Started**, kill the app right away, reopen | Opens on Auth Options (bug 4) |
| O4 | Tap **Log In** | Login opens. Back goes to Auth Options (bug 7) |
| O5 | Double-tap **Get Started** | Auth Options opens once |

### Login
| # | Steps | Expected |
|---|---|---|
| G1 | Tap **Log In** with empty fields | "Please enter your email" + "Please enter your password" |
| G2 | Enter `abc` as email, tap **Log In** | "Enter a valid email address" |
| G3 | Type in the email field after G2 | Error disappears |
| G4 | Valid email + 3-char password, tap **Log In** | No password error (login only requires a value) |
| G5 | Valid email + password, tap **Log In** | Button shows loading, then Home. Back exits the app |
| G6 | Tap **Sign up** | Sign Up opens. Back goes to Auth Options |
| G7 | Switch to Arabic and repeat G1 | Errors in Arabic |

### Sign Up
| # | Steps | Expected |
|---|---|---|
| S1 | Valid email, password `1234`, tap **Create account** | "Password must be at least 8 characters" (a number, not `%1$d`: bug 3) |
| S2 | Valid email + valid password, empty confirm | "Please confirm your password". Does **not** navigate (bug 1) |
| S3 | Type a different confirm password | "Passwords do not match" while typing |
| S4 | All fields valid | Green ticks on all three fields (bug 2) |
| S5 | All valid, tap **Create account** | Loading, then Home |
| S6 | Tap **Log in** in the footer | Login opens and replaces Sign Up |
