package com.example.dukkanapp.core.common.components.CheckBox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.example.dukkanapp.core.utils.constants.AppDimens

object AppCheckBoxDefaults {
    val boxSize = AppDimens.sizeXs
    val boxShape = RoundedCornerShape(AppDimens.radiusXs)
    val borderWidth = AppDimens.borderWidth
    val iconSize = AppDimens.size3Xs
    val spacing = AppDimens.spaceSm
    val minHeight = AppDimens.sizeXl

}

/*
   interaction = null => remove the ripple effect once the user press check box
   interfaction source -> is a channel that record what the user is doing to the element (pressed focused)
   interfaction -> listen to interaction source to know what to draw once the interfaction source changed
   remember => ensure one object is created once and the same one is reused on every recomposition.
 */
@Composable
fun AppCheckBox(

    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(AppCheckBoxDefaults.boxShape)
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckChanged,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox
            )
            .heightIn(AppCheckBoxDefaults.minHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppCheckBoxDefaults.spacing)
    ) {
        CustomCheckBox(
            checked = checked
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

}


@Composable
fun CustomCheckBox(
    checked: Boolean,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .size(AppCheckBoxDefaults.boxSize),
        shape = AppCheckBoxDefaults.boxShape,
        color = if (checked) scheme.primary else scheme.background,
        border = BorderStroke(
            width = AppCheckBoxDefaults.borderWidth,
            color = if (checked) scheme.primary else scheme.outlineVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppDimens.icon2Xs)
                )
            }
        }
    }
}