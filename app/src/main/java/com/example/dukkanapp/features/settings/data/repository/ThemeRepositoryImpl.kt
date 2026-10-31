package com.example.dukkanapp.features.settings.data.repository

import com.example.dukkanapp.features.settings.data.datasources.local.ThemePreferenceLocalDataSource
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val localDataSource: ThemePreferenceLocalDataSource
) : ThemeRepository {
    override val themeMode: Flow<ThemeMode>
        get() = localDataSource.savedThemeMode.map { saved ->
            ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        localDataSource.saveThemeMode(mode.name)
    }
}