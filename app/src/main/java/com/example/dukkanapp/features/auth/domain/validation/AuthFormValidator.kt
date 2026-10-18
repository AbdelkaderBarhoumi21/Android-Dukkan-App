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

    /** Sign up: enforce the password policy. */
    fun validateNewPassword(password: String): ValidationError? = when {
        password.isBlank() -> ValidationError.PasswordEmpty
        !password.trim().isValidPassword() -> ValidationError.PasswordTooShort(
            AppValidationConstants.MIN_PASSWORD_LENGTH
        )

        else -> null
    }

    /** Login: only require a value. Older accounts may predate the current policy. */
    fun validateExistingPassword(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.PasswordEmpty else null

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationError? =
        when {
            confirmPassword.isEmpty() -> ValidationError.ConfirmPasswordEmpty
            password != confirmPassword -> ValidationError.PasswordsMismatch
            else -> null
        }
}