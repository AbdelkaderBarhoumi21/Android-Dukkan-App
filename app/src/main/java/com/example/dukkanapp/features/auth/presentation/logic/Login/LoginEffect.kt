package com.example.dukkanapp.features.auth.presentation.logic.Login

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToSignUp : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
    data object NavigateBack : LoginEffect
}