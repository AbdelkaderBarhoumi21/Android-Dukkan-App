package com.example.dukkanapp.features.auth.presentation.components.signup

import AppMessageBanner
import AppPasswordTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.common.components.display.AppProgressBar
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.scaffold.AppScreenBackground
import com.example.dukkanapp.core.common.components.selection.AppCheckbox
import com.example.dukkanapp.core.common.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.common.components.textfield.AppPasswordStrength
import com.example.dukkanapp.core.common.components.textfield.AppTextField
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthFooterLink
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.components.common.legalText
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpIntent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpUiState

private const val SIGN_UP_STEP = 1
private const val SIGN_UP_TOTAL_STEPS = 2

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    AppScaffold(
        background = AppScreenBackground.Plain,
        topBarActions = {
            AppProgressBar(
                progress = SIGN_UP_STEP.toFloat() / SIGN_UP_TOTAL_STEPS,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(R.string.signup_step, SIGN_UP_STEP, SIGN_UP_TOTAL_STEPS),
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
        }
    ) { padding ->
        AuthFormColumn(
            padding = padding
        ) {
            Spacer(
                Modifier.height(AppDimens.space24)
            )
            AppAuthHeader(
                title = stringResource(R.string.signup_title),
                subtitle = stringResource(R.string.signup_subtitle),
                logo = AuthHeaderLogo.Tile(icon = AppIcons.UserPlus)
            )
            Spacer(
                Modifier.height(AppDimens.space24)
            )
            state.generalError?.let { error ->
                AppMessageBanner(
                    message = error.asString()
                )
                Spacer(
                    Modifier.height(AppDimens.space16)
                )

            }

            AppTextField(
                value = state.fullName,
                onValueChange = { onIntent(SignUpIntent.FullNameChanged(it)) },
                label = stringResource(R.string.label_full_name),
                placeholder = stringResource(R.string.placeholder_full_name),
                leadingIcon = painterResource(AppIcons.UserRound),
                helperText = state.fullNameError?.asString(),
                status = if (state.fullNameError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppTextField(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = painterResource(AppIcons.Mail),
                helperText = state.emailError?.asString()
                    ?: if (state.isValidEmail) stringResource(R.string.helper_email_ok) else null,
                status = when {
                    state.emailError != null -> AppFieldStatus.Error
                    state.isValidEmail -> AppFieldStatus.Success
                    else -> AppFieldStatus.Default
                },
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppPasswordTextField(
                value = state.password,
                onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                label = stringResource(R.string.label_password),
                placeholder = stringResource(R.string.auth_password_placeholder),
                helperText = state.passwordError?.asString(),
                status = if (state.passwordError != null) AppFieldStatus.Error else AppFieldStatus.Default,
                enabled = !state.isLoading,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            if (state.passwordError == null) {
                AppPasswordStrength(
                    password = state.password,
                    modifier = Modifier.padding(top = AppDimens.space12)
                )
            }
            Spacer(Modifier.height(AppDimens.space16))
            AppCheckbox(
                checked = state.acceptedTerms,
                onCheckedChange = { onIntent(SignUpIntent.AcceptTermsChanged(it)) },
                label = legalText(
                    templateRes = R.string.signup_accept_terms,
                    onTermsClick = { onIntent(SignUpIntent.TermsClicked) },
                    onPrivacyClick = { onIntent(SignUpIntent.PrivacyClicked) },
                ),
            )
            state.termsError?.let { error ->
                Text(
                    text = error.asString(),
                    style = AppTheme.textStyles.helper,
                    color = AppTheme.colors.error,
                    modifier = Modifier.padding(start = AppDimens.space32),
                )
            }
            AppCheckbox(
                checked = state.marketingOptIn,
                onCheckedChange = { onIntent(SignUpIntent.MarketingOptInChanged(it)) },
                label = stringResource(R.string.signup_marketing_opt_in),
            )
            Spacer(Modifier.height(AppDimens.space16))
            AppButton(
                text = stringResource(R.string.signup_create_account),
                onClick = {
                    focusManager.clearFocus()
                    onIntent(SignUpIntent.SignUpClicked)
                },
                trailingIcon = painterResource(AppIcons.ArrowRight),
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(
                Modifier
                    .weight(1f)
                    .heightIn(min = AppDimens.space24)
            )

            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = { onIntent(SignUpIntent.LoginClicked) },
            )

        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SignUpPreview() {
    AppTheme {
        SignUpScreenContent(
            state = SignUpUiState(
                fullName = "Amira Ben Salah",
                email = "amira.bensalah@gmail.com",
                password = "Dukkan#2026",
                acceptedTerms = true,
            ),
            onIntent = {},
        )
    }
}