package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<SignUpUiState>(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()
    fun onEmailChanged(email: String) {
        // clear the error as soon as the user edits the field
        _state.update { current -> current.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { current ->
            current.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = if (current.confirmPassword.isNotEmpty() && password != current.confirmPassword) {
                    R.string.signup_error_password_mismatch

                } else {
                    null
                }
            )
        }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update { current ->
            current.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = if (confirmPassword.isNotEmpty() && current.password != confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else {
                    null
                }
            )
        }
    }

    fun onSignUpClicked(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.isLoading) return

        val emailError = when {
            current.email.isBlank() -> R.string.signup_error_email_empty
            !current.email.isValidEmail() -> R.string.signup_error_email_invalid
            else -> null
        }

        val passwordError = when {
            current.password.isEmpty() -> R.string.signup_error_confirm_password_empty
            !current.password.isValidPassword() -> R.string.signup_error_password_short
            else -> null
        }
        val confirmPasswordError = when {
            current.confirmPassword.isNotEmpty() -> R.string.signup_error_confirm_password_empty
            current.password != current.confirmPassword -> R.string.signup_error_password_mismatch
            else -> null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
        }
        if (emailError != null && passwordError != null && confirmPasswordError != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }

            // TODO: Implement actual signup logic using Firebase Auth later
            _state.update { it.copy(isLoading = false) }
            onSuccess()
        }

    }

}