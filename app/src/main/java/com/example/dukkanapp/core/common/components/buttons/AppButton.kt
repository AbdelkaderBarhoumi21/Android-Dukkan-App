package com.example.dukkanapp.core.common.components.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppButtonSize
import com.example.dukkanapp.core.theme.AppButtonVariant
import com.example.dukkanapp.core.theme.appButtonStyle

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
    enabled: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val style = appButtonStyle(variant, enabled, pressed = isPressed && !isLoading)
    val loadingDescription = stringResource(R.string.cd_loading)


}

@Composable
internal fun AppButtonContainer(
    onClick: () -> Unit,
    style:
) {
}