package com.example.dukkanapp.core.common.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.example.dukkanapp.core.common.components.buttons.AppIconButton
import com.example.dukkanapp.core.common.components.buttons.AppIconButtonVariant
import com.example.dukkanapp.core.theme.AppDimens
import com.example.dukkanapp.core.theme.AppPillShape
import com.example.dukkanapp.core.theme.AppTheme
import com.example.dukkanapp.core.theme.appShadow
import com.example.dukkanapp.core.utils.constants.AppIcons

enum class AppSearchBarVariant { Elevated, Field }

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    variant: AppSearchBarVariant = AppSearchBarVariant.Field,
    onSearch: () -> Unit = {},
    onFilterClick: (() -> Unit)? = null,
    filterContentDescription: String? = null,
) {
    val colors = AppTheme.colors
    val shape = AppPillShape
    val isElevated = variant == AppSearchBarVariant.Elevated
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = colors.textPrimary
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(
                if (isElevated) AppDimens.input else AppDimens.searchField
            )
            .then(
                if (isElevated) Modifier.appShadow(AppTheme.shadows.float, shape) else Modifier
            )
            .background(
                if (isElevated) colors.card else colors.field,
                shape,
            )
            .padding(
                start = AppDimens.space20, end = AppDimens.space8
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(AppIcons.Search),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(AppDimens.iconMd) // Figma: 18 in both variants
        )
        Spacer(Modifier.width(AppDimens.space8))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.brand),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { basicTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder, style = textStyle.copy(
                                color = colors.textTertiary,

                                ),
                            maxLines = 1
                        )
                    }
                    basicTextField()
                }
            }
        )
        if (onFilterClick != null) {
            AppIconButton(
                icon = painterResource(AppIcons.SlidersHorizontal),
                contentDescription = filterContentDescription,
                onClick = onFilterClick,
                variant = AppIconButtonVariant.Brand,
                size = AppDimens.iconButtonSm
            )
        }
    }
}