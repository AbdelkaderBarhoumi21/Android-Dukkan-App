package com.example.dukkanapp.features.language.data.datasource

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

class PlatformLocaleDataSource @Inject constructor() {
    fun setAppLocal(languageCode: String) {
        val localList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localList)
    }

    fun currentLocaleTag(): String? =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
}