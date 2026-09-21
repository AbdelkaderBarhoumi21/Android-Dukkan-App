package com.example.dukkanapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.dukkanapp.core.config.theme.AppTheme
import com.example.dukkanapp.features.language.presentation.screens.LanguageSelectionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanguageSelectionScreen(
                onBack = {},
                onContinue = {}
            )

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