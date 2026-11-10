package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme

@Composable
fun AppOtpField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isError: Boolean = false,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppDimens.radiusLg)
    val digitStyle = MaterialTheme.typography.headlineMedium.copy(color = colors.textPrimary)

    // Codes are read left to right in every language, including Arabic.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code,
            onValueChange = { input -> onCodeChange(input.filter(Char::isDigit).take(length)) },
            modifier = modifier,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
                    repeat(length) { index ->
                        val isCurrent = index == code.length
                        val borderColor = when {
                            isError -> colors.error
                            isCurrent -> colors.brand
                            else -> colors.border
                        }

                        Box(
                            modifier = Modifier
                                .size(
                                    width = AppDimens.otpBoxWidth, height = AppDimens.otpBoxHeight
                                )
                                .background(colors.card, shape)
                                .border(
                                    width = AppDimens.strokeFocus,
                                    color = borderColor,
                                    shape = shape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code.getOrNull(index)?.toString() ?: "", style = digitStyle
                            )
                        }

                    }
                }
            },
        )
    }
}