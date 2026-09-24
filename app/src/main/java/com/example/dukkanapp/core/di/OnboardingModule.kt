package com.example.dukkanapp.core.di

import com.example.dukkanapp.features.onboarding.data.repository.OnboardingRepositoryImpl
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Singleton
    @Binds
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl,
    ): OnboardingRepository
}