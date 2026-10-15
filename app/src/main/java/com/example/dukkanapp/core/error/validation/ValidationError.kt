package com.example.dukkanapp.core.error.validation

sealed interface ValidationError {
    data object EmailEmpty : ValidationError
    data object EmailInvalid : ValidationError
    data object PasswordEmpty : ValidationError
    data class PasswordShort(val minLength: Int) : ValidationError
    data object ConfirmPasswordEmpty : ValidationError
    data object PasswordsMismatch : ValidationError
}
