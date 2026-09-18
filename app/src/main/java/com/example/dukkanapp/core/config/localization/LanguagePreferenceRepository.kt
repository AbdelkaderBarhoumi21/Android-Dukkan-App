package com.example.dukkanapp.core.config.localization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val localeManager: LocaleManager
) {
    private val languageKey= stringPreferencesKey("selected_language_code")
    val selectedLanguageCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[languageKey] ?: localeManager.currentLocaleTag() ?: "en"
    }

    suspend fun selectLanguage(code: String){
        dataStore.edit { it[languageKey] = code }
        localeManager.setAppLocal(code)
    }

}