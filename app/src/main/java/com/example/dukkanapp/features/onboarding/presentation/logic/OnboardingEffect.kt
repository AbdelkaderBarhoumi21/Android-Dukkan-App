package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingEffect {
    data object NavigateToAuthOptions : OnboardingEffect
    data object NavigateToLogin : OnboardingEffect
}