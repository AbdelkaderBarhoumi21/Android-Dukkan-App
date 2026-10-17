package com.example.dukkanapp.features.language.presentation.logic

sealed interface LanguageSelectionEffect {
    data object NavigateToOnboarding : LanguageSelectionEffect
}