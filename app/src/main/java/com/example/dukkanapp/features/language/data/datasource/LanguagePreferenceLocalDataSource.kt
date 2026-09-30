package com.example.dukkanapp.features.language.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dukkanapp.core.utils.constants.AppPreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguagePreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val savedLanguageCode: Flow<String?> =
        dataStore.data.map { prefs -> prefs[AppPreferencesKeys.SELECTED_LANGUAGE_CODE] }

    suspend fun saveLanguageCode(code: String) {
        dataStore.edit { it[AppPreferencesKeys.SELECTED_LANGUAGE_CODE] = code }
    }
}