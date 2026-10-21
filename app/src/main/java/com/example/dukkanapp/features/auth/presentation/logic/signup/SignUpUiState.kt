package com.example.dukkanapp.features.auth.presentation.logic.signup

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = false,
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val confirmPasswordError: AppUiText? = null,
    val isLoading: Boolean = false
) {
    val isValidEmail: Boolean get() = emailError == null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean
        get() = confirmPasswordError == null && confirmPassword.isNotEmpty() && password == confirmPassword
}