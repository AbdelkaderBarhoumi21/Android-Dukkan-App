package com.example.dukkanapp.features.auth.presentation.components.email

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
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.auth.presentation.components.authoptions.LoginBrandHeader

@Composable
fun EmailScreenContent(
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onNavigationBack: () -> Unit,
) {

    AppScaffold(
        onNavigateBack = onNavigationBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            LoginBrandHeader(
                title = stringResource(R.string.email_login_title),
                subTitle = stringResource(R.string.email_login_subtitle),
                logo = painterResource(R.drawable.ic_app_logo)
            )
            Spacer(Modifier.height(AppDimens.sizeMd))


        }
    }


}