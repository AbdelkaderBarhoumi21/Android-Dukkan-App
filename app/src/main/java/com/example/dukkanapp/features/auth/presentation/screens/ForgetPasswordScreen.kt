package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.forgetpassword.ForgetPasswordScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.forgetpassword.ForgetPasswordEffect
import com.example.dukkanapp.features.auth.presentation.logic.forgetpassword.ForgetPasswordViewModel

@Composable
fun ForgetPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    viewModel: ForgetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppCollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            ForgetPasswordEffect.NavigateToChangePassword -> onNavigateToChangePassword()
            ForgetPasswordEffect.NavigateBack -> onNavigateBack()
        }
    }
    ForgetPasswordScreenContent(
        onIntent = viewModel::onIntent,
        state = state
    )
}