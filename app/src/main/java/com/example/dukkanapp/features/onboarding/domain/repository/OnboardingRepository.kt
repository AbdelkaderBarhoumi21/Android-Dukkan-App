package com.example.dukkanapp.features.onboarding.domain.repository

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel

interface OnboardingRepository {
    fun getPages(): List<OnboardingPageModel>
}