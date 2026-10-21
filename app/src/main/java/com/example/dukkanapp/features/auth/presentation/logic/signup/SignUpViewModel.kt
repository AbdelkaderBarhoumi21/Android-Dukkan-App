package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() :
    AppViewModel<SignUpUiState, SignUpIntent, SignUpEffect>(
        initialState = SignUpUiState()
    ) {
    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> setState {
                copy(
                    email = intent.email,
                    emailError = null
                )
            }

            is SignUpIntent.PasswordChanged -> setState {
                copy(
                    password = intent.password,
                    passwordError = null,
                    confirmPasswordError = liveMismatchError(intent.password, confirmPassword)
                )
            }

            is SignUpIntent.ConfirmPasswordChanged -> setState {
                copy(
                    confirmPassword = intent.confirmPassword,
                    confirmPasswordError = liveMismatchError(password, intent.confirmPassword)
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

        // Values
        val email = current.email.trim()
        // Passwords are never trimmed: a space is a valid password character.
        val password = current.password
        val confirmPassword = current.confirmPassword

        // Errors
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateNewPassword(password)
        val confirmPasswordError =
            AuthFormValidator.validateConfirmPassword(password, confirmPassword)

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            setState {
                copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText(),
                    confirmPasswordError = confirmPasswordError?.toUiText()
                )
            }
            return
        }

        setState { copy(isLoading = true) }
        viewModelScope.launch {
            // TODO : Add Firebase auth
            setState { copy(isLoading = false) }
            sendEffect(SignUpEffect.NavigateToHome)
        }
    }

    private fun liveMismatchError(password: String, confirmPassword: String): AppUiText? =
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            ValidationError.PasswordsMismatch.toUiText()
        } else {
            null
        }
}