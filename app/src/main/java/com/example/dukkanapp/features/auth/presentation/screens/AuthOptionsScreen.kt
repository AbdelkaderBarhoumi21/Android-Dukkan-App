package com.example.dukkanapp.features.auth.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.example.dukkanapp.core.utils.constants.AppLinks
import com.example.dukkanapp.features.auth.presentation.components.authoptions.AuthOptionsScreenContent

@Composable
fun AuthOptionsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit

) {
    val uriHandler = LocalUriHandler.current
    AuthOptionsScreenContent(
        onGoogleClick = { /* TODO: Google sign-in (auth guide §15) */ },
        onFacebookClick = { /* TODO: Facebook sign-in */ },
        onEmailClick = onNavigateToSignUp,
        onAlreadyHaveAccountClick = onNavigateToLogin,
        onTermsClick = { uriHandler.openUri(AppLinks.TERMS) },
        onPrivacyClick = { uriHandler.openUri(AppLinks.PRIVACY) },
    )
}