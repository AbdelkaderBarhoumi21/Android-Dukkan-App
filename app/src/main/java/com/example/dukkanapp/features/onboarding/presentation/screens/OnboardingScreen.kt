package com.example.dukkanapp.features.onboarding.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.onboarding.presentation.components.OnboardingScreenContent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingEffect
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onNavigateToAuthOptions: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppCollectEffect<OnboardingEffect>(viewModel.effect) { effect ->
        when (effect) {
            is OnboardingEffect.NavigateToAuthOptions -> onNavigateToAuthOptions()
            is OnboardingEffect.NavigateToLogin -> onNavigateToLogin()
        }
    }

    OnboardingScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}