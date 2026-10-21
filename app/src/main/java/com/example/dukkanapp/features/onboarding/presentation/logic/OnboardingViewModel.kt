package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.common.mvi.AppViewModel
import com.example.dukkanapp.features.onboarding.domain.usecases.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.domain.usecases.SetOnboardingCompletedUseCase
import com.example.dukkanapp.features.onboarding.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    getOnboardingPages: GetOnboardingPagesUseCase,
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
) : AppViewModel<OnboardingUiState, OnboardingIntent, OnboardingEffect>(
    initialState = OnboardingUiState(pages = getOnboardingPages().map { it.toUiModel() })
) {
    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.PageChanged -> onPageChanged(intent.page)
            OnboardingIntent.PrimaryActionClicked -> onPrimaryActionClicked()
            OnboardingIntent.LoginClicked -> finish(OnboardingEffect.NavigateToLogin)
        }
    }

    private fun onPageChanged(page: Int) {
        if (currentState.pages.isNotEmpty()) return // coerceIn(0, -1) would throw
        setState { copy(currentPage = page.coerceIn(0, pages.lastIndex)) }
    }

    private fun onPrimaryActionClicked() {
        if (currentState.isLastPage) finish(OnboardingEffect.NavigateToAuthOptions)
        else setState { copy(currentPage = currentPage + 1) }
    }

    /** Save first, navigate after: the "seen" flag is on disk before the screen leaves. */
    private fun finish(destination: OnboardingEffect) {
        if (currentState.isFinishing) return
        setState { copy(isFinishing = true) }
        viewModelScope.launch {
            setOnboardingCompletedUseCase()
            sendEffect(destination)

        }
    }
}