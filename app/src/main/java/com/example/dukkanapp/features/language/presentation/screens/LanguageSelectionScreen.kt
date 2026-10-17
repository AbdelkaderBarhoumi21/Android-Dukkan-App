package com.example.dukkanapp.features.language.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.appCollectEffect
import com.example.dukkanapp.features.language.presentation.components.LanguageSelectionScreenContent
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionEffect
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionViewModel

@Composable
fun LanguageSelectionScreen(
    onNavigationToOnboarding: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    appCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is LanguageSelectionEffect.NavigateToOnboarding -> onNavigationToOnboarding()
        }
    }
    LanguageSelectionScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}