package com.example.dukkanapp.config.theme


import AppDarkColorScheme
import AppLightColorScheme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.example.dukkanapp.core.utils.constants.AppExtendedColors
import com.example.dukkanapp.core.utils.constants.AppShapes
import com.example.dukkanapp.core.utils.constants.DarkExtendedColors
import com.example.dukkanapp.core.utils.constants.LightExtendedColors
import com.example.dukkanapp.core.utils.constants.LocalAppExtendedColors

object AppTheme{
    val extendedColors: AppExtendedColors @Composable @ReadOnlyComposable get()= LocalAppExtendedColors.current
}
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean=false,
    content:@Composable () -> Unit
){
    val context=LocalContext.current
    val colorScheme = when{
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if(darkTheme){
                dynamicDarkColorScheme(context)
            }else{
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }

    val extendedColors = if(darkTheme){
        DarkExtendedColors
    }else{
        LightExtendedColors
    }

    CompositionLocalProvider(
        LocalAppExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }




}