package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.example.dukkanapp.core.utils.constants.AppDimens

object AppTextFieldDefault {
    val shape = RoundedCornerShape(AppDimens.sizeXl)
    val minHeight = AppDimens.size2Xl

    @Composable
    fun colors(isSuccess: Boolean): TextFieldColors {
        val scheme = MaterialTheme.colorScheme
        val container = scheme.surfaceContainer
        return OutlinedTextFieldDefaults.colors(
            // Background: always the same
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
            // Border: none by default, primary on focus, green on success, red on error
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isSuccess) MaterialTheme.colorScheme.tertiary else Color.Transparent,
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
    imeAction: ImeAction = ImeAction.Next, // What happens when the user taps that button? => Move to the next field, hide the keyboard, submit the form,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTextFieldDefault.minHeight),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(text = placeHolder) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = errorText?.let {
            {
                Text(
                    text = it
                )
            }
        },
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
}