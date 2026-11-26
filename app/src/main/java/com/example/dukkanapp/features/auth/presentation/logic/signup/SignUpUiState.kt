package com.example.dukkanapp.features.auth.presentation.logic.signup

import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    val marketingOptIn: Boolean = false,
    val fullNameError: AppUiText? = null,
    val emailError: AppUiText? = null,
    val passwordError: AppUiText? = null,
    val termsError: AppUiText? = null,
    val generalError: AppUiText? = null,
    val isLoading: Boolean = false
) {
    val isValidEmail: Boolean get() = emailError != null && email.trim().isValidEmail()
    val isValidPassword: Boolean get() = passwordError != null && password.trim().isValidPassword()
}