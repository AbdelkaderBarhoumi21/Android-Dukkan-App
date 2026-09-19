package com.example.dukkanapp.features.language.domain.usecase

import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import javax.inject.Inject

class SelectLanguageUseCase @Inject constructor(
    private val repository: LanguageRepository
) {
    suspend operator fun invoke(code: String) = repository.selectLanguage(code)
}