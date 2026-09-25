package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collectLatest { page ->
            onEvent(
                OnboardingEvent.OnPageChanged(page)
            )
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = AppDimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHorizontalPager(
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            OnboardingPageItem(page = state.pages[page])
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        AppPagerIndicator(
            pageCount = state.pages.size,
            currentPage = state.currentPage
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceXl))

        AppPrimaryButton(
            text = stringResource(state.primaryActionRes),
            onClick = {
                if (state.isLastPage) {
                    onGetStarted()
                } else {
                    onEvent(OnboardingEvent.OnNextClicked)
                }
            },
            modifier = Modifier.padding(horizontal = AppDimens.screenHorizontalPadding)
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))

        OnboardingAlreadyHaveAccount(
            onActionClick = {}
        )

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