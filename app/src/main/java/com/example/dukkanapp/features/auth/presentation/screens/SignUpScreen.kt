package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.features.auth.presentation.components.signup.SignUpScreenContent

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
) {

    SignUpScreenContent(
        onNavigationBack = onNavigationBack
    )
}