package com.example.dukkanapp.core.common.components.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppIconButton
import com.example.dukkanapp.core.common.components.buttons.AppIconButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    background: AppScreenBackground = AppScreenBackground.Plain,
    title: String? = null,
    subtitle: String? = null,
    onNavigationBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    toastState: AppToastState? = null,
    toastPlacement: AppToastPlacement = AppToastPlacement.Top,
    content: @Composable (PaddingValues) -> Unit,
) {
    AppScreenBackgroundBox(background = background, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                val hasTopBar = onNavigationBack != null || title != null
                if (hasTopBar) {
                    AppTopBar(
                        title = title,
                        subtitle = subtitle,
                        onNavigationBack = onNavigationBack,
                        actions = topBarActions
                    )
                }
            },
            bottomBar = bottomBar,
            content = content
        )
        // Drawn over the whole screen, not in Scaffold's snackbar slot, which only sits at the bottom.
        toastState?.let { AppToastHost(state = it, placement = toastPlacement) }

    }
}

@Composable
private fun AppTopBar(
    title: String?,
    subtitle: String?,
    onNavigationBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = AppDimens.screenPadding
            )
            .padding(
                top = AppDimens.space8
            )
            .height(AppDimens.iconButton)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onNavigationBack != null) {
                AppIconButton(
                    icon = painterResource(AppIcons.ArrowLeft),
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onNavigationBack,
                    variant = AppIconButtonVariant.White
                )
            }
            // Actions get the rest of the row (sign-up progress uses weight(1f) inside).
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space12, Alignment.End),
                content = actions
            )
        }
        if (title != null) {
            Column(
                modifier = Modifier
                    .align(
                        Alignment.Center,
                    )
                    .padding(
                        horizontal = AppDimens.iconButton + AppDimens.space12
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Figma `App bar/Default`: M3/titleLarge 17 ExtraBold + Dukkan/supporting 11.5.
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                subtitle?.let {
                    Text(
                        it,
                        style = AppTheme.textStyles.supporting,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1
                    )
                }

            }
        }

    }
}