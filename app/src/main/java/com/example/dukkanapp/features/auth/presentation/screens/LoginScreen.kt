package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.features.auth.presentation.components.authoptions.LoginScreenContent

@Composable
fun LoginScreen(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit,

    ) {
    LoginScreenContent(
        onGoogleClick = onGoogleClick,
        onAppleClick = onAppleClick,
        onEmailClick = onEmailClick,
        onAlreadyHaveAccountClick = onAlreadyHaveAccountClick
    )
}