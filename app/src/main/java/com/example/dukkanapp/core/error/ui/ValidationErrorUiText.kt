package com.example.dukkanapp.core.error.ui

import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.AppUiText
import com.example.dukkanapp.core.error.validation.ValidationError

fun ValidationError.toUiText(): AppUiText = when (this) {
    ValidationError.EmailEmpty -> AppUiText.Res(R.string.validation_email_empty)
    ValidationError.EmailInvalid -> AppUiText.Res(R.string.validation_email_invalid)
    ValidationError.PasswordEmpty -> AppUiText.Res(R.string.validation_password_empty)
    is ValidationError.PasswordTooShort -> AppUiText.Res(
        R.string.validation_password_too_short,
        listOf(minLength)
    )

    ValidationError.ConfirmPasswordEmpty -> AppUiText.Res(R.string.validation_confirm_password_empty)
    ValidationError.PasswordsMismatch -> AppUiText.Res(R.string.validation_passwords_mismatch)
}