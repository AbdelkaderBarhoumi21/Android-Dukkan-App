package com.example.dukkanapp.features.language.presentation.logic

sealed interface LanguageSelectionIntent {
    data class QueryChanged(val query: String) : LanguageSelectionIntent
    data class LanguageSelected(val code: String) : LanguageSelectionIntent
    data object ContinueClicked : LanguageSelectionIntent
}