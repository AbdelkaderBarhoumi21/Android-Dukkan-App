package com.example.dukkanapp.features.auth.presentation.logic.login

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
}

