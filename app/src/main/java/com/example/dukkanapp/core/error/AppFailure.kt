package com.example.dukkanapp.core.error

sealed interface AppFailure {
    sealed interface Network : AppFailure {
        data object NoConnection : Network
        data object ServiceUnavailable : Network

        /** The backend is throttling us. Do not offer an immediate retry. */
        data object RateLimited : Network
    }

    sealed interface Auth : AppFailure {
        data object InvalidEmail : Auth
        data object InvalidCredentials : Auth
        data object EmailAlreadyInUse : Auth
        data object WeakPassword : Auth
        data object UserDisabled : Auth
        data object EmailNotVerified : Auth
        data object AccountExistsWithDifferentCredential : Auth
        data object RequiresRecentLogin : Auth
        data object SessionExpired : Auth

        /** Email link (password reset, verification) or SMS code expired, used or wrong. */
        data object CodeInvalidOrExpired : Auth
    }

    sealed interface Data : AppFailure {
        data object NotFound : Data
        data object PermissionDenied : Data
        data object AlreadyExists : Data

        /** Concurrent modification (e.g. a transaction lost a race on stock). Safe to retry. */
        data object Conflict : Data
    }

    /**
     * Extension point for business failures owned by a feature (cart, checkout, orders...).
     * Deliberately NOT sealed: features declare their own sealed hierarchy implementing it.
     */
    interface Feature : AppFailure

    /** Anything we did not anticipate. Already reported to crash reporting. */
    data object Unknown : AppFailure
}

val AppFailure.isRetryable: Boolean
    get() = when (this) {
        AppFailure.Network.NoConnection,
        AppFailure.Network.ServiceUnavailable,
        AppFailure.Data.Conflict -> true

        else -> false
    }