# Firebase Authentication Integration Guide

Email/password authentication with email verification and a Firestore user profile for the Dukkan B2C e-commerce app. It follows the app's **MVI** pattern (State + Intent + Effect).

> **Prerequisite:** implement [Error Handling Architecture](../core/error_handling_architecture.md) first. This guide uses `AppResult`, `AppError`, `ErrorMapper.safeCall`, `UiText` and `ValidationError` from there.

```
SignUp ──► Firebase Auth (create) ──► Firestore users/{uid} (create if absent) ──► send verification email
                                                     │
                                       EmailVerification screen
                                (re-checks on every resume + manual button)
                                                     │
                                               verified ──► Home

SignIn ──► Firebase Auth ──► ensure profile exists ──► verified ? Home : EmailVerification
```

---

## Table of Contents

0. [Review: What Changed and Why](#0-review-what-changed-and-why)
1. [Dependencies](#1-dependencies)
2. [Firebase Console and Security Rules](#2-firebase-console-and-security-rules)
3. [Domain Layer](#3-domain-layer)
4. [Data Layer](#4-data-layer)
5. [Dependency Injection](#5-dependency-injection)
6. [Sign Up (MVI)](#6-sign-up-mvi)
7. [Login (MVI)](#7-login-mvi)
8. [Email Verification (MVI)](#8-email-verification-mvi)
9. [Navigation and App Start](#9-navigation-and-app-start)
10. [String Resources](#10-string-resources)
11. [Folder Structure](#11-folder-structure)
12. [Implementation Checklist](#12-implementation-checklist)
13. [Manual Testing](#13-manual-testing)

---

## 0. Review: What Changed and Why

| # | Previous version | Problem | Now |
|---|---|---|---|
| 1 | `firebase-auth-ktx`, `firebase-firestore-ktx` | KTX modules were **removed** from the Firebase BoM in v34. The Kotlin APIs now live in the main modules. | `firebase-auth`, `firebase-firestore` |
| 2 | `.await()` used without its dependency | `kotlinx.coroutines.tasks.await` comes from `kotlinx-coroutines-play-services`, which wasn't declared. | Added |
| 3 | Rule `allow create: if request.auth != null` | **Security hole:** any signed-in user could create or overwrite *anyone's* `users/{id}` document. | `request.auth.uid == userId`, plus a field whitelist on update |
| 4 | One `AuthRepository` doing auth **and** Firestore profile CRUD | Violates Single Responsibility and Interface Segregation. Profile grows in e-commerce (addresses, phone, FCM token). | `AuthRepository` (session) + `UserRepository` (profile) |
| 5 | One `User` model with `createdAt = System.currentTimeMillis()` defaults | Domain invented timestamps. Auth identity and profile data were mixed. | `AuthUser` (identity) + `UserProfile` (Firestore). No fake defaults. |
| 6 | `UserDto` with `@ServerTimestamp createdAt` written via `set(merge)` on update | **Bug:** `createdAt` is `null` in `fromDomain()`, so every update **overwrote `createdAt`** with the current time. | Create once in a transaction. Updates write only the changed fields plus `FieldValue.serverTimestamp()`. |
| 7 | `isEmailVerified` stored in Firestore | Duplicates the source of truth (Auth). Kotlin `is`-prefixed properties also serialize as `emailVerified`, so they never read back. | Not stored. Read from `FirebaseUser`. |
| 8 | Sign-up: if the Firestore write failed, the use case returned **Error** although the Auth account existed | Retrying then gave "email already in use", and the user was stuck without a profile. | Auth success = sign-up success. The profile is created *if absent* at sign-up **and** at every sign-in, so it self-heals. |
| 9 | Data sources and repositories each had `try/catch` in every method | Boilerplate. Also swallowed `CancellationException`. | Repositories use `errorMapper.safeCall { }`. Data sources are thin. |
| 10 | `isEmailVerified()` separate from `reloadUser()`; `isLoggedIn()` suspend | Two calls for one fact. Needless `suspend`. | `reloadUser(): AppResult<AuthUser>` carries `isEmailVerified`. `currentUser` is a property. |
| 11 | Verification polling `while (true) { delay(5000) }` in `init` | Runs while the app is in background, drains battery, risks Firebase quota, can navigate twice. | Check on every **screen resume** (user returns from mail app) + manual button, guarded by a single `Job`. |
| 12 | Resend button had no cooldown | Firebase rate-limits verification mail, so users hit `TooManyRequests`. | 60 s cooldown in state |
| 13 | `isLoading = true` set *inside* `launch` | Double-tap race: two sign-ups could start. | Loading set synchronously before `launch`. |
| 14 | Every server error went to a top banner | "Email already registered" belongs on the email field. | Field-specific errors are routed to their field. |
| 15 | `@StringRes Int` errors | `%1$d` in password-length strings rendered literally | `UiText` with args |
| 16 | `Intent(ACTION_MAIN).addCategory(CATEGORY_APP_EMAIL)` + hardcoded `"Open Email"` | Wrong pattern for app categories. Crashes with no mail app. Unlocalized string. | `Intent.makeMainSelectorActivity` + `ActivityNotFoundException` handled |
| 17 | Duplicate strings (`signup_error_password_weak`, `signup_error_unknown`...) next to `error_*` | Two sources of truth | Removed. Core `error_*` / `validation_*` only. |

**Bugs already in the current code** (`SignUpViewModel` / `SignUpUiState`). The code in section 6 fixes all of them:
- `if (emailError != null && passwordError != null && confirmPasswordError != null) return`: `&&` should be `||`. Sign-up proceeds while fields are invalid.
- `current.confirmPassword.isNotEmpty() -> signup_error_confirm_password_empty`: inverted. It shows "please confirm" when the user *did* confirm.
- `current.password.isEmpty() -> R.string.signup_error_confirm_password_empty`: wrong string.
- `SignUpUiState.isValidEmail = emailError != null && ...`: inverted (`== null`).
- `stringResource(R.string.signup_error_password_short)` without the `%1$d` argument.

---

## 1. Dependencies

### Step 1.1: `gradle/libs.versions.toml`

```toml
[versions]
firebaseBom = "34.0.0"        # use the latest 34.x+ (KTX modules removed since 34.0.0)
googleServices = "4.4.2"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

### Step 1.2: root `build.gradle.kts`

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.google.services) apply false
}
```

### Step 1.3: `app/build.gradle.kts`

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.google.services)
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services) // Task.await()
}
```

### Step 1.4: `google-services.json`

1. [Firebase Console](https://console.firebase.google.com/) → add an Android app with package `com.example.dukkanapp`
2. Download `google-services.json` into `app/`
3. Add it to `.gitignore` if the repository is public

---

## 2. Firebase Console and Security Rules

1. **Authentication → Sign-in method:** enable **Email/Password**.
2. **Authentication → Settings:** keep **Email enumeration protection** enabled (default). Wrong password and unknown user both return `ERROR_INVALID_CREDENTIAL`, mapped to `AppError.Auth.InvalidCredentials`.
3. **Firestore Database:** create the database in your users' region. Start in **production mode** with these rules (never ship test mode):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }

    match /users/{userId} {
      allow read: if isOwner(userId);

      allow create: if isOwner(userId)
        && request.resource.data.keys().hasOnly(['email', 'displayName', 'photoUrl', 'createdAt', 'updatedAt'])
        && request.resource.data.email == request.auth.token.email
        && request.resource.data.createdAt == request.time;

      allow update: if isOwner(userId)
        && request.resource.data.diff(resource.data).affectedKeys()
             .hasOnly(['displayName', 'photoUrl', 'updatedAt']);

      allow delete: if false; // account deletion goes through a Cloud Function
    }
  }
}
```

> **Production tip:** for a B2C app, the most robust way to create the profile is a Cloud Function triggered on Auth user creation. The client-side "create if absent" below is the no-backend alternative and self-heals on next sign-in.

---

## 3. Domain Layer

### Step 3.1: Models

**File:** `features/auth/domain/model/AuthUser.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.model

/** The authenticated identity. Source of truth: Firebase Auth. */
data class AuthUser(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
)
```

**File:** `features/user/domain/model/UserProfile.kt`

```kotlin
package com.example.dukkanapp.features.user.domain.model

/** The customer's profile. Source of truth: Firestore `users/{uid}`. */
data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val createdAtMillis: Long?,
)
```

### Step 3.2: Repositories

**File:** `features/auth/domain/repository/AuthRepository.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    val authState: Flow<AuthUser?>

    suspend fun signUp(email: String, password: String): AppResult<AuthUser>
    suspend fun signIn(email: String, password: String): AppResult<AuthUser>
    suspend fun sendEmailVerification(): EmptyResult
    suspend fun reloadUser(): AppResult<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): EmptyResult
    fun signOut()
}
```

**File:** `features/user/domain/repository/UserRepository.kt`

```kotlin
package com.example.dukkanapp.features.user.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.user.domain.model.UserProfile

interface UserRepository {
    suspend fun createProfileIfAbsent(user: AuthUser): EmptyResult
    suspend fun getProfile(uid: String): AppResult<UserProfile>
    suspend fun updateProfile(uid: String, displayName: String, photoUrl: String?): EmptyResult
}
```

### Step 3.3: Use Cases

**File:** `features/auth/domain/usecase/SignUpUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    /**
     * Once the Auth account exists, sign-up has succeeded. The follow-up steps are best effort:
     * the profile is re-ensured on every sign-in, and the verification email can be resent
     * from the verification screen. Failing here would leave the user unable to retry
     * ("email already in use").
     */
    suspend operator fun invoke(email: String, password: String): AppResult<AuthUser> =
        authRepository.signUp(email, password)
            .onSuccess { user ->
                userRepository.createProfileIfAbsent(user)
                authRepository.sendEmailVerification()
            }
}
```

**File:** `features/auth/domain/usecase/SignInUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<AuthUser> =
        authRepository.signIn(email, password)
            .onSuccess { user -> userRepository.createProfileIfAbsent(user) } // self-heals a failed sign-up write
}
```

**File:** `features/auth/domain/usecase/CheckEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.map
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class CheckEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    /** Reloads the user from the server; the cached flag never changes on its own. */
    suspend operator fun invoke(): AppResult<Boolean> =
        authRepository.reloadUser().map { it.isEmailVerified }
}
```

**File:** `features/auth/domain/usecase/SendEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SendEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): EmptyResult = authRepository.sendEmailVerification()
}
```

**File:** `features/auth/domain/usecase/GetCurrentUserUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): AuthUser? = authRepository.currentUser
}
```

### Step 3.4: AuthFormValidator

Shared by Login and Sign Up, so the rules are written once.

**File:** `features/auth/domain/validation/AuthFormValidator.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.validation

import com.example.dukkanapp.core.domain.validation.ValidationError
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

object AuthFormValidator {

    fun validateEmail(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EMAIL_EMPTY
        !email.trim().isValidEmail() -> ValidationError.EMAIL_INVALID
        else -> null
    }

    /** Sign-up: enforce the password policy. */
    fun validateNewPassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.PASSWORD_EMPTY
        !password.isValidPassword() -> ValidationError.PASSWORD_TOO_SHORT
        else -> null
    }

    /** Login: only require a value. Older accounts may predate the current policy. */
    fun validateExistingPassword(password: String): ValidationError? =
        if (password.isEmpty()) ValidationError.PASSWORD_EMPTY else null

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationError? = when {
        confirmPassword.isEmpty() -> ValidationError.CONFIRM_PASSWORD_EMPTY
        password != confirmPassword -> ValidationError.PASSWORDS_MISMATCH
        else -> null
    }
}
```

---

## 4. Data Layer

Data sources are thin wrappers that only call Firebase and let exceptions propagate. Repositories are the single error boundary (`errorMapper.safeCall`).

### Step 4.1: FirebaseUser Mapper

**File:** `features/auth/data/mapper/AuthUserMapper.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.mapper

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    email = email.orEmpty(),
    isEmailVerified = isEmailVerified,
)
```

### Step 4.2: AuthRemoteDataSource

**File:** `features/auth/data/datasource/AuthRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.core.data.error.AppErrorException
import com.example.dukkanapp.core.error.AppError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Thin wrapper over FirebaseAuth. Throws raw Firebase exceptions; the repository maps them. */
class AuthRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser =
        checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)

    suspend fun signIn(email: String, password: String): FirebaseUser =
        checkNotNull(auth.signInWithEmailAndPassword(email, password).await().user)

    suspend fun sendEmailVerification() {
        requireUser().sendEmailVerification().await()
    }

    suspend fun reloadUser(): FirebaseUser {
        requireUser().reload().await()
        return requireUser()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw AppErrorException(AppError.Auth.SessionExpired)
}
```

### Step 4.3: AuthRepositoryImpl

**File:** `features/auth/data/repository/AuthRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.repository

import com.example.dukkanapp.core.data.error.ErrorMapper
import com.example.dukkanapp.core.data.error.safeCall
import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.data.datasource.AuthRemoteDataSource
import com.example.dukkanapp.features.auth.data.mapper.toAuthUser
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val errorMapper: ErrorMapper,
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = remote.currentUser?.toAuthUser()

    override val authState: Flow<AuthUser?> =
        remote.authState.map { it?.toAuthUser() }

    override suspend fun signUp(email: String, password: String): AppResult<AuthUser> =
        errorMapper.safeCall { remote.signUp(email, password).toAuthUser() }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> =
        errorMapper.safeCall { remote.signIn(email, password).toAuthUser() }

    override suspend fun sendEmailVerification(): EmptyResult =
        errorMapper.safeCall { remote.sendEmailVerification() }

    override suspend fun reloadUser(): AppResult<AuthUser> =
        errorMapper.safeCall { remote.reloadUser().toAuthUser() }

    override suspend fun sendPasswordResetEmail(email: String): EmptyResult =
        errorMapper.safeCall { remote.sendPasswordResetEmail(email) }

    override fun signOut() = remote.signOut()
}
```

Compare with the previous version: 8 `try/catch` blocks are gone, and the error behavior is identical everywhere.

### Step 4.4: Firestore Profile DTO

**File:** `features/user/data/model/UserProfileDto.kt`

```kotlin
package com.example.dukkanapp.features.user.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore shape of `users/{uid}`.
 * - [uid] comes from the document id ([DocumentId] fields are not written).
 * - [createdAt]/[updatedAt] are filled by the server when written as null.
 * - Email verification is NOT stored: Firebase Auth is its source of truth.
 */
data class UserProfileDto(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    companion object {
        const val COLLECTION = "users"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
```

**File:** `features/user/data/mapper/UserProfileMapper.kt`

```kotlin
package com.example.dukkanapp.features.user.data.mapper

import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.user.data.model.UserProfileDto
import com.example.dukkanapp.features.user.domain.model.UserProfile

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    createdAtMillis = createdAt?.toDate()?.time,
)

fun AuthUser.toNewProfileDto(): UserProfileDto = UserProfileDto(email = email)
```

### Step 4.5: UserRemoteDataSource

**File:** `features/user/data/datasource/UserRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.user.data.datasource

import com.example.dukkanapp.core.data.error.AppErrorException
import com.example.dukkanapp.core.error.AppError
import com.example.dukkanapp.features.user.data.model.UserProfileDto
import com.example.dukkanapp.features.user.data.model.UserProfileDto.Companion.COLLECTION
import com.example.dukkanapp.features.user.data.model.UserProfileDto.Companion.FIELD_DISPLAY_NAME
import com.example.dukkanapp.features.user.data.model.UserProfileDto.Companion.FIELD_PHOTO_URL
import com.example.dukkanapp.features.user.data.model.UserProfileDto.Companion.FIELD_UPDATED_AT
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun userDoc(uid: String) = firestore.collection(COLLECTION).document(uid)

    /** Idempotent: never overwrites an existing profile (and its createdAt). */
    suspend fun createProfileIfAbsent(uid: String, profile: UserProfileDto) {
        val ref = userDoc(uid)
        firestore.runTransaction { tx ->
            if (!tx.get(ref).exists()) tx.set(ref, profile)
            null
        }.await()
    }

    suspend fun getProfile(uid: String): UserProfileDto =
        userDoc(uid).get().await().toObject(UserProfileDto::class.java)
            ?: throw AppErrorException(AppError.Data.NotFound)

    /** Writes only the editable fields, so createdAt is never touched. */
    suspend fun updateProfile(uid: String, displayName: String, photoUrl: String?) {
        userDoc(uid).update(
            mapOf(
                FIELD_DISPLAY_NAME to displayName,
                FIELD_PHOTO_URL to photoUrl,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            )
        ).await()
    }
}
```

> **Offline behavior:** Firestore queues writes offline, and `update(...).await()` only resumes after the **server** acknowledges. For profile edits you can skip awaiting the server and trust the local cache. Transactions (`createProfileIfAbsent`) fail fast offline with `UNAVAILABLE`, which maps to `AppError.Network.ServiceUnavailable`.

### Step 4.6: UserRepositoryImpl

**File:** `features/user/data/repository/UserRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.user.data.repository

import com.example.dukkanapp.core.data.error.ErrorMapper
import com.example.dukkanapp.core.data.error.safeCall
import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser
import com.example.dukkanapp.features.user.data.datasource.UserRemoteDataSource
import com.example.dukkanapp.features.user.data.mapper.toDomain
import com.example.dukkanapp.features.user.data.mapper.toNewProfileDto
import com.example.dukkanapp.features.user.domain.model.UserProfile
import com.example.dukkanapp.features.user.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val errorMapper: ErrorMapper,
) : UserRepository {

    override suspend fun createProfileIfAbsent(user: AuthUser): EmptyResult =
        errorMapper.safeCall { remote.createProfileIfAbsent(user.uid, user.toNewProfileDto()) }

    override suspend fun getProfile(uid: String): AppResult<UserProfile> =
        errorMapper.safeCall { remote.getProfile(uid).toDomain() }

    override suspend fun updateProfile(uid: String, displayName: String, photoUrl: String?): EmptyResult =
        errorMapper.safeCall { remote.updateProfile(uid, displayName, photoUrl) }
}
```

---

## 5. Dependency Injection

**File:** `core/di/FirebaseModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

**File:** `core/di/AuthModule.kt` (updated)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Singleton
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
```

**File:** `core/di/UserModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Singleton
    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

`ErrorModule` (binding `ErrorMapper`) comes from the [Error Handling guide](../core/error_handling_architecture.md#11-dependency-injection). Data sources and use cases need no bindings: they are concrete `@Inject constructor` classes.

---

## 6. Sign Up (MVI)

### Step 6.1: SignUpUiState

**File:** `features/auth/presentation/logic/signup/SignUpUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val generalError: UiText? = null,
    val isLoading: Boolean = false,
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean
        get() = confirmPasswordError == null && confirmPassword.isNotEmpty() && password == confirmPassword
}
```

### Step 6.2: SignUpIntent and SignUpEffect

```kotlin
sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object ErrorDismissed : SignUpIntent
}

sealed interface SignUpEffect {
    data object NavigateToEmailVerification : SignUpEffect
}
```

### Step 6.3: SignUpViewModel

**File:** `features/auth/presentation/logic/signup/SignUpViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.error.toUiText
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.domain.validation.ValidationError
import com.example.dukkanapp.core.error.AppError
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.usecase.SignUpUseCase
import com.example.dukkanapp.features.auth.domain.validation.AuthFormValidator
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
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _effect = Channel<SignUpEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** Single entry point: the screen only sends intents. */
    fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> onEmailChanged(intent.email)
            is SignUpIntent.PasswordChanged -> onPasswordChanged(intent.password)
            is SignUpIntent.ConfirmPasswordChanged -> onConfirmPasswordChanged(intent.confirmPassword)
            SignUpIntent.SignUpClicked -> onSignUpClicked()
            SignUpIntent.ErrorDismissed -> _state.update { it.copy(generalError = null) }
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
                confirmPasswordError = liveMismatchError(password, it.confirmPassword),
                generalError = null,
            )
        }
    }

    private fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = liveMismatchError(it.password, confirmPassword),
                generalError = null,
            )
        }
    }

    private fun onSignUpClicked() {
        val current = _state.value
        if (current.isLoading) return

        val email = current.email.trim()
        val emailError = AuthFormValidator.validateEmail(email)
        val passwordError = AuthFormValidator.validateNewPassword(current.password)
        val confirmError = AuthFormValidator.validateConfirmPassword(current.password, current.confirmPassword)

        if (emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(
                    emailError = emailError?.toUiText(),
                    passwordError = passwordError?.toUiText(),
                    confirmPasswordError = confirmError?.toUiText(),
                )
            }
            return
        }

        // Set synchronously, before launch, so a double tap can't start two sign-ups.
        _state.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            signUp(email, current.password)
                .onSuccess { _effect.send(SignUpEffect.NavigateToEmailVerification) }
                .onFailure(::showError)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Errors that concern a single field are shown on that field; everything else in the banner. */
    private fun showError(error: AppError) {
        val message = error.toUiText()
        _state.update {
            when (error) {
                AppError.Auth.InvalidEmail,
                AppError.Auth.EmailAlreadyInUse -> it.copy(emailError = message)
                AppError.Auth.WeakPassword -> it.copy(passwordError = message)
                else -> it.copy(generalError = message)
            }
        }
    }

    private fun liveMismatchError(password: String, confirmPassword: String): UiText? =
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            ValidationError.PASSWORDS_MISMATCH.toUiText()
        } else {
            null
        }
}
```

### Step 6.4: SignUpScreen

**File:** `features/auth/presentation/screens/SignUpScreen.kt`

```kotlin
@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToEmailVerification: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToEmailVerification by rememberUpdatedState(onNavigateToEmailVerification)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SignUpEffect.NavigateToEmailVerification -> currentOnNavigateToEmailVerification()
            }
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigationBack = onNavigationBack,
        onForgetPasswordClick = onForgetPasswordClick,
        onLoginClick = onLoginClick,
    )
}
```

### Step 6.5: SignUpScreenContent changes

Field errors switch from `stringResource(id)` to `UiText.asString()`, which also fixes the `%1$d` argument:

```kotlin
errorText = state.emailError?.asString(),
// ...
errorText = state.passwordError?.asString(),
// ...
errorText = state.confirmPasswordError?.asString(),
```

General error banner (after `AppAuthHeader`):

```kotlin
state.generalError?.let { error ->
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(AppDimens.sizeXs),
        )
    }
    Spacer(Modifier.height(AppDimens.sizeXs))
}
```

> Extract this banner to `core/common/components/banner/AppErrorBanner.kt` once Login uses it too.

---

## 7. Login (MVI)

Login follows the same pattern. Here are only the parts that differ:

```kotlin
sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToEmailVerification : LoginEffect
}

private fun onLoginClicked() {
    val current = _state.value
    if (current.isLoading) return

    val email = current.email.trim()
    val emailError = AuthFormValidator.validateEmail(email)
    val passwordError = AuthFormValidator.validateExistingPassword(current.password)
    if (emailError != null || passwordError != null) {
        _state.update { it.copy(emailError = emailError?.toUiText(), passwordError = passwordError?.toUiText()) }
        return
    }

    _state.update { it.copy(isLoading = true, generalError = null) }
    viewModelScope.launch {
        signIn(email, current.password)
            .onSuccess { user ->
                _effect.send(
                    if (user.isEmailVerified) LoginEffect.NavigateToHome
                    else LoginEffect.NavigateToEmailVerification
                )
            }
            .onFailure { error -> _state.update { it.copy(generalError = error.toUiText()) } }
        _state.update { it.copy(isLoading = false) }
    }
}
```

---

## 8. Email Verification (MVI)

### Step 8.1: State, Intent, Effect

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationUiState.kt`

```kotlin
data class EmailVerificationUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val message: UiText? = null,
) {
    val canResend: Boolean get() = !isResending && resendCooldownSeconds == 0
}
```

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationIntent.kt`

```kotlin
sealed interface EmailVerificationIntent {
    /** Sent on every ON_RESUME, e.g. when the user comes back from their mail app. */
    data object ScreenResumed : EmailVerificationIntent
    data object CheckClicked : EmailVerificationIntent
    data object ResendClicked : EmailVerificationIntent
    data object OpenEmailAppClicked : EmailVerificationIntent
    data object EmailAppUnavailable : EmailVerificationIntent
    data object MessageDismissed : EmailVerificationIntent
}
```

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationEffect.kt`

```kotlin
sealed interface EmailVerificationEffect {
    data object NavigateToHome : EmailVerificationEffect
    data object OpenEmailApp : EmailVerificationEffect
}
```

### Step 8.2: EmailVerificationViewModel

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.error.toUiText
import com.example.dukkanapp.core.common.text.UiText
import com.example.dukkanapp.core.error.onFailure
import com.example.dukkanapp.core.error.onSuccess
import com.example.dukkanapp.features.auth.domain.usecase.CheckEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.GetCurrentUserUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SendEmailVerificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val checkEmailVerification: CheckEmailVerificationUseCase,
    private val sendEmailVerification: SendEmailVerificationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EmailVerificationUiState(email = getCurrentUser()?.email.orEmpty())
    )
    val state: StateFlow<EmailVerificationUiState> = _state.asStateFlow()

    private val _effect = Channel<EmailVerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var checkJob: Job? = null

    fun onIntent(intent: EmailVerificationIntent) {
        when (intent) {
            EmailVerificationIntent.ScreenResumed -> checkVerification(silent = true)
            EmailVerificationIntent.CheckClicked -> checkVerification(silent = false)
            EmailVerificationIntent.ResendClicked -> resend()
            EmailVerificationIntent.OpenEmailAppClicked -> viewModelScope.launch {
                _effect.send(EmailVerificationEffect.OpenEmailApp)
            }
            EmailVerificationIntent.EmailAppUnavailable -> showMessage(UiText.Res(R.string.email_verification_no_email_app))
            EmailVerificationIntent.MessageDismissed -> _state.update { it.copy(message = null) }
        }
    }

    /** One check at a time: ignores taps while a check is already running. */
    private fun checkVerification(silent: Boolean) {
        if (checkJob?.isActive == true) return
        if (!silent) _state.update { it.copy(isChecking = true, message = null) }

        checkJob = viewModelScope.launch {
            checkEmailVerification()
                .onSuccess { verified ->
                    when {
                        verified -> _effect.send(EmailVerificationEffect.NavigateToHome)
                        !silent -> showMessage(UiText.Res(R.string.email_verification_not_verified_yet))
                    }
                }
                .onFailure { error -> if (!silent) showMessage(error.toUiText()) }
            _state.update { it.copy(isChecking = false) }
        }
    }

    private fun resend() {
        if (!_state.value.canResend) return
        _state.update { it.copy(isResending = true, message = null) }

        viewModelScope.launch {
            sendEmailVerification()
                .onSuccess {
                    showMessage(UiText.Res(R.string.email_verification_resent_success))
                    startResendCooldown()
                }
                .onFailure { error -> showMessage(error.toUiText()) }
            _state.update { it.copy(isResending = false) }
        }
    }

    private fun startResendCooldown() {
        viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 0) {
                _state.update { it.copy(resendCooldownSeconds = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private fun showMessage(message: UiText) {
        _state.update { it.copy(message = message) }
    }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
```

### Step 8.3: EmailVerificationScreen

**File:** `features/auth/presentation/screens/EmailVerificationScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.emailverification.EmailVerificationScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationEffect
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationIntent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationViewModel

@Composable
fun EmailVerificationScreen(
    onNavigationBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToHome by rememberUpdatedState(onNavigateToHome)
    val context = LocalContext.current

    // Re-check whenever the user returns to the app (e.g. after tapping the link in their mail app).
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(EmailVerificationIntent.ScreenResumed)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EmailVerificationEffect.NavigateToHome -> currentOnNavigateToHome()
                EmailVerificationEffect.OpenEmailApp -> {
                    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onIntent(EmailVerificationIntent.EmailAppUnavailable)
                    }
                }
            }
        }
    }

    EmailVerificationScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigationBack = onNavigationBack,
    )
}
```

### Step 8.4: EmailVerificationScreenContent

**File:** `features/auth/presentation/components/emailverification/EmailVerificationScreenContent.kt`

```kotlin
@Composable
fun EmailVerificationScreenContent(
    state: EmailVerificationUiState,
    onIntent: (EmailVerificationIntent) -> Unit,
    onNavigationBack: () -> Unit,
) {
    AppScaffold(onNavigateBack = onNavigationBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Iconsax.Linear.Sms,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            Text(
                text = stringResource(R.string.email_verification_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            Text(
                text = stringResource(R.string.email_verification_subtitle, state.email),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppDimens.sizeMd))

            state.message?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        text = message.asString(),
                        modifier = Modifier.padding(AppDimens.sizeXs),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(AppDimens.sizeXs))
            }

            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.OpenEmailAppClicked) },
                text = stringResource(R.string.email_verification_open_email_app),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.CheckClicked) },
                text = stringResource(R.string.email_verification_check_status),
                isLoading = state.isChecking,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppTextButton(
                onClick = { onIntent(EmailVerificationIntent.ResendClicked) },
                enabled = state.canResend,
                text = when {
                    state.isResending -> stringResource(R.string.email_verification_resending)
                    state.resendCooldownSeconds > 0 ->
                        stringResource(R.string.email_verification_resend_in, state.resendCooldownSeconds)
                    else -> stringResource(R.string.email_verification_resend)
                },
            )
        }
    }
}
```

> If `AppTextButton` has no `enabled` parameter yet, add one (default `true`).

---

## 9. Navigation and App Start

### Step 9.1: Route

**File:** `core/navigation/AppRoutes.kt`

```kotlin
@Serializable
data object EmailVerificationScreen : AppRoute
```

> While you're there: `LoginScreen` and `SignUpScreen` don't implement `AppRoute` yet. Add `: AppRoute` to both.

### Step 9.2: NavHost

```kotlin
composable<AppRoute.SignUpScreen> {
    SignUpScreen(
        onNavigationBack = { navController.popBackStack() },
        onNavigateToEmailVerification = {
            navController.navigate(AppRoute.EmailVerificationScreen) {
                // The account now exists: going "back" to the sign-up form makes no sense.
                popUpTo(AppRoute.AuthOptionsScreen) { inclusive = true }
            }
        },
        onForgetPasswordClick = {},
        onLoginClick = {
            navController.navigate(AppRoute.LoginScreen) {
                popUpTo(AppRoute.SignUpScreen) { inclusive = true }
                launchSingleTop = true
            }
        },
    )
}

composable<AppRoute.EmailVerificationScreen> {
    EmailVerificationScreen(
        onNavigationBack = { navController.popBackStack() },
        onNavigateToHome = {
            navController.navigate(AppRoute.HomeScreen) {
                popUpTo(AppRoute.EmailVerificationScreen) { inclusive = true }
            }
        },
    )
}
```

### Step 9.3: Start Destination

Users who signed up but never verified must land on the verification screen, not Home.

**File:** `core/navigation/startup/ResolveStartDestinationUseCase.kt`

```kotlin
class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppRoute = coroutineScope {
        val hasLanguageDeferred = async { languageRepository.hasSelectedLanguage() }
        val hasOnboardingDeferred = async { onboardingRepository.hasSeenOnboarding() }
        val user = authRepository.currentUser // cached locally by Firebase, no network

        when {
            !hasLanguageDeferred.await() -> AppRoute.LanguageSelectionScreen
            !hasOnboardingDeferred.await() -> AppRoute.OnboardingScreen
            user == null -> AppRoute.AuthOptionsScreen
            !user.isEmailVerified -> AppRoute.EmailVerificationScreen
            else -> AppRoute.HomeScreen
        }
    }
}
```

---

## 10. String Resources

Error and validation strings come from the [Error Handling guide](../core/error_handling_architecture.md#12-string-resources). Only screen-specific copy lives here.

### `values/strings.xml`

```xml
<!-- Email Verification -->
<string name="email_verification_title">Verify your email</string>
<string name="email_verification_subtitle">We\'ve sent a verification link to %1$s</string>
<string name="email_verification_open_email_app">Open Email App</string>
<string name="email_verification_check_status">I\'ve Verified My Email</string>
<string name="email_verification_resend">Resend Verification Email</string>
<string name="email_verification_resend_in">Resend in %1$ds</string>
<string name="email_verification_resending">Sending…</string>
<string name="email_verification_resent_success">Verification email sent!</string>
<string name="email_verification_not_verified_yet">Email not verified yet. Please check your inbox.</string>
<string name="email_verification_no_email_app">No email app found on this device.</string>
```

### `values-ar/strings.xml`

```xml
<!-- تأكيد البريد الإلكتروني -->
<string name="email_verification_title">تأكيد البريد الإلكتروني</string>
<string name="email_verification_subtitle">أرسلنا رابط التأكيد إلى %1$s</string>
<string name="email_verification_open_email_app">فتح تطبيق البريد</string>
<string name="email_verification_check_status">لقد أكدت بريدي الإلكتروني</string>
<string name="email_verification_resend">إعادة إرسال رسالة التأكيد</string>
<string name="email_verification_resend_in">إعادة الإرسال خلال %1$d ث</string>
<string name="email_verification_resending">جارٍ الإرسال…</string>
<string name="email_verification_resent_success">تم إرسال رسالة التأكيد!</string>
<string name="email_verification_not_verified_yet">لم يتم تأكيد البريد الإلكتروني بعد. يرجى التحقق من صندوق الوارد.</string>
<string name="email_verification_no_email_app">لا يوجد تطبيق بريد على هذا الجهاز.</string>
```

### `values-fr/strings.xml`

```xml
<!-- Vérification de l'e-mail -->
<string name="email_verification_title">Vérifiez votre e-mail</string>
<string name="email_verification_subtitle">Nous avons envoyé un lien de vérification à %1$s</string>
<string name="email_verification_open_email_app">Ouvrir l\'application e-mail</string>
<string name="email_verification_check_status">J\'ai vérifié mon e-mail</string>
<string name="email_verification_resend">Renvoyer l\'e-mail de vérification</string>
<string name="email_verification_resend_in">Renvoyer dans %1$d s</string>
<string name="email_verification_resending">Envoi en cours…</string>
<string name="email_verification_resent_success">E-mail de vérification envoyé !</string>
<string name="email_verification_not_verified_yet">E-mail non vérifié. Consultez votre boîte de réception.</string>
<string name="email_verification_no_email_app">Aucune application e-mail trouvée sur cet appareil.</string>
```

### Firebase Auth → UI mapping (reference)

| Firebase | AppError | String |
|---|---|---|
| `ERROR_INVALID_EMAIL` | `Auth.InvalidEmail` (email field) | `error_invalid_email` |
| `ERROR_WEAK_PASSWORD` | `Auth.WeakPassword` (password field) | `error_weak_password` |
| `ERROR_EMAIL_ALREADY_IN_USE` | `Auth.EmailAlreadyInUse` (email field) | `error_email_already_in_use` |
| `ERROR_INVALID_CREDENTIAL`, `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND` | `Auth.InvalidCredentials` | `error_invalid_credentials` |
| `ERROR_USER_DISABLED` | `Auth.UserDisabled` | `error_user_disabled` |
| `FirebaseTooManyRequestsException` | `Auth.TooManyRequests` | `error_too_many_requests` |
| `FirebaseNetworkException` | `Network.NoConnection` | `error_no_connection` |
| Firestore `UNAVAILABLE` | `Network.ServiceUnavailable` | `error_service_unavailable` |
| Firestore `PERMISSION_DENIED` | `Data.PermissionDenied` | `error_permission_denied` |

---

## 11. Folder Structure

```
core/
├── error/                       AppError.kt, AppResult.kt              (Error guide)
├── data/error/                  AppErrorException.kt, ErrorMapper.kt, FirebaseErrorMapper.kt
├── common/text/                 UiText.kt
├── common/error/                AppErrorUiText.kt, ValidationErrorUiText.kt
├── domain/validation/           ValidationError.kt
├── di/
│   ├── ErrorModule.kt           (NEW)
│   ├── FirebaseModule.kt        (NEW)
│   ├── AuthModule.kt            (UPDATED)
│   └── UserModule.kt            (NEW)
└── navigation/
    ├── AppRoutes.kt             (UPDATED: EmailVerificationScreen)
    ├── AppNavHost.kt            (UPDATED)
    └── startup/ResolveStartDestinationUseCase.kt (UPDATED)

features/auth/
├── data/
│   ├── datasource/AuthRemoteDataSource.kt
│   ├── mapper/AuthUserMapper.kt
│   └── repository/AuthRepositoryImpl.kt
├── domain/
│   ├── model/AuthUser.kt
│   ├── repository/AuthRepository.kt
│   ├── validation/AuthFormValidator.kt
│   └── usecase/
│       ├── SignUpUseCase.kt
│       ├── SignInUseCase.kt
│       ├── SendEmailVerificationUseCase.kt
│       ├── CheckEmailVerificationUseCase.kt
│       └── GetCurrentUserUseCase.kt
└── presentation/
    ├── components/emailverification/EmailVerificationScreenContent.kt
    ├── logic/signup/               SignUpUiState, SignUpIntent, SignUpEffect, SignUpViewModel
    ├── logic/Login/                (UPDATED to use SignInUseCase)
    ├── logic/emailverification/    UiState, Intent, Effect, ViewModel
    └── screens/                    SignUpScreen.kt, EmailVerificationScreen.kt

features/user/
├── data/
│   ├── datasource/UserRemoteDataSource.kt
│   ├── mapper/UserProfileMapper.kt
│   ├── model/UserProfileDto.kt
│   └── repository/UserRepositoryImpl.kt
└── domain/
    ├── model/UserProfile.kt
    └── repository/UserRepository.kt
```

---

## 12. Implementation Checklist

**Core error handling (do first):** see the [Error Handling checklist](../core/error_handling_architecture.md#14-implementation-checklist).

**Firebase setup**
- [ ] BoM 34+, `firebase-auth`, `firebase-firestore`, `kotlinx-coroutines-play-services`
- [ ] Google Services plugin + `google-services.json`
- [ ] Email/Password enabled, enumeration protection on
- [ ] Firestore created in production mode with the rules above

**Domain**
- [ ] `AuthUser`, `UserProfile`
- [ ] `AuthRepository`, `UserRepository`
- [ ] Use cases: SignUp, SignIn, SendEmailVerification, CheckEmailVerification, GetCurrentUser
- [ ] `AuthFormValidator`

**Data**
- [ ] `AuthRemoteDataSource`, `AuthUserMapper`, `AuthRepositoryImpl`
- [ ] `UserProfileDto`, `UserProfileMapper`, `UserRemoteDataSource`, `UserRepositoryImpl`

**DI**
- [ ] `ErrorModule`, `FirebaseModule`, `UserModule`; update `AuthModule`

**Presentation**
- [ ] SignUp: state with `UiText`, ViewModel, screen, content
- [ ] Login: `SignInUseCase` + verified/unverified routing
- [ ] EmailVerification: state, intent, effect, ViewModel, screen, content
- [ ] Routes, NavHost, start destination

**Resources**
- [ ] Email verification strings (EN, AR, FR)
- [ ] Remove `signup_error_*` / `login_error_*` duplicates after migration

---

## 13. Manual Testing

1. **Sign up (happy path):** valid email + password → Email Verification screen; `users/{uid}` exists with `createdAt`; verification email arrives.
2. **Verify:** tap the link in the email, return to the app → navigates to Home **automatically** (resume check).
3. **Resend:** tap Resend → success message, button disabled with a 60 s countdown.
4. **Unverified restart:** sign up, kill the app, relaunch → starts on Email Verification.
5. **Errors:**
   - Existing email → message under the **email field**
   - Short password → "at least 8 characters" (number rendered, not `%1$d`)
   - Wrong password at login → "Incorrect email or password"
   - Airplane mode → "No internet connection…"
6. **Self-healing profile:** delete `users/{uid}` in the console, sign in → document recreated, original account intact.
7. **Security rules:** in the Rules Playground, try writing `users/otherUid` as another user → denied.
