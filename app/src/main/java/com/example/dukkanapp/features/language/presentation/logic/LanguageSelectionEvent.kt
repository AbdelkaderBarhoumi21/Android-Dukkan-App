package com.example.dukkanapp.features.language.presentation.logic

sealed interface LanguageSelectionEvent {
    data class OnQueryChanged(val query: String) : LanguageSelectionEvent
    data class OnLanguageSelected(val code: String) : LanguageSelectionEvent
}