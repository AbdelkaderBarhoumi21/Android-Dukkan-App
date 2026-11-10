import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import com.example.dukkanapp.core.common.components.textfield.AppFieldStatus
import com.example.dukkanapp.core.common.components.textfield.appFieldColors
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppIcons

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: Painter? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    helperText: String? = null,
    status: AppFieldStatus = AppFieldStatus.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val fieldColors = appFieldColors(status, isFocused, enabled)
    val ringColor = AppTheme.colors.brandRing.copy(alpha = 0.7f)
    val textStyles = AppTheme.textStyles

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fieldColors.label
            )
            Spacer(Modifier.height(AppDimens.space8))
        }
        // Focus ring drawn OUTSIDE the box: no layout shift when focus changes.
        // The box is a pill, so the ring is a pill too: radius = half the ring height.
        // TalkBack reads "Error: <helper>" on the field itself.
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.input)
                .drawBehind {
                    if (fieldColors.showRing) {
                        val ring = AppDimens.focusRing.toPx()
                        val radius = size.height / 2 + ring
                        drawRoundRect(
                            color = ringColor,
                            topLeft = Offset(-ring, -ring),
                            size = Size(size.width + ring * 2, size.height + ring * 2),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }
                }
                .background(fieldColors.fill, AppPillShape)
                .then(
                    if (fieldColors.border != null) {
                        Modifier.border(AppDimens.strokeFocus, fieldColors.border, AppPillShape)
                    } else {
                        Modifier
                    }
                )
                .semantics {
                    if (status == AppFieldStatus.Error && helperText != null) error(helperText)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = textStyles.fieldText.copy(color = fieldColors.text),
            cursorBrush = SolidColor(AppTheme.colors.brand),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = source,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(start = AppDimens.space20, end = AppDimens.space12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            painter = leadingIcon,
                            contentDescription = null,
                            tint = fieldColors.leadingIcon,
                            modifier = Modifier.size(AppDimens.icon)
                        )
                        Spacer(Modifier.width(AppDimens.space8))
                    }
                    Box(
                        modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textTertiary,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            }
        )

        if (helperText != null) {
            Spacer(Modifier.height(AppDimens.space4))
            AppFieldHelper(text = helperText, status = status, fieldColors.helper)

        }
    }
}

@Composable
private fun AppFieldHelper(text: String, status: AppFieldStatus, color: Color) {
    Row(
        modifier = Modifier.padding(start = AppDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space4)
    ) {
        val icon = when (status) {
            AppFieldStatus.Error -> AppIcons.CircleAlert
            AppFieldStatus.Success -> AppIcons.CircleCheck
            AppFieldStatus.Default -> null
        }
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(AppDimens.iconXs)
            )
        }
        Text(
            text = text,
            style = AppTheme.textStyles.helper, color = color
        )
    }
}