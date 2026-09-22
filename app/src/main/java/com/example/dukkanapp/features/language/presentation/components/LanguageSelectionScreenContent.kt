package com.example.dukkanapp.features.language.presentation.components


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.scaffold.AppScaffold
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionEvent
import com.example.dukkanapp.features.language.presentation.logic.LanguageSelectionUiState


@Composable
fun LanguageSelectionScreenContent(
    state: LanguageSelectionUiState,
    onEvent: (LanguageSelectionEvent) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    AppScaffold(
        onNavigateBack = onBack,
        bottomBar = {
            AppPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                modifier = Modifier
                    .padding(horizontal = AppDimens.SpaceMd, vertical = AppDimens.SpaceLg)

            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding,

            modifier = Modifier.padding(horizontal = AppDimens.ScreenHorizontalPadding),
        ) {
            item {
                Text(
                    text = stringResource(R.string.language_selection_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                Spacer(Modifier.height(AppDimens.SpaceSm))
            }
            item {
                Text(
                    text = stringResource(R.string.language_selection_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Spacer(Modifier.height(AppDimens.SpaceLg))
            }

            item {
                Text(
                    text = stringResource(R.string.language_selection_you_selected),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                Spacer(Modifier.height(AppDimens.SpaceSm))
            }
            state.selectedLanguage?.let { selected ->
                item(key = "selected_${selected.code}") {
                    LanguageItemCard(
                        language = selected,
                        isSelected = true,
                        isHighlightedStyle = true,
                        onClick = {},
                    )
                }
            }
            item {
                Spacer(Modifier.height(AppDimens.SpaceLg))
            }


            item {
                Text(
                    text = stringResource(R.string.language_selection_all_languages),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                Spacer(Modifier.height(AppDimens.SpaceSm))
            }
            item {
                LanguageSearchField(
                    query = state.query,
                    onQueryChange = { onEvent(LanguageSelectionEvent.OnQueryChanged(it)) },
                )
            }

            items(state.filteredLanguages, key = { it.code }) { language ->
                LanguageItemCard(
                    language = language,
                    isSelected = language.code == state.selectedCode,
                    isHighlightedStyle = false,
                    onClick = { onEvent(LanguageSelectionEvent.OnLanguageSelected(language.code)) },
                    modifier = Modifier.padding(vertical = AppDimens.Space2Xs)
                )
            }
        }
    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "French", locale = "fr")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun LanguageSelectionScreenPreview() {
    AppTheme {
        LanguageSelectionScreenContent(
            state = LanguageSelectionUiState(
                selectedCode = "ar",
                languages = emptyList(), // swap in fake LanguageUiModel list to preview real rows
            ),
            onEvent = {},
            onBack = {},
            onContinue = {},
        )
    }
}