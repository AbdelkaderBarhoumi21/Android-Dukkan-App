package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.AuthOptionsScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.auth.presentation.screens.SignUpScreen
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
        //  Language Selection Screen
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

        //  Onboarding Screen
        composable<AppRoute.OnboardingScreen> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(AppRoute.AuthOptionsScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                }
            )
        }
        // Auth options screen
        composable<AppRoute.AuthOptionsScreen> {
            AuthOptionsScreen(
                onEmailClick = {
                    navController.navigate(AppRoute.SignUpScreen)
                },

                onGoogleClick = {},
                onAppleClick = {},
                onAlreadyHaveAccountClick = {
                    navController.navigate(AppRoute.LoginScreen)
                }

            )

        }
        // Home screen
        composable<AppRoute.HomeScreen> {

        }
        // Login Screen
        composable<AppRoute.LoginScreen> {
            LoginScreen(
                onForgetPasswordClick = {},
                onNavigationBack = {
                    navController.popBackStack()
                },
                onSignupClick = {
                    navController.navigate(AppRoute.SignUpScreen) {
                        popUpTo(AppRoute.LoginScreen) {
                            inclusive = true
                        }
                        launchSingleTop =
                            true // prevent pushing a second copy of that screen if that screen already in the top
                    }
                }
            )
        }

        // SignUp screen
        composable<AppRoute.SignUpScreen> {
            SignUpScreen(
                onNavigationBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(AppRoute.HomeScreen) {
                        // Todo
                    }
                },
                onForgetPasswordClick = {},
                onLoginClick = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.SignUpScreen) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}