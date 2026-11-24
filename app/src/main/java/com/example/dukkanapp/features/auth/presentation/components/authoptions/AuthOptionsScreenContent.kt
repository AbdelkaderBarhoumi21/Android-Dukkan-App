package com.example.dukkanapp.features.auth.presentation.components.authoptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppButton
import com.example.dukkanapp.core.common.components.buttons.AppSocialButton
import com.example.dukkanapp.core.common.components.footer.AppAuthFooterLink
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.common.components.scaffold.AppScreenBackground
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons
import com.example.dukkanapp.features.auth.presentation.components.common.AppAuthHeader
import com.example.dukkanapp.features.auth.presentation.components.common.AuthFormColumn
import com.example.dukkanapp.features.auth.presentation.components.common.AuthHeaderLogo
import com.example.dukkanapp.features.auth.presentation.components.common.AuthOrDivider
import com.example.dukkanapp.features.auth.presentation.components.common.legalText

@Composable
fun AuthOptionsScreenContent(
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    AppScaffold(
        background = AppScreenBackground.AuroraTall
    ) { padding ->
        AuthFormColumn(
            padding = padding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(
                    AppDimens.space64
                )
            )
            AppAuthHeader(
                title = stringResource(R.string.auth_welcome_title),
                subtitle = stringResource(R.string.auth_welcome_subtitle),
                logo = AuthHeaderLogo.Mark(size = AppDimens.logoMark)
            )
            Spacer(Modifier.height(AppDimens.space16))
            AuthPerksRow()
            Spacer(
                Modifier
                    .weight(1f)
                    .heightIn(
                        min = AppDimens.space32
                    )
            )

            AppSocialButton(
                text = stringResource(R.string.auth_welcome_google),
                logo = painterResource(AppIcons.Google),
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppDimens.space12))
            AppSocialButton(
                text = stringResource(R.string.auth_welcome_facebook),
                logo = painterResource(AppIcons.Facebook),
                onClick = onFacebookClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppDimens.space20))
            AuthOrDivider(
                text = stringResource(R.string.auth_welcome_or)
            )
            Spacer(Modifier.height(AppDimens.space20))
            AppButton(
                text = stringResource(R.string.auth_welcome_email),
                leadingIcon = painterResource(AppIcons.Mail),
                onClick = onEmailClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppDimens.space16))
            Text(
                text = legalText(
                    templateRes = R.string.auth_legal_consent,
                    onTermsClick = onTermsClick,
                    onPrivacyClick = onPrivacyClick
                ),
                style = AppTheme.textStyles.caption,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(
                Modifier
                    .weight(1f)
                    .heightIn(min = AppDimens.space24)
            )
            AppAuthFooterLink(
                promptText = stringResource(R.string.auth_already_have_account),
                actionText = stringResource(R.string.action_log_in),
                onActionClick = onAlreadyHaveAccountClick,
            )


        }
    }
}

@Composable
private fun AuthPerksRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space16)) {
        AuthPerk(icon = AppIcons.Clock, text = stringResource(R.string.auth_perk_fast))
        AuthPerk(icon = AppIcons.Banknote, text = stringResource(R.string.auth_perk_cash))
        AuthPerk(icon = AppIcons.RefreshCw, text = stringResource(R.string.auth_perk_returns))
    }
}

@Composable
private fun AuthPerk(icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
    ) {
        Icon(
            painterResource(icon),
            null,
            tint = AppTheme.colors.brand,
            modifier = Modifier.size(AppDimens.iconXs)
        )
        Text(text, style = AppTheme.textStyles.helper, color = AppTheme.colors.textLabel)
    }
}
