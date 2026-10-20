package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpEffect {
    data object NavigateToHome : SignUpEffect
    data object NavigateToLogin : SignUpEffect
    data object NavigateBack : SignUpEffect
}