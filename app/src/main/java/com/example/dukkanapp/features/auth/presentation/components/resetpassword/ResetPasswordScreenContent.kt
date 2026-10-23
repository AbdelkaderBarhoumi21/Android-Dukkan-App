package com.example.dukkanapp.features.auth.presentation.components.resetpassword


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.footer.AppAccountRecoveryFooter
import com.example.dukkanapp.core.common.components.header.AppAccountRecoveryHeader
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppPasswordTextField
import com.example.dukkanapp.core.common.text.asString
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.logic.forgetpassword.ForgetPasswordIntent
import com.example.dukkanapp.features.auth.presentation.logic.forgetpassword.ForgetPasswordUiState
import com.example.dukkanapp.features.auth.presentation.logic.signup.SignUpIntent
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun ResetPasswordScreenContent(
    state: ForgetPasswordUiState,
    onIntent: (ForgetPasswordIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    AppScaffold(
        bottomBar = {
            AppAccountRecoveryFooter(
                onActionClick = { onIntent(ForgetPasswordIntent.ForgetPasswordClicked) },
                onBackClick = { onIntent(ForgetPasswordIntent.BackClicked) },
                actionButtonLabel = stringResource(R.string.forgetpassword_send_reset_link),
                backButtonLabel = stringResource(R.string.forgetpassword_back_to_login)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            AppAccountRecoveryHeader(
                title = stringResource(R.string.forgetpassword_title),
                subTitle = stringResource(R.string.forgetpassword_subtitle),
            )
            Spacer(Modifier.height(AppDimens.spaceXl))
            AppPasswordTextField(
                value = state.password,
                onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                placeHolder = stringResource(R.string.auth_password_placeholder),
                errorText = state.passwordError?.asString(),
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
                errorText = state.confirmPasswordError?.asString(),
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


        }
    }
}