package com.example.dukkanapp.features.settings.di

import com.example.dukkanapp.features.settings.data.repository.ThemeRepositoryImpl
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}