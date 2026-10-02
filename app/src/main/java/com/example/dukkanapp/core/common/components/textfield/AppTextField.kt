package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
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

            )
    }
}