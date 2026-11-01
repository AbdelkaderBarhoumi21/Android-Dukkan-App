package com.example.dukkanapp.features.settings.domain.usecase

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.usecase.UseCase
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repo: ThemeRepository
) : UseCase<ThemeMode, Unit> {
    override suspend fun invoke(params: ThemeMode): AppResult<Unit> =
        repo.setThemeMode(params)

}