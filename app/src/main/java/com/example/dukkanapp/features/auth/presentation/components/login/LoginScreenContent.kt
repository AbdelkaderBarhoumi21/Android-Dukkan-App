package com.example.dukkanapp.features.auth.presentation.components.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.dukkanapp.core.common.components.CheckBox.AppCheckBox
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
import com.example.dukkanapp.features.auth.presentation.logic.Login.LoginUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onLoginClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onNavigationBack: () -> Unit,
    onSignupClick: () -> Unit,
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
                title = stringResource(R.string.login_title),
                subTitle = stringResource(R.string.login_subtitle),
            )
            Spacer(Modifier.height(AppDimens.sizeMd))
            AppTextFiled(
                value = state.email,
                onValueChange = onEmailChanged,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                placeHolder = stringResource(R.string.login_email_placeholder),
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
                onValueChange = onPasswordChanged,
                placeHolder = stringResource(R.string.login_password_placeholder),
                errorText = state.passwordError?.let {
                    stringResource(
                        it,
                        AppValidationConstants.MIN_PASSWORD_LENGTH
                    )
                },
                isSuccess = state.isValidPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                }, keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                )
            )
            Spacer(Modifier.height(AppDimens.spaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppCheckBox(
                    checked = state.rememberMe,
                    onCheckChanged = onRememberMeChanged,
                    label = stringResource(R.string.login_remember_me),
                )
                AppTextButton(
                    onClick = onForgetPasswordClick,
                    text = stringResource(R.string.login_forgot_password),
                )
            }
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppPrimaryButton(
                onClick = onLoginClick,
                text = stringResource(R.string.login_button),
            )
            Spacer(Modifier.height(AppDimens.size3Xs))
            AppDivider()
            Spacer(Modifier.height(AppDimens.size5Xs))
            AppAuthFooterLink(
                promptText = stringResource(R.string.login_no_account),
                actionText = stringResource(R.string.login_sign_up),
                onActionClick = onSignupClick
            )
        }
    }


}