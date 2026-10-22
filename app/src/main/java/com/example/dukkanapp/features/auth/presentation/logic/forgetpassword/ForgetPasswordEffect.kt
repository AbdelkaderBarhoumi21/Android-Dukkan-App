package com.example.dukkanapp.features.auth.presentation.logic.forgetpassword

sealed interface ForgetPasswordEffect {
    data object NavigateToChangePassword : ForgetPasswordEffect
    data object NavigateBack : ForgetPasswordEffect
}