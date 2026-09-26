package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.pager.AppHorizontalPager
import com.example.dukkanapp.core.common.pager.AppPagerIndicator
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingEvent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingUiState
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OnboardingScreenContent(
    state: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
) {
    if (state.pages.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pages.size }
    )
    // Direction 1: pager -> ViewModel (user swipes)
    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collectLatest { page ->
            onEvent(
                OnboardingEvent.OnPageChanged(page)
            )
        }
    }

    // Direction 2: ViewModel -> pager (Next button tapped)
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.onPrimary)

    ) {
        // ZONE 1: illustration + dots, plain background, swipeable
        Box(
            modifier = Modifier.weight(1f)
        ) {
            AppHorizontalPager(
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                OnboardingIllustration(page = state.pages[page])

            }

            AppPagerIndicator(
                pageCount = state.pages.size,
                currentPage = state.currentPage,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(bottom = AppDimens.spaceLg)
            )
        }

        // ZONE 2: rounded card, fixed, NOT inside the pager

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .border(
                    width = AppDimens.borderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(
                        topStart = AppDimens.radiusLg,
                        topEnd = AppDimens.radiusLg
                    )
                )
                .navigationBarsPadding()
                .padding(
                    AppDimens.spaceLg
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(state.pages[state.currentPage].titleRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,

                )
            Spacer(Modifier.height(AppDimens.spaceSm))
            Text(
                text = stringResource(state.pages[state.currentPage].subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center

            )
            Spacer(Modifier.height(AppDimens.spaceLg))
            AppPrimaryButton(
                text = stringResource(state.primaryActionRes),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (state.isLastPage) {
                        onGetStarted()
                    } else {
                        onEvent(OnboardingEvent.OnNextClicked)
                    }
                }
            )
            Spacer(Modifier.height(AppDimens.spaceMd))
            OnboardingAlreadyHaveAccount(
                onActionClick = { onEvent(OnboardingEvent.OnLoginClicked) },

                )


        }

    }
}

@Preview(name = "English", locale = "en")
@Preview(name = "French", locale = "fr")
@Preview(name = "Arabic", locale = "ar")
@Composable
private fun OnboardingScreenPreview() {
    AppTheme {
        OnboardingScreenContent(
            state = OnboardingUiState(
                pages = listOf(
                    OnboardingPageUiModel(
                        id = "discover",
                        titleRes = R.string.onboarding_discover_title,
                        subtitleRes = R.string.onboarding_discover_subtitle,
                        imageRes = R.drawable.img_onboarding_discover,
                    ),
                    OnboardingPageUiModel(
                        id = "checkout",
                        titleRes = R.string.onboarding_checkout_title,
                        subtitleRes = R.string.onboarding_checkout_subtitle,
                        imageRes = R.drawable.img_onboarding_checkout,
                    ),
                    OnboardingPageUiModel(
                        id = "shopping",
                        titleRes = R.string.onboarding_shopping_title,
                        subtitleRes = R.string.onboarding_shopping_subtitle,
                        imageRes = R.drawable.img_onboarding_shopping,
                    ),
                ),
                currentPage = 0,
            ),
            onEvent = {},
            onGetStarted = {},
            onLogin = {},
        )
    }
}