# App Startup & Navigation Flow Recommended Architecture

## The "Stepper" vs. "Derived State" Approach

You mentioned the idea of saving a stepper (e.g., `0 = Language`, `1 = Onboarding`, `2 = Login`). While this sounds intuitive at first, **it is generally not the recommended approach** in modern Android development.

### Why not use a saved integer (stepper)?
If you save an integer like `step = 2` when the user reaches the Login screen, what happens when the user successfully logs in and later **logs out**? You would have to remember to manually reset `step = 2`. If you add more steps later (e.g., Email Verification), managing this integer becomes a source of bugs.

### The Recommended Way: Derived State from Independent Flags
Instead of saving "which screen the user should be on", we save **independent facts** about the user:
1. `hasSelectedLanguage` (Saved permanently in DataStore)
2. `hasSeenOnboarding` (Saved permanently in DataStore)
3. `isLoggedIn` (Derived from your Auth Token/Session, usually cleared on logout)

When the app starts, we run a simple `if/else` check (our `ResolveStartDestinationUseCase`) to determine where they should go. This means if a user logs out, `isLoggedIn` becomes false, and the app naturally routes them to `Login` without us having to manually reverse a "stepper".

---

## Complete Implementation Code

Below is the complete, well-detailed code required to implement this architecture.

### 1. DataStore Preferences (The Saved Facts)

**`LanguagePreferences.kt`**
```kotlin
package com.example.dukkanapp.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguagePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")

    val hasSelectedLanguage: Flow<Boolean> =
        dataStore.data.map { it[SELECTED_LANGUAGE] != null }

    suspend fun hasSelectedLanguageOnce(): Boolean = hasSelectedLanguage.first()

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[SELECTED_LANGUAGE] = code }
    }
}
```

**`OnboardingPreferences.kt`**
```kotlin
package com.example.dukkanapp.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

    val hasSeenOnboarding: Flow<Boolean> =
        dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }

    suspend fun hasSeenOnboardingOnce(): Boolean = hasSeenOnboarding.first()

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[HAS_SEEN_ONBOARDING] = true }
    }
}
```

### 2. Auth State (The Dynamic Fact)

**`AuthRepository.kt`**
```kotlin
package com.example.dukkanapp.core.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    
    // Reads the current state once (useful for the splash screen decision)
    suspend fun isLoggedInOnce(): Boolean
}
```

### 3. Navigation Decision Logic

**`StartDestination.kt`**
```kotlin
package com.example.dukkanapp.core.navigation

enum class StartDestination(val route: String) {
    LANGUAGE_SELECTION("language_selection"),
    ONBOARDING("onboarding"),
    LOGIN("login"),
    HOME("home")
}
```

**`ResolveStartDestinationUseCase.kt`**
This is the brain of the operation. It checks the flags in order.
```kotlin
package com.example.dukkanapp.core.domain.usecases

import com.example.dukkanapp.core.data.local.LanguagePreferences
import com.example.dukkanapp.core.data.local.OnboardingPreferences
import com.example.dukkanapp.core.domain.auth.AuthRepository
import com.example.dukkanapp.core.navigation.StartDestination
import javax.inject.Inject

class ResolveStartDestinationUseCase @Inject constructor(
    private val languagePreferences: LanguagePreferences,
    private val onboardingPreferences: OnboardingPreferences,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): StartDestination {
        val hasSelectedLanguage = languagePreferences.hasSelectedLanguageOnce()
        val hasSeenOnboarding = onboardingPreferences.hasSeenOnboardingOnce()
        val isLoggedIn = authRepository.isLoggedInOnce()

        return when {
            !hasSelectedLanguage -> StartDestination.LANGUAGE_SELECTION
            !hasSeenOnboarding -> StartDestination.ONBOARDING
            !isLoggedIn -> StartDestination.LOGIN
            else -> StartDestination.HOME
        }
    }
}
```

### 4. App-Level ViewModel

**`AppStartViewModel.kt`**
```kotlin
package com.example.dukkanapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.domain.usecases.ResolveStartDestinationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val resolveStartDestination: ResolveStartDestinationUseCase,
) : ViewModel() {

    // Null means we are still loading/deciding
    private val _startDestination = MutableStateFlow<StartDestination?>(null)
    val startDestination: StateFlow<StartDestination?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value = resolveStartDestination()
        }
    }
}
```

### 5. Navigation Host

**`AppNavHost.kt`**
```kotlin
package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(
    appStartViewModel: AppStartViewModel = hiltViewModel()
) {
    val startDestination by appStartViewModel.startDestination.collectAsState()

    // Show a splash screen or loading indicator while we read DataStore
    if (startDestination == null) {
        // SplashScreen() 
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = startDestination!!.route
    ) {
        composable(StartDestination.LANGUAGE_SELECTION.route) { 
            // Pass navController to LanguageSelectionScreen so it can navigate and save language
            // LanguageSelectionScreen(navController) 
        }
        composable(StartDestination.ONBOARDING.route) { 
            OnboardingScreen(navController) 
        }
        composable(StartDestination.LOGIN.route) { 
            // LoginScreen(navController) 
        }
        composable(StartDestination.HOME.route) { 
            // HomeScreen(navController) 
        }
    }
}
```

### 6. Onboarding Feature Updates

**`OnboardingEvent.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

sealed interface OnboardingEvent {
    data class OnPageChanged(val page: Int) : OnboardingEvent
    object OnNextClicked : OnboardingEvent
    object OnGetStartedClicked : OnboardingEvent
    object OnLoginClicked : OnboardingEvent
}
```

**`OnboardingViewModel.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.presentation.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.data.local.OnboardingPreferences
import com.example.dukkanapp.features.onboarding.domain.usecases.GetOnboardingPagesUseCase
import com.example.dukkanapp.features.onboarding.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getOnboardingPages: GetOnboardingPagesUseCase,
    private val onboardingPreferences: OnboardingPreferences,
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

            is OnboardingEvent.OnNextClicked -> {
                _state.update { current ->
                    if (current.isLastPage) current
                    else current.copy(currentPage = current.currentPage + 1)
                }
            }

            OnboardingEvent.OnGetStartedClicked,
            OnboardingEvent.OnLoginClicked -> {
                // Save that the user has completed onboarding!
                viewModelScope.launch {
                    onboardingPreferences.setOnboardingCompleted()
                }
            }
        }
    }
}
```

**`OnboardingScreen.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dukkanapp.core.navigation.StartDestination
import com.example.dukkanapp.features.onboarding.presentation.components.OnboardingScreenContent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingEvent
import com.example.dukkanapp.features.onboarding.presentation.logic.OnboardingViewModel

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    OnboardingScreenContent(
        state = state,
        onEvent = { event ->
            when (event) {
                OnboardingEvent.OnGetStartedClicked -> {
                    viewModel.onEvent(event) // This saves the preference
                    // Go to Login, remove Onboarding from backstack
                    navController.navigate(StartDestination.LOGIN.route) {
                        popUpTo(StartDestination.ONBOARDING.route) { inclusive = true }
                    }
                }
                OnboardingEvent.OnLoginClicked -> {
                    viewModel.onEvent(event) // This saves the preference
                    // Go to Login, remove Onboarding from backstack
                    navController.navigate(StartDestination.LOGIN.route) {
                        popUpTo(StartDestination.ONBOARDING.route) { inclusive = true }
                    }
                }
                else -> viewModel.onEvent(event) // Handle normal page changes
            }
        },
    )
}
```

---

### Summary of How This Works
1. When the app is fresh, it hits the `ResolveStartDestinationUseCase`.
2. It sees `hasSelectedLanguage` is `false`. It goes to `LANGUAGE_SELECTION`.
3. User selects language, you save it via `LanguagePreferences`. Navigate to `ONBOARDING`.
4. User clicks "Get Started". `OnboardingViewModel` calls `setOnboardingCompleted()`. Navigates to `LOGIN`.
5. User closes the app and reopens.
6. `ResolveStartDestinationUseCase` runs again.
   - `hasSelectedLanguage` -> `true`
   - `hasSeenOnboarding` -> `true`
   - `isLoggedIn` -> `false`
7. It goes directly to `LOGIN`.

This is standard, completely robust, and prevents you from ever dealing with a desynced "stepper" variable.