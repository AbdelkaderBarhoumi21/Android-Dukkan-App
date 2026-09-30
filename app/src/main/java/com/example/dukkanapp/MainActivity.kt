package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import com.example.dukkanapp.core.navigation.startup.AppStartViewModel
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Get the ViewModel at the activity level to check the destination
    private val appStartViewModel: AppStartViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash screen on-screen until the start destination is decided
        splashScreen.setKeepOnScreenCondition { appStartViewModel.startDestination.value == null }
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val destinationState by appStartViewModel.startDestination.collectAsStateWithLifecycle()
                val destination = destinationState                   // Once it's not null, we render the NavHost
                if (destination != null) {
                    AppNavHost(startDestination = destination)
                }

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppTheme {
        LanguageSelectionScreen(
            onBack = {},
            onContinue = {}
        )
    }
}
