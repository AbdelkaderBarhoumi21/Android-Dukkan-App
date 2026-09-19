package com.example.dukkanapp.features.language.domain.repository

import com.example.dukkanapp.features.language.domain.model.LanguageModel
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun getSupportedLanguages(): List<LanguageModel>
    val selectedLanguageCode: Flow<String>
    suspend fun selectLanguage(code: String)
}