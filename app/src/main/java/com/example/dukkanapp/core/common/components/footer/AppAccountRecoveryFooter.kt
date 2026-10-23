package com.example.dukkanapp.core.common.components.footer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.buttons.AppTextButton
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppAccountRecoveryFooter(
    modifier: Modifier = Modifier,
    actionButtonLabel: String,
    backButtonLabel: String,
    onActionClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    // imePadding makes the  button sit just above the keyboard when it open
    // Add navigationBarsPadding() on the bottom bar (safe Area)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AppDimens.size2Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppPrimaryButton(
            text = actionButtonLabel,
            onClick = onActionClick
        )
        Spacer(Modifier.height(AppDimens.size4Xs))
        AppTextButton(
            text = backButtonLabel,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = onBackClick
        )

    }
}