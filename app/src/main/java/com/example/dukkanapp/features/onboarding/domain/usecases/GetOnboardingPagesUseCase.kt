package com.example.dukkanapp.features.onboarding.domain.usecases

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import javax.inject.Inject

class GetOnboardingPagesUseCase @Inject constructor(
    private val repository: OnboardingRepository
) {
    operator fun invoke(): List<OnboardingPageModel> = repository.getPages()
}