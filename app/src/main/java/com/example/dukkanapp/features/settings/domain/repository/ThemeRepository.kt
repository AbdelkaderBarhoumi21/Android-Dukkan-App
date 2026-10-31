package com.example.dukkanapp.features.settings.domain.repository

import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}