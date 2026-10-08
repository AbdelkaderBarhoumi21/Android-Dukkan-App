package com.example.dukkanapp.features.auth.presentation.components.SignUp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.dividers.AppDivider
import com.example.dukkanapp.core.common.components.headers.AppAuthHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.common.components.texts.AppAuthFooterLink
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.constants.AppValidationConstants
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpIntent
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpUiState

import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun SignUpScreenContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    onNavigationBack: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

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

            AppTextFiled(
                value = state.email,
                onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                placeHolder = stringResource(R.string.signup_email_placeholder),
                errorText = state.emailError?.let { stringResource(it) },
                isSuccess = state.isValidEmail,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPasswordTextField(
                value = state.password,
                onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                placeHolder = stringResource(R.string.signup_password_placeholder),
                errorText = state.passwordError?.let {
                    stringResource(it, AppValidationConstants.MIN_PASSWORD_LENGTH)
                },
                isSuccess = state.isValidPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            Spacer(Modifier.height(AppDimens.sizeXs))

            AppPasswordTextField(
                value = state.confirmPassword,
                onValueChange = { onIntent(SignUpIntent.ConfirmPasswordChanged(it)) },
                placeHolder = stringResource(R.string.signup_confirm_password_placeholder),
                errorText = state.confirmPasswordError?.let { stringResource(it) },
                isSuccess = state.isValidConfirmPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onIntent(SignUpIntent.SignUpClicked)
                    }
                )
            )
            Spacer(Modifier.height(AppDimens.spaceSm))
            AppTextButton(
                onClick = onForgetPasswordClick,
                text = stringResource(R.string.login_forgot_password),
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppPrimaryButton(
                onClick = { onIntent(SignUpIntent.SignUpClicked) },
                text = stringResource(R.string.signup_button),
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppDivider()
            Spacer(Modifier.height(AppDimens.size5Xs))
            AppAuthFooterLink(
                promptText = stringResource(R.string.signup_have_account),
                actionText = stringResource(R.string.signup_log_in),
                onActionClick = onLogin
            )
        }
    }
}