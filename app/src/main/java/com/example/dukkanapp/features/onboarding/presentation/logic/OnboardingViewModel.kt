package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.lifecycle.ViewModel
import com.example.dukkanapp.features.onboarding.domain.usecases.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getOnboardingPages: GetOnboardingPagesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState(
            pages = getOnboardingPages().map { it.toUiModel() },
        )
    )

    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnPageChanged -> {
                _state.update { current ->
                    val safePage = event.page.coerceIn(0, current.pages.lastIndex)
                    if (current.currentPage == safePage) current
                    else current.copy(currentPage = safePage)

                }
            }

            is OnboardingEvent.OnNextClicked -> {
                _state.update { current ->
                    if (current.isLastPage) current
                    else current.copy(currentPage = current.currentPage + 1)
                }
            }

            is OnboardingEvent.OnGetStartedClick -> {}
            is OnboardingEvent.OnLoginClicked -> {}
        }

    }
}