package com.mekarsari.kasir.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    ORANGE, BLUE, GREEN, PURPLE
}

private val OrangeColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeTertiary,
    background = LightGray,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = DarkGray,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

private val BlueColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = BlueTertiary,
    background = LightGray,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = DarkGray,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = LightGray,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = DarkGray,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

private val PurpleColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    tertiary = PurpleTertiary,
    background = LightGray,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = DarkGray,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

@Composable
fun MekarSariKasirTheme(
    theme: AppTheme = AppTheme.ORANGE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.ORANGE -> OrangeColorScheme
        AppTheme.BLUE -> BlueColorScheme
        AppTheme.GREEN -> GreenColorScheme
        AppTheme.PURPLE -> PurpleColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
