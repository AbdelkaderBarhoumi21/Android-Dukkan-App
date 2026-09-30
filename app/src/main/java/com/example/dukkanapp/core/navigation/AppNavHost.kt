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
    // Start at Language Selection
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Language Selection Screen
        composable<AppRoute.LanguageSelection> {
            LanguageSelectionScreen(
                onBack = {},
                onContinue = {
                    navController.navigate(AppRoute.Onboarding) {
                        // Navigate to Onboarding and remove LanguageSelection from the backstack
                        popUpTo(AppRoute.LanguageSelection) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable<AppRoute.Onboarding> {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.Onboarding) { inclusive = true }
                    }
                },
                onLogin = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoute.Login> {

        }

        composable<AppRoute.Home> {

        }
    }
}