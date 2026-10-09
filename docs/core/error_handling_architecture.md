# Error Handling Architecture Guide

Centralized, Firebase-aware error handling for the Dukkan B2C e-commerce app (Firebase Auth + Cloud Firestore).

```
Firebase SDK ──throws──► DataSource ──throws──► Repository ── safeCall ──► AppResult<T>
                                                    │          (the ONLY place a Throwable
                                                    │           becomes an AppError)
                                                    ▼
                                    UseCase composes AppResult (map / flatMap / onSuccess)
                                                    ▼
                                    ViewModel folds it into UiState (AppError → UiText)
```

---

## Table of Contents

0. [Review: What Changed and Why](#0-review-what-changed-and-why)
1. [Principles](#1-principles)
2. [Folder Structure](#2-folder-structure)
3. [AppError](#3-apperror)
4. [AppResult](#4-appresult)
5. [ErrorMapper and safeCall](#5-errormapper-and-safecall)
6. [FirebaseErrorMapper](#6-firebaseerrormapper)
7. [Presentation: UiText and AppError Mapping](#7-presentation-uitext-and-apperror-mapping)
8. [Form Validation](#8-form-validation)
9. [Usage in Each Layer](#9-usage-in-each-layer)
10. [Feature Errors for E-commerce](#10-feature-errors-for-e-commerce)
11. [Dependency Injection](#11-dependency-injection)
12. [String Resources](#12-string-resources)
13. [Testing](#13-testing)
14. [Implementation Checklist](#14-implementation-checklist)

---

## 0. Review: What Changed and Why

This section is a review of the previous version of this guide. It explains every change.

| # | Previous design | Problem | New design |
|---|---|---|---|
| 1 | `ErrorCode` enum **and** ~35 `AppException` subclasses kept 1:1 | Two parallel hierarchies. One new error means editing enum + exception + mapper + `ErrorCodeMapper` + 3 string files. Violates DRY and the Open/Closed principle. | One `sealed interface AppError` made of `data object`s. One new error means editing `AppError` + the UI mapper + strings. |
| 2 | Errors grouped by **SDK** (`FIRESTORE_OUT_OF_RANGE`, `FIRESTORE_DATA_LOSS`, `AUTH_INVALID_VERIFICATION_ID`...) | The UI cannot act differently on most of them, so 41 strings mostly said "try again". | Errors grouped by **what the user/UI can do** (`Network`, `Auth`, `Data`, `Feature`, `Unknown`). Total: 17 user messages. |
| 3 | `AppResult.Error(exception: AppException)` | Domain carried `Throwable`s and stack traces. `Error` shadows `kotlin.Error`. | `AppResult.Failure(error: AppError)`. `AppError` is plain Kotlin data with no stack trace (except `Unknown.cause`, kept for crash logging). |
| 4 | `runCatching` / data sources use `catch (e: Exception)` | **Bug:** swallows `CancellationException`, so a cancelled `viewModelScope` keeps running and reports a fake "unknown error". | `safeCall` rethrows `CancellationException` before catching anything else. |
| 5 | `try { } catch (e: AppException) { throw e } catch (e: Exception) { throw map(e) }` repeated in **every** data source method, then `try/catch` again in **every** repository method | ~150 lines of duplicated boilerplate. Two layers doing the same job. | Exactly **one** boundary: repositories call `errorMapper.safeCall { }`. Data sources stay thin and just call Firebase. |
| 6 | `object FirebaseAuthExceptionMapper`, `object FirebaseFirestoreExceptionMapper` | Global singletons: they can't be injected or faked in tests (Dependency Inversion). Auth and Firestore network errors were handled twice. | `fun interface ErrorMapper` with one injectable `FirebaseErrorMapper`. Tests use `ErrorMapper { AppError.Unknown(it) }`. |
| 7 | `ErrorCodeMapper` in `core/error` imports `R` | Android resources leaked into the error/domain package. | `AppError.toUiText()` lives in the presentation package (`core/common/error`). |
| 8 | ViewModel state held `@StringRes Int` | Could not pass format args. `signup_error_password_short` uses `%1$d`, so it rendered literally as "at least %1$d characters". | `UiText.Res(id, args)` supports arguments, and feature errors like "Only 3 left in stock" work. |
| 9 | `ERROR_USER_NOT_FOUND` and `ERROR_WRONG_PASSWORD` mapped to distinct messages | Lets attackers enumerate registered emails. Since 2023, Firebase with *email enumeration protection* returns `ERROR_INVALID_CREDENTIAL` for both anyway. | Both map to `AppError.Auth.InvalidCredentials` ("Incorrect email or password"). |
| 10 | Firestore `FAILED_PRECONDITION` → "Operation failed, try again" | In practice this almost always means **a missing composite index**. That's a developer bug, and a retry never helps. | Mapped to `AppError.Unknown(cause)` so it reaches crash reporting. |
| 11 | `INVALID_ARGUMENT` built an `InvalidFormatException` and **dropped the cause** | Lost the stack trace. | Every unmapped case becomes `Unknown(cause)`. |
| 12 | `AppResultExtension.kt`: `combine`, `combineResults`, `filter`, `swap`, `toAppResult`, `recover`, `recoverWith` | Unused API surface (YAGNI). `swap` is meaningless. `filter` and `toAppResult` invented `UnknownException`s. | Small set: `map`, `flatMap`, `onSuccess`, `onFailure`, `fold`, `getOrNull`, `asEmptyResult`. |
| 13 | `isSuccess`, `getOrDefault(@UnsafeVariance ...)`, `getOrThrow`, companion `success()/error()` | Duplicates of what `when`/`fold` already express. `getOrThrow` reintroduces exceptions. | Removed. |
| 14 | No support for real-time Firestore listeners | E-commerce needs live cart, stock, and order status. | `Flow<T>.asResult(errorMapper)`. |
| 15 | No way for a data source to fail with a precise domain error ("no signed-in user", "out of stock") | It had to pick an arbitrary `AppException` subclass. | `AppErrorException(error)` is unwrapped by the mapper. |

---

## 1. Principles

1. **One translation boundary.** A `Throwable` becomes an `AppError` in exactly one place: `ErrorMapper.safeCall`, called by repositories.
2. **Expected failures are values, not exceptions.** Repositories and use cases return `AppResult<T>` and never throw for expected failures.
3. **Programmer errors are not caught.** `safeCall` catches `Exception`, not `Throwable`, and never catches `CancellationException`.
4. **Errors describe meaning, not origin.** `AppError.Auth.InvalidCredentials`, not `FIREBASE_AUTH_ERROR_WRONG_PASSWORD`. Swapping Firebase for another backend only changes `FirebaseErrorMapper`.
5. **The domain is pure Kotlin.** `core/error` imports no Android or Firebase classes.
6. **Strings live in presentation.** Only the UI layer turns an `AppError` into text.
7. **Loading is UI state, not a result.** `AppResult` has no `Loading` case. Use `isLoading` in `UiState`.

---

## 2. Folder Structure

```
core/
├── error/                          # Pure Kotlin. Safe for domain.
│   ├── AppError.kt                 # What can go wrong
│   └── AppResult.kt                # Success / Failure + operators
├── data/
│   └── error/                      # Firebase-aware. Data layer only.
│       ├── AppErrorException.kt    # Lets a data source fail with a precise AppError
│       ├── ErrorMapper.kt          # fun interface + safeCall + Flow.asResult
│       └── FirebaseErrorMapper.kt  # Auth + Firestore + network → AppError
├── common/
│   ├── text/
│   │   └── UiText.kt               # String resource with args, or raw string
│   └── error/
│       ├── AppErrorUiText.kt       # AppError → UiText
│       └── ValidationErrorUiText.kt
├── domain/
│   └── validation/
│       └── ValidationError.kt      # Field-level form errors (not AppResult)
└── di/
    └── ErrorModule.kt              # binds ErrorMapper → FirebaseErrorMapper
```

---

## 3. AppError

**File:** `core/error/AppError.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Every expected failure in the app, described by what it means to the user,
 * not by which SDK produced it. Pure Kotlin: safe to use in the domain layer.
 */
sealed interface AppError {

    sealed interface Network : AppError {
        data object NoConnection : Network
        data object ServiceUnavailable : Network
    }

    sealed interface Auth : AppError {
        data object InvalidEmail : Auth
        data object InvalidCredentials : Auth
        data object EmailAlreadyInUse : Auth
        data object WeakPassword : Auth
        data object UserDisabled : Auth
        data object TooManyRequests : Auth
        data object EmailNotVerified : Auth
        data object AccountExistsWithDifferentCredential : Auth
        data object RequiresRecentLogin : Auth
        data object SessionExpired : Auth
    }

    sealed interface Data : AppError {
        data object NotFound : Data
        data object PermissionDenied : Data
        data object AlreadyExists : Data

        /** Concurrent modification (e.g. a transaction lost a race on stock). Safe to retry. */
        data object Conflict : Data
    }

    /**
     * Extension point for business errors owned by a feature (cart, checkout, orders...).
     * Deliberately NOT sealed: features declare their own sealed hierarchy implementing it.
     * See section 10.
     */
    interface Feature : AppError

    /** Anything we did not anticipate. [cause] is for logging / crash reporting only. */
    data class Unknown(val cause: Throwable? = null) : AppError
}

/** The UI may offer a "Retry" action. */
val AppError.isRetryable: Boolean
    get() = this is AppError.Network || this == AppError.Data.Conflict

/** The user must sign in again before continuing. */
val AppError.requiresSignIn: Boolean
    get() = this == AppError.Auth.SessionExpired || this == AppError.Auth.RequiresRecentLogin
```

> **Why `data object`?** Equality, `toString()` and `when` exhaustiveness for free, with zero allocation per error.

---

## 4. AppResult

**File:** `core/error/AppResult.kt`

```kotlin
package com.example.dukkanapp.core.error

/**
 * Outcome of an operation that can fail in an expected way.
 * Repositories return it, use cases compose it, ViewModels fold it into UI state.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
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

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.asEmptyResult(): EmptyResult = map { }
```

That's the whole API. Because every operator is `inline`, lambdas can call `suspend` functions when used inside a coroutine:

```kotlin
signUp(email, password)
    .onSuccess { _effect.send(SignUpEffect.NavigateToEmailVerification) } // suspend call: OK
```

---

## 5. ErrorMapper and safeCall

**File:** `core/data/error/AppErrorException.kt`

```kotlin
package com.example.dukkanapp.core.data.error

import com.example.dukkanapp.core.error.AppError

/**
 * Lets a data source fail with a precise [AppError]
 * (e.g. no signed-in user, product out of stock). Unwrapped by [ErrorMapper].
 */
class AppErrorException(val error: AppError) : Exception(error.toString())
```

**File:** `core/data/error/ErrorMapper.kt`

```kotlin
package com.example.dukkanapp.core.data.error

import com.example.dukkanapp.core.error.AppError
import com.example.dukkanapp.core.error.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Translates any throwable from the data layer into an [AppError]. */
fun interface ErrorMapper {
    fun map(throwable: Throwable): AppError
}

/**
 * The single place where exceptions become [AppResult.Failure].
 * - Rethrows [CancellationException] so structured concurrency keeps working.
 * - Catches [Exception] only: real programmer errors (`Error`s) still crash loudly.
 */
inline fun <T> ErrorMapper.safeCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Failure(map(e))
}

/** For real-time Firestore listeners (cart, stock, order status). */
fun <T> Flow<T>.asResult(errorMapper: ErrorMapper): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }
        .catch { emit(AppResult.Failure(errorMapper.map(it))) }
```

> `safeCall` is `inline`, so `block` may call `suspend` functions (`await()`) when invoked from a `suspend fun`.
>
> **Timeouts:** do **not** wrap calls in `withTimeout`. Its `TimeoutCancellationException` *is* a `CancellationException` and will be rethrown. Use `withTimeoutOrNull` and map `null` explicitly.

---

## 6. FirebaseErrorMapper

**File:** `core/data/error/FirebaseErrorMapper.kt`

```kotlin
package com.example.dukkanapp.core.data.error

import com.example.dukkanapp.core.error.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import java.io.IOException
import javax.inject.Inject

class FirebaseErrorMapper @Inject constructor() : ErrorMapper {

    override fun map(throwable: Throwable): AppError = when (throwable) {
        is AppErrorException -> throwable.error
        is FirebaseNetworkException, is IOException -> AppError.Network.NoConnection
        is FirebaseTooManyRequestsException -> AppError.Auth.TooManyRequests
        is FirebaseAuthException -> mapAuth(throwable)
        is FirebaseFirestoreException -> mapFirestore(throwable)
        else -> AppError.Unknown(throwable)
    }

    private fun mapAuth(e: FirebaseAuthException): AppError = when (e) {
        // Order matters: WeakPassword is a subclass of InvalidCredentials.
        is FirebaseAuthWeakPasswordException -> AppError.Auth.WeakPassword
        is FirebaseAuthRecentLoginRequiredException -> AppError.Auth.RequiresRecentLogin
        is FirebaseAuthUserCollisionException ->
            if (e.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") {
                AppError.Auth.AccountExistsWithDifferentCredential
            } else {
                AppError.Auth.EmailAlreadyInUse
            }
        is FirebaseAuthInvalidUserException -> when (e.errorCode) {
            "ERROR_USER_DISABLED" -> AppError.Auth.UserDisabled
            "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AppError.Auth.SessionExpired
            // ERROR_USER_NOT_FOUND: never reveal whether an email is registered.
            else -> AppError.Auth.InvalidCredentials
        }
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_EMAIL") {
                AppError.Auth.InvalidEmail
            } else {
                AppError.Auth.InvalidCredentials
            }
        else -> when (e.errorCode) {
            "ERROR_NETWORK_REQUEST_FAILED" -> AppError.Network.NoConnection
            "ERROR_TOO_MANY_REQUESTS" -> AppError.Auth.TooManyRequests
            else -> AppError.Unknown(e)
        }
    }

    private fun mapFirestore(e: FirebaseFirestoreException): AppError = when (e.code) {
        Code.UNAVAILABLE,
        Code.DEADLINE_EXCEEDED,
        Code.RESOURCE_EXHAUSTED -> AppError.Network.ServiceUnavailable
        Code.UNAUTHENTICATED -> AppError.Auth.SessionExpired
        Code.PERMISSION_DENIED -> AppError.Data.PermissionDenied
        Code.NOT_FOUND -> AppError.Data.NotFound
        Code.ALREADY_EXISTS -> AppError.Data.AlreadyExists
        Code.ABORTED -> AppError.Data.Conflict
        // FAILED_PRECONDITION is almost always a missing composite index: a developer
        // bug that must reach crash reporting, so it falls through to Unknown.
        else -> AppError.Unknown(e)
    }
}
```

> **Extending:** when you add Cloud Functions (checkout/payments) or Storage (product images), add one branch: `is FirebaseFunctionsException -> mapFunctions(throwable)`. Nothing else changes.

---

## 7. Presentation: UiText and AppError Mapping

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

**File:** `core/common/error/AppErrorUiText.kt`

```kotlin
package com.example.dukkanapp.core.common.error

import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.error.AppError

/** Generic user message for any [AppError]. Features map their own [AppError.Feature] first. */
fun AppError.toUiText(): UiText = UiText.Res(messageRes)

private val AppError.messageRes: Int
    @StringRes get() = when (this) {
        AppError.Network.NoConnection -> R.string.error_no_connection
        AppError.Network.ServiceUnavailable -> R.string.error_service_unavailable

        AppError.Auth.InvalidEmail -> R.string.error_invalid_email
        AppError.Auth.InvalidCredentials -> R.string.error_invalid_credentials
        AppError.Auth.EmailAlreadyInUse -> R.string.error_email_already_in_use
        AppError.Auth.WeakPassword -> R.string.error_weak_password
        AppError.Auth.UserDisabled -> R.string.error_user_disabled
        AppError.Auth.TooManyRequests -> R.string.error_too_many_requests
        AppError.Auth.EmailNotVerified -> R.string.error_email_not_verified
        AppError.Auth.AccountExistsWithDifferentCredential -> R.string.error_account_exists_different_credential
        AppError.Auth.RequiresRecentLogin -> R.string.error_requires_recent_login
        AppError.Auth.SessionExpired -> R.string.error_session_expired

        AppError.Data.NotFound -> R.string.error_not_found
        AppError.Data.PermissionDenied -> R.string.error_permission_denied
        AppError.Data.AlreadyExists -> R.string.error_already_exists
        AppError.Data.Conflict -> R.string.error_conflict

        is AppError.Feature, is AppError.Unknown -> R.string.error_unknown
    }
```

The `when` is exhaustive. Adding a new `AppError` case is a **compile error** until it gets a message.

---

## 8. Form Validation

Field validation is **not** an `AppResult` concern. It is synchronous, needs no I/O, and its errors belong to individual fields. It gets its own small type.

**File:** `core/domain/validation/ValidationError.kt`

```kotlin
package com.example.dukkanapp.core.domain.validation

enum class ValidationError {
    EMAIL_EMPTY,
    EMAIL_INVALID,
    PASSWORD_EMPTY,
    PASSWORD_TOO_SHORT,
    CONFIRM_PASSWORD_EMPTY,
    PASSWORDS_MISMATCH,
}
```

**File:** `core/common/error/ValidationErrorUiText.kt`

```kotlin
package com.example.dukkanapp.core.common.error

import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.domain.validation.ValidationError
import com.example.dukkanapp.core.utils.constants.AppValidationConstants.MIN_PASSWORD_LENGTH

fun ValidationError.toUiText(): UiText = when (this) {
    ValidationError.EMAIL_EMPTY -> UiText.Res(R.string.validation_email_empty)
    ValidationError.EMAIL_INVALID -> UiText.Res(R.string.validation_email_invalid)
    ValidationError.PASSWORD_EMPTY -> UiText.Res(R.string.validation_password_empty)
    ValidationError.PASSWORD_TOO_SHORT -> UiText.Res(R.string.validation_password_too_short, listOf(MIN_PASSWORD_LENGTH))
    ValidationError.CONFIRM_PASSWORD_EMPTY -> UiText.Res(R.string.validation_confirm_password_empty)
    ValidationError.PASSWORDS_MISMATCH -> UiText.Res(R.string.validation_passwords_mismatch)
}
```

The auth-specific validator (`AuthFormValidator`) lives in the auth feature. See the [Firebase Auth guide](../auth/firebase_auth_integration_guide.md#step-34-authformvalidator).

---

## 9. Usage in Each Layer

### Data source: thin, just calls Firebase

```kotlin
class ProductRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) {

    suspend fun getProduct(id: String): ProductDto =
        firestore.collection("products").document(id).get().await()
            .toObject(ProductDto::class.java)
            ?: throw AppErrorException(AppError.Data.NotFound)

    fun observeProduct(id: String): Flow<ProductDto?> =
        firestore.collection("products").document(id).snapshots()
            .map { it.toObject(ProductDto::class.java) }
}
```

No `try/catch` here. Raw Firebase exceptions propagate to the repository.

### Repository: the single boundary

```kotlin
class ProductRepositoryImpl @Inject constructor(
    private val remote: ProductRemoteDataSource,
    private val errorMapper: ErrorMapper,
) : ProductRepository {

    override suspend fun getProduct(id: String): AppResult<Product> =
        errorMapper.safeCall { remote.getProduct(id).toDomain() }

    override fun observeProduct(id: String): Flow<AppResult<Product?>> =
        remote.observeProduct(id)
            .map { it?.toDomain() }
            .asResult(errorMapper)
}
```

### Use case: composes results

```kotlin
class AddToCartUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) {
    suspend operator fun invoke(productId: String, quantity: Int): EmptyResult =
        productRepository.getProduct(productId)
            .flatMap { product ->
                if (product.stock < quantity) AppResult.Failure(CartError.OutOfStock(product.name, product.stock))
                else cartRepository.add(product.id, quantity)
            }
}
```

### ViewModel: folds into state

```kotlin
private fun onAddToCart(productId: String) {
    _state.update { it.copy(isAdding = true, error = null) }
    viewModelScope.launch {
        addToCart(productId, quantity = 1)
            .onSuccess { _effect.send(ProductEffect.ShowAddedToCart) }
            .onFailure { error -> _state.update { it.copy(error = error.toCartUiText()) } }
        _state.update { it.copy(isAdding = false) }
    }
}
```

### Composable: renders `UiText`

```kotlin
state.error?.let { Text(text = it.asString(), color = MaterialTheme.colorScheme.error) }
```

---

## 10. Feature Errors for E-commerce

Core errors cover infrastructure. Business rules belong to features and plug in through `AppError.Feature`, so **core never changes** when a feature adds an error (Open/Closed).

**File:** `features/cart/domain/model/CartError.kt`

```kotlin
sealed interface CartError : AppError.Feature {
    data class OutOfStock(val productName: String, val available: Int) : CartError
    data object CouponExpired : CartError
    data object CouponNotApplicable : CartError
}
```

**File:** `features/cart/presentation/CartErrorUiText.kt`

```kotlin
fun AppError.toCartUiText(): UiText = when (this) {
    is CartError.OutOfStock -> UiText.Res(R.string.cart_error_out_of_stock, listOf(productName, available))
    CartError.CouponExpired -> UiText.Res(R.string.cart_error_coupon_expired)
    CartError.CouponNotApplicable -> UiText.Res(R.string.cart_error_coupon_not_applicable)
    else -> toUiText() // fall back to the generic core messages
}
```

A feature error can come from two places:
- **Use cases** returning `AppResult.Failure(CartError.OutOfStock(...))` after checking a rule.
- **Data sources** throwing `AppErrorException(CartError.CouponExpired)`, for example after reading a Cloud Function error detail.

> **Production note:** anything involving money or stock (checkout, coupon redemption, order creation) must be validated **server-side** (Cloud Functions or Firestore transactions plus Security Rules). Client checks only give fast feedback.

### Global "session expired" handling

Use `requiresSignIn` once, centrally, instead of in every screen:

```kotlin
.onFailure { error ->
    if (error.requiresSignIn) _effect.send(Effect.NavigateToLogin)
    else _state.update { it.copy(error = error.toUiText()) }
}
```

---

## 11. Dependency Injection

**File:** `core/di/ErrorModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.core.data.error.ErrorMapper
import com.example.dukkanapp.core.data.error.FirebaseErrorMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {
    @Singleton
    @Binds
    abstract fun bindErrorMapper(impl: FirebaseErrorMapper): ErrorMapper
}
```

---

## 12. String Resources

These replace the 41 `error_*` strings from the previous guide. Validation strings replace the duplicated `login_error_*` and `signup_error_*` field messages. Delete those once both screens are migrated.

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

## 13. Testing

Because `ErrorMapper` is a `fun interface` and repositories are interfaces, every layer is testable on the plain JVM.

```kotlin
class FakeAuthRepository : AuthRepository {
    var signUpResult: AppResult<AuthUser> = AppResult.Success(AuthUser("uid", "a@b.com", false))
    override suspend fun signUp(email: String, password: String) = signUpResult
    // ...
}

@Test
fun `sign up fails with EmailAlreadyInUse`() = runTest {
    val auth = FakeAuthRepository().apply {
        signUpResult = AppResult.Failure(AppError.Auth.EmailAlreadyInUse)
    }
    val result = SignUpUseCase(auth, FakeUserRepository())("a@b.com", "password1")
    assertEquals(AppResult.Failure(AppError.Auth.EmailAlreadyInUse), result)
}

@Test
fun `safeCall rethrows cancellation`() {
    val mapper = ErrorMapper { AppError.Unknown(it) }
    assertThrows(CancellationException::class.java) {
        mapper.safeCall { throw CancellationException() }
    }
}
```

> `android.util.Patterns` (used by `isValidEmail()`) is `null` in plain JVM unit tests. Either test validators with Robolectric, or set `unitTests.isReturnDefaultValues = true` and keep validator tests in `androidTest`.

---

## 14. Implementation Checklist

- [ ] `core/error/AppError.kt`
- [ ] `core/error/AppResult.kt`
- [ ] `core/data/error/AppErrorException.kt`
- [ ] `core/data/error/ErrorMapper.kt`
- [ ] `core/data/error/FirebaseErrorMapper.kt`
- [ ] `core/common/text/UiText.kt`
- [ ] `core/common/error/AppErrorUiText.kt`
- [ ] `core/domain/validation/ValidationError.kt` + `core/common/error/ValidationErrorUiText.kt`
- [ ] `core/di/ErrorModule.kt`
- [ ] Error + validation strings (EN, AR, FR)
- [ ] Remove duplicated `login_error_*` / `signup_error_*` field strings after migrating both screens
- [ ] Unit tests: `safeCall` cancellation, use cases with fakes
