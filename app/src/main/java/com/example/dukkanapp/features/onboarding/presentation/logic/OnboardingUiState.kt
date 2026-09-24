package com.example.dukkanapp.features.onboarding.presentation.logic

import com.example.dukkanapp.R
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel

data class OnboardingUiState(
    val currentPage: Int,
    val pages: List<OnboardingPageUiModel> = emptyList()
) {
    val isLastPage: Boolean get() = pages.isNotEmpty() && currentPage >= pages.lastIndex
    val primaryActionRes: Int get() = if (isLastPage) R.string.action_get_started else R.string.action_next
}