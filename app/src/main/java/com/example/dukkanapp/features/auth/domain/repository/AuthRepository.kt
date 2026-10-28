package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser

interface AuthRepository {
    /** Cached on disk by firebase: no network call, safe to read at app start*/
    val currentUser: AuthUser

    suspend fun signUpWithEmail(email: String, password: String): AppResult<AuthUser>
    suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser>

    /** [languageCode] picks the language of Firebase's email template*/
    suspend fun sendEmailVerification(languageCode: String): EmptyResult

    /** Fetches the user from the server: the cached isEmailVerified never changes on its own */
    suspend fun reloadCurrentUser(): AppResult<AuthUser>
    fun signOut()

    // TODO(social-login) : suspend fun signInWithGoogle(idToken: string):AppResult<AuthUser>
    
}