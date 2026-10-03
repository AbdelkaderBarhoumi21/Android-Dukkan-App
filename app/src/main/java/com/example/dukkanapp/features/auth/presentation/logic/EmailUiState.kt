package com.example.dukkanapp.features.auth.presentation.logic

import androidx.annotation.StringRes
import com.example.dukkanapp.core.utils.extension.isValidEmail

data class EmailUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
}