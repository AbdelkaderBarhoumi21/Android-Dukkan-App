# App Startup & Navigation Flow Implementation Guide

Based on the optimized architecture, this document contains all the hand-written code you need to copy and paste to implement the derived-state navigation approach.

Instead of saving a generic "stepper", we save independent facts (`hasSelectedLanguage`, `hasSeenOnboarding`) and derive `isLoggedIn` dynamically. A UseCase decides the start destination upon app launch, and the Android Splash Screen API hides the blank screen during this swift evaluation.

---

## 1. Setup Splash Screen Dependency

Add the AndroidX Splash Screen API to your app module's dependencies.

**`app/build.gradle.kts`**
```kotlin
dependencies {
    // ... other dependencies
    implementation("androidx.core:core-splashscreen:1.0.1")
}
```
*(Remember to Sync Project with Gradle Files)*

---

## 2. Type-Safe Routes

Make sure your `AppRoute` has all the necessary screens.

**`app/src/main/java/com/example/dukkanapp/core/navigation/AppRoute.kt`**
```kotlin
package com.example.dukkanapp.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object LanguageSelection : AppRoute

    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Home : AppRoute
}
```

---

## 3. Language Feature Updates

Extend your existing language repository to expose whether a language was ever explicitly selected.

**`app/src/main/java/com/example/dukkanapp/features/language/domain/repository/LanguageRepository.kt`**
```kotlin
package com.example.dukkanapp.features.language.domain.repository

import com.example.dukkanapp.features.language.domain.model.LanguageModel
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun getSupportedLanguages(): List<LanguageModel>
    val selectedLanguageCode: Flow<String>
    suspend fun selectLanguage(code: String)
    
    // NEW: Check if the user has explicitly selected a language
    suspend fun hasSelectedLanguage(): Boolean
}
```

**`app/src/main/java/com/example/dukkanapp/features/language/data/repository/LanguageRepositoryImpl.kt`**
```kotlin
package com.example.dukkanapp.features.language.data.repository

import com.example.dukkanapp.features.language.data.datasource.LanguagePreferenceLocalDataSource
import com.example.dukkanapp.features.language.data.datasource.PlatformLocaleDataSource
import com.example.dukkanapp.features.language.domain.model.LanguageModel
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
    private val localDataStore: LanguagePreferenceLocalDataSource,
    private val platformLocaleDataSource: PlatformLocaleDataSource
) : LanguageRepository {
    override fun getSupportedLanguages(): List<LanguageModel> = listOf(
        LanguageModel("ar", "العربية"),
        LanguageModel("en", "English"),
        LanguageModel("fr", "Français"),
    )

    override val selectedLanguageCode: Flow<String> =
        localDataStore.savedLanguageCode.map { saved ->
            saved ?: platformLocaleDataSource.currentLocaleTag() ?: "en"
        }

    override suspend fun selectLanguage(code: String) {
        localDataStore.saveLanguageCode(code = code)
        platformLocaleDataSource.setAppLocal(code)
    }

    // NEW: Implements the check by looking directly at the raw saved value
    override suspend fun hasSelectedLanguage(): Boolean {
        return localDataStore.savedLanguageCode.first() != null
    }
}
```

---

## 4. Onboarding Feature (New Repository)

Create a repository to track whether the user has completed onboarding.

**`app/src/main/java/com/example/dukkanapp/features/onboarding/data/datasource/OnboardingPreferenceLocalDataSource.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingPreferenceLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val hasSeenOnboardingKey = booleanPreferencesKey("has_seen_onboarding")

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { prefs -> 
        prefs[hasSeenOnboardingKey] ?: false 
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[hasSeenOnboardingKey] = true }
    }
}
```

**`app/src/main/java/com/example/dukkanapp/features/onboarding/domain/repository/OnboardingRepository.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.domain.repository

interface OnboardingRepository {
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun setOnboardingCompleted()
}
```

**`app/src/main/java/com/example/dukkanapp/features/onboarding/data/repository/OnboardingRepositoryImpl.kt`**
```kotlin
package com.example.dukkanapp.features.onboarding.data.repository

import com.example.dukkanapp.features.onboarding.data.datasource.OnboardingPreferenceLocalDataSource
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val localDataSource: OnboardingPreferenceLocalDataSource
) : OnboardingRepository {

    override suspend fun hasSeenOnboarding(): Boolean {
        return localDataSource.hasSeenOnboarding.first()
    }

    override suspend fun setOnboardingCompleted() {
        localDataSource.setOnboardingCompleted()
    }
}
```

*(Don't forget to bind `OnboardingRepositoryImpl` to `OnboardingRepository` in your Hilt DI module!)*

---

## 5. Auth Feature Placeholder

If you don't have an Auth module yet, create a simple interface that you can implement later.

**`app/src/main/java/com/example/dukkanapp/features/auth/domain/repository/AuthRepository.kt`**
```kotlin
package com.example.dukkanapp.features.auth.domain.repository

interface AuthRepository {
    suspend fun isLoggedInOnce(): Boolean
}
```
*(For now, you can mock the implementation to always return `false` until login is built).*

---

## 6. App Startup Orchestration

This logic lives in the `app` module (e.g. `com.example.dukkanapp.startup`) because it needs access to multiple feature modules.

**`app/src/main/java/com/example/dukkanapp/startup/ResolveStartDestinationUseCase.kt`**
```kotlin
package com.example.dukkanapp.startup

import com.example.dukkanapp.core.navigation.AppRoute
import com.example.dukkanapp.features.auth.domain.repository.AuthRepository
import com.example.dukkanapp.features.language.domain.repository.LanguageRepository
import com.example.dukkanapp.features.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class ResolveStartDestinationUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppRoute = coroutineScope {
        // Run reads concurrently for maximum performance during app startup
        val hasLanguageDeferred = async { languageRepository.hasSelectedLanguage() }
        val hasOnboardingDeferred = async { onboardingRepository.hasSeenOnboarding() }
        val isLoggedInDeferred = async { authRepository.isLoggedInOnce() }

        when {
            !hasLanguageDeferred.await() -> AppRoute.LanguageSelection
            !hasOnboardingDeferred.await() -> AppRoute.Onboarding
            !isLoggedInDeferred.await() -> AppRoute.Login
            else -> AppRoute.Home
        }
    }
}
```

**`app/src/main/java/com/example/dukkanapp/startup/AppStartViewModel.kt`**
```kotlin
package com.example.dukkanapp.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukkanapp.core.navigation.AppRoute
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
    private val _startDestination = MutableStateFlow<AppRoute?>(null)
    val startDestination: StateFlow<AppRoute?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value = resolveStartDestination()
        }
    }
}
```

---

## 7. Main Activity & Splash Screen

Use the official Splash Screen API to hold the splash screen until our destination is resolved.

**`app/src/main/java/com/example/dukkanapp/MainActivity.kt`**
```kotlin
package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import com.example.dukkanapp.startup.AppStartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Get the ViewModel at the activity level to check the destination
    private val appStartViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep the splash screen on-screen until the start destination is decided
        splashScreen.setKeepOnScreenCondition { 
            appStartViewModel.startDestination.value == null 
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                val destination = appStartViewModel.startDestination.value
                // Once it's not null, we render the NavHost
                if (destination != null) {
                    AppNavHost(startDestination = destination)
                }
            }
        }
    }
}
```

---

## 8. App Navigation Host

Update your `AppNavHost` to accept the calculated `startDestination`.

**`app/src/main/java/com/example/dukkanapp/core/navigation/AppNavHost.kt`**
```kotlin
package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

@Composable
fun AppNavHost(startDestination: AppRoute) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = startDestination
    ) {
        composable<AppRoute.LanguageSelection> {
            LanguageSelectionScreen(
                onBack = { /* handle back */ },
                onContinue = {
                    navController.navigate(AppRoute.Onboarding) {
                        popUpTo(AppRoute.LanguageSelection) { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoute.Onboarding> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        
        composable<AppRoute.Login> {
            // LoginScreen()
        }

        composable<AppRoute.Home> {
            // HomeScreen()
        }
    }
}
```

---

## 9. Onboarding Screen Cleanup

Update your `OnboardingScreen.kt` to handle the consolidated routing efficiently.

**`app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/screens/OnboardingScreen.kt`**
*(Your exact file path might vary depending on how you structured it. Update the events to call `onFinished`)*

```kotlin
// Inside your OnboardingScreen composable...
OnboardingScreenContent(
    state = state,
    onEvent = { event ->
        when (event) {
            // Merge both terminal events into a single branch
            OnboardingEvent.OnGetStartedClicked,
            OnboardingEvent.OnLoginClicked -> {
                viewModel.onEvent(event) // Make sure ViewModel calls setOnboardingCompleted()
                onFinished() // Bubbles up to AppNavHost to navigate
            }
            else -> viewModel.onEvent(event) // Handle normal page changes
        }
    }
)
```
```kotlin
// Inside OnboardingViewModel
OnboardingEvent.OnGetStartedClicked,
OnboardingEvent.OnLoginClicked -> {
    viewModelScope.launch {
        onboardingRepository.setOnboardingCompleted()
    }
}
```