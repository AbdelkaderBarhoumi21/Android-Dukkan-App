package com.example.dukkanapp.config.localization

import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor() {
    fun setAppLocal(languageCode:String){
        val localList= LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localList)
    }

    fun currentLocaleTag(): String? = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
}