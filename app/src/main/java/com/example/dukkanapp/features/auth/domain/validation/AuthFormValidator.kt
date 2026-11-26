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
        password.isEmpty() -> ValidationError.PasswordEmpty
        !password.isValidPassword() -> ValidationError.PasswordTooShort(
            AppValidationConstants.MIN_PASSWORD_LENGTH
        )

        else -> null
    }

    /** Login: only require a value. Older accounts may predate the current policy. */
    fun validateExistingPassword(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.PasswordEmpty else null

    fun validateFullName(fullName: String): ValidationError? =
        if (fullName.isBlank()) ValidationError.FullNameEmpty else null

    fun validateTermsAccepted(
        accepted: Boolean
    ): ValidationError? =
        if (accepted) null else ValidationError.TermsNotAccepted

}