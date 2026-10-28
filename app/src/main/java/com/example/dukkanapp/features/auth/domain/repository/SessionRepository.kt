package com.example.dukkanapp.features.auth.domain.repository

/**
 * Local session policy
 * Firebase keeps the session
 * this decides whether we keep it
 */
interface SessionRepository {
    suspend fun SetRememberMe(enabled: Boolean)
    suspend fun isRememberMeEnabled(): Boolean
    suspend fun clear()
}