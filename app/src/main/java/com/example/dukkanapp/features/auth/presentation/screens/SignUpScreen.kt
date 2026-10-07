package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold

@Composable
fun SignUpScreen(
    onNavigationBack: () -> Unit,
) {

    AppScaffold(
        onNavigateBack = onNavigationBack
    ) { }
}