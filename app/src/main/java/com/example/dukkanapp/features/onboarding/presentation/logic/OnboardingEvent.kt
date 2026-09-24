package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingEvent {
    data class OnPageChanged(val page: Int) : OnboardingEvent
    object OnNextClicked : OnboardingEvent
}
