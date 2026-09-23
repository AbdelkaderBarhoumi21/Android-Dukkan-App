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
        startDestination = AppRoutes.LanguageSelection
    ) {
        // 1. Language Selection Screen
        composable<AppRoutes.LanguageSelection> {
            LanguageSelectionScreen(
                onBack = {},
                onContinue = {
                    navController.navigate(AppRoutes.Onboarding) {
                        // Navigate to Onboarding and remove LanguageSelection from the backstack
                        popUpTo(AppRoutes.LanguageSelection) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable<AppRoutes.Onboarding> {
            OnboardingScreen(
                onGetStarted = {
                    // We will handle navigating to the Home screen here later
                }
            )
        }
    }
}