package com.example.dukkanapp.core.error.ui

import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.texts.AppUiText
import com.example.dukkanapp.core.error.AppFailure

/** Generic user message for any [AppFailure]. Features map their own [AppFailure.Feature] first. */
fun AppFailure.toUiText(): AppUiText = AppUiText.Res(messageRes)

private val AppFailure.messageRes: Int
    @StringRes get() = when (this) {
        AppFailure.Network.NoConnection -> R.string.error_no_connection
        AppFailure.Network.ServiceUnavailable -> R.string.error_service_unavailable
        AppFailure.Network.RateLimited -> R.string.error_too_many_requests
        AppFailure.Auth.InvalidEmail -> R.string.validation_email_invalid
        AppFailure.Auth.InvalidCredentials -> R.string.error_invalid_credentials
        AppFailure.Auth.EmailAlreadyInUse -> R.string.error_email_already_in_use
        AppFailure.Auth.WeakPassword -> R.string.error_weak_password
        AppFailure.Auth.UserDisabled -> R.string.error_user_disabled
        AppFailure.Auth.EmailNotVerified -> R.string.error_email_not_verified
        AppFailure.Auth.AccountExistsWithDifferentCredential -> R.string.error_account_exists_different_credential
        AppFailure.Auth.RequiresRecentLogin -> R.string.error_requires_recent_login
        AppFailure.Auth.SessionExpired -> R.string.error_session_expired
        AppFailure.Auth.CodeInvalidOrExpired -> R.string.error_code_invalid_or_expired
        AppFailure.Data.NotFound -> R.string.error_not_found
        AppFailure.Data.PermissionDenied -> R.string.error_permission_denied
        AppFailure.Data.AlreadyExists -> R.string.error_already_exists
        AppFailure.Data.Conflict -> R.string.error_conflict
        is AppFailure.Feature, AppFailure.Unknown -> R.string.error_unknown
    }