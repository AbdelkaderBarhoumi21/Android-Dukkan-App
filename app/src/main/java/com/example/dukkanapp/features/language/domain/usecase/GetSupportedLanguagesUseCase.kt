package com.example.dukkanapp.features.language.domain.usecase

import com.example.dukkanapp.features.language.domain.model.LanguageModel
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import javax.inject.Inject

class GetSupportedLanguagesUseCase @Inject constructor(
    private val repository: LanguageRepository
) {
    operator fun invoke(): List<LanguageModel> = repository.getSupportedLanguages()
}