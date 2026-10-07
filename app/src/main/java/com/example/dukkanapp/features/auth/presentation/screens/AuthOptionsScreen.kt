package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.features.auth.presentation.components.authoptions.AuthOptionsScreenContent

@Composable
fun AuthOptionsScreen(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit,

    ) {
    AuthOptionsScreenContent(
        onGoogleClick = onGoogleClick,
        onAppleClick = onAppleClick,
        onEmailClick = onEmailClick,
        onAlreadyHaveAccountClick = onAlreadyHaveAccountClick
    )
}