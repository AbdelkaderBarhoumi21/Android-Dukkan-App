package com.example.dukkanapp.core.navigation.startup

import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppRoute = coroutineScope {
        // Run reads concurrently for maximum performance during app startup
        val hasLanguageDeferred = async { languageRepository.hasSelectedLanguage() }
        val hasOnboardingDeferred = async { onboardingRepository.hasSeenOnboarding() }
        val isLoggedInDeferred = async { authRepository.isLoggedIn() }

        when {
            !hasLanguageDeferred.await() -> AppRoute.LanguageSelectionScreen
            !hasOnboardingDeferred.await() -> AppRoute.OnboardingScreen
            !isLoggedInDeferred.await() -> AppRoute.LoginScreen
            else -> AppRoute.HomeScreen
        }
    }


}