package com.example.dukkanapp.core.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

// Without uiMode, AppTheme reads isSystemInDarkTheme() as false, so you get a light screen twice.
@Preview(name = "Light", showBackground = true, widthDp = 393)
@Preview(
    name = "Dark", showBackground = true, widthDp = 393,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Arabic", showBackground = true, widthDp = 393, locale = "ar")
annotation class AppPreviews