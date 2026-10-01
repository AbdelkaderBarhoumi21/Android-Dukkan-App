package com.example.dukkanapp.features.auth.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.texts.AppAlreadyHaveAccount
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun LoginScreenContent(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit
) {
    AppScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            LoginBrandHeader(
                logo = painterResource(R.drawable.ic_app_logo),
                title = stringResource(R.string.login_title),
                subTitle = stringResource(R.string.login_subtitle)
            )
            Spacer(Modifier.height(AppDimens.size2Xl))
            LoginAuthOptions(
                onGoogleClick = onGoogleClick,
                onAppleClick = onAppleClick,
                onEmailClick = onEmailClick,
            )
            Spacer(Modifier.height(AppDimens.size2Xs))
            LoginOrDivider()
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppAlreadyHaveAccount(
                onActionClick = onAlreadyHaveAccountClick,
            )
        }

    }
}