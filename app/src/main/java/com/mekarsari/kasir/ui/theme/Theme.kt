package com.mekarsari.kasir.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// App-wide shape definitions for design consistency
private val AppShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(12.dp),      // TextFields and small inputs
    medium = RoundedCornerShape(16.dp),     // Cards and containers
    large = RoundedCornerShape(20.dp),      // Large components
    extraLarge = RoundedCornerShape(24.dp)  // Dialogs, Bottom Sheets
)

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
    outline = BorderGray,
    outlineVariant = BorderGray
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
    outline = BorderGray,
    outlineVariant = BorderGray
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
    outline = BorderGray,
    outlineVariant = BorderGray
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
    outline = BorderGray,
    outlineVariant = BorderGray
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
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

