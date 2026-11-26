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

    data object LoginScreen : AppRoute

    @Serializable
    data object SignUpScreen : AppRoute

    @Serializable
    data class EmailVerificationScreen(val emailJustSent: Boolean = false) : AppRoute

    @Serializable
    data object ForgetPasswordScreen : AppRoute

    @Serializable
    data object ResetPasswordScreen : AppRoute
}
