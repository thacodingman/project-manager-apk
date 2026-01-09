package com.example.projectmanager.ui.theme
import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = TextBlack,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = GrayText,
    secondary = NeonGreen,
    onSecondary = TextBlack,
    tertiary = Color(0xFF00FF88),
    onTertiary = TextBlack,
    outline = DarkOutline,
    outlineVariant = Color(0xFF4C4C4C),
    error = Color(0xFFFF5252),
    onError = TextBlack,
    primaryContainer = Color(0xFF1A3A1A),
    onPrimaryContainer = NeonGreen,
    secondaryContainer = DarkCard,
    onSecondaryContainer = TextWhite,
    tertiaryContainer = Color(0xFF1A2A2A),
    onTertiaryContainer = Color(0xFF00FF88),
    errorContainer = Color(0xFF3A1A1A),
    onErrorContainer = Color(0xFFFF8888)
)
private val LightColorScheme = lightColorScheme(
    primary = NeonGreen,
    secondary = Color(0xFF00CC00),
    tertiary = Color(0xFF00FF88)
)
@Composable
fun ProjectManagerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}