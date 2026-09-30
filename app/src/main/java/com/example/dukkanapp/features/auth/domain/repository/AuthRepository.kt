package com.example.dukkanapp.features.auth.domain.repository

interface AuthRepository {
    suspend fun isLoggedIn(): Boolean
}