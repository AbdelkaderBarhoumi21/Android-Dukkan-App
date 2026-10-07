package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class EmailChanged(val value: String) : SignUpIntent
    data class PasswordChanged(val value: String) : SignUpIntent
    data class ConfirmPasswordChanged(val value: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
}