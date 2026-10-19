package com.example.dukkanapp.features.auth.presentation.logic.login

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
}

