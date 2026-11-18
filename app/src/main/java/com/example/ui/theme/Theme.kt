package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// True Black (OLED) Farbschema mit striktem #000000 Hintergrund
private val TrueBlackColorScheme = darkColorScheme(
    primary = ChargingCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = Color(0xFF80DEEA),
    secondary = ElectricGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00391A),
    onSecondaryContainer = Color(0xFF81C784),
    tertiary = WarningAmber,
    onTertiary = Color.Black,
    background = TrueBlack,
    onBackground = Color(0xFFF1F5F9),
    surface = TrueBlack,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TrueBlackSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = TrueBlackCard,
    outline = TrueBlackCardBorder,
    error = AlertRed,
    onError = Color.White
)

// Standard Dark Farbschema
private val StandardDarkColorScheme = darkColorScheme(
    primary = ChargingCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C243B),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = ElectricGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0B2E1C),
    onSecondaryContainer = Color(0xFFA5D6A7),
    tertiary = WarningAmber,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = DarkCard,
    outline = DarkCardBorder,
    error = AlertRed,
    onError = Color.White
)

// Standard Light Farbschema
private val StandardLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = LightCard,
    outline = LightCardBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun AmpereMasterTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        ThemeMode.TRUE_BLACK -> TrueBlackColorScheme
        ThemeMode.DARK -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                StandardDarkColorScheme
            }
        }
        ThemeMode.LIGHT -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicLightColorScheme(context)
            } else {
                StandardLightColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Bei TrueBlack und Dark sind helle Icons auf dunkler Statusleiste erwünscht
                val isDark = themeMode != ThemeMode.LIGHT
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
