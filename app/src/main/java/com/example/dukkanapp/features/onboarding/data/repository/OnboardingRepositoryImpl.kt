package com.example.dukkanapp.features.onboarding.data.repository

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import jakarta.inject.Inject

class OnboardingRepositoryImpl @Inject constructor() : OnboardingRepository {
    override fun getPages(): List<OnboardingPageModel> = listOf(
        OnboardingPageModel(id = "discover"),
        OnboardingPageModel(id = "checkout"),
        OnboardingPageModel(id = "shopping"),
    )
}