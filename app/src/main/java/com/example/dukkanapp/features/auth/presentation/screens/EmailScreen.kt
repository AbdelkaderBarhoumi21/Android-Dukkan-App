package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.features.auth.presentation.components.email.EmailScreenContent

@Composable
fun EmailScreen(
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
) {
    EmailScreenContent(
        onLoginClick = onLoginClick,
        onGoogleClick = onGoogleClick,
        onAppleClick = onAppleClick,
        onForgetPasswordClick = onForgetPasswordClick
    )
}