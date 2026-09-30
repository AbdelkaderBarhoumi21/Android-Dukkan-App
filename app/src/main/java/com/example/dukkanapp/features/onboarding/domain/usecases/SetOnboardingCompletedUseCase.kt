package com.example.dukkanapp.features.onboarding.domain.usecases

import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import javax.inject.Inject

class SetOnboardingCompletedUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke() = onboardingRepository.setOnboardingCompleted()
}