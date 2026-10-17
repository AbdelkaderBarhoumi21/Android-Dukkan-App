package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingIntent {
    data class PageChanged(val page: Int) : OnboardingIntent

    /** "Next" on pages 1..n-1, "Get Started" on the last page. */
    data object PrimaryActionClicked : OnboardingIntent
    data object LoginClicked : OnboardingIntent
}