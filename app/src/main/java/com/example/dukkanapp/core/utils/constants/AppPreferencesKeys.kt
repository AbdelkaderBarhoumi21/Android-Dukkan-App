package com.example.dukkanapp.core.utils.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferencesKeys {
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
}