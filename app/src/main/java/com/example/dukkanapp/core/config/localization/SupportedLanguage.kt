package com.example.dukkanapp.core.config.localization

data class SupportedLanguage(
    val code: String,       // BCP-47 tag, e.g. "en", "fr", "ar"
    val displayName: String,
    val flagAssetName: String, // maps to a drawable, see §6
)

val SupportedLanguages = listOf(
    SupportedLanguage("ar", "Arabic", "flag_ar"),
    SupportedLanguage("en", "English", "flag_en"),
    SupportedLanguage("fr", "French", "flag_fr"),
)