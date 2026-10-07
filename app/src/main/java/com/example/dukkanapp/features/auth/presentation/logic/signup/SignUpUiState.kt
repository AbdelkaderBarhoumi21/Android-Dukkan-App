package com.example.dukkanapp.features.auth.presentation.logic.signup

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
    val isLoading: Boolean = false
) {
    val isValidEmail: Boolean get() = emailError != null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError != null && password.isValidPassword()
    val isValidConfirmPassword: Boolean get() = confirmPasswordError != null && confirmPassword.isNotEmpty() && password == confirmPassword

}