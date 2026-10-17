package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingIntent {
    /** "Next" on pages 1..n-1, "Get Started" on the last page. */
    data object PrimaryActionClick : OnboardingIntent
    data object LoginClick : OnboardingIntent
}