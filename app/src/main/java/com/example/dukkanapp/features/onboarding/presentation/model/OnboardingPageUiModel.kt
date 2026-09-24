package com.example.dukkanapp.features.onboarding.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel

data class OnboardingPageUiModel(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    @get:DrawableRes val imageRes: Int
)

fun OnboardingPageModel.toUiModel(): OnboardingPageUiModel = when (id) {
    "discover" -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_discover_title,
        subtitleRes = R.string.onboarding_discover_subtitle,
        imageRes = R.drawable.img_onboarding_discover,
    )

    "checkout" -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_checkout_title,
        subtitleRes = R.string.onboarding_checkout_subtitle,
        imageRes = R.drawable.img_onboarding_checkout,
    )

    else -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_shopping_title,
        subtitleRes = R.string.onboarding_shopping_subtitle,
        imageRes = R.drawable.img_onboarding_shopping,
    )
}