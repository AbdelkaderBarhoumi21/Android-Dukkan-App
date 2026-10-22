package com.example.dukkanapp.features.auth.presentation.components.forgetpassword


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.core.utils.extension.clearFocusOnTap
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun ForgetPasswordScreenContent(
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    AppScaffold(
        bottomBar = {
            // imePadding makes the  button sit just above the keyboard when it open
            // Add navigationBarsPadding() on the bottom bar (safe Area)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = AppDimens.size2Xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppPrimaryButton(
                    text = stringResource(R.string.forgetpassword_send_reset_link),
                    onClick = {},
                )
                Spacer(Modifier.height(AppDimens.size3Xs))
                AppTextButton(
                    text = stringResource(R.string.forgetpassword_back_to_login),
                    onClick = onNavigateBack,
                )

            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearFocusOnTap()
                .padding(padding)
                .padding(horizontal = AppDimens.size2Xs)
        ) {
            ForgetPasswordHeader(
                title = stringResource(R.string.forgetpassword_title),
                subTitle = stringResource(R.string.forgetpassword_subtitle),
            )
            Spacer(Modifier.height(AppDimens.spaceXl))
            AppTextFiled(
                value = "",
                onValueChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Iconsax.Linear.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.iconSm)
                    )
                },
                placeHolder = stringResource(R.string.auth_email_placeholder),
                errorText = "",
                isSuccess = true,
                keyboardType = KeyboardType.Email,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                )
            )


        }
    }
}