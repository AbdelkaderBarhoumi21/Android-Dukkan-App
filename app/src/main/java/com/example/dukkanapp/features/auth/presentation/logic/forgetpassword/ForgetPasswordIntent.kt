package com.example.dukkanapp.features.auth.presentation.logic.forgetpassword

sealed interface ForgetPasswordIntent {
    data class EmailChanged(val email: String) : ForgetPasswordIntent
    data object ForgetPasswordClicked : ForgetPasswordIntent
    data object BackClicked : ForgetPasswordIntent
}