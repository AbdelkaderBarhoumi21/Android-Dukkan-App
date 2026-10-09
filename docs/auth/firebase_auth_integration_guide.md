# Firebase Authentication Integration Guide

Complete guide for integrating Firebase Authentication with email verification and Firestore user collection in the Dukkan App. Follows the existing **MVI architecture** (State + Intent + Effect).

> **Prerequisites:** Read [Error Handling Architecture](../core/error_handling_architecture.md) first. This guide uses `AppResult`, `AppException`, and the Firebase exception mappers defined there.

```
User SignUp → Firebase Auth → Send Verification Email → Create User in Firestore
                    ↓
           Navigate to Email Verification Screen
                    ↓
           User verifies email → Navigate to Home
```

---

## Table of Contents

1. [Dependencies Setup](#1-dependencies-setup)
2. [Firebase Console Setup](#2-firebase-console-setup)
3. [Domain Layer](#3-domain-layer)
4. [Data Layer](#4-data-layer)
5. [Dependency Injection](#5-dependency-injection)
6. [Presentation Layer - Updated MVI](#6-presentation-layer---updated-mvi)
7. [Email Verification Screen](#7-email-verification-screen)
8. [Navigation Setup](#8-navigation-setup)
9. [String Resources](#9-string-resources)
10. [Error Handling](#10-error-handling)

> **Note:** This guide uses the centralized error handling from `core/error/`. See [Error Handling Architecture](../core/error_handling_architecture.md) for details on `AppResult`, `AppException`, and `ErrorCodeMapper`.

---

## 1. Dependencies Setup

### Step 1.1: Add Firebase BOM and dependencies to `gradle/libs.versions.toml`

```toml
[versions]
# ... existing versions ...
firebaseBom = "33.7.0"

[libraries]
# ... existing libraries ...

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }

[plugins]
# ... existing plugins ...
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

### Step 1.2: Update root `build.gradle.kts`

```kotlin
plugins {
    // ... existing plugins ...
    alias(libs.plugins.google.services) apply false
}
```

### Step 1.3: Update `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)  // Add this
}

dependencies {
    // ... existing dependencies ...

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
}
```

### Step 1.4: Add `google-services.json`

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add Android app with package name: `com.example.dukkanapp`
4. Download `google-services.json`
5. Place it in `app/` directory

---

## 2. Firebase Console Setup

### Step 2.1: Enable Email/Password Authentication

1. Go to Firebase Console → Authentication → Sign-in method
2. Enable **Email/Password** provider
3. Enable **Email link (passwordless sign-in)** if needed

### Step 2.2: Create Firestore Database

1. Go to Firebase Console → Firestore Database
2. Click **Create database**
3. Choose **Start in test mode** (for development)
4. Select your region

### Step 2.3: Firestore Security Rules (Production)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read, update, delete: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null;
    }
  }
}
```

---

## 3. Domain Layer

### Step 3.1: Create User Model

**File:** `features/auth/domain/model/User.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Step 3.2: Use Core Error Handling

> **Important:** We use the centralized `AppResult` and `AppException` from `core/error/` instead of creating auth-specific error classes. See [Error Handling Architecture](../core/error_handling_architecture.md).

The following are already defined in core:
- `core/error/AppResult.kt` - Generic result wrapper
- `core/error/AppException.kt` - All exception types including auth exceptions
- `core/error/firebase/FirebaseAuthExceptionMapper.kt` - Maps Firebase Auth errors
- `core/error/firebase/FirebaseFirestoreExceptionMapper.kt` - Maps Firestore errors

### Step 3.3: Update AuthRepository Interface

**File:** `features/auth/domain/repository/AuthRepository.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    
    val currentUser: Flow<User?>
    
    suspend fun isLoggedIn(): Boolean
    
    suspend fun signUpWithEmail(
        email: String,
        password: String
    ): AppResult<User>
    
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): AppResult<User>
    
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

### Step 3.4: Create Use Cases

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
        }
        
        return AppResult.Success(user)
    }
}
```

**File:** `features/auth/domain/usecase/SendEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SendEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return authRepository.sendEmailVerification()
    }
}
```

**File:** `features/auth/domain/usecase/CheckEmailVerificationUseCase.kt`

```kotlin
package com.example.dukkanapp.features.auth.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class CheckEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Boolean> {
        // Reload user to get latest verification status
        val reloadResult = authRepository.reloadUser()
        
        if (reloadResult is AppResult.Error) {
            return reloadResult
        }
        
        val isVerified = authRepository.isEmailVerified()
        return AppResult.Success(isVerified)
    }
}
```

---

## 4. Data Layer

> **Architecture:** Data sources throw `AppException`, repositories catch and wrap in `AppResult`. See [Error Handling Architecture](../core/error_handling_architecture.md).

### Step 4.1: Create Firestore User DTO

**File:** `features/auth/data/model/UserDto.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.model

import com.example.dukkanapp.features.auth.domain.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserDto(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): User = User(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )
    
    companion object {
        fun fromDomain(user: User): UserDto = UserDto(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            isEmailVerified = user.isEmailVerified
        )
    }
}
```

### Step 4.2: Create Remote Data Sources

> See [Error Handling Architecture](../core/error_handling_architecture.md) for full data source implementations with `AuthRemoteDataSource` and `UserRemoteDataSource`.

**File:** `features/auth/data/datasource/AuthRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.google.firebase.auth.FirebaseUser

/**
 * Remote data source for authentication operations.
 * All methods throw AppException on failure.
 */
interface AuthRemoteDataSource {
    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser
    suspend fun signInWithEmail(email: String, password: String): FirebaseUser
    suspend fun sendEmailVerification()
    suspend fun reloadUser(): FirebaseUser
    fun getCurrentUser(): FirebaseUser?
    fun isEmailVerified(): Boolean
    fun signOut()
    suspend fun sendPasswordResetEmail(email: String)
}
```

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

**File:** `features/auth/data/datasource/UserRemoteDataSource.kt`

```kotlin
package com.example.dukkanapp.features.auth.data.datasource

import com.example.dukkanapp.features.auth.data.model.UserDto

/**
 * Remote data source for user Firestore operations.
 * All methods throw AppException on failure.
 */
interface UserRemoteDataSource {
    suspend fun createUser(userDto: UserDto)
    suspend fun getUser(uid: String): UserDto?
    suspend fun updateUser(userDto: UserDto)
    suspend fun deleteUser(uid: String)
    suspend fun updateUserFields(uid: String, fields: Map<String, Any?>)
}
```

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

### Step 4.3: Update AuthRepositoryImpl

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

## 5. Dependency Injection

### Step 5.1: Create Firebase Module

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
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
```

### Step 5.2: Update AuthModule

**File:** `core/di/AuthModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.features.auth.data.datasource.AuthRemoteDataSource
import com.example.dukkanapp.features.auth.data.datasource.AuthRemoteDataSourceImpl
import com.example.dukkanapp.features.auth.data.datasource.UserRemoteDataSource
import com.example.dukkanapp.features.auth.data.datasource.UserRemoteDataSourceImpl
import com.example.dukkanapp.features.auth.data.repository.AuthRepositoryImpl
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        authRemoteDataSourceImpl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource
    
    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        userRemoteDataSourceImpl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource
}
```

> **Note:** Use cases don't need explicit `@Provides` when using `@Inject constructor` - Hilt will inject them automatically.

---

## 6. Presentation Layer - Updated MVI

### Step 6.1: Update SignUpUiState

**File:** `features/auth/presentation/logic/signup/SignUpUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.annotation.StringRes
import com.example.dukkanapp.core.utils.extension.isValidEmail
import com.example.dukkanapp.core.utils.extension.isValidPassword

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val signUpSuccess: Boolean = false
) {
    val isValidEmail: Boolean get() = emailError == null && email.isValidEmail()
    val isValidPassword: Boolean get() = passwordError == null && password.isValidPassword()
    val isValidConfirmPassword: Boolean get() = confirmPasswordError == null &&
            confirmPassword.isNotEmpty() && password == confirmPassword
}
```

### Step 6.2: Update SignUpIntent

**File:** `features/auth/presentation/logic/signup/SignUpIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object DismissError : SignUpIntent
}
```

### Step 6.3: Update SignUpEffect

**File:** `features/auth/presentation/logic/signup/SignUpEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.signup

import androidx.annotation.StringRes

sealed interface SignUpEffect {
    data object NavigateToEmailVerification : SignUpEffect
    data class ShowSnackbar(@StringRes val message: Int) : SignUpEffect
}
```

### Step 6.4: Update SignUpViewModel

**File:** `features/auth/presentation/logic/signup/SignUpViewModel.kt`

> Uses `ErrorCodeMapper.toStringRes()` from core to convert `AppException` to `@StringRes`. See [Error Handling Architecture](../core/error_handling_architecture.md).

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
                    // Use ErrorCodeMapper extension function
                    val errorRes = result.exception.toStringRes()
                    _state.update { it.copy(isLoading = false, generalError = errorRes) }
                }
            }
        }
    }
}
```

### Step 6.5: Update SignUpScreen

**File:** `features/auth/presentation/screens/SignUpScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.SignUp.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpEffect
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpViewModel

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
                is SignUpEffect.ShowSnackbar -> {
                    // Handle snackbar if needed
                }
            }
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = { intent -> viewModel.onIntent(intent) },
        onNavigationBack = onNavigationBack,
        onForgetPasswordClick = onForgetPasswordClick,
        onLoginClick = onLoginClick,
    )
}
```

### Step 6.6: Update SignUpScreenContent (Add Error Banner)

**File:** `features/auth/presentation/components/signup/SignUpScreenContent.kt`

Add general error display at the top:

```kotlin
// Add this import
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

// Inside the Column, after AppAuthHeader:

// General error banner
state.generalError?.let { errorRes ->
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = stringResource(errorRes),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(AppDimens.sizeXs),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(AppDimens.sizeXs))
}
```

---

## 7. Email Verification Screen

### Step 7.1: Create EmailVerificationUiState

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationUiState.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import androidx.annotation.StringRes

data class EmailVerificationUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val isCheckingVerification: Boolean = false,
    @StringRes val message: Int? = null,
    val isVerified: Boolean = false
)
```

### Step 7.2: Create EmailVerificationIntent

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationIntent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationIntent {
    data object ResendEmailClicked : EmailVerificationIntent
    data object CheckVerificationClicked : EmailVerificationIntent
    data object DismissMessage : EmailVerificationIntent
    data object OpenEmailApp : EmailVerificationIntent
}
```

### Step 7.3: Create EmailVerificationEffect

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationEffect.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

sealed interface EmailVerificationEffect {
    data object NavigateToHome : EmailVerificationEffect
    data object OpenEmailApp : EmailVerificationEffect
}
```

### Step 7.4: Create EmailVerificationViewModel

**File:** `features/auth/presentation/logic/emailverification/EmailVerificationViewModel.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.logic.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.R
import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.auth.domain.usecase.CheckEmailVerificationUseCase
import com.example.dukkanapp.features.auth.domain.usecase.SendEmailVerificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository,
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase,
    private val checkEmailVerificationUseCase: CheckEmailVerificationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EmailVerificationUiState())
    val state: StateFlow<EmailVerificationUiState> = _state.asStateFlow()

    private val _effect = Channel<EmailVerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadUserEmail()
        startPeriodicVerificationCheck()
    }

    private fun loadUserEmail() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                user?.let {
                    _state.update { state -> state.copy(email = it.email) }
                }
            }
        }
    }

    private fun startPeriodicVerificationCheck() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                checkVerificationStatus(silent = true)
            }
        }
    }

    fun onIntent(intent: EmailVerificationIntent) {
        when (intent) {
            EmailVerificationIntent.ResendEmailClicked -> resendVerificationEmail()
            EmailVerificationIntent.CheckVerificationClicked -> checkVerificationStatus(silent = false)
            EmailVerificationIntent.DismissMessage -> dismissMessage()
            EmailVerificationIntent.OpenEmailApp -> openEmailApp()
        }
    }

    private fun resendVerificationEmail() {
        viewModelScope.launch {
            _state.update { it.copy(isResending = true, message = null) }

            when (val result = sendEmailVerificationUseCase()) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isResending = false,
                            message = R.string.email_verification_resent_success
                        )
                    }
                }
                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            isResending = false,
                            message = R.string.email_verification_resent_error
                        )
                    }
                }
            }
        }
    }

    private fun checkVerificationStatus(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) {
                _state.update { it.copy(isCheckingVerification = true, message = null) }
            }

            when (val result = checkEmailVerificationUseCase()) {
                is AppResult.Success -> {
                    if (result.data) {
                        _state.update { it.copy(isVerified = true, isCheckingVerification = false) }
                        _effect.send(EmailVerificationEffect.NavigateToHome)
                    } else if (!silent) {
                        _state.update {
                            it.copy(
                                isCheckingVerification = false,
                                message = R.string.email_verification_not_verified_yet
                            )
                        }
                    }
                }
                is AppResult.Error -> {
                    if (!silent) {
                        _state.update {
                            it.copy(
                                isCheckingVerification = false,
                                message = R.string.email_verification_check_error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun openEmailApp() {
        viewModelScope.launch {
            _effect.send(EmailVerificationEffect.OpenEmailApp)
        }
    }
}
```

### Step 7.5: Create EmailVerificationScreenContent

**File:** `features/auth/presentation/components/emailverification/EmailVerificationScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.components.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationIntent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun EmailVerificationScreenContent(
    state: EmailVerificationUiState,
    onIntent: (EmailVerificationIntent) -> Unit,
    onNavigationBack: () -> Unit
) {
    AppScaffold(
        onNavigateBack = onNavigationBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email icon
            Icon(
                imageVector = Iconsax.Linear.Sms,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(AppDimens.sizeMd))

            // Title
            Text(
                text = stringResource(R.string.email_verification_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppDimens.sizeXs))

            // Subtitle with email
            Text(
                text = stringResource(R.string.email_verification_subtitle, state.email),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(AppDimens.sizeMd))

            // Message card (success or error)
            state.message?.let { messageRes ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(messageRes),
                        modifier = Modifier.padding(AppDimens.sizeXs),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(AppDimens.sizeXs))
            }

            // Open Email App button
            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.OpenEmailApp) },
                text = stringResource(R.string.email_verification_open_email_app),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppDimens.sizeXs))

            // Check verification button
            AppPrimaryButton(
                onClick = { onIntent(EmailVerificationIntent.CheckVerificationClicked) },
                text = stringResource(R.string.email_verification_check_status),
                isLoading = state.isCheckingVerification,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(AppDimens.sizeXs))

            // Resend email button
            AppTextButton(
                onClick = { onIntent(EmailVerificationIntent.ResendEmailClicked) },
                text = if (state.isResending) {
                    stringResource(R.string.email_verification_resending)
                } else {
                    stringResource(R.string.email_verification_resend)
                }
            )
        }
    }
}
```

### Step 7.6: Create EmailVerificationScreen

**File:** `features/auth/presentation/screens/EmailVerificationScreen.kt`

```kotlin
package com.example.dukkanapp.features.auth.presentation.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.emailverification.EmailVerificationScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationEffect
import com.example.dukkanapp.features.auth.presentation.logic.emailverification.EmailVerificationViewModel

@Composable
fun EmailVerificationScreen(
    onNavigationBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToHome by rememberUpdatedState(onNavigateToHome)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EmailVerificationEffect.NavigateToHome -> currentOnNavigateToHome()
                EmailVerificationEffect.OpenEmailApp -> {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_EMAIL)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open Email"))
                }
            }
        }
    }

    EmailVerificationScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigationBack = onNavigationBack
    )
}
```

---

## 8. Navigation Setup

### Step 8.1: Add Navigation Route

**File:** Update your navigation routes file

```kotlin
// Add to your Routes sealed class or object
@Serializable
data object EmailVerification : Route
```

### Step 8.2: Add to NavHost

```kotlin
// In your NavHost composable
composable<Routes.EmailVerification> {
    EmailVerificationScreen(
        onNavigationBack = { navController.popBackStack() },
        onNavigateToHome = {
            navController.navigate(Routes.Home) {
                popUpTo(Routes.AuthOptions) { inclusive = true }
            }
        }
    )
}

// Update SignUpScreen navigation
composable<Routes.SignUp> {
    SignUpScreen(
        onNavigationBack = { navController.popBackStack() },
        onNavigateToEmailVerification = { 
            navController.navigate(Routes.EmailVerification) 
        },
        onForgetPasswordClick = { navController.navigate(Routes.ForgotPassword) },
        onLoginClick = { 
            navController.navigate(Routes.Login) {
                popUpTo(Routes.SignUp) { inclusive = true }
            }
        }
    )
}
```

---

## 9. String Resources

### Step 9.1: Add to `values/strings.xml`

```xml
<!-- Email Verification -->
<string name="email_verification_title">Verify your email</string>
<string name="email_verification_subtitle">We\'ve sent a verification link to %1$s</string>
<string name="email_verification_open_email_app">Open Email App</string>
<string name="email_verification_check_status">I\'ve Verified My Email</string>
<string name="email_verification_resend">Resend Verification Email</string>
<string name="email_verification_resending">Sending...</string>
<string name="email_verification_resent_success">Verification email sent successfully!</string>
<string name="email_verification_resent_error">Failed to send verification email. Try again.</string>
<string name="email_verification_not_verified_yet">Email not verified yet. Please check your inbox.</string>
<string name="email_verification_check_error">Could not check verification status. Try again.</string>

<!-- Additional SignUp Errors -->
<string name="signup_error_password_weak">Password is too weak</string>
<string name="signup_error_user_not_found">User not found</string>
<string name="signup_error_wrong_password">Wrong password</string>
<string name="signup_error_email_not_verified">Email not verified</string>
<string name="signup_error_unknown">An unexpected error occurred</string>
```

### Step 9.2: Add to `values-ar/strings.xml`

```xml
<!-- Email Verification -->
<string name="email_verification_title">تأكيد البريد الإلكتروني</string>
<string name="email_verification_subtitle">أرسلنا رابط التأكيد إلى %1$s</string>
<string name="email_verification_open_email_app">فتح تطبيق البريد</string>
<string name="email_verification_check_status">لقد أكدت بريدي الإلكتروني</string>
<string name="email_verification_resend">إعادة إرسال رسالة التأكيد</string>
<string name="email_verification_resending">جارٍ الإرسال...</string>
<string name="email_verification_resent_success">تم إرسال رسالة التأكيد بنجاح!</string>
<string name="email_verification_resent_error">فشل إرسال رسالة التأكيد. حاول مرة أخرى.</string>
<string name="email_verification_not_verified_yet">لم يتم تأكيد البريد الإلكتروني بعد. يرجى التحقق من صندوق الوارد.</string>
<string name="email_verification_check_error">لم نتمكن من التحقق من حالة التأكيد. حاول مرة أخرى.</string>

<!-- Additional SignUp Errors -->
<string name="signup_error_password_weak">كلمة المرور ضعيفة جدًا</string>
<string name="signup_error_user_not_found">المستخدم غير موجود</string>
<string name="signup_error_wrong_password">كلمة المرور خاطئة</string>
<string name="signup_error_email_not_verified">البريد الإلكتروني غير مؤكد</string>
<string name="signup_error_unknown">حدث خطأ غير متوقع</string>
```

### Step 9.3: Add to `values-fr/strings.xml`

```xml
<!-- Email Verification -->
<string name="email_verification_title">Vérifiez votre email</string>
<string name="email_verification_subtitle">Nous avons envoyé un lien de vérification à %1$s</string>
<string name="email_verification_open_email_app">Ouvrir l\'application Email</string>
<string name="email_verification_check_status">J\'ai vérifié mon email</string>
<string name="email_verification_resend">Renvoyer l\'email de vérification</string>
<string name="email_verification_resending">Envoi en cours...</string>
<string name="email_verification_resent_success">Email de vérification envoyé avec succès !</string>
<string name="email_verification_resent_error">Échec de l\'envoi. Réessayez.</string>
<string name="email_verification_not_verified_yet">Email non vérifié. Vérifiez votre boîte de réception.</string>
<string name="email_verification_check_error">Impossible de vérifier le statut. Réessayez.</string>

<!-- Additional SignUp Errors -->
<string name="signup_error_password_weak">Le mot de passe est trop faible</string>
<string name="signup_error_user_not_found">Utilisateur non trouvé</string>
<string name="signup_error_wrong_password">Mot de passe incorrect</string>
<string name="signup_error_email_not_verified">Email non vérifié</string>
<string name="signup_error_unknown">Une erreur inattendue s\'est produite</string>
```

---

## 10. Error Handling

> **Important:** See [Error Handling Architecture](../core/error_handling_architecture.md) for the complete error handling system.

### Error Flow

```
Firebase SDK Error → FirebaseAuthExceptionMapper / FirebaseFirestoreExceptionMapper
                            ↓
                     AppException (in DataSource - thrown)
                            ↓
                     AppResult.Error (in Repository - caught)
                            ↓
                     ErrorCodeMapper.toStringRes() (in ViewModel)
                            ↓
                     @StringRes displayed in UI
```

### Common Firebase Auth Error Codes

| Error Code | AppException | @StringRes |
|------------|--------------|------------|
| `ERROR_INVALID_EMAIL` | `InvalidEmailException` | `error_invalid_email` |
| `ERROR_WEAK_PASSWORD` | `WeakPasswordException` | `error_weak_password` |
| `ERROR_EMAIL_ALREADY_IN_USE` | `EmailAlreadyInUseException` | `error_email_already_in_use` |
| `ERROR_USER_NOT_FOUND` | `UserNotFoundException` | `error_user_not_found` |
| `ERROR_WRONG_PASSWORD` | `WrongPasswordException` | `error_wrong_password` |
| `ERROR_NETWORK_REQUEST_FAILED` | `NetworkException` | `error_network` |
| `ERROR_TOO_MANY_REQUESTS` | `TooManyRequestsException` | `error_too_many_requests` |

---

## Folder Structure Summary

```
core/
├── error/                                (from Error Handling Architecture)
│   ├── AppException.kt
│   ├── AppResult.kt
│   ├── ErrorCode.kt
│   ├── ErrorCodeMapper.kt
│   └── firebase/
│       ├── FirebaseAuthExceptionMapper.kt
│       └── FirebaseFirestoreExceptionMapper.kt
├── di/
│   ├── AuthModule.kt                     (UPDATED)
│   └── FirebaseModule.kt                 (NEW)
└── utils/
    └── extension/
        └── AppResultExtension.kt         (NEW)

features/auth/
├── data/
│   ├── datasource/
│   │   ├── AuthRemoteDataSource.kt       (NEW)
│   │   ├── AuthRemoteDataSourceImpl.kt   (NEW)
│   │   ├── UserRemoteDataSource.kt       (NEW)
│   │   └── UserRemoteDataSourceImpl.kt   (NEW)
│   ├── model/
│   │   └── UserDto.kt                    (NEW)
│   └── repository/
│       └── AuthRepositoryImpl.kt         (UPDATED)
├── domain/
│   ├── model/
│   │   └── User.kt                       (NEW)
│   ├── repository/
│   │   └── AuthRepository.kt             (UPDATED)
│   └── usecase/
│       ├── SignUpUseCase.kt              (NEW)
│       ├── SendEmailVerificationUseCase.kt (NEW)
│       └── CheckEmailVerificationUseCase.kt (NEW)
└── presentation/
    ├── components/
    │   ├── signup/
    │   │   └── SignUpScreenContent.kt    (UPDATED)
    │   └── emailverification/
    │       └── EmailVerificationScreenContent.kt (NEW)
    ├── logic/
    │   ├── signup/
    │   │   ├── SignUpUiState.kt          (UPDATED)
    │   │   ├── SignUpIntent.kt           (UPDATED)
    │   │   ├── SignUpEffect.kt           (UPDATED)
    │   │   └── SignUpViewModel.kt        (UPDATED)
    │   └── emailverification/
    │       ├── EmailVerificationUiState.kt (NEW)
    │       ├── EmailVerificationIntent.kt  (NEW)
    │       ├── EmailVerificationEffect.kt  (NEW)
    │       └── EmailVerificationViewModel.kt (NEW)
    └── screens/
        ├── SignUpScreen.kt               (UPDATED)
        └── EmailVerificationScreen.kt    (NEW)
```

---

## Implementation Checklist

### Core Error Handling (do first)
- [ ] Create `core/error/ErrorCode.kt`
- [ ] Create `core/error/AppException.kt`
- [ ] Create `core/error/AppResult.kt`
- [ ] Create `core/error/ErrorCodeMapper.kt`
- [ ] Create `core/error/firebase/FirebaseAuthExceptionMapper.kt`
- [ ] Create `core/error/firebase/FirebaseFirestoreExceptionMapper.kt`
- [ ] Create `core/utils/extension/AppResultExtension.kt`
- [ ] Add error string resources (EN, AR, FR)

### Firebase Setup
- [ ] Add Firebase dependencies to `libs.versions.toml`
- [ ] Add Google Services plugin to `build.gradle.kts`
- [ ] Download and add `google-services.json`
- [ ] Enable Email/Password auth in Firebase Console
- [ ] Create Firestore database

### Domain Layer
- [ ] Create `User.kt` domain model
- [ ] Update `AuthRepository.kt` interface
- [ ] Create `SignUpUseCase.kt`
- [ ] Create `SendEmailVerificationUseCase.kt`
- [ ] Create `CheckEmailVerificationUseCase.kt`

### Data Layer
- [ ] Create `UserDto.kt` data model
- [ ] Create `AuthRemoteDataSource.kt` interface
- [ ] Create `AuthRemoteDataSourceImpl.kt`
- [ ] Create `UserRemoteDataSource.kt` interface
- [ ] Create `UserRemoteDataSourceImpl.kt`
- [ ] Update `AuthRepositoryImpl.kt`

### DI
- [ ] Create `FirebaseModule.kt`
- [ ] Update `AuthModule.kt` (add data source bindings)

### Presentation Layer
- [ ] Update SignUp MVI components (State, Intent, Effect, ViewModel)
- [ ] Create EmailVerification MVI components
- [ ] Add navigation routes

### Resources
- [ ] Add auth string resources (EN, AR, FR)
- [ ] Add email verification string resources (EN, AR, FR)

### Testing
- [ ] Test signup flow
- [ ] Test email verification
- [ ] Test Firestore user creation
- [ ] Test error handling (invalid email, weak password, etc.)

---

## Testing

### Manual Testing Steps

1. **Sign Up Flow**
   - Enter valid email and password
   - Click "Create Account"
   - Verify redirect to Email Verification screen
   - Check Firestore for new user document

2. **Email Verification**
   - Check inbox for verification email
   - Click link in email
   - Return to app and click "I've Verified"
   - Verify redirect to Home screen

3. **Error Handling**
   - Try signing up with existing email
   - Try signing up with weak password
   - Try signing up with invalid email format
   - Test without internet connection
