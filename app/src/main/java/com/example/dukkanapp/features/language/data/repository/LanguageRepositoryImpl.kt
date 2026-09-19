package com.example.dukkanapp.features.language.data.repository

import com.example.dukkanapp.features.language.data.datasource.LanguagePreferenceLocalDataSource
import com.example.dukkanapp.features.language.data.datasource.PlatformLocaleDataSource
import com.example.dukkanapp.features.language.domain.model.LanguageModel
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
    private val localDataStore: LanguagePreferenceLocalDataSource,
    private val platformLocaleDataSource: PlatformLocaleDataSource

) : LanguageRepository {
    override fun getSupportedLanguages(): List<LanguageModel> = listOf(
        LanguageModel("ar", "العربية"),
        LanguageModel("en", "English"),
        LanguageModel("fr", "Français"),
    )

    override val selectedLanguageCode: Flow<String> =
        localDataStore.savedLanguageCode.map { saved ->
            saved ?: platformLocaleDataSource.currentLocaleTag() ?: "en"
        }

    override suspend fun selectLanguage(code: String) {
        localDataStore.saveLanguageCode(code = code)
        platformLocaleDataSource.setAppLocal(code)
    }


}