package com.example.dukkanapp.core.error

sealed class AppException(cause: Throwable?) : Exception(cause) {
    // ── Network (any backend) ─────────────────────────────────────────────
    sealed class Network(cause: Throwable?) : AppException(cause) {
        class NoConnection(cause: Throwable? = null) : Network(cause)
        class Timeout(cause: Throwable? = null) : Network(cause)
        class TooManyRequests(cause: Throwable? = null) : Network(cause)
    }

    // ── Firebase Auth: one case per error code ───────────────────────────
    sealed class Auth(cause: Throwable?) : AppException(cause) {
        // Email / password
        class InvalidEmail(cause: Throwable? = null) :
            Auth(cause)                 // ERROR_INVALID_EMAIL

        class WrongPassword(cause: Throwable? = null) :
            Auth(cause)                // ERROR_WRONG_PASSWORD

        class UserNotFound(cause: Throwable? = null) :
            Auth(cause)                 // ERROR_USER_NOT_FOUND

        class InvalidCredential(cause: Throwable? = null) :
            Auth(cause)            // ERROR_INVALID_CREDENTIAL

        class WeakPassword(cause: Throwable? = null) :
            Auth(cause)                 // ERROR_WEAK_PASSWORD

        class PasswordRequirementsNotMet(cause: Throwable? = null) :
            Auth(cause)   // password policy (console)

        // Account
        class EmailAlreadyInUse(cause: Throwable? = null) :
            Auth(cause)            // ERROR_EMAIL_ALREADY_IN_USE

        class CredentialAlreadyInUse(cause: Throwable? = null) :
            Auth(cause)       // ERROR_CREDENTIAL_ALREADY_IN_USE

        class AccountExistsWithDifferentCredential(cause: Throwable? = null) :
            Auth(cause) // ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL

        class UserDisabled(cause: Throwable? = null) :
            Auth(cause)                 // ERROR_USER_DISABLED

        // Session
        class UserTokenExpired(cause: Throwable? = null) :
            Auth(cause)             // ERROR_USER_TOKEN_EXPIRED

        class InvalidUserToken(cause: Throwable? = null) :
            Auth(cause)             // ERROR_INVALID_USER_TOKEN

        class UserMismatch(cause: Throwable? = null) :
            Auth(cause)                 // ERROR_USER_MISMATCH

        class RequiresRecentLogin(cause: Throwable? = null) :
            Auth(cause)          // ERROR_REQUIRES_RECENT_LOGIN

        class NoCurrentUser :
            Auth(null)                                            // ours: no signed-in user

        // Email links and codes
        class InvalidActionCode(cause: Throwable? = null) :
            Auth(cause)            // expired/used reset or verify link

        class EmailSendFailed(cause: Throwable? = null) :
            Auth(cause)              // verification/reset mail not sent

        class InvalidVerificationCode(cause: Throwable? = null) :
            Auth(cause)      // ERROR_INVALID_VERIFICATION_CODE

        class InvalidVerificationId(cause: Throwable? = null) :
            Auth(cause)        // ERROR_INVALID_VERIFICATION_ID

        class CodeSessionExpired(cause: Throwable? = null) :
            Auth(cause)           // ERROR_SESSION_EXPIRED (SMS code)

        // Project configuration
        class OperationNotAllowed(cause: Throwable? = null) :
            Auth(cause)          // ERROR_OPERATION_NOT_ALLOWED

        class QuotaExceeded(cause: Throwable? = null) :
            Auth(cause)                // ERROR_QUOTA_EXCEEDED
    }


    // ── Cloud Firestore: one case per FirebaseFirestoreException.Code ─────
    sealed class Firestore(cause: Throwable?) : AppException(cause) {
        class Cancelled(cause: Throwable? = null) : Firestore(cause)
        class Unknown(cause: Throwable? = null) : Firestore(cause)
        class InvalidArgument(cause: Throwable? = null) : Firestore(cause)
        class DeadlineExceeded(cause: Throwable? = null) : Firestore(cause)
        class NotFound(cause: Throwable? = null) :
            Firestore(cause)       // also: document doesn't exist on read

        class AlreadyExists(cause: Throwable? = null) : Firestore(cause)
        class PermissionDenied(cause: Throwable? = null) : Firestore(cause)
        class ResourceExhausted(cause: Throwable? = null) : Firestore(cause)
        class FailedPrecondition(cause: Throwable? = null) :
            Firestore(cause) // usually a missing index

        class Aborted(cause: Throwable? = null) : Firestore(cause)        // transaction contention
        class OutOfRange(cause: Throwable? = null) : Firestore(cause)
        class Unimplemented(cause: Throwable? = null) : Firestore(cause)
        class Internal(cause: Throwable? = null) : Firestore(cause)
        class Unavailable(cause: Throwable? = null) :
            Firestore(cause)    // also: offline with no cache

        class DataLoss(cause: Throwable? = null) : Firestore(cause)
        class Unauthenticated(cause: Throwable? = null) : Firestore(cause)
    }


    /** A business rule failed in the data layer (e.g. a Cloud Function said "out of stock"). */
//    class Feature(val feature: AppFailure.Feature, cause: Throwable?) : AppException(cause)

    /** Anything not in this catalog. Always reported. */
    class Unexpected(cause: Throwable? = null) : AppException(cause)


}