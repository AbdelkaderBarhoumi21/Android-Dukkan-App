package com.example.dukkanapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import com.example.dukkanapp.core.navigation.startup.AppStartViewModel
import com.example.dukkanapp.features.settings.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appStartViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until we know where to go AND which theme to paint
        splashScreen.setKeepOnScreenCondition {
            appStartViewModel.startDestination.value == null ||
                    appStartViewModel.themeMode.value == null
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by appStartViewModel.themeMode.collectAsStateWithLifecycle()
            val destination by appStartViewModel.startDestination.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDarkTheme()

            SystemBarsEffect(darkTheme)

            AppTheme(darkTheme = darkTheme) {
                destination?.let { AppNavHost(startDestination = it) }
            }
        }
    }

    /** Status and navigation bar icons follow the app theme, not only the system.
     * Why not just call enableEdgeToEdge directly in the composable? Compose may re-run composable code many times.
     * A side effect placed there would run on every recomposition.
     * An effect with a key runs only when the key changes.
     * */
    @Composable
    private fun SystemBarsEffect(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                ) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                ) { darkTheme },
            )
            onDispose {}
        }
    }
}

@Composable
private fun ThemeMode?.isDarkTheme(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
}