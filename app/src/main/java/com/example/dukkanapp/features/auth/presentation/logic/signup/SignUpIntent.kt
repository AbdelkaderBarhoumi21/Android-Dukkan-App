package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class FullNameChanged(val fullName: String) : SignUpIntent
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class AcceptTermsChanged(val checked: Boolean) : SignUpIntent
    data class MarketingOptInChanged(val checked: Boolean) : SignUpIntent
    data object TermsClicked : SignUpIntent
    data object PrivacyClicked : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object LoginClicked : SignUpIntent
    data object BackClicked : SignUpIntent
}