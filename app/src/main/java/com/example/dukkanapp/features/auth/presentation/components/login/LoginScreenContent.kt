package com.example.dukkanapp.features.auth.presentation.components.login

import AppMessageBanner
import AppPasswordTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.common.components.buttons.AppSocialButton
import com.example.dukkanapp.core.common.components.footer.AppAuthFooterLink
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.scaffold.AppScreenBackground
import com.example.dukkanapp.core.common.components.selection.AppCheckbox
import com.example.dukkanapp.core.common.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.common.components.textfield.AppTextField
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.components.common.AuthOrDivider
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

            AppPasswordTextField(
                value = state.password,
                onValueChange = {
                    onIntent(LoginIntent.PasswordChanged(it))
                },
                label = stringResource(R.string.label_password),
                placeholder = stringResource(R.string.auth_password_placeholder),
                helperText = state.passwordError?.asString(),
                status = if (state.passwordError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onIntent(LoginIntent.LoginClicked)
                })
            )
            Spacer(Modifier.height(AppDimens.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppCheckbox(
                    checked = state.rememberMe,
                    onCheckedChange = { onIntent(LoginIntent.RememberMeChanged(it)) },
                    label = stringResource(R.string.login_remember_me)
                )

                AppButton(
                    text = stringResource(R.string.login_forgot_password),
                    onClick = { onIntent(LoginIntent.ForgotPasswordClicked) },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Small
                )
            }
            Spacer(Modifier.height(AppDimens.space12))
            AppButton(
                text = stringResource(R.string.action_log_in),
                onClick = {
                    focusManager.clearFocus()
                    onIntent(LoginIntent.LoginClicked)
                },
                trailingIcon = painterResource(AppIcons.ArrowRight),
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppDimens.space24))
            AuthOrDivider(
                text = stringResource(R.string.auth_or_continue_with)
            )
            Spacer(Modifier.height(AppDimens.space16))
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)
            ) {
                AppSocialButton(
                    text = stringResource(R.string.auth_google),
                    logo = painterResource(AppIcons.Google),
                    onClick = {
                        onIntent(LoginIntent.GoogleClicked)
                    }, modifier = Modifier.weight(1f)
                )
                AppSocialButton(
                    text = stringResource(R.string.auth_facebook),
                    logo = painterResource(AppIcons.Facebook),
                    onClick = {
                        onIntent(LoginIntent.FacebookClicked)
                    }, modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                Modifier
                    .weight(1f)
                    .heightIn(min = AppDimens.space24)
            )

            AppAuthFooterLink(
                promptText = stringResource(R.string.login_no_account),
                actionText = stringResource(R.string.login_sign_up),
                onActionClick = {
                    onIntent(LoginIntent.SignUpClicked)
                }
            )

        }
    }
}

@Preview(showBackground = false, widthDp = 399, heightDp = 852)
@Composable
private fun LoginScreenPreview(

) {
    AppTheme {
        LoginScreenContent(state = LoginUiState(email = "amira.bensalah@gmail.com"), onIntent = {})
    }
}