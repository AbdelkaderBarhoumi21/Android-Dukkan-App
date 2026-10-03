package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun AppPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeHolder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    isSuccess: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    AppTextFiled(
        value = value,
        onValueChange = onValueChange,
        placeHolder = placeHolder,
        modifier = modifier,
        errorText = errorText,
        isSuccess = isSuccess,
        leadingIcon = leadingIcon,
        keyboardType = KeyboardType.Password,
        keyboardActions = keyboardActions,
        imeAction = imeAction,
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()

        },
        trailingIcon = {
            IconButton(
                onClick = {
                    isVisible = !isVisible
                }
            ) {
                Icon(
                    imageVector = if (isVisible) Iconsax.Linear.Eye else Iconsax.Linear.EyeSlash,
                    contentDescription = stringResource(
                        if (isVisible) R.string.email_hide_password
                        else R.string.email_show_password
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppDimens.iconSm)
                )
            }
        }
    )
}