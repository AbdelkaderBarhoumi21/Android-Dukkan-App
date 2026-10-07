package com.example.dukkanapp.features.auth.presentation.logic

import androidx.lifecycle.ViewModel
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<LoginUiState>(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChanged(value: String) {
        // clear the error as soon as the user edits the field
        _state.update { current ->
            current.copy(
                email = value,
                emailError = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    fun onRememberMeChanged(value: Boolean) {
        _state.update { it.copy(rememberMe = value) }
    }

    fun onLoginClick() {
        val current = _state.value

        val emailError = when {
            current.email.isBlank() -> R.string.login_error_email_empty
            !current.email.isValidEmail() -> R.string.login_error_email_invalid
            else -> null
        }
        val passwordError = if (!current.password.isValidPassword()) {
            R.string.login_error_password_short
        } else null
        _state.update { it.copy(emailError = emailError, passwordError = passwordError) }

        if (emailError == null && passwordError == null) {
            // TODO call firebase auth
        } else {
            _state.update { it.copy(isLoading = false) }
        }

    }
}