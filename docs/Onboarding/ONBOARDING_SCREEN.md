# Onboarding Screen — HorizontalPager (Compose)

This document is the full implementation blueprint for the Dukkan onboarding flow.
Copy every file as written. Do not skip a file. Do not invent extra colors or a
new theme: the mock is only for **layout**. Colors, type, and the primary button
already come from `AppTheme` and `AppPrimaryButton`.

Package root:

```text
com.example.dukkanapp
```

---

## 0. What you are building

One **route**. One **screen**. Three **pages** that swipe in place.

```text
LanguageSelection  -->  Onboarding (this screen)  -->  Home / Login later
                              |
                              +-- page 0  Discover Our Products
                              +-- page 1  Hassle-Free Checkout
                              +-- page 2  Easy and Happy Shopping
```

The user never leaves `OnboardingScreen` when they tap **Next** or swipe.
Only the last page (**Get Started**) or **Log In** leaves this route.

Layout of every page (top to bottom):

```text
illustration (takes remaining height)
pager dots
title
subtitle
AppPrimaryButton   ("Next" or "Get Started")
Already have an account?  Log In
```

Do **not** copy the mock's black canvas or lime button. Use:

- `MaterialTheme.colorScheme.background`
- `MaterialTheme.colorScheme.onBackground`
- `MaterialTheme.colorScheme.onSurfaceVariant`
- `MaterialTheme.colorScheme.primary` (already inside `AppPrimaryButton`)

Replace the three placeholder drawables later with the real illustrations.

---

## 1. Flutter → Compose: how the pager works

In Flutter you stay on one widget tree and swap children with `PageView`.
Compose does the same job with `HorizontalPager`.

| Flutter | Compose |
|---|---|
| `PageView` / `PageView.builder` | `HorizontalPager` |
| `PageController` | `PagerState` from `rememberPagerState` |
| `itemCount` | `rememberPagerState(pageCount = { pages.size })` |
| `itemBuilder: (context, index)` | `HorizontalPager { page -> ... }` |
| `controller.page` / `controller.page.round()` | `pagerState.currentPage` |
| `onPageChanged: (index) { ... }` | `snapshotFlow { pagerState.currentPage }` |
| `controller.animateToPage(i)` | `pagerState.animateScrollToPage(i)` |
| `controller.nextPage()` | `animateScrollToPage(currentPage + 1)` |

`PagerState` is a **Compose UI object**. It must live in a `@Composable`, not in
the ViewModel. The ViewModel only stores the integer `currentPage`.

Sync both directions:

```text
user swipes
    -> HorizontalPager updates PagerState.currentPage
    -> snapshotFlow emits the new index
    -> OnboardingEvent.OnPageChanged
    -> ViewModel writes uiState.currentPage
    -> dots + button label recompose

user taps Next
    -> OnboardingEvent.OnNextClicked
    -> ViewModel increments currentPage
    -> LaunchedEffect sees the new index
    -> pagerState.animateScrollToPage(...)
    -> the same page composable is reused, only `page` changes
```

`HorizontalPager` is already on the Compose BOM you use
(`composeBom = "2026.02.01"`). You do **not** add Accompanist Pager.
You do **not** add a new Gradle library.

Import:

```kotlin
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
```

---

## 2. Clean architecture for this feature

Same rules as language selection.

```text
presentation -> domain <- data
       |
       +---> core (theme, AppPrimaryButton, AppHorizontalPager)
```

1. `domain` has no Compose, no `R.drawable`, no `R.string`.
2. `presentation` maps domain pages to string/drawable resources.
3. `data` only supplies the static page list (no network).
4. Navigation stays in `AppNavHost`. The ViewModel does not navigate.
5. Reusable pager UI lives in `core`, not inside the feature.

Onboarding does **not** need DataStore yet. Completing onboarding is still the
existing `onGetStarted` callback. Add a "has seen onboarding" preference later
when splash routing needs it.

---

## 3. Files to create or edit

Create these files:

```text
core/common/components/pager/AppHorizontalPager.kt
core/common/components/pager/AppPagerIndicator.kt
core/di/OnboardingModule.kt

features/onboarding/domain/model/OnboardingPageModel.kt
features/onboarding/domain/repository/OnboardingRepository.kt
features/onboarding/domain/usecase/GetOnboardingPagesUseCase.kt
features/onboarding/data/repository/OnboardingRepositoryImpl.kt
features/onboarding/presentation/model/OnboardingPageUiModel.kt
features/onboarding/presentation/logic/OnboardingUiState.kt
features/onboarding/presentation/logic/OnboardingEvent.kt
features/onboarding/presentation/logic/OnboardingViewModel.kt
features/onboarding/presentation/components/OnboardingPageItem.kt
features/onboarding/presentation/components/OnboardingScreenContent.kt
features/onboarding/presentation/screens/OnboardingScreen.kt   (replace)

res/drawable/img_onboarding_discover.xml
res/drawable/img_onboarding_checkout.xml
res/drawable/img_onboarding_shopping.xml
```

Edit these existing files (full contents are in this document):

```text
core/utils/constants/AppDimens.kt
res/values/strings.xml
res/values-fr/string-fr.xml
res/values-ar/string-ar.xml
core/navigation/AppNavHost.kt
```

---

## 4. Shared pager — `AppHorizontalPager`

This is the shared equivalent of Flutter `PageView` + `PageController`.
Any later feature (product gallery, banners) reuses this file.

**Location:** `app/src/main/java/com/example/dukkanapp/core/common/components/pager/AppHorizontalPager.kt`

```kotlin
package com.example.dukkanapp.core.common.components.pager

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppHorizontalPager(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = userScrollEnabled,
        verticalAlignment = verticalAlignment,
        pageContent = pageContent,
    )
}
```

Why this wrapper exists:

- call sites never import `HorizontalPager` directly;
- the pager API stays in `core`, next to `AppPrimaryButton`;
- `pagerState` is passed in, exactly like passing a Flutter `PageController`.

Create the controller in the **screen content**, not in the shared component:

```kotlin
val pagerState = rememberPagerState(
    initialPage = state.currentPage,
    pageCount = { state.pages.size },
)
```

---

## 5. Shared dots — `AppPagerIndicator`

**Location:** `app/src/main/java/com/example/dukkanapp/core/common/components/pager/AppPagerIndicator.kt`

```kotlin
package com.example.dukkanapp.core.common.components.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.dukkanapp.R
import com.example.dukkanapp.core.utils.constants.AppDimens

@Composable
fun AppPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.cd_pager_indicator,
        currentPage + 1,
        pageCount,
    )

    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(AppDimens.pagerIndicatorSize)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}
```

---

## 6. Domain

### 6.1 `OnboardingPageModel.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/domain/model/OnboardingPageModel.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.domain.model

data class OnboardingPageModel(
    val id: String,
)
```

`id` is a stable key (`discover`, `checkout`, `shopping`). Titles and images are
UI resources, so they belong in presentation, not here.

### 6.2 `OnboardingRepository.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/domain/repository/OnboardingRepository.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.domain.repository

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel

interface OnboardingRepository {
    fun getPages(): List<OnboardingPageModel>
}
```

### 6.3 `GetOnboardingPagesUseCase.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/domain/usecase/GetOnboardingPagesUseCase.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.domain.usecase

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import javax.inject.Inject

class GetOnboardingPagesUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {
    operator fun invoke(): List<OnboardingPageModel> = repository.getPages()
}
```

---

## 7. Data

### 7.1 `OnboardingRepositoryImpl.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/data/repository/OnboardingRepositoryImpl.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.data.repository

import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor() : OnboardingRepository {
    override fun getPages(): List<OnboardingPageModel> = listOf(
        OnboardingPageModel(id = "discover"),
        OnboardingPageModel(id = "checkout"),
        OnboardingPageModel(id = "shopping"),
    )
}
```

### 7.2 `OnboardingModule.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/core/di/OnboardingModule.kt`

```kotlin
package com.example.dukkanapp.core.di

import com.example.dukkanapp.features.onboarding.data.repository.OnboardingRepositoryImpl
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl,
    ): OnboardingRepository
}
```

Hilt picks this module up automatically. You do not register it in
`DukkanApplication`.

---

## 8. Presentation

### 8.1 `OnboardingPageUiModel.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/model/OnboardingPageUiModel.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.dukkanapp.R
import com.example.dukkanapp.features.onboarding.domain.model.OnboardingPageModel

data class OnboardingPageUiModel(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    @get:DrawableRes val illustrationRes: Int,
)

fun OnboardingPageModel.toUiModel(): OnboardingPageUiModel = when (id) {
    "discover" -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_discover_title,
        subtitleRes = R.string.onboarding_discover_subtitle,
        illustrationRes = R.drawable.img_onboarding_discover,
    )
    "checkout" -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_checkout_title,
        subtitleRes = R.string.onboarding_checkout_subtitle,
        illustrationRes = R.drawable.img_onboarding_checkout,
    )
    else -> OnboardingPageUiModel(
        id = id,
        titleRes = R.string.onboarding_shopping_title,
        subtitleRes = R.string.onboarding_shopping_subtitle,
        illustrationRes = R.drawable.img_onboarding_shopping,
    )
}
```

### 8.2 `OnboardingUiState.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/logic/OnboardingUiState.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

import com.example.dukkanapp.R
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel

data class OnboardingUiState(
    val pages: List<OnboardingPageUiModel> = emptyList(),
    val currentPage: Int = 0,
) {
    val isLastPage: Boolean
        get() = pages.isNotEmpty() && currentPage >= pages.lastIndex

    val primaryActionRes: Int
        get() = if (isLastPage) {
            R.string.action_get_started
        } else {
            R.string.action_next
        }
}
```

`primaryActionRes` is how the same button swaps **Next** → **Get Started**
without creating a second screen.

### 8.3 `OnboardingEvent.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/logic/OnboardingEvent.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingEvent {
    data class OnPageChanged(val page: Int) : OnboardingEvent
    data object OnNextClicked : OnboardingEvent
}
```

`OnGetStarted` and `OnLogin` are **navigation**. They stay as screen lambdas,
same as `onContinue` on language selection.

### 8.4 `OnboardingViewModel.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/logic/OnboardingViewModel.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.lifecycle.ViewModel
import com.example.dukkanapp.features.onboarding.domain.usecase.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    getOnboardingPages: GetOnboardingPagesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(
        OnboardingUiState(
            pages = getOnboardingPages().map { it.toUiModel() },
        )
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnPageChanged -> {
                _state.update { current ->
                    val safePage = event.page.coerceIn(0, current.pages.lastIndex)
                    if (current.currentPage == safePage) current
                    else current.copy(currentPage = safePage)
                }
            }

            OnboardingEvent.OnNextClicked -> {
                _state.update { current ->
                    if (current.isLastPage) current
                    else current.copy(currentPage = current.currentPage + 1)
                }
            }
        }
    }
}
```

`jakarta.inject.Inject` matches `LanguageSelectionViewModel`. Do not mix it with
`javax.inject.Inject` in ViewModels.

### 8.5 `OnboardingPageItem.kt`

One page inside the pager. This is the `itemBuilder` body.

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/components/OnboardingPageItem.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.dukkanapp.core.utils.constants.AppDimens
import com.example.dukkanapp.features.onboarding.presentation.model.OnboardingPageUiModel

@Composable
fun OnboardingPageItem(
    page: OnboardingPageUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(page.illustrationRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.onboardingIllustrationHeight),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
        Text(
            text = stringResource(page.subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
```

`contentDescription = null` is correct: the title already describes the page.

### 8.6 `OnboardingScreenContent.kt`

This is the Flutter `build` method: one scaffold, pager, dots, shared button,
login row.

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/components/OnboardingScreenContent.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.R
import com.example.dukkanapp.core.common.components.buttons.AppPrimaryButton
import com.example.dukkanapp.core.common.components.pager.AppHorizontalPager
import com.example.dukkanapp.core.common.components.pager.AppPagerIndicator
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
        pageCount = { state.pages.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                onEvent(OnboardingEvent.OnPageChanged(page))
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppHorizontalPager(
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            OnboardingPageItem(page = state.pages[page])
        }

        AppPagerIndicator(
            pageCount = state.pages.size,
            currentPage = state.currentPage,
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
            modifier = Modifier.padding(horizontal = AppDimens.screenHorizontalPadding),
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_already_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.action_log_in),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClick = onLogin)
                    .padding(start = AppDimens.space2Xs),
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
                        illustrationRes = R.drawable.img_onboarding_discover,
                    ),
                    OnboardingPageUiModel(
                        id = "checkout",
                        titleRes = R.string.onboarding_checkout_title,
                        subtitleRes = R.string.onboarding_checkout_subtitle,
                        illustrationRes = R.drawable.img_onboarding_checkout,
                    ),
                    OnboardingPageUiModel(
                        id = "shopping",
                        titleRes = R.string.onboarding_shopping_title,
                        subtitleRes = R.string.onboarding_shopping_subtitle,
                        illustrationRes = R.drawable.img_onboarding_shopping,
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
```

### 8.7 `OnboardingScreen.kt`

Thin route wrapper. Same pattern as `LanguageSelectionScreen`.

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/screens/OnboardingScreen.kt`

Replace the current placeholder with this entire file:

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.features.onboarding.presentation.components.OnboardingScreenContent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGetStarted = onGetStarted,
        onLogin = onLogin,
    )
}
```

---

## 9. Resources

### 9.1 `AppDimens.kt`

**Location:** `app/src/main/java/com/example/dukkanapp/core/utils/constants/AppDimens.kt`

Replace the file with this. Only two new values were added:
`onboardingIllustrationHeight` and `pagerIndicatorSize`.

```kotlin
package com.example.dukkanapp.core.utils.constants

import androidx.compose.ui.unit.dp

object AppDimens {
    // Spacing: one 4 dp scale
    val space2Xs = 4.dp
    val spaceXs = 8.dp
    val spaceSm = 12.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp
    val space2Xl = 48.dp

    // Corner radius
    val radiusXs = 4.dp
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 24.dp
    val radiusFull = 999.dp

    // Reusable component measurements
    val screenHorizontalPadding = 16.dp
    val circleButtonSize = 24.dp
    val buttonHeight = 45.dp
    val textFieldMinHeight = 45.dp
    val productImageHeight = 180.dp
    val bottomBarHeight = 80.dp
    val touchTargetMin = 48.dp
    val borderWidth = 1.dp
    val onboardingIllustrationHeight = 280.dp
    val pagerIndicatorSize = 8.dp

    // Image & Icon size
    val flagSize = 37.dp
    val iconXs = 16.dp
    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconXl = 48.dp
    val iconXxl = 64.dp
}
```

### 9.2 English — `res/values/strings.xml`

**Location:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">Dukkan App</string>
    <string name="language_selection_title">Choose the language</string>
    <string name="language_selection_subtitle">Select your preferred language below. This helps us serve you better.</string>
    <string name="language_selection_you_selected">You Selected</string>
    <string name="language_selection_all_languages">All Languages</string>
    <string name="language_selection_search_hint">Search</string>
    <string name="action_continue">Continue</string>
    <string name="cd_back">Back</string>

    <string name="onboarding_discover_title">Discover Our Products</string>
    <string name="onboarding_discover_subtitle">Browse thousands of products, from fashion to tech. Find what you love, effortlessly.</string>
    <string name="onboarding_checkout_title">Hassle-Free Checkout</string>
    <string name="onboarding_checkout_subtitle">Seamless payments and speedy delivery. Start shopping smarter today.</string>
    <string name="onboarding_shopping_title">Easy and Happy Shopping</string>
    <string name="onboarding_shopping_subtitle">Start shopping now and enjoy a world of convenience!</string>
    <string name="action_next">Next</string>
    <string name="action_get_started">Get Started</string>
    <string name="onboarding_already_have_account">Already have an account?</string>
    <string name="action_log_in">Log In</string>
    <string name="cd_pager_indicator">Page %1$d of %2$d</string>
</resources>
```

### 9.3 French — `res/values-fr/string-fr.xml`

**Location:** `app/src/main/res/values-fr/string-fr.xml`

```xml
<resources>
    <string name="app_name">Dukkan App</string>
    <string name="language_selection_title">Choisissez la langue</string>
    <string name="language_selection_subtitle">Sélectionnez votre langue préférée ci-dessous. Cela nous aide à mieux vous servir.</string>
    <string name="language_selection_you_selected">Vous avez sélectionné</string>
    <string name="language_selection_all_languages">Toutes les langues</string>
    <string name="language_selection_search_hint">Rechercher</string>
    <string name="action_continue">Continuer</string>
    <string name="cd_back">Retour</string>

    <string name="onboarding_discover_title">Découvrez nos produits</string>
    <string name="onboarding_discover_subtitle">Parcourez des milliers de produits, de la mode à la tech. Trouvez ce que vous aimez, sans effort.</string>
    <string name="onboarding_checkout_title">Paiement sans tracas</string>
    <string name="onboarding_checkout_subtitle">Paiements fluides et livraison rapide. Commencez à acheter plus intelligemment dès aujourd\'hui.</string>
    <string name="onboarding_shopping_title">Shopping facile et agréable</string>
    <string name="onboarding_shopping_subtitle">Commencez vos achats maintenant et profitez d\'un monde de commodité !</string>
    <string name="action_next">Suivant</string>
    <string name="action_get_started">Commencer</string>
    <string name="onboarding_already_have_account">Vous avez déjà un compte ?</string>
    <string name="action_log_in">Se connecter</string>
    <string name="cd_pager_indicator">Page %1$d sur %2$d</string>
</resources>
```

### 9.4 Arabic — `res/values-ar/string-ar.xml`

**Location:** `app/src/main/res/values-ar/string-ar.xml`

```xml
<resources>
    <string name="app_name">تطبيق دكان</string>
    <string name="language_selection_title">اختر اللغة</string>
    <string name="language_selection_subtitle">اختر لغتك المفضلة أدناه. هذا يساعدنا على خدمتك بشكل أفضل.</string>
    <string name="language_selection_you_selected">لقد اخترت</string>
    <string name="language_selection_all_languages">جميع اللغات</string>
    <string name="language_selection_search_hint">بحث</string>
    <string name="action_continue">متابعة</string>
    <string name="cd_back">رجوع</string>

    <string name="onboarding_discover_title">اكتشف منتجاتنا</string>
    <string name="onboarding_discover_subtitle">تصفح آلاف المنتجات، من الموضة إلى التقنية. اعثر على ما تحب بسهولة.</string>
    <string name="onboarding_checkout_title">دفع سهل وسريع</string>
    <string name="onboarding_checkout_subtitle">مدفوعات سلسة وتوصيل سريع. تسوّق بذكاء بدءًا من اليوم.</string>
    <string name="onboarding_shopping_title">تسوق سهل وممتع</string>
    <string name="onboarding_shopping_subtitle">ابدأ التسوق الآن واستمتع بعالم من الراحة!</string>
    <string name="action_next">التالي</string>
    <string name="action_get_started">ابدأ الآن</string>
    <string name="onboarding_already_have_account">لديك حساب بالفعل؟</string>
    <string name="action_log_in">تسجيل الدخول</string>
    <string name="cd_pager_indicator">الصفحة %1$d من %2$d</string>
</resources>
```

`HorizontalPager` and `start`/`end` padding already flip in Arabic. You do not
write extra RTL code.

---

## 10. Placeholder illustrations

Put these three vectors in `app/src/main/res/drawable/`.
They compile and preview today. Later, replace each file with the real artwork
(keep the **same resource names** so Kotlin does not change).

### 10.1 `img_onboarding_discover.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="280dp"
    android:height="280dp"
    android:viewportWidth="280"
    android:viewportHeight="280">
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M70,190 a40,18 0 1,0 80,0 a40,18 0 1,0 -80,0" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M96,188 m-18,0 a18,18 0 1,1 36,0 a18,18 0 1,1 -36,0" />
    <path
        android:fillColor="#78716C"
        android:pathData="M108,150 h24 v38 h-24 z" />
    <path
        android:fillColor="#44403C"
        android:pathData="M100,128 h40 v22 h-40 z" />
    <path
        android:fillColor="#E7E5E4"
        android:pathData="M168,70 h72 v130 h-72 z" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M184,88 h40 v40 h-40 z" />
    <path
        android:fillColor="#78716C"
        android:pathData="M190,148 h28 v12 h-28 z" />
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M230,210 h12 v28 h-12 z" />
</vector>
```

### 10.2 `img_onboarding_checkout.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="280dp"
    android:height="280dp"
    android:viewportWidth="280"
    android:viewportHeight="280">
    <path
        android:fillColor="#E7E5E4"
        android:pathData="M96,62 h88 v156 h-88 z" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M112,86 h56 v16 h-56 z" />
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M112,116 h56 v44 h-56 z" />
    <path
        android:fillColor="#78716C"
        android:pathData="M58,150 h36 v12 h-36 z" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M70,168 m-16,0 a16,16 0 1,1 32,0 a16,16 0 1,1 -32,0" />
    <path
        android:fillColor="#44403C"
        android:pathData="M78,184 h20 v28 h-20 z" />
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M210,168 h14 v40 h-14 z" />
</vector>
```

### 10.3 `img_onboarding_shopping.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="280dp"
    android:height="280dp"
    android:viewportWidth="280"
    android:viewportHeight="280">
    <path
        android:fillColor="#E7E5E4"
        android:pathData="M108,58 h72 v140 h-72 z" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M128,88 m-14,0 a14,14 0 1,1 28,0 a14,14 0 1,1 -28,0" />
    <path
        android:fillColor="#78716C"
        android:pathData="M150,150 h36 v10 h-36 z" />
    <path
        android:fillColor="#A8A29E"
        android:pathData="M168,168 m-18,0 a18,18 0 1,1 36,0 a18,18 0 1,1 -36,0" />
    <path
        android:fillColor="#44403C"
        android:pathData="M176,186 h22 v30 h-22 z" />
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M70,150 h20 v12 h-20 z" />
    <path
        android:fillColor="#D6D3D1"
        android:pathData="M210,150 h16 v40 h-16 z" />
</vector>
```

These placeholders use neutral greys only so they work in light and dark theme.
When you drop in the real illustrations, keep the file names.

---

## 11. Navigation — `AppNavHost.kt`

`OnboardingScreen` now needs `onLogin` as well as `onGetStarted`.
`AppRoutes` does not change.

**Location:** `app/src/main/java/com/example/dukkanapp/core/navigation/AppNavHost.kt`

```kotlin
package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.LanguageSelection
    ) {
        composable<AppRoutes.LanguageSelection> {
            LanguageSelectionScreen(
                onBack = {},
                onContinue = {
                    navController.navigate(AppRoutes.Onboarding) {
                        popUpTo(AppRoutes.LanguageSelection) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<AppRoutes.Onboarding> {
            OnboardingScreen(
                onGetStarted = {
                    // Navigate to Home later and pop Onboarding.
                },
                onLogin = {
                    // Navigate to Login later.
                }
            )
        }
    }
}
```

---

## 12. Copy order

Create files in this order so Android Studio can resolve imports as you go:

1. `AppDimens.kt` (add the two new dimens)
2. Three `strings.xml` files
3. Three placeholder drawables
4. `AppHorizontalPager.kt`
5. `AppPagerIndicator.kt`
6. Domain model + repository + use case
7. `OnboardingRepositoryImpl.kt`
8. `OnboardingModule.kt`
9. Presentation model, state, event, ViewModel
10. `OnboardingPageItem.kt`
11. `OnboardingScreenContent.kt`
12. Replace `OnboardingScreen.kt`
13. Update `AppNavHost.kt`
14. Sync Gradle, then Build

---

## 13. How to verify

1. Language selection → Continue opens this screen (same route as today).
2. Page 1 shows Discover title, **Next**, three dots with the first selected.
3. Swipe left (or right in Arabic) moves to Checkout. Dots update. Still the
   same `Onboarding` route.
4. Tap **Next** on page 1 and page 2. The pager animates. You do not navigate.
5. Page 3 button label is **Get Started**.
6. **Get Started** calls `onGetStarted`.
7. **Log In** calls `onLogin`.
8. Switch the app language to French and Arabic. Titles and the button change.
   The pager still works.
9. Open the three previews on `OnboardingScreenContent`.

---

## 14. What not to do

- Do not create `OnboardingPage1Screen`, `OnboardingPage2Screen`,
  `OnboardingPage3Screen`. That would be three routes. This feature is one
  route and one pager.
- Do not put `PagerState` or `HorizontalPager` in the ViewModel.
- Do not put `R.string` or `R.drawable` in `domain`.
- Do not recreate `AppPrimaryButton`. Import the existing one.
- Do not hardcode `"Next"` or `"Get Started"` in Kotlin.
- Do not add hex colors in the feature. Use `MaterialTheme.colorScheme`.
- Do not use `AppScaffold` here. That scaffold always draws a top bar. This
  mock has no back button and no app bar.

---

## 15. Definition of done

- Shared `AppHorizontalPager` and `AppPagerIndicator` live in `core`.
- Feature layers match language selection: `data` / `domain` / `presentation`.
- `OnboardingScreen` only collects state and forwards events.
- Three pages share one composable (`OnboardingPageItem`).
- The primary action uses `AppPrimaryButton`.
- Swipe and **Next** stay on `AppRoutes.Onboarding`.
- Last page uses **Get Started** and leaves through `onGetStarted`.
- Strings exist in `en`, `fr`, and `ar`.
- No raw `Color(0x...)` in the onboarding feature.
