package com.example.dukkanapp.core.common.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

private val CheckboxShape = RoundedCornerShape(AppDimens.radiusCheckbox)

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val fill by animateColorAsState(
        if (checked) colors.brand else colors.card,
        label = "checkboxFill"
    )

    Row(
        modifier = modifier
            .heightIn(min = AppDimens.touchTargetMin)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.checkbox)
                .background(fill, CheckboxShape)
                .then(
                    if (checked) Modifier
                    else Modifier.border(AppDimens.strokeControl, colors.borderControl, CheckboxShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(AppIcons.Check),
                    contentDescription = null,
                    tint = colors.onBrand,
                    modifier = Modifier.size(AppDimens.iconXs),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textLabel,
        )
    }
}

/** Convenience overload for a plain string label. */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = AppCheckbox(checked, onCheckedChange, AnnotatedString(label), modifier, enabled)