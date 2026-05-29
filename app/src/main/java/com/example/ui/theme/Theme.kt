package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// --- Dynamic Reactive Theme Accent State ---
val AppThemeState = mutableStateOf("purple") // Deep Royal Purple is the default now!

fun loadPersistedTheme(context: Context): String {
    val prefs = context.getSharedPreferences("musewriter_settings", Context.MODE_PRIVATE)
    return prefs.getString("selected_theme", "purple") ?: "purple"
}

fun savePersistedTheme(context: Context, theme: String) {
    val prefs = context.getSharedPreferences("musewriter_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("selected_theme", theme).apply()
    AppThemeState.value = theme
}

// ==========================================
// 1. Royal Purple Theme
// ==========================================
private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF211B2F),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF13101E),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1A162B),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2240),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFDF8FD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

// ==========================================
// 2. Deep Ocean Blue Theme
// ==========================================
private val BlueDark = darkColorScheme(
    primary = Color(0xFF78A9FF),
    onPrimary = Color(0xFF00278A),
    primaryContainer = Color(0xFF0038A6),
    onPrimaryContainer = Color(0xFFD0E1FD),
    secondary = Color(0xFF82CFFF),
    onSecondary = Color(0xFF003250),
    secondaryContainer = Color(0xFF101B2E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFC5C5FC),
    background = Color(0xFF0B111A),
    onBackground = Color(0xFFE0E2EC),
    surface = Color(0xFF141A25),
    onSurface = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFF202B3C),
    onSurfaceVariant = Color(0xFFC3D1E0)
)

private val BlueLight = lightColorScheme(
    primary = Color(0xFF0F62FE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E1FD),
    onPrimaryContainer = Color(0xFF001550),
    secondary = Color(0xFF00539C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0ECFF),
    onSecondaryContainer = Color(0xFF001438),
    tertiary = Color(0xFF3F51B5),
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E)
)

// ==========================================
// 3. Sleek Monochrome Slate Theme (White/Black)
// ==========================================
private val SlateDark = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color(0xFFE5E5EA),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1C1C1E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF3A3A3C),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE5E5EA),
    surface = Color(0xFF111112),
    onSurface = Color(0xFFE5E5EA),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2)
)

private val SlateLight = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF48484A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2F2F7),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFF8E8E93),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF636366)
)

// ==========================================
// 4. Crimson Cherry Theme
// ==========================================
private val RedDark = darkColorScheme(
    primary = Color(0xFFFFB3B8),
    onPrimary = Color(0xFF680016),
    primaryContainer = Color(0xFF920023),
    onPrimaryContainer = Color(0xFFFCD3DE),
    secondary = Color(0xFFE6BFC1),
    onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF281416),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFEFA9B1),
    background = Color(0xFF1A1011),
    onBackground = Color(0xFFECE0E0),
    surface = Color(0xFF241517),
    onSurface = Color(0xFFECE0E0),
    surfaceVariant = Color(0xFF3A2022),
    onSurfaceVariant = Color(0xFFD8C2C3)
)

private val RedLight = lightColorScheme(
    primary = Color(0xFFBC002D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCD3DE),
    onPrimaryContainer = Color(0xFF41000B),
    secondary = Color(0xFF775657),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE0E0),
    onSecondaryContainer = Color(0xFF2C1517),
    tertiary = Color(0xFF904F54),
    background = Color(0xFFFCF5F5),
    onBackground = Color(0xFF201A1A),
    surface = Color.White,
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF4DDDE),
    onSurfaceVariant = Color(0xFF534344)
)

// ==========================================
// 5. Forest Sage Theme
// ==========================================
private val GreenDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003815),
    primaryContainer = Color(0xFF005322),
    onPrimaryContainer = Color(0xFFC0EFC5),
    secondary = Color(0xFFADCDB0),
    onSecondary = Color(0xFF1A3620),
    secondaryContainer = Color(0xFF101B13),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFA2CED0),
    background = Color(0xFF0C120E),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF142018),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF233227),
    onSurfaceVariant = Color(0xFFC2C9C1)
)

private val GreenLight = lightColorScheme(
    primary = Color(0xFF0D8040),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0EFC5),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF516353),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E8D7),
    onSecondaryContainer = Color(0xFF0F1F12),
    tertiary = Color(0xFF386567),
    background = Color(0xFFF3FAF5),
    onBackground = Color(0xFF191C19),
    surface = Color.White,
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDDE5DC),
    onSurfaceVariant = Color(0xFF414942)
)

// ==========================================
// 6. Midnight Amber Theme (Original)
// ==========================================

private val GoldDark = darkColorScheme(
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

private val GoldLight = lightColorScheme(
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
    val theme = AppThemeState.value
    
    val colorScheme = when (theme) {
        "purple" -> if (darkTheme) PurpleDark else PurpleLight
        "blue" -> if (darkTheme) BlueDark else BlueLight
        "white" -> if (darkTheme) SlateDark else SlateLight
        "red" -> if (darkTheme) RedDark else RedLight
        "green" -> if (darkTheme) GreenDark else GreenLight
        "gold" -> if (darkTheme) GoldDark else GoldLight
        else -> if (darkTheme) PurpleDark else PurpleLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
