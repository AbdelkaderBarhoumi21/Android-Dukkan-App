# SignUp Screen: UI Components, State, Intent, Effect, and ViewModel

Dukkan App (`com.example.dukkanapp`). Pattern: **MVI** (State + Intent + Effect), Hilt ViewModel.

This version replaces the old `onSuccess` callback and the separate `onEmailChanged / onPasswordChanged / ...` functions with:

- **`SignUpIntent`**: everything the user can do (one entry point: `onIntent`).
- **`SignUpUiState`**: everything the screen shows (single source of truth).
- **`SignUpEffect`**: one-time events (navigation) sent from the ViewModel to the screen.

```
User action → SignUpIntent → ViewModel.onIntent() → new SignUpUiState → Screen re-renders
                                      └─────────→ SignUpEffect (one-time) → Screen navigates
```

It stays aligned with the `Login` flow: `@StringRes` for errors, visibility toggling handled inside `AppPasswordTextField`, and validation through the core extensions `isValidEmail()` / `isValidPassword()`.

## Folder structure

Same folders as before. Two new files in `logic/SignUp/`.

```
features/auth/presentation/
├── logic/
│   └── SignUp/
│       ├── SignUpUiState.kt
│       ├── SignUpIntent.kt         (new)
│       ├── SignUpEffect.kt         (new)
│       └── SignUpViewModel.kt
├── components/
│   └── SignUp/
│       └── SignUpScreenContent.kt
└── screens/
    └── SignUpScreen.kt
```

---

## 1. String resources

Unchanged. Keys prefixed with `signup_`. Ensure these exist in `values/strings.xml`, `values-ar/string-ar.xml`, and `values-fr/string-fr.xml`.

```xml
<!-- Sign up -->
<string name="signup_title">Create your account</string>
<string name="signup_subtitle">Join Dukkan to discover great deals and enjoy a faster checkout.</string>
<string name="signup_email_placeholder">Email address</string>
<string name="signup_password_placeholder">Password</string>
<string name="signup_confirm_password_placeholder">Confirm password</string>
<string name="signup_button">Create account</string>
<string name="signup_loading">Creating account…</string>

<!-- Errors -->
<string name="signup_error_email_empty">Please enter your email</string>
<string name="signup_error_email_invalid">Enter a valid email address</string>
<string name="signup_error_password_empty">Please enter a password</string>
<string name="signup_error_password_short">Password must be at least %1$d characters</string>
<string name="signup_error_password_mismatch">Passwords do not match</string>
<string name="signup_error_confirm_password_empty">Please confirm your password</string>
<string name="signup_error_email_taken">This email is already registered</string>
<string name="signup_error_network">No internet connection. Please try again.</string>
```

---

## 2. UiState

`logic/SignUp/SignUpUiState.kt`

Unchanged. It stores `@StringRes` for errors directly, and has no `isVisible` fields because `AppPasswordTextField` manages its own visibility state.

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.SignUp

import androidx.annotation.StringRes
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean get() = confirmPasswordError == null &&
            confirmPassword.isNotEmpty() && password == confirmPassword
}
```

---

## 3. Intent (new)

`logic/SignUp/SignUpIntent.kt`

Every action the user can perform on this screen.

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.SignUp

sealed interface SignUpIntent {
    data class EmailChanged(val value: String) : SignUpIntent
    data class PasswordChanged(val value: String) : SignUpIntent
    data class ConfirmPasswordChanged(val value: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
}
```

---

## 4. Effect (new)

`logic/SignUp/SignUpEffect.kt`

One-time events. This replaces the old `onSuccess: () -> Unit` callback. Add more later (for example `ShowMessage`) when you connect Firebase Auth.

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.SignUp

sealed interface SignUpEffect {
    data object NavigateToHome : SignUpEffect
}
```

---

## 5. ViewModel

`logic/SignUp/SignUpViewModel.kt`

One public function: `onIntent`. The old logic is kept, but moved into private functions. Navigation is sent as an effect through a `Channel`, so it is delivered exactly once even if the screen is recreated.

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.SignUp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** Single entry point: the screen only sends intents. */
    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> onEmailChanged(intent.value)
            is SignUpIntent.PasswordChanged -> onPasswordChanged(intent.value)
            is SignUpIntent.ConfirmPasswordChanged -> onConfirmPasswordChanged(intent.value)
            SignUpIntent.SignUpClicked -> onSignUpClicked()
        }
    }

    private fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, emailError = null) }
    }

    private fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty() && password != it.confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else null
            )
        }
    }

    private fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = if (confirmPassword.isNotEmpty() && it.password != confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else null
            )
        }
    }

    private fun onSignUpClicked() {
        val currentState = _state.value
        if (currentState.isLoading) return

        val emailError = when {
            currentState.email.isBlank() -> R.string.signup_error_email_empty
            !currentState.email.isValidEmail() -> R.string.signup_error_email_invalid
            else -> null
        }

        val passwordError = when {
            currentState.password.isEmpty() -> R.string.signup_error_password_empty
            !currentState.password.isValidPassword() -> R.string.signup_error_password_short
            else -> null
        }

        val confirmPasswordError = when {
            currentState.confirmPassword.isEmpty() -> R.string.signup_error_confirm_password_empty
            currentState.password != currentState.confirmPassword -> R.string.signup_error_password_mismatch
            else -> null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // TODO: Implement actual signup logic using Firebase Auth later
            delay(1000)
            _state.update { it.copy(isLoading = false) }
            _effect.send(SignUpEffect.NavigateToHome) // replaces onSuccess()
        }
    }
}
```

---

## 6. Screen (stateful)

`screens/SignUpScreen.kt`

The screen does two things: shows the `state`, and collects `effect` to navigate. It no longer passes `onSuccess` to the ViewModel.

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.SignUp.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpEffect
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpViewModel

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToHome by rememberUpdatedState(onNavigateToHome)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SignUpEffect.NavigateToHome -> currentOnNavigateToHome()
            }
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigationBack = onNavigationBack
    )
}
```

---

## 7. Screen content (stateless) with reusable text fields

`components/SignUp/SignUpScreenContent.kt`

Four callbacks (`onEmailChanged`, `onPasswordChanged`, `onConfirmPasswordChanged`, `onSignUpClick`) are replaced by one: `onIntent`. Back navigation stays a plain callback because it is pure navigation with no logic.

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.SignUp

import androidx.compose.foundation.layout.*
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
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.constants.AppValidationConstants
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpIntent
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    onNavigationBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AppScaffold(
        onNavigateBack = onNavigationBack
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
                placeHolder = stringResource(R.string.signup_email_placeholder),
                errorText = state.emailError?.let { stringResource(it) },
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
                placeHolder = stringResource(R.string.signup_password_placeholder),
                errorText = state.passwordError?.let {
                    stringResource(it, AppValidationConstants.MIN_PASSWORD_LENGTH)
                },
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
                errorText = state.confirmPasswordError?.let { stringResource(it) },
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

            AppPrimaryButton(
                onClick = { onIntent(SignUpIntent.SignUpClicked) },
                text = stringResource(R.string.signup_button),
                isLoading = state.isLoading
            )
        }
    }
}
```

---

## What changed (summary)

| Before | After (MVI) |
|---|---|
| `onEmailChanged`, `onPasswordChanged`, `onConfirmPasswordChanged`, `onSignUpClicked` public in ViewModel | One public `onIntent(SignUpIntent)` |
| `onSignUpClicked(onSuccess: () -> Unit)` callback | `SignUpEffect.NavigateToHome` sent through a `Channel` |
| `SignUpScreenContent` took 4 callbacks | Takes one `onIntent` (+ `onNavigationBack`) |
| `SignUpScreen` passed `onSuccess` into the ViewModel | `SignUpScreen` collects `viewModel.effect` in a `LaunchedEffect` |
| Files: `SignUpUiState.kt`, `SignUpViewModel.kt` | Added `SignUpIntent.kt`, `SignUpEffect.kt` |

Unchanged: string resources, `SignUpUiState`, validation logic, `@StringRes` errors, reusable text fields, folder names.

**Notes**

- `data object` needs Kotlin 1.9 or newer. On older versions, use `object` instead.
- When you add Firebase Auth, add effects like `ShowMessage(@StringRes val message: Int)` for errors such as `signup_error_email_taken` and `signup_error_network`.
