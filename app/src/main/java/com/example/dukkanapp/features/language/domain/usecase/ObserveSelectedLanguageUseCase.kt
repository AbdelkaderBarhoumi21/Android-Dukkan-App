package com.example.dukkanapp.features.language.domain.usecase

import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import javax.inject.Inject


class ObserveSelectedLanguageUseCase @Inject constructor(
    private val repository: LanguageRepository
) {
    operator fun invoke() = repository.selectedLanguageCode
}