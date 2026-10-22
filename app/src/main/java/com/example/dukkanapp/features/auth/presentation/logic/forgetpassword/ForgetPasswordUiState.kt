package com.example.dukkanapp.features.auth.presentation.logic.forgetpassword

import com.example.dukkanapp.core.utils.extension.isValidEmail

data class ForgetPasswordUiState(
    val email: String = "",
    val emailError: String = "",
    val isLoading: Boolean = false
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
}