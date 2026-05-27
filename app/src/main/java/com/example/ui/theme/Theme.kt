package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldQuill,
    onPrimary = Color(0xFF1E1400),
    primaryContainer = Color(0xFF2C2415),
    onPrimaryContainer = GoldQuillLight,
    secondary = GoldQuillLight,
    onSecondary = Color(0xFF1E1B15),
    secondaryContainer = SlateCard,
    onSecondaryContainer = Color.White,
    tertiary = CrimsonAccent,
    background = CharcoalDark,
    onBackground = Color(0xFFE2E2E6),
    surface = SlateCard,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF252932),
    onSurfaceVariant = Color(0xFFC3C6D1)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBEEDB),
    onPrimaryContainer = Color(0xFF522800),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5ECF2),
    onSecondaryContainer = Color(0xFF1E242B),
    tertiary = LightTertiary,
    background = PaperWhite,
    onBackground = InkSlate,
    surface = Color.White,
    onSurface = InkSlate,
    surfaceVariant = Color(0xFFF2EFE9),
    onSurfaceVariant = Color(0xFF5A4D3E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // We force standard styled palettes rather than dynamic color to maintain a professional Antique Novel feeling!
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
