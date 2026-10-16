package com.example.dukkanapp.features.auth.domain.validation

import com.example.dukkanapp.core.error.validation.ValidationError
import com.example.dukkanapp.core.utils.constants.AppValidationConstants
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

object AuthFormValidator {
    fun validateEmail(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EmailEmpty
        !email.trim().isValidEmail() -> ValidationError.EmailInvalid
        else -> null
    }

    fun validateNewPassword(password: String): ValidationError? = when {
        password.isBlank() -> ValidationError.PasswordEmpty
        !password.trim().isValidPassword() -> ValidationError.PasswordTooShort(
            AppValidationConstants.MIN_PASSWORD_LENGTH
        )

        else -> null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationError? =
        when {
            confirmPassword.isEmpty() -> ValidationError.ConfirmPasswordEmpty
            password != confirmPassword -> ValidationError.PasswordsMismatch
            else -> null
        }
}