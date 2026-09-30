package com.example.dukkanapp.features.onboarding.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dukkanapp.core.utils.constants.AppPreferencesKeys
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingPreferenceLocalDataSource @Inject constructor(
    private val datastore: DataStore<Preferences>
) {

    val hasSeenOnboarding: Flow<Boolean> =
        datastore.data.map { pref -> pref[AppPreferencesKeys.HAS_SEEN_ONBOARDING] ?: false }

    suspend fun setOnboardingCompleted() {
        datastore.edit { mutPref -> mutPref[AppPreferencesKeys.HAS_SEEN_ONBOARDING] = true }
    }
}