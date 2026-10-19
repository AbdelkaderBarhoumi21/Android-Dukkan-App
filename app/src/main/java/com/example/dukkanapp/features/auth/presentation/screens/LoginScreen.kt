package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.login.LoginScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.Login.LoginEffect
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToForgetPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    AppCollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is LoginEffect.NavigateToHome -> onNavigateToHome()
            is LoginEffect.NavigateToForgotPassword -> onNavigateToForgetPassword()
            is LoginEffect.NavigateToSignUp -> onNavigateToSignUp()
            is LoginEffect.NavigateBack -> onNavigateBack()
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}