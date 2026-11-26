package com.example.dukkanapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dukkanapp.features.auth.presentation.screens.AuthOptionsScreen
import com.example.dukkanapp.features.auth.presentation.screens.ForgetPasswordScreen
import com.example.dukkanapp.features.auth.presentation.screens.LoginScreen
import com.example.dukkanapp.features.auth.presentation.screens.ResetPasswordScreen
import com.example.dukkanapp.features.auth.presentation.screens.SignUpScreen
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import com.example.dukkanapp.features.onboarding.presentation.screens.OnboardingScreen

/*
   AppNavHost                -> HOW to navigate (popUpTo, launchSingleTop). Knows routes.
   XxxScreen                 -> glue: collects state + effects, maps effects to the lambdas below
   XxxViewModel              -> WHEN and WHERE to navigate (sends an Effect). Never sees NavController.
   XxxScreenContent          -> stateless UI: state + onIntent
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

                onNavigationToOnboarding = {
                    navController.navigate(AppRoute.OnboardingScreen) {
                        popUpTo(AppRoute.LanguageSelectionScreen) { inclusive = true }
                    }
                }
            )
        }

        //  Onboarding Screen
        composable<AppRoute.OnboardingScreen> {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                },
                onNavigateToAuthOptions = {
                    navController.navigate(AppRoute.AuthOptionsScreen) {
                        popUpTo(AppRoute.OnboardingScreen) { inclusive = true }
                    }
                }
            )
        }
        // Auth options screen
        composable<AppRoute.AuthOptionsScreen> {
            AuthOptionsScreen(
                onNavigateToSignUp = {
                    navController.navigate(AppRoute.SignUpScreen) { launchSingleTop = true }
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen) { launchSingleTop = true }
                },
            )
        }
        // Home screen
        composable<AppRoute.HomeScreen> {

        }
        composable<AppRoute.LoginScreen> {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateAsRoot(AppRoute.HomeScreen) },
                onNavigateToSignUp = {
                    navController.navigate(AppRoute.SignUpScreen) {
                        popUpTo(AppRoute.LoginScreen) { inclusive = true }
                        launchSingleTop = true // no second copy if it's already on top
                    }
                },
                onNavigateToForgetPassword = { navController.navigate(AppRoute.ForgetPasswordScreen) },
            )
        }

        composable<AppRoute.SignUpScreen> {
            SignUpScreen(
                onNavigationBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoute.LoginScreen)
                },
                onNavigateToEmailVerification = { emailJustSent ->
                    navController.navigateAsRoot(AppRoute.EmailVerificationScreen(emailJustSent))
                }
            )
        }
        composable<AppRoute.EmailVerificationScreen> {
            // TODO: EmailVerificationScreen
        }
        composable<AppRoute.ForgetPasswordScreen> {
            ForgetPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChangePassword = {
                    navController.navigate(AppRoute.ResetPasswordScreen)
                }
            )
        }
        composable<AppRoute.ResetPasswordScreen> {
            ResetPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(AppRoute.LoginScreen) }
            )
        }

    }


}


private fun NavController.test(route: AppRoute) {
    navigate(
        route
    ) {
        popUpTo(graph.id) {
            inclusive = true
        }
    }
}

/** [route] becomes the only screen: "back" exits the app instead of returning to the auth forms. */
private fun NavController.navigateAsRoot(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}