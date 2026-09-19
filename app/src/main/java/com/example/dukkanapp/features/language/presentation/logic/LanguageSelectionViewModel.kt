package com.example.dukkanapp.features.language.presentation.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.features.language.domain.usecase.GetSupportedLanguagesUseCase
import com.example.dukkanapp.features.language.domain.usecase.ObserveSelectedLanguageUseCase
import com.example.dukkanapp.features.language.domain.usecase.SelectLanguageUseCase
import com.example.dukkanapp.features.language.presentation.model.toUiModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LanguageSelectionViewModel @Inject constructor(
    private val getSupportedLanguages: GetSupportedLanguagesUseCase,
    private val observeSelectedLanguage: ObserveSelectedLanguageUseCase,
    private val selectLanguage: SelectLanguageUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LanguageSelectionUiState(
            languages = getSupportedLanguages().map { it.toUiModel() }
        )
    )
    val state: StateFlow<LanguageSelectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSelectedLanguage().collect { code ->
                _state.update {
                    it.copy(selectedCode = code)
                }
            }
        }
    }

    fun onEvent(event: LanguageSelectionEvent) {
        when (event) {
            is LanguageSelectionEvent.OnQueryChanged -> _state.update { it.copy(query = event.query) }
            is LanguageSelectionEvent.OnLanguageSelected -> _state.update { it.copy(selectedCode = event.code) }
            is LanguageSelectionEvent.OnContinueClicked -> viewModelScope.launch {
                selectLanguage(_state.value.selectedCode)
            }
        }
    }

}