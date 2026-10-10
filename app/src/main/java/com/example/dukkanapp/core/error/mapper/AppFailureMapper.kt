package com.example.dukkanapp.core.error.mapper

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.reporter.ErrorReporter
import javax.inject.Inject

/** Step 2 of 2: anything a data source threw → [AppFailure]. */
fun interface FailureMapper {
    fun map(throwable: Throwable): AppFailure
}

class DefaultFailureMapper @Inject constructor(
    private val reporter: ErrorReporter,
) : FailureMapper {

    override fun map(throwable: Throwable): AppFailure {
        val failure = (throwable as? AppException)?.toFailure() ?: AppFailure.Unknown
        if (failure == AppFailure.Unknown) reporter.report(throwable)
        return failure
    }

    /** No `else`: a new AppException case doesn't compile until it gets a decision here. */
    private fun AppException.toFailure(): AppFailure = when (this) {
        // Network
        is AppException.Network.NoConnection -> AppFailure.Network.NoConnection
        is AppException.Network.Timeout -> AppFailure.Network.ServiceUnavailable
        is AppException.Network.TooManyRequests -> AppFailure.Network.RateLimited

        // Auth: wrong password / unknown user / wrong account look identical (no email enumeration)
        is AppException.Auth.InvalidEmail -> AppFailure.Auth.InvalidEmail
        is AppException.Auth.WrongPassword,
        is AppException.Auth.UserNotFound,
        is AppException.Auth.InvalidCredential,
        is AppException.Auth.UserMismatch -> AppFailure.Auth.InvalidCredentials

        is AppException.Auth.WeakPassword,
        is AppException.Auth.PasswordRequirementsNotMet -> AppFailure.Auth.WeakPassword

        is AppException.Auth.EmailAlreadyInUse -> AppFailure.Auth.EmailAlreadyInUse
        is AppException.Auth.CredentialAlreadyInUse,
        is AppException.Auth.AccountExistsWithDifferentCredential -> AppFailure.Auth.AccountExistsWithDifferentCredential

        is AppException.Auth.UserDisabled -> AppFailure.Auth.UserDisabled
        is AppException.Auth.UserTokenExpired,
        is AppException.Auth.InvalidUserToken,
        is AppException.Auth.NoCurrentUser -> AppFailure.Auth.SessionExpired

        is AppException.Auth.RequiresRecentLogin -> AppFailure.Auth.RequiresRecentLogin
        is AppException.Auth.InvalidActionCode,
        is AppException.Auth.InvalidVerificationCode,
        is AppException.Auth.CodeSessionExpired -> AppFailure.Auth.CodeInvalidOrExpired

        is AppException.Auth.EmailSendFailed -> AppFailure.Network.ServiceUnavailable
        is AppException.Auth.QuotaExceeded -> AppFailure.Network.RateLimited

        // Firestore
        is AppException.Firestore.Unavailable,
        is AppException.Firestore.DeadlineExceeded -> AppFailure.Network.ServiceUnavailable

        is AppException.Firestore.ResourceExhausted -> AppFailure.Network.RateLimited
        is AppException.Firestore.Unauthenticated -> AppFailure.Auth.SessionExpired
        is AppException.Firestore.PermissionDenied -> AppFailure.Data.PermissionDenied
        is AppException.Firestore.NotFound -> AppFailure.Data.NotFound
        is AppException.Firestore.AlreadyExists -> AppFailure.Data.AlreadyExists
        is AppException.Firestore.Aborted -> AppFailure.Data.Conflict

        // Business rules
        is AppException.Feature -> failure

        // Developer / configuration bugs: generic message for the user, crash report for the team.
        is AppException.Auth.InvalidVerificationId,
        is AppException.Auth.OperationNotAllowed,
        is AppException.Firestore.FailedPrecondition,
        is AppException.Firestore.InvalidArgument,
        is AppException.Firestore.OutOfRange,
        is AppException.Firestore.Cancelled,
        is AppException.Firestore.Unknown,
        is AppException.Firestore.Unimplemented,
        is AppException.Firestore.Internal,
        is AppException.Firestore.DataLoss,
        is AppException.Unexpected -> AppFailure.Unknown
    }
}