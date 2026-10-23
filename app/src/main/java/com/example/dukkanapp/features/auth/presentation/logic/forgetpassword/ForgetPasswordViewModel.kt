package com.example.dukkanapp.features.auth.presentation.logic.forgetpassword

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.core.error.ui.toUiText
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
import kotlinx.coroutines.launch

class ForgetPasswordViewModel :
    AppViewModel<ForgetPasswordUiState, ForgetPasswordIntent, ForgetPasswordEffect>(initialState = ForgetPasswordUiState()) {
    override fun onIntent(intent: ForgetPasswordIntent) {
        when (intent) {
            is ForgetPasswordIntent.EmailChanged -> setState { copy(email = intent.email) }
            ForgetPasswordIntent.ForgetPasswordClicked -> onForgetPasswordClicked()
            ForgetPasswordIntent.BackClicked -> sendEffect(ForgetPasswordEffect.NavigateBack)
        }
    }

    private fun onForgetPasswordClicked() {
        val current = currentState
        if (current.isLoading) return

        val email = current.email
        val emailError = AuthFormValidator.validateEmail(email)
        if (emailError != null) {
            setState {
                copy(
                    emailError = emailError.toUiText()
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
            sendEffect(ForgetPasswordEffect.NavigateToChangePassword)
        }
    }
}