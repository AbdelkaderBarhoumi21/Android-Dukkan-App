package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.resetpassword.ResetPasswordScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.resetpassword.ResetPasswordEffect
import com.example.dukkanapp.features.auth.presentation.logic.resetpassword.ResetPasswordViewModel

@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ResetPasswordEffect.NavigateToLogin -> onNavigateToLogin()
            ResetPasswordEffect.NavigateBack -> onNavigateBack()
        }
    }
    ResetPasswordScreenContent(
        onIntent = viewModel::onIntent,
        state = state
    )
}