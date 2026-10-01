package com.example.dukkanapp.features.auth.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold

@Composable
fun LoginScreenContent(
    modifier: Modifier = Modifier,

    ) {
    AppScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LoginBrandHeader(
                logo = painterResource(R.drawable.ic_app_logo),
                title = stringResource(R.string.login_title),
                subTitle = stringResource(R.string.login_subtitle)
            )
        }

    }
}