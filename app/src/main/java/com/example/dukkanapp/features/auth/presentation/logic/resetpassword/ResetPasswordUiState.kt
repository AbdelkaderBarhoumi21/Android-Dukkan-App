package com.example.dukkanapp.features.auth.presentation.logic.resetpassword

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: AppUiText? = null,
    val confirmPasswordError: AppUiText? = null,
    val isLoading: Boolean = false
) {
    val isValidNewPassword: Boolean get() = newPasswordError == null && newPassword.isValidPassword()
    val isValidConfirmPassword: Boolean get() = confirmPasswordError == null && confirmPassword.isNotBlank() && newPassword == confirmPassword
}