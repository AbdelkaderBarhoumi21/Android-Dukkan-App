package com.example.dukkanapp.features.settings.data.datasources.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dukkanapp.core.error.guard.guardLocalException
import com.example.dukkanapp.core.utils.constants.AppPreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemePreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** The saved enum name, or null when the user never chose. */
    val savedThemeMode: Flow<String?> =
        dataStore.data.map { prefs -> prefs[AppPreferencesKeys.THEME_MODE] }.guardLocalException()

    suspend fun saveThemeMode(themeName: String) {
        guardLocalException {
            dataStore.edit { prefs ->
                prefs[AppPreferencesKeys.THEME_MODE] = themeName
            }
        }
    }
}