package com.example.dukkanapp.features.auth.presentation.components.login

import AppMessageBanner
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.scaffold.AppScreenBackground
import com.example.dukkanapp.core.common.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.common.components.textfield.AppTextField
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginIntent
import com.example.dukkanapp.features.auth.presentation.logic.login.LoginUiState

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    AppScaffold(
        background = AppScreenBackground.AuroraShort,
        onNavigationBack = {
            onIntent(LoginIntent.BackClicked)
        }
    ) { padding ->
        AuthFormColumn(
            padding = padding
        ) {
            Spacer(Modifier.height(AppDimens.space8))
            AppAuthHeader(
                title = stringResource(R.string.login_title),
                subtitle = stringResource(R.string.login_subtitle),
                logo = AuthHeaderLogo.Mark(size = AppDimens.space32)
            )
            Spacer(Modifier.height(AppDimens.space24))
            state.generalError?.let { error ->
                AppMessageBanner(
                    message = error.asString()
                )
                Spacer(Modifier.height(AppDimens.space16))
            }
            AppTextField(
                value = state.email,
                onValueChange = {
                    onIntent(LoginIntent.EmailChanged(it))
                },
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = painterResource(AppIcons.Mail),
                helperText = state.emailError?.asString(),
                // Login never shows green: no Success status here.
                status = if (state.emailError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                trailingIcon = if (state.email.isNotEmpty()) {
                    {
                        IconButton(onClick = { onIntent(LoginIntent.EmailChanged("")) }) {
                            Icon(
                                painter = painterResource(AppIcons.X),
                                contentDescription = stringResource(R.string.cd_clear_text),
                                tint = AppTheme.colors.textTertiary,
                                modifier = Modifier.size(AppDimens.iconSm),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(AppDimens.space16))
        }
    }
}