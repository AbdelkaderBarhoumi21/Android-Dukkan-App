package com.example.dukkanapp.features.language.domain.usecase

import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import jakarta.inject.Inject


class ObserveSelectedLanguageUseCase @Inject constructor(
    private val repository: LanguageRepository
) {
    operator fun invoke() = repository.selectedLanguageCode
}