package com.example.dukkanapp.features.language.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguagePreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val languageKey = stringPreferencesKey("selected_language_code")
    val savedLanguageCode: Flow<String?> = dataStore.data.map { prefs -> prefs[languageKey] }
    suspend fun saveLanguageCode(code: String) {
        dataStore.edit { it[languageKey] = code }
    }
}