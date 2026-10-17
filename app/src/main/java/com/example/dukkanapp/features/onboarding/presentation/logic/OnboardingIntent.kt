package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingIntent {
    data object PrimaryActionClick : OnboardingIntent
    data object LoginClick : OnboardingIntent
}