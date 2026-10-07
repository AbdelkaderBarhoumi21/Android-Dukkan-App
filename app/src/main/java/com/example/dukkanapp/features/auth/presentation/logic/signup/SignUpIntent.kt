package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
}