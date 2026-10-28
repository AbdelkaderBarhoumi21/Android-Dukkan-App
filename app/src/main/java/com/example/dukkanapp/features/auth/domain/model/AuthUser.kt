package com.example.dukkanapp.features.auth.domain.model

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photUrl: String?,
    val isEmailVerified: Boolean,
    /** how the user signs in : "password" "google" "apple"*/
    val providerIds: List<String>,
)