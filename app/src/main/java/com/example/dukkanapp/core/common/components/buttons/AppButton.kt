package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonStyle
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appButtonStyle
import com.example.dukkanapp.core.theme.appButtonTextStyle
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Large,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val style = appButtonStyle(variant, enabled, pressed = isPressed && !isLoading)
    val loadingDescription = stringResource(R.string.cd_loading)

    AppButtonContainer(
        onClick = onClick,
        style = style,
        interactionSource = interaction,
        height = size.height,
        horizontalPadding = size.horizontalPadding,
        enabled = enabled,
        isLoading = isLoading,
        modifier = modifier.semantics {
            if (isLoading) stateDescription = loadingDescription
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)
        ) {
            leadingIcon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(AppDimens.icon)
                )
            }
            Text(
                text = text,
                style = appButtonTextStyle(size),
                color = style.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // trailing icon replace loading
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.icon),
                    color = style.content,
                    strokeWidth = AppDimens.strokeFocus,
                )

                trailingIcon != null -> Icon(
                    painter = trailingIcon,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(AppDimens.icon)
                )
            }
        }
    }


}

@Composable
internal fun AppButtonContainer(
    onClick: () -> Unit,
    style: AppButtonStyle,
    interactionSource: MutableInteractionSource,
    height: Dp,
    horizontalPadding: Dp,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .then(
                if (style.shadow != null) Modifier.appShadow(
                    style.shadow,
                    AppPillShape
                ) else Modifier
            )
            .clip(AppPillShape)
            .background(style.background)
            .then(
                if (style.border != null) Modifier.border(style.border, AppPillShape) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = style.content),
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun AppButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            AppButtonVariant.entries.forEach { variant ->
                AppButton(
                    text = variant.name,
                    onClick = {},
                    variant = variant,
                    trailingIcon = painterResource(AppIcons.ArrowRight),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AppButton(text = "Compact", onClick = {}, size = AppButtonSize.Compact)
            AppButton(text = "Medium", onClick = {}, size = AppButtonSize.Medium)
            AppButton(text = "Small", onClick = {}, size = AppButtonSize.Small)
            AppButton(text = "Apply", onClick = {}, size = AppButtonSize.ExtraSmall)
            AppButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = "Continue",
                onClick = {},
                isLoading = true,
                trailingIcon = painterResource(AppIcons.ArrowRight),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}