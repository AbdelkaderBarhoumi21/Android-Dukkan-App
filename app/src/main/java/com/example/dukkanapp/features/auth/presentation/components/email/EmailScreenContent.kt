package com.example.dukkanapp.features.auth.presentation.components.email

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.features.auth.presentation.components.authoptions.LoginBrandHeader

@Composable
fun EmailScreenContent(
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
) {
    LoginBrandHeader(
        title = stringResource(R.string.email_login_title),
        subTitle = stringResource(R.string.email_login_subtitle),
        logo = painterResource(R.drawable.ic_app_logo)
    )

}