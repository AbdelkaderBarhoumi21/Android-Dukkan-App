package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.common.mvi.AppCollectEffect
import com.example.dukkanapp.features.auth.presentation.components.signup.SignUpScreenContent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpEffect
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpViewModel

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
    onNavigateToEmailVerification: (emailJustSent: Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AppCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is SignUpEffect.NavigateToEmailVerification -> onNavigateToEmailVerification(effect.emailJustSent)
            is SignUpEffect.OpenUrl -> uriHandler.openUri(effect.url)
            SignUpEffect.NavigateToLogin -> onNavigateToLogin()
            SignUpEffect.NavigateBack -> onNavigationBack()
        }
    }

    SignUpScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}