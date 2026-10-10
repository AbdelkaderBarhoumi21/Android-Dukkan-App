package com.example.dukkanapp.core.error.mapper

import com.example.dukkanapp.core.error.AppException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException

object AppFirebaseExceptionMapper {
    fun map(throwable: Throwable): AppException = when (throwable) {
        is AppException -> throwable
        is FirebaseNetworkException, is IOException -> AppException.Network.NoConnection(throwable)
        is FirebaseTooManyRequestsException -> AppException.Network.TooManyRequests(throwable)
        is FirebaseAuthException -> mapAuth(throwable)
        is FirebaseFirestoreException -> mapFirestore(throwable)
        else -> AppException.Unexpected(throwable)
    }

    private fun mapAuth(e: FirebaseAuthException): AppException = when (e) {
        // Exceptions identified by class (their error codes vary between SDK versions).
        is FirebaseAuthWeakPasswordException -> AppException.Auth.WeakPassword(e)
        is FirebaseAuthActionCodeException -> AppException.Auth.InvalidActionCode(e)
        is FirebaseAuthEmailException -> AppException.Auth.EmailSendFailed(e)

        // Everything else identified by error code.
        else -> when (e.errorCode) {
            AuthErrorCode.INVALID_EMAIL -> AppException.Auth.InvalidEmail(e)
            AuthErrorCode.WRONG_PASSWORD -> AppException.Auth.WrongPassword(e)
            AuthErrorCode.USER_NOT_FOUND -> AppException.Auth.UserNotFound(e)
            AuthErrorCode.INVALID_CREDENTIAL -> AppException.Auth.InvalidCredential(e)
            AuthErrorCode.WEAK_PASSWORD -> AppException.Auth.WeakPassword(e)
            AuthErrorCode.EMAIL_ALREADY_IN_USE -> AppException.Auth.EmailAlreadyInUse(e)
            AuthErrorCode.CREDENTIAL_ALREADY_IN_USE -> AppException.Auth.CredentialAlreadyInUse(e)
            AuthErrorCode.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> AppException.Auth.AccountExistsWithDifferentCredential(
                e
            )

            AuthErrorCode.USER_DISABLED -> AppException.Auth.UserDisabled(e)
            AuthErrorCode.USER_TOKEN_EXPIRED -> AppException.Auth.UserTokenExpired(e)
            AuthErrorCode.INVALID_USER_TOKEN -> AppException.Auth.InvalidUserToken(e)
            AuthErrorCode.USER_MISMATCH -> AppException.Auth.UserMismatch(e)
            AuthErrorCode.REQUIRES_RECENT_LOGIN -> AppException.Auth.RequiresRecentLogin(e)
            AuthErrorCode.INVALID_VERIFICATION_CODE -> AppException.Auth.InvalidVerificationCode(e)
            AuthErrorCode.INVALID_VERIFICATION_ID -> AppException.Auth.InvalidVerificationId(e)
            AuthErrorCode.SESSION_EXPIRED -> AppException.Auth.CodeSessionExpired(e)
            AuthErrorCode.OPERATION_NOT_ALLOWED -> AppException.Auth.OperationNotAllowed(e)
            AuthErrorCode.QUOTA_EXCEEDED -> AppException.Auth.QuotaExceeded(e)
            AuthErrorCode.TOO_MANY_REQUESTS -> AppException.Network.TooManyRequests(e)
            AuthErrorCode.NETWORK_REQUEST_FAILED -> AppException.Network.NoConnection(e)
            else -> AppException.Unexpected(e)

        }

    }

    /** No `else`: if a Firebase update adds a code, this stops compiling until you add a case. */
    private fun mapFirestore(e: FirebaseFirestoreException): AppException = when (e.code) {
        Code.CANCELLED -> AppException.Firestore.Cancelled(e)
        Code.UNKNOWN -> AppException.Firestore.Unknown(e)
        Code.INVALID_ARGUMENT -> AppException.Firestore.InvalidArgument(e)
        Code.DEADLINE_EXCEEDED -> AppException.Firestore.DeadlineExceeded(e)
        Code.NOT_FOUND -> AppException.Firestore.NotFound(e)
        Code.ALREADY_EXISTS -> AppException.Firestore.AlreadyExists(e)
        Code.PERMISSION_DENIED -> AppException.Firestore.PermissionDenied(e)
        Code.RESOURCE_EXHAUSTED -> AppException.Firestore.ResourceExhausted(e)
        Code.FAILED_PRECONDITION -> AppException.Firestore.FailedPrecondition(e)
        Code.ABORTED -> AppException.Firestore.Aborted(e)
        Code.OUT_OF_RANGE -> AppException.Firestore.OutOfRange(e)
        Code.UNIMPLEMENTED -> AppException.Firestore.Unimplemented(e)
        Code.INTERNAL -> AppException.Firestore.Internal(e)
        Code.UNAVAILABLE -> AppException.Firestore.Unavailable(e)
        Code.DATA_LOSS -> AppException.Firestore.DataLoss(e)
        Code.UNAUTHENTICATED -> AppException.Firestore.Unauthenticated(e)
        Code.OK -> AppException.Unexpected(e) // never thrown in practice
    }
}


/** Firebase Auth error codes. One place, so a typo can't hide anywhere else. */
private object AuthErrorCode {
    const val INVALID_EMAIL = "ERROR_INVALID_EMAIL"
    const val WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
    const val USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    const val INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL"
    const val WEAK_PASSWORD = "ERROR_WEAK_PASSWORD"
    const val EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
    const val CREDENTIAL_ALREADY_IN_USE = "ERROR_CREDENTIAL_ALREADY_IN_USE"
    const val ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL =
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
    const val USER_DISABLED = "ERROR_USER_DISABLED"
    const val USER_TOKEN_EXPIRED = "ERROR_USER_TOKEN_EXPIRED"
    const val INVALID_USER_TOKEN = "ERROR_INVALID_USER_TOKEN"
    const val USER_MISMATCH = "ERROR_USER_MISMATCH"
    const val REQUIRES_RECENT_LOGIN = "ERROR_REQUIRES_RECENT_LOGIN"
    const val INVALID_VERIFICATION_CODE = "ERROR_INVALID_VERIFICATION_CODE"
    const val INVALID_VERIFICATION_ID = "ERROR_INVALID_VERIFICATION_ID"
    const val SESSION_EXPIRED = "ERROR_SESSION_EXPIRED"
    const val OPERATION_NOT_ALLOWED = "ERROR_OPERATION_NOT_ALLOWED"
    const val QUOTA_EXCEEDED = "ERROR_QUOTA_EXCEEDED"
    const val TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
    const val NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
}