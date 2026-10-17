package com.example.dukkanapp.features.language.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.features.language.domain.usecase.GetSupportedLanguagesUseCase
import com.example.dukkanapp.features.language.domain.usecase.ObserveSelectedLanguageUseCase
import com.example.dukkanapp.features.language.domain.usecase.SelectLanguageUseCase
import com.example.dukkanapp.features.language.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    getSupportedLanguagesUseCase: GetSupportedLanguagesUseCase,
    private val observeSelectedLanguage: ObserveSelectedLanguageUseCase,
    private val selectLanguage: SelectLanguageUseCase

) :
    AppViewModel<LanguageSelectionUiState, LanguageSelectionIntent, LanguageSelectionEffect>(
        initialState = LanguageSelectionUiState(languages = getSupportedLanguagesUseCase().map { it.toUiModel() })
    ) {
    override fun onIntent(intent: LanguageSelectionIntent) {
        when (intent) {
            is LanguageSelectionIntent.QueryChanged -> setState { copy(query = intent.query) }
            is LanguageSelectionIntent.LanguageSelected -> onLanguageSelected(intent.code)
            is LanguageSelectionIntent.ContinueClicked -> onContinueClicked()
        }
    }

    private fun onLanguageSelected(code: String) {
        if (code == currentState.selectedCode) return
        setState { copy(selectedCode = code) }
        viewModelScope.launch {
            selectLanguage(code)
        }
    }

    private fun onContinueClicked() {
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            selectLanguage(currentState.selectedCode)
            setState { copy(isSaving = false) }
            sendEffect(LanguageSelectionEffect.NavigateToOnboarding)
        }
    }
}