package com.projectu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pixiv风格的颜色定义
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0096FA),         // Pixiv蓝
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E7FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFFFF6B9D),       // Pixiv粉
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3D0019),
    tertiary = Color(0xFF00D4AA),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFFA6C8FF),
    surfaceTint = Color(0xFF0096FA)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF00497E),
    onPrimaryContainer = Color(0xFFD4E7FF),
    secondary = Color(0xFFFFB1C8),
    onSecondary = Color(0xFF5F0029),
    secondaryContainer = Color(0xFF7D003E),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFF00BEAD),
    onTertiary = Color(0xFF003731),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8E9099),
    inverseOnSurface = Color(0xFF1A1C1E),
    inverseSurface = Color(0xFFE2E2E6),
    inversePrimary = Color(0xFF0096FA),
    surfaceTint = Color(0xFFA6C8FF)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

