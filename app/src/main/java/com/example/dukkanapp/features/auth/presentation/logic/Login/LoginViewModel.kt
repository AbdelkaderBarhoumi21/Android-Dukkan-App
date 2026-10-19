package com.example.dukkanapp.features.auth.presentation.logic.login

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import com.example.dukkanapp.features.auth.presentation.logic.Login.LoginEffect
import com.example.dukkanapp.features.auth.presentation.logic.Login.LoginIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : AppViewModel<LoginUiState, LoginIntent, LoginEffect>(
    initialState = LoginUiState()
) {
    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> setState { copy(email = intent.email) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.password) }
            is LoginIntent.RememberMeChanged -> setState { copy(rememberMe = intent.checked) }
            LoginIntent.LoginClicked -> onLoginClick()

        }
    }

    private fun onLoginClick() {
        val current = currentState
        if (current.isLoading) return

        val email = current.email
        val password = current.password
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateExistingPassword(password)
        if (email != null || password != null) {
            setState {
                copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText()
                )
            }
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