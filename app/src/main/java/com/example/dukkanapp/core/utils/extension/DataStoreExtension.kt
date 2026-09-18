package com.example.dukkanapp.core.utils.extension


import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "app_preferences")