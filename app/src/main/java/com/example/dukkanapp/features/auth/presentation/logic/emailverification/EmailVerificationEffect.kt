package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationEffect {
    data object NavigateToHome : EmailVerificationEffect
    data class NavigateToAllSet(val firstName: String) : EmailVerificationEffect
    data object NavigateToAuthOptions : EmailVerificationEffect
    data object NavigateToSignUp : EmailVerificationEffect

    /** Needs a Context, so the screen does it, not the ViewModel. */
    data object OpenEmailApp : EmailVerificationEffect
}