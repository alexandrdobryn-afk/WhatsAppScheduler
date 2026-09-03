package com.example.wascheduler.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val Teal40 = Color(0xFF006A60)
private val Teal80 = Color(0xFF78D7C8)
private val BlueGrey40 = Color(0xFF4F5F6B)
private val BlueGrey80 = Color(0xFFB8C8D5)
private val Amber40 = Color(0xFF715C00)
private val Amber80 = Color(0xFFE2C557)

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9AF3E3),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = BlueGrey40,
    secondaryContainer = Color(0xFFD3E4F1),
    onSecondaryContainer = Color(0xFF0B1D27),
    tertiary = Amber40,
    tertiaryContainer = Color(0xFFFFE17D),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFAFBFA),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFAFBFA),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outlineVariant = Color(0xFFBEC9C5)
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF9AF3E3),
    secondary = BlueGrey80,
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD3E4F1),
    tertiary = Amber80,
    tertiaryContainer = Color(0xFF554500),
    onTertiaryContainer = Color(0xFFFFE17D),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outlineVariant = Color(0xFF3F4946)
)

private val WaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val WaTypography = Typography()

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun WaSchedulerTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WaTypography,
        shapes = WaShapes,
        content = content
    )
}
