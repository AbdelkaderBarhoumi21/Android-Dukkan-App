package com.example.dukkanapp.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoutes {
    @Serializable
    data object LanguageSelection : AppRoutes

    @Serializable
    data object Onboarding : AppRoutes
}