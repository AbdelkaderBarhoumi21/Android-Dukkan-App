package com.example.dukkanapp.core.common.components.textfield

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.dukkanapp.R
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme

enum class PasswordStrength(val segments: Int, @StringRes val labelRes: Int) {
    Weak(1, R.string.password_strength_weak),
    Fair(2, R.string.password_strength_fair),
    Good(3, R.string.password_strength_good),
    Strong(4, R.string.password_strength_strong);

    companion object {
        /** UI hint only. The real rule stays in AuthFormValidator.validateNewPassword. */
        fun of(password: String): PasswordStrength {
            var score = 0
            if (password.length >= 8) score++
            if (password.any(Char::isDigit)) score++
            if (password.any { !it.isLetterOrDigit() }) score++
            if (password.any(Char::isUpperCase) && password.any(Char::isLowerCase)) score++
            return entries[(score - 1).coerceAtLeast(0)]
        }
    }
}


@Composable
fun AppPasswordStrength(
    password: String, modifier: Modifier = Modifier
) {
    if (password.isEmpty()) return
    val strength = PasswordStrength.of(password)
    val colors = AppTheme.colors
    val activeColor by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.Weak -> colors.error
            PasswordStrength.Good -> colors.warning
            PasswordStrength.Fair, PasswordStrength.Strong -> colors.success
        },
        label = "strengthColor"
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
        ) {
            repeat(
                PasswordStrength.entries.size
            ) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimens.space4)
                        .background(
                            if (index < strength.segments) activeColor else colors.border,
                            shape = AppPillShape
                        )
                )
            }
            Text(
                text = stringResource(
                    R.string.password_strength_format, stringResource(strength.labelRes),
                ),
                style = AppTheme.textStyles.helper,
                color = activeColor,
                modifier = Modifier.padding(top = AppDimens.space8)
            )
        }
    }
}