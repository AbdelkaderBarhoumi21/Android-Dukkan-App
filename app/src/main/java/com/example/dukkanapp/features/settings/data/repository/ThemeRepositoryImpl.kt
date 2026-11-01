package com.example.dukkanapp.features.settings.data.repository

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.guard.guardFailure
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.features.settings.data.datasources.local.ThemePreferenceLocalDataSource
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val localDataSource: ThemePreferenceLocalDataSource,
    private val failureMapper: AppFailureMapper,
) : ThemeRepository {
    override val themeMode: Flow<AppResult<ThemeMode>> =
        localDataSource.savedThemeMode.map { saved ->
            ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
        }.guardFailure(failureMapper)

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit> =
        failureMapper.guardFailure {
            localDataSource.saveThemeMode(mode.name)
        }
}