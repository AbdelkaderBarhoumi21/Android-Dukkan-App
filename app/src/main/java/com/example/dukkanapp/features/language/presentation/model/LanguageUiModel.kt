package com.example.dukkanapp.features.language.presentation.model

import androidx.annotation.DrawableRes
import com.example.dukkanapp.R
import com.example.dukkanapp.features.language.domain.model.LanguageModel

data class LanguageUiModel(
    val code: String,
    val displayName: String,
    @get:DrawableRes val flagRes: Int
)

fun LanguageModel.toUiModel(): LanguageUiModel = LanguageUiModel(
    code = code,
    displayName = displayName,
    flagRes = when (code) {
        "ar" -> R.drawable.ic_flag_ar
        "fr" -> R.drawable.ic_flag_fr
        else -> R.drawable.ic_flag_en // Default fallback
    }
)