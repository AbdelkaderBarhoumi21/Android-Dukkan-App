# Error Handling Architecture Guide

Centralized error handling for the Dukkan B2C e-commerce app (Firebase Auth + Cloud Firestore), built for an **MVI** presentation layer.

```
Firebase SDK ──throws──► DataSource ─────────throws──► RepositoryImpl ──returns──► UseCase ──► ViewModel ──► UI
 FirebaseAuthException    guardException { }             guardFailure { }            AppResult    UiState     UiText
 FirebaseFirestoreExc.    SDK exception → AppException   AppException → AppFailure   <T>          (MVI)
```

**Two translations, one per layer:**

| Step | Where | From | To | Tool |
|---|---|---|---|---|
| 1 | **DataSource** | Firebase SDK exceptions | `AppException` (thrown) | `guardException { }` / `Flow.guardException()` → `FirebaseExceptionMapper` |
| 2 | **RepositoryImpl** | `AppException` | `AppFailure` (returned in `AppResult`) | `failureMapper.guardFailure { }` / `Flow.guardFailure()` → `DefaultFailureMapper` |

| Type | Kind | Package | Seen by |
|---|---|---|---|
| `AppException` | Sealed `Exception`, **one case per Firebase error**, keeps the SDK `cause` for crash reports | `core/error` | Thrown by data sources, caught by repositories |
| `AppFailure` | Sealed value grouped by what the user can do, no text, no stack trace | `core/error` | Use cases, ViewModels, UI |

The whole error system lives in **one folder, `core/error/`** (section 3). Only `core/error/mapper/FirebaseExceptionMapper.kt` imports Firebase, and only `core/error/ui/` imports `R`.

---

## Table of Contents

0. [Review: What Changed and Why](#0-review-what-changed-and-why)
1. [Principles](#1-principles)
2. [Layer Rules](#2-layer-rules)
3. [Folder Structure](#3-folder-structure)
4. [AppException](#4-appexception)
5. [guardException: Data Source Guard](#5-guardexception-data-source-guard)
6. [AppFailure](#6-appfailure)
7. [AppResult](#7-appresult)
8. [guardFailure: Repository Guard](#8-guardfailure-repository-guard)
9. [Presentation: UiText and AppFailure Mapping](#9-presentation-uitext-and-appfailure-mapping)
10. [Form Validation](#10-form-validation)
11. [MVI Base: MviViewModel and CollectEffect](#11-mvi-base-mviviewmodel-and-collecteffect)
12. [Usage in Each Layer](#12-usage-in-each-layer)
13. [Feature Failures for E-commerce](#13-feature-failures-for-e-commerce)
14. [Dependency Injection](#14-dependency-injection)
15. [String Resources](#15-string-resources)
16. [Testing](#16-testing)
17. [Implementation Checklist](#17-implementation-checklist)

---

## 0. Review: What Changed and Why

### Design goal: every case listed, interpretation in one place

The first version of this guide listed every Firebase error in an `ErrorCode` enum. That full list is worth keeping: you can read every failure the backend can produce in one file. This design keeps it, but as **sealed classes**, and splits the work in two:

| Step | Job | Rule |
|---|---|---|
| 1. `FirebaseExceptionMapper` (data source) | **Translate.** One `AppException` case per Firebase case. | Mechanical 1:1. No decisions. |
| 2. `DefaultFailureMapper` (repository) | **Interpret.** Group ~45 exception cases into ~18 user-meaningful failures. | All decisions live here: what the user sees, what's retryable, what's a developer bug to report. |

### Why sealed classes instead of the `ErrorCode` enum

The enum gave four guarantees. Sealed classes give all four with **one** list instead of two:

| Goal of the enum | With sealed classes |
|---|---|
| No typos (compile-checked list) | Each case is a class. A typo doesn't compile. |
| The compiler forces a decision for every case | `when` with no `else` over a sealed type is exhaustive. A new case doesn't compile until it's mapped. |
| Multi-language via `@StringRes` | `AppFailure.toUiText()` maps each case to a string resource. |
| The app doesn't depend on Firebase | Only `FirebaseExceptionMapper` imports Firebase. |

The enum also needed each case written **twice** (`AUTH_WRONG_PASSWORD` + `WrongPasswordException`) and a third time in `ErrorCodeMapper`. A sealed class *is* the code, and it can carry data (`cause`, `failure`).

### Compared with the Pulse (Retrofit) design

The Pulse structure (`safeApiCall` → `AppException` → `AppErrorMapper` → `AppFailure`) is the same shape as this guide. Kept as is: the sealed `AppException`, the sealed `AppFailure`, and one helper instead of `try/catch` everywhere. Changed for Firebase and for this app:

| Pulse | Problem here | Dukkan |
|---|---|---|
| `safeApiCall` ends with `catch (e: Exception)` | **Bug:** `CancellationException` becomes `UnknownException`, so a cancelled screen reports a fake error. | `guardException` and `guardFailure` rethrow cancellation first. |
| `HttpException` + status codes (404, 422, 5xx) | Firebase has no HTTP codes. It throws typed exceptions (`FirebaseAuthException` + `errorCode`, `FirebaseFirestoreException` + `Code`). | `FirebaseExceptionMapper` handles every Auth code and all 16 Firestore error codes. |
| `AppFailure(message = AppStrings.ERR_…)` | A `String` in the domain can't follow the device language (EN/AR/FR), and `ValidationFailure(serverMessage)` shows raw backend text to users. | `AppFailure` carries no text. `toUiText()` in presentation picks a localized `@StringRes`. |
| `AppErrorMapper` is an `object` with `else -> UnknownFailure()` | The `else` hides unmapped cases. Unknown failures are never reported. | `DefaultFailureMapper` is injected, has **no `else`** over `AppException`, and reports every `Unknown` with its cause. |

### Other fixes from earlier versions

- Wrong password and unknown user both become `Auth.InvalidCredentials`, so the app never reveals which emails are registered.
- A missing Firestore index (`FAILED_PRECONDITION`) is reported as a developer bug instead of telling users to "try again".
- `AppResult.Failure(failure: AppFailure)` replaces `AppResult.Error(exception)`. The domain holds plain values, and `Error` no longer shadows `kotlin.Error`.
- Field validation is a separate `ValidationError` (section 10), not an exception.
- The `AppResult` API is small: `map`, `flatMap`, `onSuccess`, `onFailure`, `fold`, `getOrNull`, `asEmptyResult`.

---

## 1. Principles

1. **Each layer translates once.** Data sources turn SDK exceptions into `AppException`. Repositories turn `AppException` into `AppFailure`. No other layer catches.
2. **Data sources throw only `AppException`.** That is their contract. Nothing Firebase-specific leaks upward.
3. **Repositories never throw.** They return `AppResult<T>`. Use cases and ViewModels never see an exception.
4. **Cancellation is never caught.** `CancellationException` passes through every helper so structured concurrency works.
5. **Developer bugs are reported, not hidden.** Anything that maps to `AppFailure.Unknown` goes to `ErrorReporter` with its original cause.
6. **Failures describe what the user can do**, not which SDK failed: `Auth.InvalidCredentials`, not `ERROR_WRONG_PASSWORD`.
7. **The domain stays pure Kotlin.** Use cases import only `AppResult`, `AppFailure` and `ValidationError`. Those files, and `AppException`, import no Android or Firebase classes.
8. **Loading is UI state, not a result.** `AppResult` has no `Loading` case.

### SOLID map

| Principle | Where |
|---|---|
| **S**ingle Responsibility | `FirebaseExceptionMapper` = translate SDK → `AppException` (1:1, no decisions). `DefaultFailureMapper` = interpret `AppException` → `AppFailure` + reporting. Data source = call Firebase. Repository = boundary. ViewModel = reduce state. |
| **O**pen/Closed | Features add failures through `AppFailure.Feature` without editing core. A new Firebase product (Storage, Functions) adds one `AppException` group and one mapper branch. |
| **L**iskov | Any `FailureMapper` or `ErrorReporter` (real or fake) is interchangeable. |
| **I**nterface Segregation | `FailureMapper` and `ErrorReporter` each have one method. `AuthRepository` and `UserRepository` are separate. |
| **D**ependency Inversion | Repositories depend on the `FailureMapper` interface; ViewModels on use cases; use cases on repository interfaces. |

Patterns: **Result** (`AppResult`), **Strategy** (`FailureMapper`), **Template Method** (`MviViewModel`).

---

## 2. Layer Rules

| Layer | Throws | Catches | Returns | Imports |
|---|---|---|---|---|
| **DataSource** | `AppException` only | Only via `guardException { }` / `Flow.guardException()` | DTOs / Firebase types | Firebase |
| **RepositoryImpl** | Never | Only via `guardFailure { }` / `Flow.guardFailure()` | `AppResult<Domain>` | DataSource, `FailureMapper` |
| **UseCase** | Never | Never | `AppResult<T>` (incl. `Failure(FeatureFailure)` for business rules) | Repository interfaces |
| **ViewModel** | Never | Never | `UiState` / `Effect` | Use cases, `AppFailure`, `UiText` |
| **Composable** | Never | Never | UI | `UiState`, `UiText` |

If you write `try` anywhere else, use a guard instead. The only exception is UI code calling Android APIs (e.g. `ActivityNotFoundException` when starting an Intent).

---

## 3. Folder Structure

```
core/
├── error/                                   # The whole error handling system
│   ├── AppResult.kt                         # Success / Failure + operators            (pure Kotlin)
│   ├── AppFailure.kt                        # What the user sees (returned)            (pure Kotlin)
│   ├── AppException.kt                      # What a data source throws                (pure Kotlin)
│   ├── ErrorReporter.kt                     # Crash reporting (Logcat now, Crashlytics later)
│   ├── guard/
│   │   ├── GuardException.kt                # guardException { }  → data sources
│   │   └── GuardFailure.kt                  # guardFailure { }    → repositories
│   ├── mapper/
│   │   ├── FirebaseExceptionMapper.kt       # Firebase SDK → AppException (only Firebase import)
│   │   └── FailureMapper.kt                 # AppException → AppFailure (+ DefaultFailureMapper)
│   ├── ui/
│   │   ├── AppFailureUiText.kt              # AppFailure → UiText (only R import)
│   │   └── ValidationErrorUiText.kt         # ValidationError → UiText
│   └── validation/
│       └── ValidationError.kt               # Field-level form errors                  (pure Kotlin)
├── common/
│   ├── mvi/
│   │   ├── MviViewModel.kt
│   │   └── CollectEffect.kt
│   └── text/
│       └── UiText.kt                        # General UI text, not error-specific
└── di/
    └── ErrorModule.kt
```

| Folder | Used by | May import |
|---|---|---|
| `core/error/` (root), `validation/` | Every layer | Kotlin only |
| `guard/`, `mapper/` | Data sources, repositories | Firebase (only `FirebaseExceptionMapper`), coroutines |
| `ui/` | ViewModels, composables | `R`, `UiText` |

---

## 4. AppException

The complete catalog of what can go wrong in the data layer: **one case per Firebase error**, plus a few of our own. It carries the original SDK exception as `cause` for crash reports. Only data sources throw it; the domain never sees it.

**File:** `core/error/AppException.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Every failure the data layer can throw, one case per backend error.
 * No Firebase types here: [cause] keeps the original SDK exception for crash reporting.
 * Never shown to the user directly; repositories turn it into an [AppFailure].
 */
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
        class InvalidEmail(cause: Throwable? = null) : Auth(cause)                 // ERROR_INVALID_EMAIL
        class WrongPassword(cause: Throwable? = null) : Auth(cause)                // ERROR_WRONG_PASSWORD
        class UserNotFound(cause: Throwable? = null) : Auth(cause)                 // ERROR_USER_NOT_FOUND
        class InvalidCredential(cause: Throwable? = null) : Auth(cause)            // ERROR_INVALID_CREDENTIAL
        class WeakPassword(cause: Throwable? = null) : Auth(cause)                 // ERROR_WEAK_PASSWORD
        class PasswordRequirementsNotMet(cause: Throwable? = null) : Auth(cause)   // password policy (console)

        // Account
        class EmailAlreadyInUse(cause: Throwable? = null) : Auth(cause)            // ERROR_EMAIL_ALREADY_IN_USE
        class CredentialAlreadyInUse(cause: Throwable? = null) : Auth(cause)       // ERROR_CREDENTIAL_ALREADY_IN_USE
        class AccountExistsWithDifferentCredential(cause: Throwable? = null) : Auth(cause) // ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL
        class UserDisabled(cause: Throwable? = null) : Auth(cause)                 // ERROR_USER_DISABLED

        // Session
        class UserTokenExpired(cause: Throwable? = null) : Auth(cause)             // ERROR_USER_TOKEN_EXPIRED
        class InvalidUserToken(cause: Throwable? = null) : Auth(cause)             // ERROR_INVALID_USER_TOKEN
        class UserMismatch(cause: Throwable? = null) : Auth(cause)                 // ERROR_USER_MISMATCH
        class RequiresRecentLogin(cause: Throwable? = null) : Auth(cause)          // ERROR_REQUIRES_RECENT_LOGIN
        class NoCurrentUser : Auth(null)                                            // ours: no signed-in user

        // Email links and codes
        class InvalidActionCode(cause: Throwable? = null) : Auth(cause)            // expired/used reset or verify link
        class EmailSendFailed(cause: Throwable? = null) : Auth(cause)              // verification/reset mail not sent
        class InvalidVerificationCode(cause: Throwable? = null) : Auth(cause)      // ERROR_INVALID_VERIFICATION_CODE
        class InvalidVerificationId(cause: Throwable? = null) : Auth(cause)        // ERROR_INVALID_VERIFICATION_ID
        class CodeSessionExpired(cause: Throwable? = null) : Auth(cause)           // ERROR_SESSION_EXPIRED (SMS code)

        // Project configuration
        class OperationNotAllowed(cause: Throwable? = null) : Auth(cause)          // ERROR_OPERATION_NOT_ALLOWED
        class QuotaExceeded(cause: Throwable? = null) : Auth(cause)                // ERROR_QUOTA_EXCEEDED
    }

    // ── Cloud Firestore: one case per FirebaseFirestoreException.Code ─────
    sealed class Firestore(cause: Throwable?) : AppException(cause) {
        class Cancelled(cause: Throwable? = null) : Firestore(cause)
        class Unknown(cause: Throwable? = null) : Firestore(cause)
        class InvalidArgument(cause: Throwable? = null) : Firestore(cause)
        class DeadlineExceeded(cause: Throwable? = null) : Firestore(cause)
        class NotFound(cause: Throwable? = null) : Firestore(cause)       // also: document doesn't exist on read
        class AlreadyExists(cause: Throwable? = null) : Firestore(cause)
        class PermissionDenied(cause: Throwable? = null) : Firestore(cause)
        class ResourceExhausted(cause: Throwable? = null) : Firestore(cause)
        class FailedPrecondition(cause: Throwable? = null) : Firestore(cause) // usually a missing index
        class Aborted(cause: Throwable? = null) : Firestore(cause)        // transaction contention
        class OutOfRange(cause: Throwable? = null) : Firestore(cause)
        class Unimplemented(cause: Throwable? = null) : Firestore(cause)
        class Internal(cause: Throwable? = null) : Firestore(cause)
        class Unavailable(cause: Throwable? = null) : Firestore(cause)    // also: offline with no cache
        class DataLoss(cause: Throwable? = null) : Firestore(cause)
        class Unauthenticated(cause: Throwable? = null) : Firestore(cause)
    }

    /** A business rule failed in the data layer (e.g. a Cloud Function said "out of stock"). */
    class Feature(val failure: AppFailure.Feature, cause: Throwable? = null) : AppException(cause)

    /** Anything not in this catalog. Always reported. */
    class Unexpected(cause: Throwable? = null) : AppException(cause)
}
```

> **Why `class` and not `data object`?** Each instance carries its own `cause` (the SDK exception and its stack trace). `AppFailure`, which has no cause, uses `data object`.
>
> **Adding a Firebase product** (Storage for product images, Functions for checkout): add a `sealed class Storage` / `Functions` group here, a branch in `FirebaseExceptionMapper`, and the compiler then shows you every place in `DefaultFailureMapper` that needs a decision.

---

## 5. guardException: Data Source Guard

### Step 5.1: The mapper (step 1 of 2: translate)

The **only** file that imports Firebase exception types. It translates 1:1 and makes no decisions about meaning.

**File:** `core/error/mapper/FirebaseExceptionMapper.kt`

```kotlin
package com.example.dukkanapp.core.error.mapper

import com.example.dukkanapp.core.error.AppException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthPasswordDoesNotMeetRequirementsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException

/**
 * Step 1 of 2: Firebase SDK exception → [AppException], one case per Firebase error.
 * A stateless pure function, so an `object` is fine: tests call it directly, nothing fakes it.
 */
object FirebaseExceptionMapper {

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
        is FirebaseAuthPasswordDoesNotMeetRequirementsException -> AppException.Auth.PasswordRequirementsNotMet(e)
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
            AuthErrorCode.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> AppException.Auth.AccountExistsWithDifferentCredential(e)
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
    const val ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL = "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
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
```

### Step 5.2: guardException

The Firebase equivalent of Pulse's `safeApiCall`. Data sources wrap every Firebase call in it.

**File:** `core/error/guard/GuardException.kt`

```kotlin
package com.example.dukkanapp.core.error.guard

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.mapper.FirebaseExceptionMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * The only try/catch a data source needs. Guarantees the call throws nothing but [AppException]
 * (or [CancellationException], which must always propagate).
 */
inline fun <T> guardException(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw FirebaseExceptionMapper.map(e)
}

/** Same contract for real-time listeners (`snapshots()`). */
fun <T> Flow<T>.guardException(): Flow<T> = catch { e ->
    throw if (e is CancellationException) e else FirebaseExceptionMapper.map(e)
}
```

> `guardException` is `inline`, so `block` may call `suspend` functions (`await()`) when used inside a `suspend fun`.
>
> **Timeouts:** don't use `withTimeout`. Its `TimeoutCancellationException` *is* a `CancellationException` and would be rethrown. Use `withTimeoutOrNull { } ?: throw AppException.Network.Timeout()`.

### Step 5.3: Complete mapping reference

Every case, both steps. **Bold** = developer or configuration bug: shown as a generic message and sent to crash reporting.

| Firebase | Step 1: AppException | Step 2: AppFailure |
|---|---|---|
| `FirebaseNetworkException`, `IOException`, `ERROR_NETWORK_REQUEST_FAILED` | `Network.NoConnection` | `Network.NoConnection` |
| `withTimeoutOrNull` → null | `Network.Timeout` | `Network.ServiceUnavailable` |
| `FirebaseTooManyRequestsException`, `ERROR_TOO_MANY_REQUESTS` | `Network.TooManyRequests` | `Network.RateLimited` |
| `ERROR_INVALID_EMAIL` | `Auth.InvalidEmail` | `Auth.InvalidEmail` |
| `ERROR_WRONG_PASSWORD` | `Auth.WrongPassword` | `Auth.InvalidCredentials` |
| `ERROR_USER_NOT_FOUND` | `Auth.UserNotFound` | `Auth.InvalidCredentials` |
| `ERROR_INVALID_CREDENTIAL` | `Auth.InvalidCredential` | `Auth.InvalidCredentials` |
| `ERROR_WEAK_PASSWORD` | `Auth.WeakPassword` | `Auth.WeakPassword` |
| `FirebaseAuthPasswordDoesNotMeetRequirementsException` | `Auth.PasswordRequirementsNotMet` | `Auth.WeakPassword` |
| `ERROR_EMAIL_ALREADY_IN_USE` | `Auth.EmailAlreadyInUse` | `Auth.EmailAlreadyInUse` |
| `ERROR_CREDENTIAL_ALREADY_IN_USE` | `Auth.CredentialAlreadyInUse` | `Auth.AccountExistsWithDifferentCredential` |
| `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL` | `Auth.AccountExistsWithDifferentCredential` | `Auth.AccountExistsWithDifferentCredential` |
| `ERROR_USER_DISABLED` | `Auth.UserDisabled` | `Auth.UserDisabled` |
| `ERROR_USER_TOKEN_EXPIRED` | `Auth.UserTokenExpired` | `Auth.SessionExpired` |
| `ERROR_INVALID_USER_TOKEN` | `Auth.InvalidUserToken` | `Auth.SessionExpired` |
| `ERROR_USER_MISMATCH` | `Auth.UserMismatch` | `Auth.InvalidCredentials` |
| `ERROR_REQUIRES_RECENT_LOGIN` | `Auth.RequiresRecentLogin` | `Auth.RequiresRecentLogin` |
| No signed-in user (ours) | `Auth.NoCurrentUser` | `Auth.SessionExpired` |
| `FirebaseAuthActionCodeException` | `Auth.InvalidActionCode` | `Auth.CodeInvalidOrExpired` |
| `ERROR_INVALID_VERIFICATION_CODE` | `Auth.InvalidVerificationCode` | `Auth.CodeInvalidOrExpired` |
| `ERROR_SESSION_EXPIRED` | `Auth.CodeSessionExpired` | `Auth.CodeInvalidOrExpired` |
| `FirebaseAuthEmailException` | `Auth.EmailSendFailed` | `Network.ServiceUnavailable` |
| `ERROR_QUOTA_EXCEEDED` | `Auth.QuotaExceeded` | `Network.RateLimited` |
| `ERROR_INVALID_VERIFICATION_ID` | `Auth.InvalidVerificationId` | **`Unknown`** |
| `ERROR_OPERATION_NOT_ALLOWED` | `Auth.OperationNotAllowed` | **`Unknown`** (provider disabled in console) |
| Firestore `UNAVAILABLE` | `Firestore.Unavailable` | `Network.ServiceUnavailable` |
| Firestore `DEADLINE_EXCEEDED` | `Firestore.DeadlineExceeded` | `Network.ServiceUnavailable` |
| Firestore `RESOURCE_EXHAUSTED` | `Firestore.ResourceExhausted` | `Network.RateLimited` |
| Firestore `UNAUTHENTICATED` | `Firestore.Unauthenticated` | `Auth.SessionExpired` |
| Firestore `PERMISSION_DENIED` | `Firestore.PermissionDenied` | `Data.PermissionDenied` |
| Firestore `NOT_FOUND`, missing document | `Firestore.NotFound` | `Data.NotFound` |
| Firestore `ALREADY_EXISTS` | `Firestore.AlreadyExists` | `Data.AlreadyExists` |
| Firestore `ABORTED` | `Firestore.Aborted` | `Data.Conflict` |
| Firestore `FAILED_PRECONDITION` | `Firestore.FailedPrecondition` | **`Unknown`** (missing index) |
| Firestore `INVALID_ARGUMENT`, `OUT_OF_RANGE` | `Firestore.InvalidArgument`, `OutOfRange` | **`Unknown`** (bad query) |
| Firestore `CANCELLED`, `UNKNOWN`, `UNIMPLEMENTED`, `INTERNAL`, `DATA_LOSS` | `Firestore.Cancelled`, `Unknown`, `Unimplemented`, `Internal`, `DataLoss` | **`Unknown`** |
| Business rule from a data source | `Feature(failure)` | `failure` (unchanged) |
| Any other throwable | `Unexpected` | **`Unknown`** |

---

## 6. AppFailure

What went wrong, described for the **user**: grouped by what the UI can do about it. No stack traces.

**File:** `core/error/AppFailure.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Every expected failure in the app, described by what it means to the user.
 * Pure Kotlin: safe to use in the domain layer.
 */
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

/** The UI may offer a "Retry" action. */
val AppFailure.isRetryable: Boolean
    get() = when (this) {
        AppFailure.Network.NoConnection,
        AppFailure.Network.ServiceUnavailable,
        AppFailure.Data.Conflict -> true
        else -> false
    }

/** The user must sign in again before continuing. */
val AppFailure.requiresSignIn: Boolean
    get() = this == AppFailure.Auth.SessionExpired || this == AppFailure.Auth.RequiresRecentLogin
```

> `EmailNotVerified` has no `AppException` counterpart: a use case returns it after reading `AuthUser.isEmailVerified`. That's the normal way for domain rules to fail.

---

## 7. AppResult

**File:** `core/error/AppResult.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Outcome of an operation that can fail in an expected way.
 * Repositories return it, use cases compose it, ViewModels reduce it into UI state.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val failure: AppFailure) : AppResult<Nothing>
}

/** For operations with no meaningful return value (send email, update, delete...). */
typealias EmptyResult = AppResult<Unit>

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppFailure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(failure)
    return this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppFailure) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(failure)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.asEmptyResult(): EmptyResult = map { }
```

That's the whole API. No `getOrThrow` (it would bring exceptions back), no `Loading` (UI state), no `isSuccess` (use `when`/`fold`). Every operator is `inline`, so lambdas may call `suspend` functions inside a coroutine.

| Function        | What it does                                | Returns           | Use when                        |
| --------------- | ------------------------------------------- | ----------------- | ------------------------------- |
| `map`           | changes the data                            | `AppResult<R>`    | simple conversion (DTO → model) |
| `flatMap`       | changes the data, with a step that can fail | `AppResult<R>`    | chaining steps                  |
| `onSuccess`     | runs an action on success                   | same result       | update state, log               |
| `onFailure`     | runs an action on failure                   | same result       | show an error                   |
| `fold`          | handles both cases                          | plain value `R`   | end of the chain                |
| `getOrNull`     | data or null                                | `T?`              | you don’t need the error        |
| `asEmptyResult` | drops the data                              | `AppResult<Unit>` | only success or failure matters |

### Two actions you can do with a box

1. **Take out**: you *read* what is inside.
2. **Put in**: you *write* something new inside.

Analogy: a **vending machine** versus a **drawer**.

- A vending machine only lets you **take out**. You can’t put your own snacks inside.
- A drawer lets you **take out** and **put in**.

`out T` says: “this box is a vending machine.”

### In code

**Take out** is reading a value, for example by returning it:

kotlin

```kotlin
val data: T          // you can read it
fun get(): T         // you can get it
```

**Put in** is writing a value, for example by receiving it as a parameter:

kotlin

```kotlin
var data: T              // you can change it (var)
fun set(value: T)        // you can give it a new value
```

### Our `AppResult`

kotlin

```kotlin
data class Success<out T>(val data: T)
```

- `val data: T`: `val` means read-only. You can **read** the data, but you can’t replace it. That is **taking out**. ✅
- There is no `var` and no function like `set(value: T)`, so nobody can **put in** new data.

So the promise “I will only take T out” is true for `AppResult`.

### What happens if you break the promise?

kotlin

```kotlin
class Box<out T>(var item: T)    // var = you can put in
```

Kotlin shows an error, because `var` allows writing, which breaks the `out` promise. Kotlin checks that you keep your promise.

### Why the promise matters

If nobody can put anything in, nobody can put the wrong thing in. So Kotlin can safely allow:

kotlin

```kotlin
AppResult<Nothing>  →  used as  →  AppResult<ProductDto>
```

The box is a vending machine, so there is no way to put a wrong item inside.

### One line to remember

> `out T` = **read-only**: the data only comes out, nobody can put new data in.

---

## 8. guardFailure: Repository Guard

### Step 8.1: Crash reporting

**File:** `core/error/ErrorReporter.kt`

```kotlin
package com.example.dukkanapp.core.error

import android.util.Log
import javax.inject.Inject

/** Sends unexpected throwables to crash reporting. Swap the binding for Crashlytics later. */
fun interface ErrorReporter {
    fun report(throwable: Throwable)
}

class LogcatErrorReporter @Inject constructor() : ErrorReporter {
    override fun report(throwable: Throwable) {
        Log.e("Dukkan", "Unexpected failure", throwable)
    }
}
```

> **Crashlytics later:** `class CrashlyticsErrorReporter @Inject constructor() : ErrorReporter { override fun report(throwable: Throwable) = Firebase.crashlytics.recordException(throwable) }` and change one `@Binds`. Never put emails or passwords in log messages.



didnt understand our ErrorRepoter please explain it simple please



Let’s make it very simple.

### What is the problem?

Sometimes the app has a **real bug**. The user sees “Something went wrong”, but you, the developer, **don’t know it happened**. You need a way to be told.

`ErrorReporter` is that way: **a button that says “tell the developer about this error.”**

### Think of a phone

- `ErrorReporter` = **“a phone”**. It can do one thing: **call** (`report`).
- `LogcatErrorReporter` = **a real phone of one brand**. When you call, it writes the error in Logcat (the log window in Android Studio).
- Later, you can swap it for another brand: `CrashlyticsErrorReporter`. When you call, it sends the error to the internet, so you see it on your computer, even from users’ phones.

The app only knows: “I have a phone, I press call.” It doesn’t care about the brand.

### The code, line by line

kotlin

```kotlin
fun interface ErrorReporter {
    fun report(throwable: Throwable)
}
```

“A reporter is anything that has a `report` function that receives an error.”

kotlin

```kotlin
class LogcatErrorReporter @Inject constructor() : ErrorReporter {
    override fun report(throwable: Throwable) {
        Log.e("Dukkan", "Unexpected failure", throwable)
    }
}
```

“This is one reporter. When `report` is called, it prints the error in red in Logcat.”

### When is it called?

Only for **bugs**. Look at `DefaultFailureMapper`:

kotlin

```kotlin
if (failure == AppFailure.Unknown) reporter.report(throwable)
```

- Wrong password → **not a bug** → no report.
- No internet → **not a bug** → no report.
- Something nobody expected (`Unknown`) → **a bug** → `report` is called.

### Example

1. A user opens a screen, and Firestore throws an error nobody planned for.
2. The user sees: “Something went wrong.”
3. At the same time, the mapper calls `reporter.report(error)`.
4. Today, you see the full error in **Logcat**, with the exact line of code. Later, with Crashlytics, you see it on your dashboard.
5. You fix the bug.

### Why an interface and not just `Log.e(...)` everywhere?

Because of this one reason: **you can change the “brand” in one place**.

- Today: Logcat.
- Later: Crashlytics. You change **one line** (the Hilt binding), and the rest of the app stays the same.

### In one sentence

> `ErrorReporter` = “when an unexpected bug happens, tell the developer.” Today it writes in Logcat, later it can send to Crashlytics.

### Step 8.2: The mapper (step 2 of 2: interpret)

All decisions about meaning live here: which cases look the same to the user, and which ones are bugs to report.

**File:** `core/error/mapper/FailureMapper.kt`

```kotlin
package com.example.dukkanapp.core.error.mapper

import com.example.dukkanapp.core.error.AppException
import com.example.dukkanapp.core.error.AppFailure
import com.example.dukkanapp.core.error.ErrorReporter
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
```

### Step 8.3: guardFailure

Repositories wrap every data source call in it.

**File:** `core/error/guard/GuardFailure.kt`

```kotlin
package com.example.dukkanapp.core.error.guard

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.mapper.FailureMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The repository boundary: nothing thrown inside escapes as an exception.
 * Catches every [Exception] (not only AppException) so a DTO mapping bug can't crash the app;
 * JVM `Error`s (OOM) and cancellation still propagate.
 */
inline fun <T> FailureMapper.guardFailure(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Failure(map(e))
}

/** For real-time listeners (cart, stock, order status). Emits one Failure, then completes. */
fun <T> Flow<T>.guardFailure(failureMapper: FailureMapper): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }
        .catch { e ->
            if (e is CancellationException) throw e
            emit(AppResult.Failure(failureMapper.map(e)))
        }
```

### “Emits one Failure, then completes”

After a Flow hits an error, **it cannot continue**. So the stream looks like this:

```
Success(cart1) → Success(cart2) → ❌ error → Failure(NoConnection) → (stream ends)
```

The Failure is the **last item**. After it, the Flow completes. The screen receives the failure as a normal value, and no crash happens.

If you want it to listen again (for example, after a retry button), you must collect the Flow again.

### Where it is used

In the Repository, the last step of the chain:

kotlin

```kotlin
fun observeCart(): Flow<AppResult<Cart>> =
    cartDoc.snapshots()                                  // real-time stream
        .map { it.toObject(CartDto::class.java) }
        .guardException()                                // step 1: Firebase error → AppException
        .guardFailure(failureMapper)                     // step 2: AppException → AppResult
```

And in the ViewModel:

kotlin

```kotlin
repository.observeCart().collect { result ->
    result
        .onSuccess { cart -> setState { copy(cart = cart) } }
        .onFailure { failure -> setState { copy(error = failure.toUiText()) } }
}
```

### The two Flow functions side by side

|                            | `guardException()`           | `guardFailure(mapper)`        |
| -------------------------- | ---------------------------- | ----------------------------- |
| Layer                      | DataSource                   | Repository                    |
| On error                   | **throws** an `AppException` | **emits** `AppResult.Failure` |
| Output                     | `Flow<T>`                    | `Flow<AppResult<T>>`          |
| Same rule for cancellation | rethrow                      | rethrow                       |

---

## 9. Presentation: UiText and AppFailure Mapping

**File:** `core/common/text/UiText.kt`

```kotlin
package com.example.dukkanapp.core.common.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text the ViewModel can produce without holding a Context. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> value
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Res -> context.getString(id, *args.toTypedArray())
    is UiText.Raw -> value
}
```

**File:** `core/error/ui/AppFailureUiText.kt`

```kotlin
package com.example.dukkanapp.core.error.ui

import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.error.AppFailure

/** Generic user message for any [AppFailure]. Features map their own [AppFailure.Feature] first. */
fun AppFailure.toUiText(): UiText = UiText.Res(messageRes)

private val AppFailure.messageRes: Int
    @StringRes get() = when (this) {
        AppFailure.Network.NoConnection -> R.string.error_no_connection
        AppFailure.Network.ServiceUnavailable -> R.string.error_service_unavailable
        AppFailure.Network.RateLimited -> R.string.error_too_many_requests

        AppFailure.Auth.InvalidEmail -> R.string.error_invalid_email
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
```

The `when` is exhaustive. Adding a new `AppFailure` case is a **compile error** until it gets a message.

---

## 10. Form Validation

Field validation is **not** an `AppResult` concern. It is synchronous, needs no I/O, and its errors belong to individual fields.

**File:** `core/error/validation/ValidationError.kt`

```kotlin
package com.example.dukkanapp.core.error.validation

sealed interface ValidationError {
    data object EmailEmpty : ValidationError
    data object EmailInvalid : ValidationError
    data object PasswordEmpty : ValidationError
    data class PasswordTooShort(val minLength: Int) : ValidationError
    data object ConfirmPasswordEmpty : ValidationError
    data object PasswordsMismatch : ValidationError
}
```

**File:** `core/error/ui/ValidationErrorUiText.kt`

```kotlin
package com.example.dukkanapp.core.error.ui

import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.error.validation.ValidationError

fun ValidationError.toUiText(): UiText = when (this) {
    ValidationError.EmailEmpty -> UiText.Res(R.string.validation_email_empty)
    ValidationError.EmailInvalid -> UiText.Res(R.string.validation_email_invalid)
    ValidationError.PasswordEmpty -> UiText.Res(R.string.validation_password_empty)
    is ValidationError.PasswordTooShort -> UiText.Res(R.string.validation_password_too_short, listOf(minLength))
    ValidationError.ConfirmPasswordEmpty -> UiText.Res(R.string.validation_confirm_password_empty)
    ValidationError.PasswordsMismatch -> UiText.Res(R.string.validation_passwords_mismatch)
}
```

### Keep email validation pure Kotlin

`android.util.Patterns` makes validators untestable on the JVM and drags Android into the domain. Replace it in `core/utils/extension/StringExtension.kt`:

```kotlin
package com.example.dukkanapp.core.utils.extension

import com.example.dukkanapp.core.utils.constants.AppValidationConstants.MIN_PASSWORD_LENGTH

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/** Format check only. Firebase remains the authority (`ERROR_INVALID_EMAIL`). */
fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)

fun String.isValidPassword(): Boolean = length >= MIN_PASSWORD_LENGTH
```

The auth-specific validator (`AuthFormValidator`) lives in the auth feature. See the [Firebase Auth guide](../auth/firebase_auth_integration_guide.md#step-34-authformvalidator).

---

## 11. MVI Base: MviViewModel and CollectEffect

Every screen has the same plumbing: a `StateFlow<State>`, a one-shot `Effect` stream, and an `onIntent` entry point. Write it once.

**File:** `core/common/mvi/MviViewModel.kt`

```kotlin
package com.example.dukkanapp.core.common.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * MVI base: the screen sends [I]ntents, renders [S]tate, and handles one-shot [E]ffects
 * (navigation, snackbars, opening other apps).
 */
abstract class MviViewModel<S, I, E>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UNLIMITED: trySend never drops; receiveAsFlow delivers each effect to exactly one collector.
    private val _effect = Channel<E>(Channel.UNLIMITED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Single entry point: the screen only sends intents. */
    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) = _state.update(reduce)

    protected fun sendEffect(effect: E) {
        _effect.trySend(effect)
    }
}
```

**File:** `core/common/mvi/CollectEffect.kt`

```kotlin
package com.example.dukkanapp.core.common.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects one-shot effects only while the screen is visible (STARTED), so navigation never
 * fires in the background. Effects sent meanwhile wait in the channel and are delivered on return.
 */
@Composable
fun <E> CollectEffect(effect: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate: an effect is never lost between emission and a recomposition.
            withContext(Dispatchers.Main.immediate) {
                effect.collect { currentOnEffect(it) }
            }
        }
    }
}
```

> Existing screens (`Onboarding`, `LanguageSelection`) use `onEvent(...Event)`. New screens use `onIntent(...Intent)`. Migrate the old ones when you next touch them so the whole app speaks one vocabulary.

---

## 12. Usage in Each Layer

Example: the user types a wrong password.

```
1. Firebase SDK  → throws FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL")
2. DataSource    → guardException { } → FirebaseExceptionMapper → throws AppException.Auth.InvalidCredential(cause)
3. Repository    → guardFailure { } → DefaultFailureMapper → AppResult.Failure(AppFailure.Auth.InvalidCredentials)
4. UseCase       → passes the AppResult up (or composes it)
5. ViewModel     → failure.toUiText() → UiText.Res(R.string.error_invalid_credentials) in UiState
6. UI            → "Incorrect email or password." (or the Arabic/French text)
```

### DataSource: calls Firebase, throws only AppException

```kotlin
class ProductRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) {

    private fun productDoc(id: String) = firestore.collection(COLLECTION).document(id)

    suspend fun getProduct(id: String): ProductDto = guardException {
        productDoc(id).get().await().toObject(ProductDto::class.java)
            ?: throw AppException.Firestore.NotFound()
    }

    fun observeProduct(id: String): Flow<ProductDto?> =
        productDoc(id).snapshots()
            .map { it.toObject(ProductDto::class.java) }
            .guardException()

    private companion object {
        const val COLLECTION = "products"
    }
}
```

A Firestore `get()` on a missing document **succeeds** with an empty snapshot; it does not throw `NOT_FOUND`. That's why the data source throws `AppException.Firestore.NotFound()` itself.

### RepositoryImpl: AppException → AppFailure

```kotlin
class ProductRepositoryImpl @Inject constructor(
    private val remote: ProductRemoteDataSource,
    private val failureMapper: FailureMapper,
) : ProductRepository {

    override suspend fun getProduct(id: String): AppResult<Product> =
        failureMapper.guardFailure { remote.getProduct(id).toDomain() }

    override fun observeProduct(id: String): Flow<AppResult<Product?>> =
        remote.observeProduct(id)
            .map { it?.toDomain() }
            .guardFailure(failureMapper)
}
```

DTO → domain mapping runs **inside** `guardFailure`, so a malformed document becomes a reported `Unknown` instead of a crash.

### UseCase: composes results, enforces business rules

```kotlin
class AddToCartUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) {
    suspend operator fun invoke(productId: String, quantity: Int): EmptyResult =
        productRepository.getProduct(productId)
            .flatMap { product ->
                if (product.stock < quantity) AppResult.Failure(CartFailure.OutOfStock(product.name, product.stock))
                else cartRepository.add(product.id, quantity)
            }
}
```

### ViewModel (MVI): reduces into state and effects

```kotlin
@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    private val addToCart: AddToCartUseCase,
) : MviViewModel<ProductDetailsUiState, ProductDetailsIntent, ProductDetailsEffect>(ProductDetailsUiState()) {

    override fun onIntent(intent: ProductDetailsIntent) {
        when (intent) {
            is ProductDetailsIntent.AddToCartClicked -> onAddToCart(intent.productId)
            ProductDetailsIntent.ErrorDismissed -> setState { copy(error = null) }
        }
    }

    private fun onAddToCart(productId: String) {
        if (currentState.isAdding) return
        setState { copy(isAdding = true, error = null) } // synchronous: no double-tap race
        viewModelScope.launch {
            addToCart(productId, quantity = 1)
                .onSuccess { sendEffect(ProductDetailsEffect.ShowAddedToCart) }
                .onFailure(::showFailure)
            setState { copy(isAdding = false) }
        }
    }

    private fun showFailure(failure: AppFailure) {
        if (failure.requiresSignIn) sendEffect(ProductDetailsEffect.NavigateToLogin)
        else setState { copy(error = failure.toCartUiText(), canRetry = failure.isRetryable) }
    }
}
```

### Composable: renders state, handles effects

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()

CollectEffect(viewModel.effect) { effect ->
    when (effect) {
        ProductDetailsEffect.ShowAddedToCart -> snackbarHostState.showSnackbar(addedMessage)
        ProductDetailsEffect.NavigateToLogin -> onNavigateToLogin()
    }
}

state.error?.let { Text(text = it.asString(), color = MaterialTheme.colorScheme.error) }
```

---

## 13. Feature Failures for E-commerce

Core failures cover infrastructure. Business rules belong to features and plug in through `AppFailure.Feature`, so **core never changes** when a feature adds a failure (Open/Closed).

**File:** `features/cart/domain/model/CartFailure.kt`

```kotlin
sealed interface CartFailure : AppFailure.Feature {
    data class OutOfStock(val productName: String, val available: Int) : CartFailure
    data object CouponExpired : CartFailure
    data object CouponNotApplicable : CartFailure
}
```

**File:** `features/cart/presentation/CartFailureUiText.kt`

```kotlin
fun AppFailure.toCartUiText(): UiText = when (this) {
    is CartFailure.OutOfStock -> UiText.Res(R.string.cart_error_out_of_stock, listOf(productName, available))
    CartFailure.CouponExpired -> UiText.Res(R.string.cart_error_coupon_expired)
    CartFailure.CouponNotApplicable -> UiText.Res(R.string.cart_error_coupon_not_applicable)
    else -> toUiText() // fall back to the generic core messages
}
```

A feature failure can come from two places:
- **Use cases** returning `AppResult.Failure(CartFailure.OutOfStock(...))` after checking a rule.
- **Data sources** throwing `AppException.Feature(CartFailure.CouponExpired)`, for example after reading a Cloud Function error detail. `DefaultFailureMapper` unwraps it unchanged.

> **Production note:** anything involving money or stock (checkout, coupon redemption, order creation) must be validated **server-side** (Cloud Functions or Firestore transactions plus Security Rules). Client checks only give fast feedback.

### Global "session expired" handling

Use `requiresSignIn` in each ViewModel's single `showFailure` function, as in section 12, instead of scattering checks across intents. When several screens need it, emit it to an app-level `SessionEvents` flow observed by `AppNavHost`.

---

## 14. Dependency Injection

`FirebaseExceptionMapper` is a pure `object` and needs no binding. Only step 2 is injected.

**File:** `core/di/ErrorModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.core.error.mapper.DefaultFailureMapper
import com.example.dukkanapp.core.error.ErrorReporter
import com.example.dukkanapp.core.error.mapper.FailureMapper
import com.example.dukkanapp.core.error.LogcatErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {

    @Binds
    @Singleton
    abstract fun bindErrorReporter(impl: LogcatErrorReporter): ErrorReporter

    @Binds
    @Singleton
    abstract fun bindFailureMapper(impl: DefaultFailureMapper): FailureMapper
}
```

---

## 15. String Resources

These replace the duplicated `login_error_*` and `signup_error_*` strings. Delete those once both screens are migrated.

### `values/strings.xml`

```xml
<!-- Errors -->
<string name="error_no_connection">No internet connection. Check your network and try again.</string>
<string name="error_service_unavailable">Service is temporarily unavailable. Please try again.</string>
<string name="error_invalid_email">Invalid email address.</string>
<string name="error_invalid_credentials">Incorrect email or password.</string>
<string name="error_email_already_in_use">This email is already registered.</string>
<string name="error_weak_password">Password is too weak. Choose a stronger one.</string>
<string name="error_user_disabled">This account has been disabled.</string>
<string name="error_too_many_requests">Too many attempts. Please try again later.</string>
<string name="error_email_not_verified">Please verify your email first.</string>
<string name="error_account_exists_different_credential">An account already exists with a different sign-in method.</string>
<string name="error_requires_recent_login">Please sign in again to continue.</string>
<string name="error_session_expired">Your session has expired. Please sign in again.</string>
<string name="error_code_invalid_or_expired">This link or code is invalid or has expired. Please request a new one.</string>
<string name="error_not_found">The requested item could not be found.</string>
<string name="error_permission_denied">You don\'t have permission to do this.</string>
<string name="error_already_exists">This item already exists.</string>
<string name="error_conflict">Something changed while processing your request. Please try again.</string>
<string name="error_unknown">Something went wrong. Please try again.</string>

<!-- Validation -->
<string name="validation_email_empty">Please enter your email</string>
<string name="validation_email_invalid">Enter a valid email address</string>
<string name="validation_password_empty">Please enter your password</string>
<string name="validation_password_too_short">Password must be at least %1$d characters</string>
<string name="validation_confirm_password_empty">Please confirm your password</string>
<string name="validation_passwords_mismatch">Passwords do not match</string>
```

### `values-ar/strings.xml`

```xml
<!-- الأخطاء -->
<string name="error_no_connection">لا يوجد اتصال بالإنترنت. تحقق من الشبكة وحاول مرة أخرى.</string>
<string name="error_service_unavailable">الخدمة غير متاحة مؤقتًا. يرجى المحاولة مرة أخرى.</string>
<string name="error_invalid_email">عنوان البريد الإلكتروني غير صالح.</string>
<string name="error_invalid_credentials">البريد الإلكتروني أو كلمة المرور غير صحيحة.</string>
<string name="error_email_already_in_use">هذا البريد الإلكتروني مسجل بالفعل.</string>
<string name="error_weak_password">كلمة المرور ضعيفة. اختر كلمة مرور أقوى.</string>
<string name="error_user_disabled">تم تعطيل هذا الحساب.</string>
<string name="error_too_many_requests">محاولات كثيرة جدًا. يرجى المحاولة لاحقًا.</string>
<string name="error_email_not_verified">يرجى تأكيد بريدك الإلكتروني أولاً.</string>
<string name="error_account_exists_different_credential">يوجد حساب بالفعل بطريقة تسجيل دخول مختلفة.</string>
<string name="error_requires_recent_login">يرجى تسجيل الدخول مرة أخرى للمتابعة.</string>
<string name="error_session_expired">انتهت جلستك. يرجى تسجيل الدخول مرة أخرى.</string>
<string name="error_code_invalid_or_expired">هذا الرابط أو الرمز غير صالح أو منتهي الصلاحية. يرجى طلب رابط جديد.</string>
<string name="error_not_found">العنصر المطلوب غير موجود.</string>
<string name="error_permission_denied">ليس لديك إذن للقيام بذلك.</string>
<string name="error_already_exists">هذا العنصر موجود بالفعل.</string>
<string name="error_conflict">حدث تغيير أثناء معالجة طلبك. يرجى المحاولة مرة أخرى.</string>
<string name="error_unknown">حدث خطأ ما. يرجى المحاولة مرة أخرى.</string>

<!-- التحقق -->
<string name="validation_email_empty">يرجى إدخال بريدك الإلكتروني</string>
<string name="validation_email_invalid">أدخل عنوان بريد إلكتروني صالح</string>
<string name="validation_password_empty">يرجى إدخال كلمة المرور</string>
<string name="validation_password_too_short">يجب أن تتكون كلمة المرور من %1$d أحرف على الأقل</string>
<string name="validation_confirm_password_empty">يرجى تأكيد كلمة المرور</string>
<string name="validation_passwords_mismatch">كلمتا المرور غير متطابقتين</string>
```

### `values-fr/strings.xml`

```xml
<!-- Erreurs -->
<string name="error_no_connection">Pas de connexion Internet. Vérifiez votre réseau et réessayez.</string>
<string name="error_service_unavailable">Service temporairement indisponible. Veuillez réessayer.</string>
<string name="error_invalid_email">Adresse e-mail invalide.</string>
<string name="error_invalid_credentials">E-mail ou mot de passe incorrect.</string>
<string name="error_email_already_in_use">Cet e-mail est déjà enregistré.</string>
<string name="error_weak_password">Mot de passe trop faible. Choisissez-en un plus robuste.</string>
<string name="error_user_disabled">Ce compte a été désactivé.</string>
<string name="error_too_many_requests">Trop de tentatives. Veuillez réessayer plus tard.</string>
<string name="error_email_not_verified">Veuillez d\'abord vérifier votre e-mail.</string>
<string name="error_account_exists_different_credential">Un compte existe déjà avec une autre méthode de connexion.</string>
<string name="error_requires_recent_login">Veuillez vous reconnecter pour continuer.</string>
<string name="error_session_expired">Votre session a expiré. Veuillez vous reconnecter.</string>
<string name="error_code_invalid_or_expired">Ce lien ou ce code est invalide ou a expiré. Veuillez en demander un nouveau.</string>
<string name="error_not_found">L\'élément demandé est introuvable.</string>
<string name="error_permission_denied">Vous n\'avez pas l\'autorisation d\'effectuer cette action.</string>
<string name="error_already_exists">Cet élément existe déjà.</string>
<string name="error_conflict">Un changement est survenu pendant le traitement. Veuillez réessayer.</string>
<string name="error_unknown">Une erreur s\'est produite. Veuillez réessayer.</string>

<!-- Validation -->
<string name="validation_email_empty">Veuillez saisir votre e-mail</string>
<string name="validation_email_invalid">Saisissez une adresse e-mail valide</string>
<string name="validation_password_empty">Veuillez saisir votre mot de passe</string>
<string name="validation_password_too_short">Le mot de passe doit contenir au moins %1$d caractères</string>
<string name="validation_confirm_password_empty">Veuillez confirmer votre mot de passe</string>
<string name="validation_passwords_mismatch">Les mots de passe ne correspondent pas</string>
```

---

## 16. Testing

Both mappers are pure functions, repositories are interfaces, and the domain is pure Kotlin, so every layer is testable on the plain JVM.

```kotlin
// Step 1: Firebase → AppException
@Test
fun `firestore missing index is a FailedPrecondition`() {
    val e = FirebaseFirestoreException("needs index", FirebaseFirestoreException.Code.FAILED_PRECONDITION)
    assertTrue(FirebaseExceptionMapper.map(e) is AppException.Firestore.FailedPrecondition)
}

@Test
fun `guardException rethrows cancellation untouched`() {
    assertThrows(CancellationException::class.java) {
        guardException { throw CancellationException() }
    }
}

// Step 2: AppException → AppFailure
@Test
fun `known failures are mapped and not reported`() {
    val reported = mutableListOf<Throwable>()
    val mapper = DefaultFailureMapper(reported::add)

    assertEquals(AppFailure.Data.NotFound, mapper.map(AppException.Firestore.NotFound()))
    assertEquals(AppFailure.Auth.InvalidCredentials, mapper.map(AppException.Auth.UserNotFound()))
    assertTrue(reported.isEmpty())
}

@Test
fun `developer bugs become Unknown and are reported`() {
    val reported = mutableListOf<Throwable>()
    val mapper = DefaultFailureMapper(reported::add)

    assertEquals(AppFailure.Unknown, mapper.map(AppException.Firestore.FailedPrecondition()))
    assertEquals(AppFailure.Unknown, mapper.map(IllegalStateException("bad DTO")))
    assertEquals(2, reported.size)
}

@Test
fun `guardFailure rethrows cancellation`() {
    val mapper = FailureMapper { AppFailure.Unknown }
    assertThrows(CancellationException::class.java) {
        mapper.guardFailure { throw CancellationException() }
    }
}

// Domain, with fakes
class FakeAuthRepository : AuthRepository {
    var signUpResult: AppResult<AuthUser> = AppResult.Success(AuthUser("uid", "a@b.com", false))
    override suspend fun signUp(email: String, password: String) = signUpResult
    // ...
}

@Test
fun `sign up fails with EmailAlreadyInUse`() = runTest {
    val auth = FakeAuthRepository().apply {
        signUpResult = AppResult.Failure(AppFailure.Auth.EmailAlreadyInUse)
    }
    val result = SignUpUseCase(auth, FakeUserRepository())("a@b.com", "password1")
    assertEquals(AppResult.Failure(AppFailure.Auth.EmailAlreadyInUse), result)
}

@Test
fun `validator rejects short password`() {
    assertEquals(
        ValidationError.PasswordTooShort(MIN_PASSWORD_LENGTH),
        AuthFormValidator.validateNewPassword("short"),
    )
}
```

> Firebase exception classes have public constructors, but they live in the Android SDK. If a JVM test can't load them, run the `FirebaseExceptionMapper` tests with Robolectric. All other tests are plain JUnit.

---

## 17. Implementation Checklist

**Domain (pure Kotlin)**
- [ ] `core/error/AppFailure.kt`
- [ ] `core/error/AppResult.kt`
- [ ] `core/error/validation/ValidationError.kt`

**Data**
- [ ] `core/error/AppException.kt`
- [ ] `core/error/mapper/FirebaseExceptionMapper.kt` + `GuardException.kt` (step 1)
- [ ] `core/error/mapper/FailureMapper.kt` + `GuardFailure.kt` + `ErrorReporter.kt` (step 2)

**Presentation**
- [ ] `core/common/text/UiText.kt`
- [ ] `core/error/ui/AppFailureUiText.kt` + `ValidationErrorUiText.kt`
- [ ] `core/common/mvi/MviViewModel.kt` + `CollectEffect.kt`
- [ ] `core/utils/extension/StringExtension.kt`: replace `Patterns` with `Regex`

**DI and resources**
- [ ] `core/di/ErrorModule.kt`
- [ ] Error + validation strings (EN, AR, FR)
- [ ] Remove duplicated `login_error_*` / `signup_error_*` strings after migrating both screens

**Tests**
- [ ] Both mappers, cancellation in `guardException` and `guardFailure`, validators, use cases with fakes
