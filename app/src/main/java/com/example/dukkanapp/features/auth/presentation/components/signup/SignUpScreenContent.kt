package com.example.dukkanapp.features.auth.presentation.components.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap

@Composable
fun SignUpScreenContent(
    onNavigationBack: () -> Unit
) {
    AppScaffold(
        onNavigateBack = onNavigationBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            AppAuthHeader(
                title = stringResource(R.string.signup_title),
                subTitle = stringResource(R.string.signup_subtitle),

                )
            Spacer(Modifier.height(AppDimens.sizeMd))

        }


    }
}