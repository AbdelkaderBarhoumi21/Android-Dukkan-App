package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.core.navigation.AppNavHost
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
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
