package com.example.dukkanapp.features.auth.presentation.components.email

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun EmailTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    placeHolder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    isSuccess: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    AppTextFiled(
        value = value,
        onValueChange = onValueChanged,
        placeHolder = placeHolder,
        modifier = modifier,
        errorText = errorText,
        isSuccess = isSuccess,
        leadingIcon = {
            Icon(
                imageVector = Iconsax.Bold.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant

            )
        },
        keyboardActions = keyboardActions
    )
}