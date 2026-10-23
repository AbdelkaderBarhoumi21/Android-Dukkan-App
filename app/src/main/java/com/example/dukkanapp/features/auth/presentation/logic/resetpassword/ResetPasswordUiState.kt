package com.example.dukkanapp.features.auth.presentation.logic.resetpassword

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class ResetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: AppUiText? = null,
    val confirmPasswordError: AppUiText? = null,
    val isLoading: Boolean = false
) {
    val isValidNewPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean get() = confirmPasswordError == null && confirmPassword.isNotBlank() && password == confirmPassword
}