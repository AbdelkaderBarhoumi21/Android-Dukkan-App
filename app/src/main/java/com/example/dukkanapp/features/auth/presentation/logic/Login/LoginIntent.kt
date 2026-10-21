package com.example.dukkanapp.features.auth.presentation.logic.login

sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data class RememberMeChanged(val checked: Boolean) : LoginIntent
    data object LoginClicked : LoginIntent
    data object ForgotPasswordClicked : LoginIntent
    data object SignUpClicked : LoginIntent
    data object BackClicked : LoginIntent
}