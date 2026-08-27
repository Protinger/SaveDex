package com.savedex.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = SaveDexPrimary,
    onPrimary = SaveDexOnPrimary,
    secondary = SaveDexSecondary,
    onSecondary = SaveDexOnSecondary,
    background = SaveDexOnBackground,
    onBackground = SaveDexBackground,
)

private val LightColors = lightColorScheme(
    primary = SaveDexPrimary,
    onPrimary = SaveDexOnPrimary,
    secondary = SaveDexSecondary,
    onSecondary = SaveDexOnSecondary,
    background = SaveDexBackground,
    onBackground = SaveDexOnBackground,
)

@Composable
fun SaveDexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaveDexTypography,
        content = content,
    )
}
