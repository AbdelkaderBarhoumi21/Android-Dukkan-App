package com.example.dukkanapp.features.language.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.textfield.AppTextFiled
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.github.yohannestz.iconsax_compose.iconsax.Iconsax

@Composable
fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTextFiled(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeHolder = stringResource(R.string.language_selection_search_hint),
        leadingIcon = {
            Icon(
                imageVector = Iconsax.Linear.SearchNormal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AppDimens.iconSm),
            )
        },
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        singleLine = true
    )
}