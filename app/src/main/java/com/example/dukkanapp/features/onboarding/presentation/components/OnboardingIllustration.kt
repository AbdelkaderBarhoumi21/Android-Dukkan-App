package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel

@Composable
fun OnboardingIllustration(
    page: OnboardingPageUiModel,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(page.imageRes),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimens.spaceLg),
        contentScale = ContentScale.Fit
    )
}