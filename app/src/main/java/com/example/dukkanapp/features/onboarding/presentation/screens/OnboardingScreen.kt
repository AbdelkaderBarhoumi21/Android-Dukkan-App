package com.example.dukkanapp.features.onboarding.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.onboarding.presentation.components.OnboardingScreenContent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreenContent(
        state = state,
        onEvent = { event -> viewModel.onEvent(event) }, // or use Method reference viewModel::onEvent
        onGetStarted = onGetStarted,
        onLogin = onLogin
    )
}