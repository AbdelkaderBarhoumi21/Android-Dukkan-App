package com.example.dukkanapp.features.auth.presentation.logic.resetpassword

sealed interface ResetPasswordIntent {
    data class PasswordChanged(val password: String) : ResetPasswordIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : ResetPasswordIntent
    data object ResetPasswordClicked : ResetPasswordIntent
    data object BackClicked : ResetPasswordIntent
}