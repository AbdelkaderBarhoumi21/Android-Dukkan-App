# SignUp Screen: UI Components, State, and ViewModel

Dukkan App (`com.example.dukkanapp`). Pattern: MVI-style state + events + effects, Hilt ViewModel.

This document has been updated to perfectly align with the `Login` flow. It uses `@StringRes` for error handling, delegates visibility toggling to `AppPasswordTextField`, and relies on core utility extensions for validation (eliminating the need for custom Enums and standalone validator objects).

## Folder structure

```
features/auth/presentation/
├── logic/
│   └── SignUp/
│       ├── SignUpUiState.kt        (state, events, effects)
│       └── SignUpViewModel.kt
├── components/
│   └── SignUp/
│       └── SignUpScreenContent.kt
└── screens/
    └── SignUpScreen.kt
```

---

## 1. String resources

Keys prefixed with `signup_`. Ensure these exist in `values/strings.xml`, `values-ar/string-ar.xml`, and `values-fr/string-fr.xml`.

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

We align with `LoginUiState` by storing `@StringRes` for errors directly, and avoiding explicit `isVisible` fields since `AppPasswordTextField` manages its own internal visibility state!

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

## 3. ViewModel

`logic/SignUp/SignUpViewModel.kt`

The ViewModel uses standard extension functions (`isValidEmail()`, `isValidPassword()`) to validate, updating the `@StringRes` state variables accordingly. 

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.SignUp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
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

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = if (confirmPassword.isNotEmpty() && it.password != confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else null
            )
        }
    }

    fun onSignUpClicked(onSuccess: () -> Unit) {
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
            onSuccess()
        }
    }
}
```

---

## 4. Screen (stateful)

`screens/SignUpScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.SignUp.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpViewModel

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignUpScreenContent(
        state = state,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onNavigationBack = onNavigationBack,
        onSignUpClick = {
            viewModel.onSignUpClicked(onSuccess = onNavigateToHome)
        }
    )
}
```

---

## 5. Screen content (stateless) with reusable text fields

`components/SignUp/SignUpScreenContent.kt`

This leverages the existing `AppTextFiled` and `AppPasswordTextField`. 

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
import com.example.dukkanapp.features.auth.presentation.logic.SignUp.SignUpUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onNavigationBack: () -> Unit,
    onSignUpClick: () -> Unit,
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
                onValueChange = onEmailChanged,
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
                onValueChange = onPasswordChanged,
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
                onValueChange = onConfirmPasswordChanged,
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
                        onSignUpClick()
                    }
                )
            )
            Spacer(Modifier.height(AppDimens.spaceSm))

            AppPrimaryButton(
                onClick = onSignUpClick,
                text = stringResource(R.string.signup_button),
                isLoading = state.isLoading
            )
        }
    }
}
```

---

## Key Alignments and Optimizations Made:
1. **Simplified Fields**: Removed `name` and `terms` fields to stick purely to `email`, `password`, and `confirmPassword` matching the initial `Login` approach.
2. **`@StringRes` Approach**: Eliminated custom `Enum` error types and `SignUpValidator.kt` entirely. Directly uses `@StringRes val emailError: Int?` which drastically reduces boilerplate and matches `LoginUiState`.
3. **Internal Component State**: Removed visibility toggling events and booleans (`isPasswordVisible` and `isConfirmPasswordVisible`) from the `ViewModel` because `AppPasswordTextField` internally handles visibility (`var isVisible by rememberSaveable { mutableStateOf(false) }`).
4. **Validation Extensions**: Reused `String.isValidEmail()` and `String.isValidPassword()` from `core/utils/extension/` to keep validation logic dry and standard across all auth screens.
5. **Folder Uniformity**: Standardized paths mapping strictly into `features/auth/presentation/logic/SignUp` and `features/auth/presentation/components/SignUp`.