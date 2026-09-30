package com.example.dukkanapp.features.onboarding.data.repository

import com.example.dukkanapp.features.onboarding.data.datasource.OnboardingPreferenceLocalDataSource
import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class OnboardingRepositoryImpl @Inject constructor(
    private val localDataSource: OnboardingPreferenceLocalDataSource
) : OnboardingRepository {
    override fun getPages(): List<OnboardingPageModel> = listOf(
        OnboardingPageModel(id = "discover"),
        OnboardingPageModel(id = "checkout"),
        OnboardingPageModel(id = "shopping"),
    )

    override suspend fun hasSeenOnboarding(): Boolean {
        return localDataSource.hasSeenOnboarding.first()
    }

    override suspend fun setOnboardingCompleted() {
        localDataSource.setOnboardingCompleted()
    }
}