package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.EmptyResult
import com.example.dukkanapp.features.auth.domain.model.AuthUser

interface UserRepository {
    /**
     * First sign in creates users/{uid}
     * Later sign-ins refresh only what can change (email,providers,language,lastLoginAt)
     *  created at is never touched
     */
    suspend fun syncProfile(user: AuthUser, languageCode: String): EmptyResult
}