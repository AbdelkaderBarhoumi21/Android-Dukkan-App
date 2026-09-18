package com.example.dukkanapp.features.language.domain


/** Pure data — no Android types. `code` is the BCP-47 tag ("en"/"fr"/"ar"),
 *  `displayName` is the autonym (e.g. "Français"), from our earlier conversation
 *  on why language names aren't translated strings. */
data class Language(
    val code: String,
    val displayName: String,
)