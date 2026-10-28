package com.example.dukkanapp.features.auth.domain.model

sealed interface SessionState {
    data object SignedOut : SessionState
    data object SignedIn : SessionState
    data object EmailNotVerified : SessionState
}