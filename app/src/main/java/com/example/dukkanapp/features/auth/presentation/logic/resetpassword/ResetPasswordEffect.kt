package com.example.dukkanapp.features.auth.presentation.logic.resetpassword

sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
    data object NavigateBack : ResetPasswordEffect
}