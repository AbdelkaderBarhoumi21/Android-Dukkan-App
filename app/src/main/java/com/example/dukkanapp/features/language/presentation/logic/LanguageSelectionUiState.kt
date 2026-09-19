package com.example.dukkanapp.features.language.presentation.logic

import com.example.dukkanapp.features.language.presentation.model.LanguageUiModel

data class LanguageSelectionUiState(
    val query: String = "",
    val selectedCode: String = "en",
    val languages: List<LanguageUiModel> = emptyList()
) {
    val filteredLanguages: List<LanguageUiModel>
        get() = if (query.isBlank()) languages
        else languages.filter { it.displayName.contains(query, ignoreCase = true) }

    val selectedLanguage: LanguageUiModel? get() = languages.firstOrNull { it.code == selectedCode }
}