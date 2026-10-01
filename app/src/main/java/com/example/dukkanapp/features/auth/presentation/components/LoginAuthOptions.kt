package com.example.dukkanapp.features.auth.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AuthButton
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun LoginAuthOptions(
    modifier: Modifier = Modifier,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        AuthButton(
            text = stringResource(R.string.sign_in_with_google),
            icon = painterResource(R.drawable.ic_google),
            containerColor = Color.Transparent,
            borderColor = MaterialTheme.colorScheme.outline,
            onClick = onGoogleClick
        )
        AuthButton(
            text = stringResource(R.string.continue_with_apple),
            icon = painterResource(R.drawable.ic_apple),
            iconColor = MaterialTheme.colorScheme.onSurface,
            containerColor = Color.Transparent,
            borderColor = MaterialTheme.colorScheme.outline,
            onClick = onAppleClick
        )
        AuthButton(
            text = stringResource(R.string.continue_with_email),
            icon = painterResource(R.drawable.ic_email),
            iconColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.surface,
            onClick = onEmailClick
        )

    }
}