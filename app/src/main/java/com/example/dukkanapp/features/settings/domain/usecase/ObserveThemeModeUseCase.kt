package com.example.dukkanapp.features.settings.domain.usecase

import com.example.dukkanapp.core.usecase.FlowUseCase
import com.example.dukkanapp.core.usecase.NoParams
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import com.example.dukkanapp.features.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val repo: ThemeRepository
) : FlowUseCase<NoParams, ThemeMode> {
    override fun invoke(params: NoParams): Flow<ThemeMode> =
        repo.themeMode

}