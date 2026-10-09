# Error Handling Architecture Guide

Centralized error handling system for the Dukkan App. This architecture provides a unified way to handle errors across all features using Firebase (Auth, Firestore, Storage, etc.).

```
Remote Data Source → throws AppException → Repository catches → returns AppResult
                                                    ↓
                                           UseCase returns AppResult
                                                    ↓
                                           ViewModel maps to UI State
```

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Folder Structure](#2-folder-structure)
3. [Core Error Components](#3-core-error-components)
4. [Firebase Exception Mapping](#4-firebase-exception-mapping)
5. [Data Source Implementation](#5-data-source-implementation)
6. [Repository Implementation](#6-repository-implementation)
7. [UseCase Implementation](#7-usecase-implementation)
8. [ViewModel Error Handling](#8-viewmodel-error-handling)
9. [String Resources](#9-string-resources)
10. [Usage Examples](#10-usage-examples)

---

## 1. Architecture Overview

### Flow Diagram

```
┌─────────────────┐    throws     ┌─────────────────┐
│  Firebase SDK   │ ───────────► │  AppException   │
└─────────────────┘               └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐    catches    ┌─────────────────┐
│ Remote DataSource│ ◄─────────── │  AppException   │
└─────────────────┘               └─────────────────┘
         │
         │ throws AppException
         ▼
┌─────────────────┐    catches    ┌─────────────────┐
│   Repository    │ ───────────► │    AppResult    │
└─────────────────┘               │  Success/Error  │
                                  └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐               ┌─────────────────┐
│    UseCase      │ ───────────► │    AppResult    │
└─────────────────┘               └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐    maps to    ┌─────────────────┐
│   ViewModel     │ ───────────► │   UI State      │
└─────────────────┘               │  @StringRes     │
                                  └─────────────────┘
```

### Key Principles

1. **Data Sources throw exceptions** - Raw Firebase exceptions are caught and rethrown as `AppException`
2. **Repositories return AppResult** - Catch exceptions and wrap in `AppResult.Error`
3. **UseCases propagate AppResult** - Business logic works with `AppResult`
4. **ViewModels map to UI** - Convert `AppException` to `@StringRes` for display

---

## 2. Folder Structure

```
core/
├── error/
│   ├── AppException.kt           # Base exception sealed class
│   ├── AppResult.kt              # Result wrapper (Success/Error)
│   ├── ErrorCode.kt              # Error code enum
│   └── firebase/
│       ├── FirebaseAuthExceptionMapper.kt
│       └── FirebaseFirestoreExceptionMapper.kt
└── utils/
    └── extension/
        └── AppResultExtension.kt  # Extension functions for AppResult
```

---

## 3. Core Error Components

### Step 3.1: Create ErrorCode Enum

**File:** `core/error/ErrorCode.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Centralized error codes for the entire application.
 * Each error code maps to a specific user-facing message.
 */
enum class ErrorCode {
    // ══════════════════════════════════════════════
    // NETWORK ERRORS
    // ══════════════════════════════════════════════
    NETWORK_ERROR,
    TIMEOUT_ERROR,
    NO_INTERNET,
    
    // ══════════════════════════════════════════════
    // FIREBASE AUTH ERRORS
    // ══════════════════════════════════════════════
    AUTH_INVALID_EMAIL,
    AUTH_WRONG_PASSWORD,
    AUTH_USER_NOT_FOUND,
    AUTH_EMAIL_ALREADY_IN_USE,
    AUTH_WEAK_PASSWORD,
    AUTH_USER_DISABLED,
    AUTH_OPERATION_NOT_ALLOWED,
    AUTH_TOO_MANY_REQUESTS,
    AUTH_INVALID_CREDENTIAL,
    AUTH_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL,
    AUTH_REQUIRES_RECENT_LOGIN,
    AUTH_EMAIL_NOT_VERIFIED,
    AUTH_INVALID_VERIFICATION_CODE,
    AUTH_INVALID_VERIFICATION_ID,
    AUTH_SESSION_EXPIRED,
    AUTH_QUOTA_EXCEEDED,
    
    // ══════════════════════════════════════════════
    // FIRESTORE ERRORS
    // ══════════════════════════════════════════════
    FIRESTORE_PERMISSION_DENIED,
    FIRESTORE_NOT_FOUND,
    FIRESTORE_ALREADY_EXISTS,
    FIRESTORE_RESOURCE_EXHAUSTED,
    FIRESTORE_FAILED_PRECONDITION,
    FIRESTORE_ABORTED,
    FIRESTORE_OUT_OF_RANGE,
    FIRESTORE_UNIMPLEMENTED,
    FIRESTORE_INTERNAL,
    FIRESTORE_UNAVAILABLE,
    FIRESTORE_DATA_LOSS,
    FIRESTORE_UNAUTHENTICATED,
    FIRESTORE_CANCELLED,
    FIRESTORE_DEADLINE_EXCEEDED,
    
    // ══════════════════════════════════════════════
    // VALIDATION ERRORS
    // ══════════════════════════════════════════════
    VALIDATION_EMPTY_FIELD,
    VALIDATION_INVALID_FORMAT,
    VALIDATION_PASSWORD_MISMATCH,
    VALIDATION_PASSWORD_TOO_SHORT,
    
    // ══════════════════════════════════════════════
    // GENERAL ERRORS
    // ══════════════════════════════════════════════
    UNKNOWN_ERROR,
    UNEXPECTED_ERROR,
    PARSE_ERROR,
    CACHE_ERROR
}
```

### Step 3.2: Create AppException Sealed Class

**File:** `core/error/AppException.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Base exception class for the entire application.
 * All feature-specific exceptions should extend from this.
 * 
 * @param errorCode The error code for mapping to UI messages
 * @param message Technical message for logging
 * @param cause Original throwable cause
 */
sealed class AppException(
    val errorCode: ErrorCode,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // ══════════════════════════════════════════════════════════════════
    // NETWORK EXCEPTIONS
    // ══════════════════════════════════════════════════════════════════
    
    class NetworkException(
        message: String = "Network error occurred",
        cause: Throwable? = null
    ) : AppException(ErrorCode.NETWORK_ERROR, message, cause)
    
    class TimeoutException(
        message: String = "Request timed out",
        cause: Throwable? = null
    ) : AppException(ErrorCode.TIMEOUT_ERROR, message, cause)
    
    class NoInternetException(
        message: String = "No internet connection",
        cause: Throwable? = null
    ) : AppException(ErrorCode.NO_INTERNET, message, cause)

    // ══════════════════════════════════════════════════════════════════
    // FIREBASE AUTH EXCEPTIONS
    // ══════════════════════════════════════════════════════════════════
    
    class InvalidEmailException(
        message: String = "Invalid email address",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_INVALID_EMAIL, message, cause)
    
    class WrongPasswordException(
        message: String = "Wrong password",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_WRONG_PASSWORD, message, cause)
    
    class UserNotFoundException(
        message: String = "User not found",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_USER_NOT_FOUND, message, cause)
    
    class EmailAlreadyInUseException(
        message: String = "Email already in use",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_EMAIL_ALREADY_IN_USE, message, cause)
    
    class WeakPasswordException(
        message: String = "Password is too weak",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_WEAK_PASSWORD, message, cause)
    
    class UserDisabledException(
        message: String = "User account is disabled",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_USER_DISABLED, message, cause)
    
    class OperationNotAllowedException(
        message: String = "Operation not allowed",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_OPERATION_NOT_ALLOWED, message, cause)
    
    class TooManyRequestsException(
        message: String = "Too many requests. Please try again later",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_TOO_MANY_REQUESTS, message, cause)
    
    class InvalidCredentialException(
        message: String = "Invalid credentials",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_INVALID_CREDENTIAL, message, cause)
    
    class AccountExistsWithDifferentCredentialException(
        message: String = "Account exists with different credential",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL, message, cause)
    
    class RequiresRecentLoginException(
        message: String = "Please login again to continue",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_REQUIRES_RECENT_LOGIN, message, cause)
    
    class EmailNotVerifiedException(
        message: String = "Email not verified",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, message, cause)
    
    class InvalidVerificationCodeException(
        message: String = "Invalid verification code",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_INVALID_VERIFICATION_CODE, message, cause)
    
    class InvalidVerificationIdException(
        message: String = "Invalid verification ID",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_INVALID_VERIFICATION_ID, message, cause)
    
    class SessionExpiredException(
        message: String = "Session expired. Please login again",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_SESSION_EXPIRED, message, cause)
    
    class QuotaExceededException(
        message: String = "Quota exceeded. Please try again later",
        cause: Throwable? = null
    ) : AppException(ErrorCode.AUTH_QUOTA_EXCEEDED, message, cause)

    // ══════════════════════════════════════════════════════════════════
    // FIRESTORE EXCEPTIONS
    // ══════════════════════════════════════════════════════════════════
    
    class PermissionDeniedException(
        message: String = "Permission denied",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_PERMISSION_DENIED, message, cause)
    
    class DocumentNotFoundException(
        message: String = "Document not found",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_NOT_FOUND, message, cause)
    
    class DocumentAlreadyExistsException(
        message: String = "Document already exists",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_ALREADY_EXISTS, message, cause)
    
    class ResourceExhaustedException(
        message: String = "Resource exhausted. Please try again later",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_RESOURCE_EXHAUSTED, message, cause)
    
    class FailedPreconditionException(
        message: String = "Operation failed due to precondition",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_FAILED_PRECONDITION, message, cause)
    
    class AbortedException(
        message: String = "Operation aborted",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_ABORTED, message, cause)
    
    class FirestoreUnavailableException(
        message: String = "Service unavailable. Please try again later",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_UNAVAILABLE, message, cause)
    
    class UnauthenticatedException(
        message: String = "User not authenticated",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_UNAUTHENTICATED, message, cause)
    
    class CancelledException(
        message: String = "Operation cancelled",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_CANCELLED, message, cause)
    
    class DeadlineExceededException(
        message: String = "Deadline exceeded",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_DEADLINE_EXCEEDED, message, cause)
    
    class DataLossException(
        message: String = "Data loss occurred",
        cause: Throwable? = null
    ) : AppException(ErrorCode.FIRESTORE_DATA_LOSS, message, cause)

    // ══════════════════════════════════════════════════════════════════
    // VALIDATION EXCEPTIONS
    // ══════════════════════════════════════════════════════════════════
    
    class EmptyFieldException(
        val fieldName: String,
        message: String = "$fieldName cannot be empty"
    ) : AppException(ErrorCode.VALIDATION_EMPTY_FIELD, message)
    
    class InvalidFormatException(
        val fieldName: String,
        message: String = "Invalid $fieldName format"
    ) : AppException(ErrorCode.VALIDATION_INVALID_FORMAT, message)

    // ══════════════════════════════════════════════════════════════════
    // GENERAL EXCEPTIONS
    // ══════════════════════════════════════════════════════════════════
    
    class UnknownException(
        message: String = "An unknown error occurred",
        cause: Throwable? = null
    ) : AppException(ErrorCode.UNKNOWN_ERROR, message, cause)
    
    class UnexpectedException(
        message: String = "An unexpected error occurred",
        cause: Throwable? = null
    ) : AppException(ErrorCode.UNEXPECTED_ERROR, message, cause)
    
    class ParseException(
        message: String = "Failed to parse data",
        cause: Throwable? = null
    ) : AppException(ErrorCode.PARSE_ERROR, message, cause)
    
    class CacheException(
        message: String = "Cache error",
        cause: Throwable? = null
    ) : AppException(ErrorCode.CACHE_ERROR, message, cause)
}
```

### Step 3.3: Create AppResult Sealed Class

**File:** `core/error/AppResult.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * A wrapper class for handling success and error states in a functional way.
 * Used across all layers: DataSource → Repository → UseCase → ViewModel
 */
sealed class AppResult<out T> {
    
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : AppResult<T>()
    
    /**
     * Represents a failed operation with an exception.
     */
    data class Error(val exception: AppException) : AppResult<Nothing>()
    
    /**
     * Returns true if this is a Success.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if this is an Error.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Returns the data if Success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Returns the exception if Error, null otherwise.
     */
    fun exceptionOrNull(): AppException? = when (this) {
        is Success -> null
        is Error -> exception
    }
    
    /**
     * Returns the data if Success, throws exception otherwise.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    /**
     * Returns the data if Success, default value otherwise.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
    }
    
    /**
     * Returns the data if Success, result of onError otherwise.
     */
    inline fun getOrElse(onError: (AppException) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> onError(exception)
    }
    
    /**
     * Maps the success value to another type.
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    /**
     * Maps the success value to another AppResult (flatMap).
     */
    inline fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
    
    /**
     * Executes the given block if this is Success.
     */
    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Executes the given block if this is Error.
     */
    inline fun onError(action: (AppException) -> Unit): AppResult<T> {
        if (this is Error) action(exception)
        return this
    }
    
    /**
     * Folds the result into a single value.
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppException) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
    }
    
    companion object {
        /**
         * Creates a Success result.
         */
        fun <T> success(data: T): AppResult<T> = Success(data)
        
        /**
         * Creates an Error result.
         */
        fun error(exception: AppException): AppResult<Nothing> = Error(exception)
        
        /**
         * Wraps a suspending block in a try-catch and returns AppResult.
         */
        suspend inline fun <T> runCatching(block: () -> T): AppResult<T> {
            return try {
                Success(block())
            } catch (e: AppException) {
                Error(e)
            } catch (e: Exception) {
                Error(AppException.UnknownException(e.message ?: "Unknown error", e))
            }
        }
    }
}
```

### Step 3.4: Create AppResult Extensions

**File:** `core/utils/extension/AppResultExtension.kt`

```kotlin
package com.example.dukkanapp.core.utils.extension

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.AppResult

/**
 * Combines two AppResults into a Pair.
 */
inline fun <T1, T2> AppResult<T1>.combine(
    other: AppResult<T2>
): AppResult<Pair<T1, T2>> {
    return when {
        this is AppResult.Error -> this
        other is AppResult.Error -> other
        else -> AppResult.Success(
            (this as AppResult.Success).data to (other as AppResult.Success).data
        )
    }
}

/**
 * Combines three AppResults into a Triple.
 */
inline fun <T1, T2, T3> combineResults(
    r1: AppResult<T1>,
    r2: AppResult<T2>,
    r3: AppResult<T3>
): AppResult<Triple<T1, T2, T3>> {
    return when {
        r1 is AppResult.Error -> r1
        r2 is AppResult.Error -> r2
        r3 is AppResult.Error -> r3
        else -> AppResult.Success(
            Triple(
                (r1 as AppResult.Success).data,
                (r2 as AppResult.Success).data,
                (r3 as AppResult.Success).data
            )
        )
    }
}

/**
 * Converts a nullable value to AppResult.
 */
fun <T> T?.toAppResult(
    errorMessage: String = "Value is null"
): AppResult<T> {
    return if (this != null) {
        AppResult.Success(this)
    } else {
        AppResult.Error(AppException.UnknownException(errorMessage))
    }
}

/**
 * Recovers from an error with a default value.
 */
inline fun <T> AppResult<T>.recover(
    transform: (AppException) -> T
): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Error -> AppResult.Success(transform(exception))
}

/**
 * Recovers from an error with another AppResult.
 */
inline fun <T> AppResult<T>.recoverWith(
    transform: (AppException) -> AppResult<T>
): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Error -> transform(exception)
}

/**
 * Filters the success value, returning error if predicate fails.
 */
inline fun <T> AppResult<T>.filter(
    errorMessage: String = "Filter predicate failed",
    predicate: (T) -> Boolean
): AppResult<T> = when (this) {
    is AppResult.Success -> if (predicate(data)) this 
        else AppResult.Error(AppException.UnknownException(errorMessage))
    is AppResult.Error -> this
}

/**
 * Swaps Success and Error.
 */
fun <T> AppResult<T>.swap(): AppResult<AppException> = when (this) {
    is AppResult.Success -> AppResult.Error(
        AppException.UnexpectedException("Swapped success to error")
    )
    is AppResult.Error -> AppResult.Success(exception)
}
```

---

## 4. Firebase Exception Mapping

### Step 4.1: Create Firebase Auth Exception Mapper

**File:** `core/error/firebase/FirebaseAuthExceptionMapper.kt`

```kotlin
package com.example.dukkanapp.core.error.firebase

import com.example.dukkanapp.core.error.AppException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.io.IOException

/**
 * Maps Firebase Authentication exceptions to AppException.
 * Use this in your Auth remote data source.
 */
object FirebaseAuthExceptionMapper {
    
    /**
     * Maps any throwable from Firebase Auth operations to AppException.
     */
    fun map(throwable: Throwable): AppException {
        return when (throwable) {
            // Specific Firebase Auth exceptions
            is FirebaseAuthWeakPasswordException -> {
                AppException.WeakPasswordException(
                    message = throwable.reason ?: "Password is too weak",
                    cause = throwable
                )
            }
            
            is FirebaseAuthInvalidCredentialsException -> {
                mapInvalidCredentialsException(throwable)
            }
            
            is FirebaseAuthInvalidUserException -> {
                mapInvalidUserException(throwable)
            }
            
            is FirebaseAuthUserCollisionException -> {
                mapUserCollisionException(throwable)
            }
            
            is FirebaseAuthException -> {
                mapFirebaseAuthException(throwable)
            }
            
            // Network errors
            is IOException -> {
                AppException.NetworkException(
                    message = throwable.message ?: "Network error",
                    cause = throwable
                )
            }
            
            // Already an AppException
            is AppException -> throwable
            
            // Unknown exception
            else -> {
                AppException.UnknownException(
                    message = throwable.message ?: "Unknown authentication error",
                    cause = throwable
                )
            }
        }
    }
    
    private fun mapInvalidCredentialsException(
        exception: FirebaseAuthInvalidCredentialsException
    ): AppException {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> AppException.InvalidEmailException(cause = exception)
            "ERROR_WRONG_PASSWORD" -> AppException.WrongPasswordException(cause = exception)
            "ERROR_INVALID_CREDENTIAL" -> AppException.InvalidCredentialException(cause = exception)
            "ERROR_INVALID_VERIFICATION_CODE" -> AppException.InvalidVerificationCodeException(cause = exception)
            "ERROR_INVALID_VERIFICATION_ID" -> AppException.InvalidVerificationIdException(cause = exception)
            else -> AppException.InvalidCredentialException(
                message = exception.message ?: "Invalid credentials",
                cause = exception
            )
        }
    }
    
    private fun mapInvalidUserException(
        exception: FirebaseAuthInvalidUserException
    ): AppException {
        return when (exception.errorCode) {
            "ERROR_USER_NOT_FOUND" -> AppException.UserNotFoundException(cause = exception)
            "ERROR_USER_DISABLED" -> AppException.UserDisabledException(cause = exception)
            "ERROR_USER_TOKEN_EXPIRED" -> AppException.SessionExpiredException(cause = exception)
            else -> AppException.UserNotFoundException(
                message = exception.message ?: "User not found",
                cause = exception
            )
        }
    }
    
    private fun mapUserCollisionException(
        exception: FirebaseAuthUserCollisionException
    ): AppException {
        return when (exception.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> AppException.EmailAlreadyInUseException(cause = exception)
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> {
                AppException.AccountExistsWithDifferentCredentialException(cause = exception)
            }
            else -> AppException.EmailAlreadyInUseException(
                message = exception.message ?: "Email already in use",
                cause = exception
            )
        }
    }
    
    private fun mapFirebaseAuthException(
        exception: FirebaseAuthException
    ): AppException {
        return when (exception.errorCode) {
            // Email errors
            "ERROR_INVALID_EMAIL" -> AppException.InvalidEmailException(cause = exception)
            "ERROR_EMAIL_ALREADY_IN_USE" -> AppException.EmailAlreadyInUseException(cause = exception)
            
            // Password errors
            "ERROR_WRONG_PASSWORD" -> AppException.WrongPasswordException(cause = exception)
            "ERROR_WEAK_PASSWORD" -> AppException.WeakPasswordException(cause = exception)
            
            // User errors
            "ERROR_USER_NOT_FOUND" -> AppException.UserNotFoundException(cause = exception)
            "ERROR_USER_DISABLED" -> AppException.UserDisabledException(cause = exception)
            "ERROR_USER_TOKEN_EXPIRED" -> AppException.SessionExpiredException(cause = exception)
            
            // Credential errors
            "ERROR_INVALID_CREDENTIAL" -> AppException.InvalidCredentialException(cause = exception)
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> {
                AppException.AccountExistsWithDifferentCredentialException(cause = exception)
            }
            
            // Operation errors
            "ERROR_OPERATION_NOT_ALLOWED" -> AppException.OperationNotAllowedException(cause = exception)
            "ERROR_TOO_MANY_REQUESTS" -> AppException.TooManyRequestsException(cause = exception)
            "ERROR_REQUIRES_RECENT_LOGIN" -> AppException.RequiresRecentLoginException(cause = exception)
            
            // Verification errors
            "ERROR_INVALID_VERIFICATION_CODE" -> AppException.InvalidVerificationCodeException(cause = exception)
            "ERROR_INVALID_VERIFICATION_ID" -> AppException.InvalidVerificationIdException(cause = exception)
            "ERROR_SESSION_EXPIRED" -> AppException.SessionExpiredException(cause = exception)
            
            // Quota errors
            "ERROR_QUOTA_EXCEEDED" -> AppException.QuotaExceededException(cause = exception)
            
            // Network error
            "ERROR_NETWORK_REQUEST_FAILED" -> AppException.NetworkException(cause = exception)
            
            // Unknown
            else -> AppException.UnknownException(
                message = exception.message ?: "Authentication error",
                cause = exception
            )
        }
    }
}
```

### Step 4.2: Create Firebase Firestore Exception Mapper

**File:** `core/error/firebase/FirebaseFirestoreExceptionMapper.kt`

```kotlin
package com.example.dukkanapp.core.error.firebase

import com.example.dukkanapp.core.error.AppException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/**
 * Maps Firebase Firestore exceptions to AppException.
 * Use this in your Firestore remote data sources.
 */
object FirebaseFirestoreExceptionMapper {
    
    /**
     * Maps any throwable from Firestore operations to AppException.
     */
    fun map(throwable: Throwable): AppException {
        return when (throwable) {
            is FirebaseFirestoreException -> mapFirestoreException(throwable)
            
            is IOException -> AppException.NetworkException(
                message = throwable.message ?: "Network error",
                cause = throwable
            )
            
            is AppException -> throwable
            
            else -> AppException.UnknownException(
                message = throwable.message ?: "Unknown Firestore error",
                cause = throwable
            )
        }
    }
    
    private fun mapFirestoreException(
        exception: FirebaseFirestoreException
    ): AppException {
        return when (exception.code) {
            FirebaseFirestoreException.Code.OK -> {
                // This shouldn't happen, but handle it
                AppException.UnknownException("Unexpected OK status", exception)
            }
            
            FirebaseFirestoreException.Code.CANCELLED -> {
                AppException.CancelledException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.UNKNOWN -> {
                AppException.UnknownException(
                    message = exception.message ?: "Unknown Firestore error",
                    cause = exception
                )
            }
            
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> {
                AppException.InvalidFormatException(
                    fieldName = "data",
                    message = exception.message ?: "Invalid argument"
                )
            }
            
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                AppException.DeadlineExceededException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.NOT_FOUND -> {
                AppException.DocumentNotFoundException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.ALREADY_EXISTS -> {
                AppException.DocumentAlreadyExistsException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                AppException.PermissionDeniedException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {
                AppException.ResourceExhaustedException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                AppException.FailedPreconditionException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.ABORTED -> {
                AppException.AbortedException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.OUT_OF_RANGE -> {
                AppException.UnknownException(
                    message = "Out of range: ${exception.message}",
                    cause = exception
                )
            }
            
            FirebaseFirestoreException.Code.UNIMPLEMENTED -> {
                AppException.UnknownException(
                    message = "Unimplemented: ${exception.message}",
                    cause = exception
                )
            }
            
            FirebaseFirestoreException.Code.INTERNAL -> {
                AppException.UnknownException(
                    message = "Internal error: ${exception.message}",
                    cause = exception
                )
            }
            
            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                AppException.FirestoreUnavailableException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.DATA_LOSS -> {
                AppException.DataLossException(cause = exception)
            }
            
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                AppException.UnauthenticatedException(cause = exception)
            }
        }
    }
}
```

---

## 5. Data Source Implementation

### Step 5.1: Create Auth Remote Data Source Interface

**File:** `features/auth/data/datasource/AuthRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.features.auth.data.model.UserDto
import com.google.firebase.auth.FirebaseUser

/**
 * Remote data source for authentication operations.
 * All methods throw AppException on failure.
 */
interface AuthRemoteDataSource {
    
    /**
     * Creates a new user with email and password.
     * @throws AppException on failure
     */
    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser
    
    /**
     * Signs in with email and password.
     * @throws AppException on failure
     */
    suspend fun signInWithEmail(email: String, password: String): FirebaseUser
    
    /**
     * Sends email verification to current user.
     * @throws AppException on failure
     */
    suspend fun sendEmailVerification()
    
    /**
     * Reloads the current user data.
     * @throws AppException on failure
     */
    suspend fun reloadUser(): FirebaseUser
    
    /**
     * Gets current user or null.
     */
    fun getCurrentUser(): FirebaseUser?
    
    /**
     * Checks if email is verified.
     */
    fun isEmailVerified(): Boolean
    
    /**
     * Signs out the current user.
     */
    fun signOut()
    
    /**
     * Sends password reset email.
     * @throws AppException on failure
     */
    suspend fun sendPasswordResetEmail(email: String)
}
```

### Step 5.2: Implement Auth Remote Data Source

**File:** `features/auth/data/datasource/AuthRemoteDataSourceImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.firebase.FirebaseAuthExceptionMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRemoteDataSource {
    
    override suspend fun signUpWithEmail(email: String, password: String): FirebaseUser {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user ?: throw AppException.UnknownException("User creation failed")
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseAuthExceptionMapper.map(e)
        }
    }
    
    override suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user ?: throw AppException.UnknownException("Sign in failed")
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseAuthExceptionMapper.map(e)
        }
    }
    
    override suspend fun sendEmailVerification() {
        try {
            val user = firebaseAuth.currentUser 
                ?: throw AppException.UserNotFoundException("No current user")
            user.sendEmailVerification().await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseAuthExceptionMapper.map(e)
        }
    }
    
    override suspend fun reloadUser(): FirebaseUser {
        return try {
            val user = firebaseAuth.currentUser 
                ?: throw AppException.UserNotFoundException("No current user")
            user.reload().await()
            firebaseAuth.currentUser 
                ?: throw AppException.UserNotFoundException("User not found after reload")
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseAuthExceptionMapper.map(e)
        }
    }
    
    override fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    
    override fun isEmailVerified(): Boolean = firebaseAuth.currentUser?.isEmailVerified ?: false
    
    override fun signOut() = firebaseAuth.signOut()
    
    override suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseAuthExceptionMapper.map(e)
        }
    }
}
```

### Step 5.3: Create User Remote Data Source Interface

**File:** `features/auth/data/datasource/UserRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.features.auth.data.model.UserDto

/**
 * Remote data source for user Firestore operations.
 * All methods throw AppException on failure.
 */
interface UserRemoteDataSource {
    
    /**
     * Creates a new user document in Firestore.
     * @throws AppException on failure
     */
    suspend fun createUser(userDto: UserDto)
    
    /**
     * Gets a user document by ID.
     * @throws AppException on failure
     * @return UserDto or null if not found
     */
    suspend fun getUser(uid: String): UserDto?
    
    /**
     * Updates a user document.
     * @throws AppException on failure
     */
    suspend fun updateUser(userDto: UserDto)
    
    /**
     * Deletes a user document.
     * @throws AppException on failure
     */
    suspend fun deleteUser(uid: String)
    
    /**
     * Updates specific fields of a user document.
     * @throws AppException on failure
     */
    suspend fun updateUserFields(uid: String, fields: Map<String, Any?>)
}
```

### Step 5.4: Implement User Remote Data Source

**File:** `features/auth/data/datasource/UserRemoteDataSourceImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.firebase.FirebaseFirestoreExceptionMapper
import com.example.dukkanapp.features.auth.data.model.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRemoteDataSource {
    
    companion object {
        private const val USERS_COLLECTION = "users"
    }
    
    override suspend fun createUser(userDto: UserDto) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userDto.uid)
                .set(userDto)
                .await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseFirestoreExceptionMapper.map(e)
        }
    }
    
    override suspend fun getUser(uid: String): UserDto? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            document.toObject(UserDto::class.java)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseFirestoreExceptionMapper.map(e)
        }
    }
    
    override suspend fun updateUser(userDto: UserDto) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userDto.uid)
                .set(userDto, SetOptions.merge())
                .await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseFirestoreExceptionMapper.map(e)
        }
    }
    
    override suspend fun deleteUser(uid: String) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .delete()
                .await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseFirestoreExceptionMapper.map(e)
        }
    }
    
    override suspend fun updateUserFields(uid: String, fields: Map<String, Any?>) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(fields)
                .await()
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseFirestoreExceptionMapper.map(e)
        }
    }
}
```

---

## 6. Repository Implementation

### Step 6.1: Update AuthRepository Interface

**File:** `features/auth/domain/repository/AuthRepository.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    
    val currentUser: Flow<User?>
    
    suspend fun isLoggedIn(): Boolean
    
    suspend fun signUpWithEmail(email: String, password: String): AppResult<User>
    
    suspend fun signInWithEmail(email: String, password: String): AppResult<User>
    
    suspend fun sendEmailVerification(): AppResult<Unit>
    
    suspend fun reloadUser(): AppResult<User>
    
    suspend fun isEmailVerified(): Boolean
    
    suspend fun createUserInFirestore(user: User): AppResult<Unit>
    
    suspend fun getUserFromFirestore(uid: String): AppResult<User?>
    
    suspend fun updateUserInFirestore(user: User): AppResult<Unit>
    
    suspend fun signOut()
    
    suspend fun sendPasswordResetEmail(email: String): AppResult<Unit>
}
```

### Step 6.2: Update AuthRepositoryImpl

**File:** `features/auth/data/repository/AuthRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.repository

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.data.datasource.AuthRemoteDataSource
import com.example.dukkanapp.features.auth.data.datasource.UserRemoteDataSource
import com.example.dukkanapp.features.auth.data.model.UserDto
import com.example.dukkanapp.features.auth.domain.model.User
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun isLoggedIn(): Boolean {
        return authRemoteDataSource.getCurrentUser() != null
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): AppResult<User> {
        return try {
            val firebaseUser = authRemoteDataSource.signUpWithEmail(email, password)
            AppResult.Success(firebaseUser.toDomain())
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AppResult<User> {
        return try {
            val firebaseUser = authRemoteDataSource.signInWithEmail(email, password)
            AppResult.Success(firebaseUser.toDomain())
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun sendEmailVerification(): AppResult<Unit> {
        return try {
            authRemoteDataSource.sendEmailVerification()
            AppResult.Success(Unit)
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun reloadUser(): AppResult<User> {
        return try {
            val firebaseUser = authRemoteDataSource.reloadUser()
            AppResult.Success(firebaseUser.toDomain())
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        return authRemoteDataSource.isEmailVerified()
    }

    override suspend fun createUserInFirestore(user: User): AppResult<Unit> {
        return try {
            val userDto = UserDto.fromDomain(user)
            userRemoteDataSource.createUser(userDto)
            AppResult.Success(Unit)
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun getUserFromFirestore(uid: String): AppResult<User?> {
        return try {
            val userDto = userRemoteDataSource.getUser(uid)
            AppResult.Success(userDto?.toDomain())
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun updateUserInFirestore(user: User): AppResult<Unit> {
        return try {
            val userDto = UserDto.fromDomain(user)
            userRemoteDataSource.updateUser(userDto)
            AppResult.Success(Unit)
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }

    override suspend fun signOut() {
        authRemoteDataSource.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): AppResult<Unit> {
        return try {
            authRemoteDataSource.sendPasswordResetEmail(email)
            AppResult.Success(Unit)
        } catch (e: AppException) {
            AppResult.Error(e)
        }
    }
    
    private fun FirebaseUser.toDomain(): User = User(
        uid = uid,
        email = email ?: "",
        displayName = displayName ?: "",
        photoUrl = photoUrl?.toString(),
        isEmailVerified = isEmailVerified
    )
}
```

---

## 7. UseCase Implementation

### Step 7.1: Update SignUpUseCase

**File:** `features/auth/domain/usecase/SignUpUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.model.User
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): AppResult<User> {
        // Step 1: Create user in Firebase Auth
        val authResult = authRepository.signUpWithEmail(email, password)
        
        if (authResult is AppResult.Error) {
            return authResult
        }
        
        val user = (authResult as AppResult.Success).data
        
        // Step 2: Create user document in Firestore
        val firestoreResult = authRepository.createUserInFirestore(user)
        
        if (firestoreResult is AppResult.Error) {
            return firestoreResult
        }
        
        // Step 3: Send email verification
        val verificationResult = authRepository.sendEmailVerification()
        
        if (verificationResult is AppResult.Error) {
            // Log but don't fail - user is created, verification can be resent
            // You might want to log this error
        }
        
        return AppResult.Success(user)
    }
}
```

---

## 8. ViewModel Error Handling

### Step 8.1: Create Error Code to StringRes Mapper

**File:** `core/error/ErrorCodeMapper.kt`

```kotlin
package com.example.dukkanapp.core.error

import androidx.annotation.StringRes
import com.example.dukkanapp.R

/**
 * Maps ErrorCode to string resource ID for UI display.
 */
object ErrorCodeMapper {
    
    @StringRes
    fun toStringRes(errorCode: ErrorCode): Int {
        return when (errorCode) {
            // Network errors
            ErrorCode.NETWORK_ERROR -> R.string.error_network
            ErrorCode.TIMEOUT_ERROR -> R.string.error_timeout
            ErrorCode.NO_INTERNET -> R.string.error_no_internet
            
            // Auth errors
            ErrorCode.AUTH_INVALID_EMAIL -> R.string.error_invalid_email
            ErrorCode.AUTH_WRONG_PASSWORD -> R.string.error_wrong_password
            ErrorCode.AUTH_USER_NOT_FOUND -> R.string.error_user_not_found
            ErrorCode.AUTH_EMAIL_ALREADY_IN_USE -> R.string.error_email_already_in_use
            ErrorCode.AUTH_WEAK_PASSWORD -> R.string.error_weak_password
            ErrorCode.AUTH_USER_DISABLED -> R.string.error_user_disabled
            ErrorCode.AUTH_OPERATION_NOT_ALLOWED -> R.string.error_operation_not_allowed
            ErrorCode.AUTH_TOO_MANY_REQUESTS -> R.string.error_too_many_requests
            ErrorCode.AUTH_INVALID_CREDENTIAL -> R.string.error_invalid_credential
            ErrorCode.AUTH_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> R.string.error_account_exists_different_credential
            ErrorCode.AUTH_REQUIRES_RECENT_LOGIN -> R.string.error_requires_recent_login
            ErrorCode.AUTH_EMAIL_NOT_VERIFIED -> R.string.error_email_not_verified
            ErrorCode.AUTH_INVALID_VERIFICATION_CODE -> R.string.error_invalid_verification_code
            ErrorCode.AUTH_INVALID_VERIFICATION_ID -> R.string.error_invalid_verification_id
            ErrorCode.AUTH_SESSION_EXPIRED -> R.string.error_session_expired
            ErrorCode.AUTH_QUOTA_EXCEEDED -> R.string.error_quota_exceeded
            
            // Firestore errors
            ErrorCode.FIRESTORE_PERMISSION_DENIED -> R.string.error_permission_denied
            ErrorCode.FIRESTORE_NOT_FOUND -> R.string.error_document_not_found
            ErrorCode.FIRESTORE_ALREADY_EXISTS -> R.string.error_document_already_exists
            ErrorCode.FIRESTORE_RESOURCE_EXHAUSTED -> R.string.error_resource_exhausted
            ErrorCode.FIRESTORE_FAILED_PRECONDITION -> R.string.error_failed_precondition
            ErrorCode.FIRESTORE_ABORTED -> R.string.error_operation_aborted
            ErrorCode.FIRESTORE_OUT_OF_RANGE -> R.string.error_out_of_range
            ErrorCode.FIRESTORE_UNIMPLEMENTED -> R.string.error_unimplemented
            ErrorCode.FIRESTORE_INTERNAL -> R.string.error_internal
            ErrorCode.FIRESTORE_UNAVAILABLE -> R.string.error_service_unavailable
            ErrorCode.FIRESTORE_DATA_LOSS -> R.string.error_data_loss
            ErrorCode.FIRESTORE_UNAUTHENTICATED -> R.string.error_unauthenticated
            ErrorCode.FIRESTORE_CANCELLED -> R.string.error_operation_cancelled
            ErrorCode.FIRESTORE_DEADLINE_EXCEEDED -> R.string.error_deadline_exceeded
            
            // Validation errors
            ErrorCode.VALIDATION_EMPTY_FIELD -> R.string.error_empty_field
            ErrorCode.VALIDATION_INVALID_FORMAT -> R.string.error_invalid_format
            ErrorCode.VALIDATION_PASSWORD_MISMATCH -> R.string.error_password_mismatch
            ErrorCode.VALIDATION_PASSWORD_TOO_SHORT -> R.string.error_password_too_short
            
            // General errors
            ErrorCode.UNKNOWN_ERROR -> R.string.error_unknown
            ErrorCode.UNEXPECTED_ERROR -> R.string.error_unexpected
            ErrorCode.PARSE_ERROR -> R.string.error_parse
            ErrorCode.CACHE_ERROR -> R.string.error_cache
        }
    }
    
    /**
     * Extension function for AppException to get StringRes.
     */
    @StringRes
    fun AppException.toStringRes(): Int = toStringRes(errorCode)
}
```

### Step 8.2: Update SignUpViewModel

**File:** `features/auth/presentation/logic/signup/SignUpViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.ErrorCodeMapper.toStringRes
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword
import com.example.dukkanapp.features.auth.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> onEmailChanged(intent.email)
            is SignUpIntent.PasswordChanged -> onPasswordChanged(intent.password)
            is SignUpIntent.ConfirmPasswordChanged -> onConfirmPasswordChanged(intent.confirmPassword)
            SignUpIntent.SignUpClicked -> onSignUpClicked()
            SignUpIntent.DismissError -> onDismissError()
        }
    }

    private fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    private fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty() && password != it.confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else null
            )
        }
    }

    private fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                generalError = null,
                confirmPasswordError = if (confirmPassword.isNotEmpty() && it.password != confirmPassword) {
                    R.string.signup_error_password_mismatch
                } else null
            )
        }
    }

    private fun onDismissError() {
        _state.update { it.copy(generalError = null) }
    }

    private fun onSignUpClicked() {
        val currentState = _state.value
        if (currentState.isLoading) return

        // Validate inputs
        val emailError = when {
            currentState.email.isBlank() -> R.string.signup_error_email_empty
            !currentState.email.isValidEmail() -> R.string.signup_error_email_invalid
            else -> null
        }

        val passwordError = when {
            currentState.password.isEmpty() -> R.string.signup_error_password_empty
            !currentState.password.isValidPassword() -> R.string.signup_error_password_short
            else -> null
        }

        val confirmPasswordError = when {
            currentState.confirmPassword.isEmpty() -> R.string.signup_error_confirm_password_empty
            currentState.password != currentState.confirmPassword -> R.string.signup_error_password_mismatch
            else -> null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null) return

        // Proceed with sign up
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }

            when (val result = signUpUseCase(currentState.email, currentState.password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false, signUpSuccess = true) }
                    _effect.send(SignUpEffect.NavigateToEmailVerification)
                }
                is AppResult.Error -> {
                    val errorRes = result.exception.toStringRes()
                    _state.update { it.copy(isLoading = false, generalError = errorRes) }
                }
            }
        }
    }
}
```

---

## 9. String Resources

### Step 9.1: Add Error Strings to `values/strings.xml`

```xml
<!-- ═══════════════════════════════════════════════════════════════════ -->
<!-- ERROR MESSAGES -->
<!-- ═══════════════════════════════════════════════════════════════════ -->

<!-- Network Errors -->
<string name="error_network">Network error. Please check your connection.</string>
<string name="error_timeout">Request timed out. Please try again.</string>
<string name="error_no_internet">No internet connection.</string>

<!-- Auth Errors -->
<string name="error_invalid_email">Invalid email address.</string>
<string name="error_wrong_password">Incorrect password.</string>
<string name="error_user_not_found">Account not found.</string>
<string name="error_email_already_in_use">This email is already registered.</string>
<string name="error_weak_password">Password is too weak. Use at least 6 characters.</string>
<string name="error_user_disabled">This account has been disabled.</string>
<string name="error_operation_not_allowed">This operation is not allowed.</string>
<string name="error_too_many_requests">Too many attempts. Please try again later.</string>
<string name="error_invalid_credential">Invalid credentials.</string>
<string name="error_account_exists_different_credential">Account exists with different sign-in method.</string>
<string name="error_requires_recent_login">Please sign in again to continue.</string>
<string name="error_email_not_verified">Please verify your email first.</string>
<string name="error_invalid_verification_code">Invalid verification code.</string>
<string name="error_invalid_verification_id">Invalid verification. Please try again.</string>
<string name="error_session_expired">Session expired. Please sign in again.</string>
<string name="error_quota_exceeded">Service quota exceeded. Try again later.</string>

<!-- Firestore Errors -->
<string name="error_permission_denied">Permission denied.</string>
<string name="error_document_not_found">Data not found.</string>
<string name="error_document_already_exists">Data already exists.</string>
<string name="error_resource_exhausted">Service busy. Please try again later.</string>
<string name="error_failed_precondition">Operation failed. Please try again.</string>
<string name="error_operation_aborted">Operation cancelled.</string>
<string name="error_out_of_range">Invalid data range.</string>
<string name="error_unimplemented">Feature not available.</string>
<string name="error_internal">Internal error. Please try again.</string>
<string name="error_service_unavailable">Service unavailable. Please try again later.</string>
<string name="error_data_loss">Data error occurred.</string>
<string name="error_unauthenticated">Please sign in to continue.</string>
<string name="error_operation_cancelled">Operation cancelled.</string>
<string name="error_deadline_exceeded">Request took too long. Please try again.</string>

<!-- Validation Errors -->
<string name="error_empty_field">This field is required.</string>
<string name="error_invalid_format">Invalid format.</string>
<string name="error_password_mismatch">Passwords do not match.</string>
<string name="error_password_too_short">Password is too short.</string>

<!-- General Errors -->
<string name="error_unknown">An error occurred. Please try again.</string>
<string name="error_unexpected">Something went wrong.</string>
<string name="error_parse">Failed to process data.</string>
<string name="error_cache">Cache error occurred.</string>
```

### Step 9.2: Add to `values-ar/strings.xml`

```xml
<!-- ═══════════════════════════════════════════════════════════════════ -->
<!-- رسائل الخطأ -->
<!-- ═══════════════════════════════════════════════════════════════════ -->

<!-- أخطاء الشبكة -->
<string name="error_network">خطأ في الشبكة. يرجى التحقق من اتصالك.</string>
<string name="error_timeout">انتهت مهلة الطلب. يرجى المحاولة مرة أخرى.</string>
<string name="error_no_internet">لا يوجد اتصال بالإنترنت.</string>

<!-- أخطاء المصادقة -->
<string name="error_invalid_email">عنوان البريد الإلكتروني غير صالح.</string>
<string name="error_wrong_password">كلمة المرور غير صحيحة.</string>
<string name="error_user_not_found">الحساب غير موجود.</string>
<string name="error_email_already_in_use">هذا البريد الإلكتروني مسجل بالفعل.</string>
<string name="error_weak_password">كلمة المرور ضعيفة. استخدم 6 أحرف على الأقل.</string>
<string name="error_user_disabled">تم تعطيل هذا الحساب.</string>
<string name="error_operation_not_allowed">هذه العملية غير مسموح بها.</string>
<string name="error_too_many_requests">محاولات كثيرة جدًا. يرجى المحاولة لاحقًا.</string>
<string name="error_invalid_credential">بيانات الاعتماد غير صالحة.</string>
<string name="error_account_exists_different_credential">الحساب موجود بطريقة تسجيل دخول مختلفة.</string>
<string name="error_requires_recent_login">يرجى تسجيل الدخول مرة أخرى للمتابعة.</string>
<string name="error_email_not_verified">يرجى التحقق من بريدك الإلكتروني أولاً.</string>
<string name="error_invalid_verification_code">رمز التحقق غير صالح.</string>
<string name="error_invalid_verification_id">تحقق غير صالح. يرجى المحاولة مرة أخرى.</string>
<string name="error_session_expired">انتهت الجلسة. يرجى تسجيل الدخول مرة أخرى.</string>
<string name="error_quota_exceeded">تم تجاوز حصة الخدمة. حاول لاحقًا.</string>

<!-- أخطاء Firestore -->
<string name="error_permission_denied">تم رفض الإذن.</string>
<string name="error_document_not_found">البيانات غير موجودة.</string>
<string name="error_document_already_exists">البيانات موجودة بالفعل.</string>
<string name="error_resource_exhausted">الخدمة مشغولة. يرجى المحاولة لاحقًا.</string>
<string name="error_failed_precondition">فشلت العملية. يرجى المحاولة مرة أخرى.</string>
<string name="error_operation_aborted">تم إلغاء العملية.</string>
<string name="error_out_of_range">نطاق بيانات غير صالح.</string>
<string name="error_unimplemented">الميزة غير متاحة.</string>
<string name="error_internal">خطأ داخلي. يرجى المحاولة مرة أخرى.</string>
<string name="error_service_unavailable">الخدمة غير متاحة. يرجى المحاولة لاحقًا.</string>
<string name="error_data_loss">حدث خطأ في البيانات.</string>
<string name="error_unauthenticated">يرجى تسجيل الدخول للمتابعة.</string>
<string name="error_operation_cancelled">تم إلغاء العملية.</string>
<string name="error_deadline_exceeded">استغرق الطلب وقتًا طويلاً. يرجى المحاولة مرة أخرى.</string>

<!-- أخطاء التحقق -->
<string name="error_empty_field">هذا الحقل مطلوب.</string>
<string name="error_invalid_format">تنسيق غير صالح.</string>
<string name="error_password_mismatch">كلمات المرور غير متطابقة.</string>
<string name="error_password_too_short">كلمة المرور قصيرة جدًا.</string>

<!-- أخطاء عامة -->
<string name="error_unknown">حدث خطأ. يرجى المحاولة مرة أخرى.</string>
<string name="error_unexpected">حدث خطأ ما.</string>
<string name="error_parse">فشل في معالجة البيانات.</string>
<string name="error_cache">حدث خطأ في ذاكرة التخزين المؤقت.</string>
```

### Step 9.3: Add to `values-fr/strings.xml`

```xml
<!-- ═══════════════════════════════════════════════════════════════════ -->
<!-- MESSAGES D'ERREUR -->
<!-- ═══════════════════════════════════════════════════════════════════ -->

<!-- Erreurs réseau -->
<string name="error_network">Erreur réseau. Vérifiez votre connexion.</string>
<string name="error_timeout">Délai d\'attente dépassé. Veuillez réessayer.</string>
<string name="error_no_internet">Pas de connexion Internet.</string>

<!-- Erreurs d\'authentification -->
<string name="error_invalid_email">Adresse e-mail invalide.</string>
<string name="error_wrong_password">Mot de passe incorrect.</string>
<string name="error_user_not_found">Compte introuvable.</string>
<string name="error_email_already_in_use">Cet e-mail est déjà enregistré.</string>
<string name="error_weak_password">Mot de passe trop faible. Utilisez au moins 6 caractères.</string>
<string name="error_user_disabled">Ce compte a été désactivé.</string>
<string name="error_operation_not_allowed">Cette opération n\'est pas autorisée.</string>
<string name="error_too_many_requests">Trop de tentatives. Veuillez réessayer plus tard.</string>
<string name="error_invalid_credential">Identifiants invalides.</string>
<string name="error_account_exists_different_credential">Le compte existe avec une autre méthode de connexion.</string>
<string name="error_requires_recent_login">Veuillez vous reconnecter pour continuer.</string>
<string name="error_email_not_verified">Veuillez d\'abord vérifier votre e-mail.</string>
<string name="error_invalid_verification_code">Code de vérification invalide.</string>
<string name="error_invalid_verification_id">Vérification invalide. Veuillez réessayer.</string>
<string name="error_session_expired">Session expirée. Veuillez vous reconnecter.</string>
<string name="error_quota_exceeded">Quota de service dépassé. Réessayez plus tard.</string>

<!-- Erreurs Firestore -->
<string name="error_permission_denied">Permission refusée.</string>
<string name="error_document_not_found">Données introuvables.</string>
<string name="error_document_already_exists">Les données existent déjà.</string>
<string name="error_resource_exhausted">Service occupé. Veuillez réessayer plus tard.</string>
<string name="error_failed_precondition">L\'opération a échoué. Veuillez réessayer.</string>
<string name="error_operation_aborted">Opération annulée.</string>
<string name="error_out_of_range">Plage de données invalide.</string>
<string name="error_unimplemented">Fonctionnalité non disponible.</string>
<string name="error_internal">Erreur interne. Veuillez réessayer.</string>
<string name="error_service_unavailable">Service indisponible. Veuillez réessayer plus tard.</string>
<string name="error_data_loss">Une erreur de données s\'est produite.</string>
<string name="error_unauthenticated">Veuillez vous connecter pour continuer.</string>
<string name="error_operation_cancelled">Opération annulée.</string>
<string name="error_deadline_exceeded">La requête a pris trop de temps. Veuillez réessayer.</string>

<!-- Erreurs de validation -->
<string name="error_empty_field">Ce champ est obligatoire.</string>
<string name="error_invalid_format">Format invalide.</string>
<string name="error_password_mismatch">Les mots de passe ne correspondent pas.</string>
<string name="error_password_too_short">Le mot de passe est trop court.</string>

<!-- Erreurs générales -->
<string name="error_unknown">Une erreur s\'est produite. Veuillez réessayer.</string>
<string name="error_unexpected">Une erreur s\'est produite.</string>
<string name="error_parse">Échec du traitement des données.</string>
<string name="error_cache">Une erreur de cache s\'est produite.</string>
```

---

## 10. Usage Examples

### Example 1: Using AppResult in Repository

```kotlin
// In any repository
suspend fun getSomeData(): AppResult<Data> {
    return try {
        val data = remoteDataSource.fetchData() // throws AppException
        AppResult.Success(data)
    } catch (e: AppException) {
        AppResult.Error(e)
    }
}
```

### Example 2: Using AppResult in ViewModel

```kotlin
// In ViewModel
viewModelScope.launch {
    when (val result = useCase()) {
        is AppResult.Success -> {
            _state.update { it.copy(data = result.data) }
        }
        is AppResult.Error -> {
            val errorRes = result.exception.toStringRes()
            _state.update { it.copy(errorMessage = errorRes) }
        }
    }
}
```

### Example 3: Chaining Operations

```kotlin
// Chain multiple operations
suspend fun complexOperation(): AppResult<FinalData> {
    return authRepository.signIn(email, password)
        .flatMap { user -> 
            userRepository.getProfile(user.uid)
        }
        .map { profile ->
            FinalData(profile)
        }
}
```

### Example 4: Using onSuccess/onError

```kotlin
useCase()
    .onSuccess { data ->
        analytics.logSuccess()
    }
    .onError { exception ->
        analytics.logError(exception.errorCode.name)
    }
```

---

## Folder Structure Summary

```
core/
├── error/
│   ├── AppException.kt              # Base exception sealed class
│   ├── AppResult.kt                 # Result wrapper
│   ├── ErrorCode.kt                 # Error code enum
│   ├── ErrorCodeMapper.kt           # Maps ErrorCode to @StringRes
│   └── firebase/
│       ├── FirebaseAuthExceptionMapper.kt
│       └── FirebaseFirestoreExceptionMapper.kt
└── utils/
    └── extension/
        └── AppResultExtension.kt    # Extension functions

features/auth/
├── data/
│   ├── datasource/
│   │   ├── AuthRemoteDataSource.kt          # Interface
│   │   ├── AuthRemoteDataSourceImpl.kt      # Implementation
│   │   ├── UserRemoteDataSource.kt          # Interface
│   │   └── UserRemoteDataSourceImpl.kt      # Implementation
│   ├── model/
│   │   └── UserDto.kt
│   └── repository/
│       └── AuthRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   └── User.kt
│   ├── repository/
│   │   └── AuthRepository.kt
│   └── usecase/
│       └── SignUpUseCase.kt
└── presentation/
    └── logic/
        └── signup/
            └── SignUpViewModel.kt
```

---

## Implementation Checklist

- [ ] Create `core/error/ErrorCode.kt`
- [ ] Create `core/error/AppException.kt`
- [ ] Create `core/error/AppResult.kt`
- [ ] Create `core/error/ErrorCodeMapper.kt`
- [ ] Create `core/error/firebase/FirebaseAuthExceptionMapper.kt`
- [ ] Create `core/error/firebase/FirebaseFirestoreExceptionMapper.kt`
- [ ] Create `core/utils/extension/AppResultExtension.kt`
- [ ] Create `AuthRemoteDataSource` interface and implementation
- [ ] Create `UserRemoteDataSource` interface and implementation
- [ ] Update `AuthRepository` to use `AppResult`
- [ ] Update `AuthRepositoryImpl` to catch and wrap exceptions
- [ ] Update use cases to use `AppResult`
- [ ] Update ViewModels to map errors to `@StringRes`
- [ ] Add all error string resources (EN, AR, FR)
- [ ] Update DI module to provide data sources
