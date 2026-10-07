package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.email.EmailScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.EmailViewModel

@Composable
fun LoginScreen(
    onForgetPasswordClick: () -> Unit,
    onNavigationBack: () -> Unit,
    onSignupClick: () -> Unit,
    viewModel: EmailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EmailScreenContent(
        onLoginClick = { viewModel.onLoginClick() },
        state = state,
        onEmailChanged = { value -> viewModel.onEmailChanged(value) },
        onPasswordChanged = { value -> viewModel.onPasswordChanged(value) },
        onForgetPasswordClick = onForgetPasswordClick,
        onRememberMeChanged = { value -> viewModel.onRememberMeChanged(value) },
        onNavigationBack = onNavigationBack,
        onSignupClick = onSignupClick
    )
}