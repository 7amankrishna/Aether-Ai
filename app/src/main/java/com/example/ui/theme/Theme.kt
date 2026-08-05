package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = TerracottaLight,
    background = CharcoalBackground,
    surface = CharcoalSurface,
    surfaceVariant = CharcoalCard,
    onBackground = CharcoalTextPrimary,
    onSurface = CharcoalTextPrimary,
    onSurfaceVariant = CharcoalTextSecondary,
    outline = CharcoalBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaPrimaryDark,
    secondary = TerracottaPrimaryDark,
    background = ParchmentBackground,
    surface = ParchmentSurface,
    surfaceVariant = ParchmentCard,
    onBackground = ParchmentTextPrimary,
    onSurface = ParchmentTextPrimary,
    onSurfaceVariant = ParchmentTextSecondary,
    outline = ParchmentBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our custom warm palette for consistent Claude vibe
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
