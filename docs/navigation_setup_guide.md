# Jetpack Compose Type-Safe Navigation Setup

This guide provides the complete code needed to implement type-safe navigation in your app using `navigation-compose` 2.8+ and Kotlin Serialization.

## 1. Version Catalog (`gradle/libs.versions.toml`)
Define the navigation and serialization dependencies.

```toml
[versions]
# ... existing versions
navigationCompose = "2.8.7"
kotlinxSerializationJson = "1.7.3"

[libraries]
# ... existing libraries
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

[plugins]
# ... existing plugins
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

## 2. App Build Gradle (`app/build.gradle.kts`)
Apply the serialization plugin and add the dependencies to your app module.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    // Add the serialization plugin
    alias(libs.plugins.kotlin.serialization)
}

// ... android block ...

dependencies {
    // ... existing dependencies

    // Navigation & Serialization
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
```

> [!IMPORTANT]
> Make sure to click **"Sync Now"** in Android Studio after making these Gradle changes.

---

## 3. Type-Safe Routes (`AppRoute.kt`)
Create this file in your core navigation package to define all screens in your app.

**Location:** `app/src/main/java/com/example/dukkanapp/core/navigation/AppRoute.kt`

```kotlin
package com.example.dukkanapp.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    
    @Serializable
    data object LanguageSelection : AppRoute

    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Home : AppRoute
}
```

---

## 4. Onboarding Screen (`OnboardingScreen.kt`)
Create a placeholder screen so we have somewhere to navigate to.

**Location:** `app/src/main/java/com/example/dukkanapp/features/onboarding/presentation/screens/OnboardingScreen.kt`

```kotlin
package com.example.dukkanapp.features.onboarding.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onFinished) {
            Text("Finish Onboarding & Go Home")
        }
    }
}
```

---

## 5. Navigation Host (`AppNavHost.kt`)
This is the core component that connects your screens to your routes.

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

    // Start at Language Selection
    NavHost(
        navController = navController, 
        startDestination = AppRoute.LanguageSelection
    ) {
        
        // 1. Language Selection Screen
        composable<AppRoute.LanguageSelection> {
            LanguageSelectionScreen(
                onBack = { 
                    // Do nothing or exit app, since this is the first screen
                },
                onContinue = {
                    // Navigate to Onboarding and remove LanguageSelection from the backstack
                    navController.navigate(AppRoute.Onboarding) {
                        popUpTo(AppRoute.LanguageSelection) { inclusive = true }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable<AppRoute.Onboarding> {
            OnboardingScreen(
                onFinished = {
                    // Navigate to Home and remove Onboarding from the backstack
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        // 3. Home Screen (Placeholder)
        composable<AppRoute.Home> {
            // Replace with your actual Home Screen composable later
            androidx.compose.material3.Text("Welcome to Home Screen!")
        }
    }
}
```

---

## 6. Main Activity (`MainActivity.kt`)
Update your `MainActivity` to load the `AppNavHost`.

**Location:** `app/src/main/java/com/example/dukkanapp/MainActivity.kt`

```kotlin
package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                // The NavHost now handles showing the correct screen
                AppNavHost()
            }
        }
    }
}
```

User rotates phone
   ↓
Android: "I need to save the current screen"
   ↓
calls the GENERATED serialize() function (written by the plugin, at build time)
   ↓
that generated function calls into the LIBRARY's real Json/Encoder classes (running now, on the phone)
   ↓
Onboarding gets turned into bytes, saved
   ↓
phone finishes rotating → bytes get turned back into Onboarding, same process reversed