package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpEffect {
    data class NavigateToEmailVerification(val emailJustSent: Boolean) : SignUpEffect
    data object NavigateToLogin : SignUpEffect
    data object NavigateBack : SignUpEffect
    data class OpenUrl(val url: String) : SignUpEffect
}