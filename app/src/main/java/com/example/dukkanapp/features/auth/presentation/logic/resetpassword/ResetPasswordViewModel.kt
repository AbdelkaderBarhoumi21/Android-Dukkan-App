package com.example.dukkanapp.features.auth.presentation.logic.resetpassword

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import kotlinx.coroutines.launch

class ResetPasswordViewModel :
    AppViewModel<ResetPasswordUiState, ResetPasswordIntent, ResetPasswordEffect>(initialState = ResetPasswordUiState()) {
    override fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.PasswordChanged -> setState {
                copy(
                    password = intent.password
                )
            }

            is ResetPasswordIntent.ConfirmPasswordChanged -> setState {
                copy(
                    confirmPassword = intent.confirmPassword
                )
            }

            ResetPasswordIntent.ResetPasswordClicked -> onResetPasswordClicked()

            ResetPasswordIntent.BackClicked -> sendEffect(ResetPasswordEffect.NavigateBack)
        }
    }

    private fun onResetPasswordClicked() {
        val current = currentState
        if (current.isLoading) return

        val password = current.password
        val confirmPassword = current.confirmPassword
        val passwordError = AuthFormValidator.validateNewPassword(password)
        val confirmPasswordError =
            AuthFormValidator.validateConfirmPassword(password, confirmPassword)
        if (passwordError != null || confirmPasswordError != null) {
            setState {
                copy(
                    passwordError = passwordError?.toUiText(),
                    confirmPasswordError = confirmPasswordError?.toUiText()
                )
            }
            return
        }
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = false
                )
            }

            // TODO firebase logic
            sendEffect(ResetPasswordEffect.NavigateToLogin)
        }
    }
}