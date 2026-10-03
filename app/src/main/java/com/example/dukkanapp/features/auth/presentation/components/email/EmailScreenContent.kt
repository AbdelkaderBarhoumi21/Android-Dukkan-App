package com.example.dukkanapp.features.auth.presentation.components.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.TextField.AppPasswordTextField
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.example.dukkanapp.features.auth.presentation.components.authoptions.LoginBrandHeader
import com.example.dukkanapp.features.auth.presentation.logic.EmailUiState
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun EmailScreenContent(
    state: EmailUiState,
    onLoginClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNavigationBack: () -> Unit,
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
            LoginBrandHeader(
                title = stringResource(R.string.email_login_title),
                subTitle = stringResource(R.string.email_login_subtitle),
                logo = painterResource(R.drawable.ic_app_logo)
            )
            Spacer(Modifier.height(AppDimens.sizeMd))
            AppTextFiled(
                value = state.email,
                onValueChange = onEmailChanged,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeHolder = stringResource(R.string.email_email_label),
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
                placeHolder = stringResource(R.string.email_password_label),
                errorText = state.passwordError?.let { stringResource(it) },
                isSuccess = state.isValidPassword,
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.PasswordCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }, keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                )
            )


        }
    }


}