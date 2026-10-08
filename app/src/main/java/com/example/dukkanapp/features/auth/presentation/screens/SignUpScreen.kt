package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.auth.presentation.components.SignUp.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpEffect
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpViewModel

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToHome by rememberUpdatedState(onNavigateToHome)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SignUpEffect.NavigateToHome -> currentOnNavigateToHome()
            }
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = { intent -> viewModel.onIntent(intent) },
        onNavigationBack = onNavigationBack,
        onForgetPasswordClick = onForgetPasswordClick,
        onLogin = onLogin,
    )
}