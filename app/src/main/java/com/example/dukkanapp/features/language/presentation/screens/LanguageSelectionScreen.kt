package com.example.dukkanapp.features.language.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.language.presentation.components.LanguageSelectionScreenContent
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionViewModel

@Composable
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LanguageSelectionScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onContinue = onContinue
    )
}