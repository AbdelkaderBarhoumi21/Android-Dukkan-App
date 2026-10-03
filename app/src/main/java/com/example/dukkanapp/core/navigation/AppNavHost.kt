package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.EmailScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

/*
   NavHost (AppNavHost)      -> owns navigation only (lambdas: onFinished, onEmailClick...)
   OnboardingScreen          -> stateful: gets ViewModel, collects state, handles events
   OnboardingScreenContent   -> stateless: pure UI, takes state + onEvent
 */
@Composable
fun AppNavHost(startDestination: AppRoute) {
    val navController = rememberNavController()
    // Start at Language Selection
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Language Selection Screen
        composable<AppRoute.LanguageSelectionScreen> {
            LanguageSelectionScreen(
                onBack = {},
                onContinue = {
                    navController.navigate(AppRoute.OnboardingScreen) {
                        // Navigate to Onboarding and remove LanguageSelection from the backstack
                        popUpTo(AppRoute.LanguageSelectionScreen) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable<AppRoute.OnboardingScreen> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoute.LoginScreen> {
            LoginScreen(
                onEmailClick = {
                    navController.navigate(AppRoute.EmailScreen)
                },

                onGoogleClick = {},
                onAppleClick = {},
                onAlreadyHaveAccountClick = {}

            )

        }

        composable<AppRoute.HomeScreen> {

        }

        composable<AppRoute.EmailScreen> {
            EmailScreen(
                onForgetPasswordClick = {},
                onNavigationBack = {
                    navController.popBackStack()
                }
            )


        }
    }
}