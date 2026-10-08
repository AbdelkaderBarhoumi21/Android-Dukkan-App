package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens

object AppTextFieldDefault {
    val shape = RoundedCornerShape(AppDimens.sizeXl)

    @Composable
    fun colors(isSuccess: Boolean): TextFieldColors {
        val scheme = MaterialTheme.colorScheme
        val extendedColors = AppTheme.extendedColors
        val container = scheme.surfaceVariant
        return OutlinedTextFieldDefaults.colors(
            // Background: always the same
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
            // Border: none by default, primary on focus, green on success, red on error
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = if (isSuccess) extendedColors.success else Color.Transparent,
            errorBorderColor = scheme.error,
            disabledBorderColor = Color.Transparent,

            // Text
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            errorTextColor = scheme.error,

            // Placeholder
            focusedPlaceholderColor = scheme.onSurfaceVariant,
            unfocusedPlaceholderColor = scheme.onSurfaceVariant,
            errorPlaceholderColor = scheme.onSurfaceVariant,

            // Icons
            focusedLeadingIconColor = scheme.onSurfaceVariant,
            unfocusedLeadingIconColor = scheme.onSurfaceVariant,
            focusedTrailingIconColor = scheme.onSurfaceVariant,
            unfocusedTrailingIconColor = scheme.onSurfaceVariant,

            // Cursor
            cursorColor = scheme.primary


        )
    }

    @Composable
    fun borderColor(isFocused: Boolean, isError: Boolean, isSuccess: Boolean): Color {
        val scheme = MaterialTheme.colorScheme
        return when {
            isError -> scheme.error
            isSuccess -> scheme.tertiary
            isFocused -> scheme.outlineVariant
            else -> Color.Transparent
        }
    }
}


@Composable
fun AppTextFiled(
    value: String,
    onValueChange: (String) -> Unit,
    placeHolder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    errorText: String? = null,
    isSuccess: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    // 1. Wrap in a Column so we can stack the error text below the field
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // 2. Keep your exact 45.dp height constraint on the input box ONLY!
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.buttonHeight),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = placeHolder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            // 3. REMOVE the supportingText parameter from here!
            isError = errorText != null,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            shape = AppTextFieldDefault.shape,
            colors = AppTextFieldDefault.colors(isSuccess)
        )

        // 4. Draw the error text outside the field so it never gets clipped
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                // Add a little padding to align it with the text inside the box
                modifier = Modifier.padding(top = AppDimens.size4Xs)
            )
        }
    }
}