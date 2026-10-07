package com.example.dukkanapp.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object LanguageSelectionScreen : AppRoute

    @Serializable
    data object OnboardingScreen : AppRoute

    @Serializable
    data object AuthOptionsScreen : AppRoute

    @Serializable
    data object HomeScreen : AppRoute

    @Serializable

    data object LoginScreen

    @Serializable
    data object SignUpScreen
}
