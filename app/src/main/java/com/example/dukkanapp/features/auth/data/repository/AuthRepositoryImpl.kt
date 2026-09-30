package com.example.dukkanapp.features.auth.data.repository

import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override suspend fun isLoggedIn(): Boolean {
        // TODO: Implement actual authentication check
        // For now, defaulting to false so the user can see the login screen
        return false
    }
}
