package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.features.auth.presentation.components.forgetpassword.ForgetPasswordScreenContent

@Composable
fun ForgetPasswordScreen(
    onNavigateBack: () -> Unit
) {
    ForgetPasswordScreenContent(onNavigateBack = onNavigateBack)
}