package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemePalette

fun parseHexColor(hex: String, fallback: Color = Color(0xFF6366F1)): Color {
    return runCatching {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(value or 0xFF000000L)
            8 -> Color(value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

private val LightPalette = lightColorScheme(
    primary = Color(0xFFFF5A36),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5DD),
    onPrimaryContainer = Color(0xFF4A1204),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color.White,
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF181622),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181622),
    surfaceVariant = Color(0xFFF1EDE7),
    onSurfaceVariant = Color(0xFF5A566B),
    outline = Color(0xFFE0DAD0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFFF785A),
    onPrimary = Color(0xFF280900),
    primaryContainer = Color(0xFF531D10),
    onPrimaryContainer = Color(0xFFFFDAD1),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFFA5B4FC),
    onTertiary = Color(0xFF1E1B4B),
    background = Color(0xFF111018),
    onBackground = Color(0xFFF4F3F8),
    surface = Color(0xFF1A1824),
    onSurface = Color(0xFFF4F3F8),
    surfaceVariant = Color(0xFF262334),
    onSurfaceVariant = Color(0xFFB3AEC6),
    outline = Color(0xFF39344D),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0808)
)

private val PastelPinkPalette = lightColorScheme(
    primary = Color(0xFFDB2777),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3),
    onPrimaryContainer = Color(0xFF500724),
    secondary = Color(0xFFF43F5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color.White,
    background = Color(0xFFFFF5F8),
    onBackground = Color(0xFF2B1220),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B1220),
    surfaceVariant = Color(0xFFFCE8F0),
    onSurfaceVariant = Color(0xFF704659),
    outline = Color(0xFFF5D0E0)
)

private val OceanBluePalette = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = Color(0xFFF2F7FE),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE5EFFC),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val EmeraldGreenPalette = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF2FBF6),
    onBackground = Color(0xFF0D261C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D261C),
    surfaceVariant = Color(0xFFE3F5EC),
    onSurfaceVariant = Color(0xFF3E6354),
    outline = Color(0xFFC2E3D3)
)

private val SunsetOrangePalette = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF2B170A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B170A),
    surfaceVariant = Color(0xFFFFEBD9),
    onSurfaceVariant = Color(0xFF7A4B2E),
    outline = Color(0xFFFED7AA)
)

private val RoyalPurplePalette = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFFC026D3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAE8FF),
    onSecondaryContainer = Color(0xFF701A75),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color.White,
    background = Color(0xFFF8F5FF),
    onBackground = Color(0xFF1F1235),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1235),
    surfaceVariant = Color(0xFFEEE8FC),
    onSurfaceVariant = Color(0xFF5D4B82),
    outline = Color(0xFFDDD6FE)
)

fun colorSchemeForPalette(palette: ThemePalette): ColorScheme = when (palette) {
    ThemePalette.LIGHT -> LightPalette
    ThemePalette.DARK -> DarkPalette
    ThemePalette.PASTEL_PINK -> PastelPinkPalette
    ThemePalette.BLUE -> OceanBluePalette
    ThemePalette.GREEN -> EmeraldGreenPalette
    ThemePalette.ORANGE -> SunsetOrangePalette
    ThemePalette.PURPLE -> RoyalPurplePalette
}

fun previewSwatchesForPalette(palette: ThemePalette): List<Color> = when (palette) {
    ThemePalette.LIGHT -> listOf(Color(0xFFFAF8F5), Color(0xFFFF5A36), Color(0xFF0D9488))
    ThemePalette.DARK -> listOf(Color(0xFF111018), Color(0xFFFF785A), Color(0xFF2DD4BF))
    ThemePalette.PASTEL_PINK -> listOf(Color(0xFFFFF5F8), Color(0xFFDB2777), Color(0xFFF43F5E))
    ThemePalette.BLUE -> listOf(Color(0xFFF2F7FE), Color(0xFF2563EB), Color(0xFF0891B2))
    ThemePalette.GREEN -> listOf(Color(0xFFF2FBF6), Color(0xFF059669), Color(0xFF0D9488))
    ThemePalette.ORANGE -> listOf(Color(0xFFFFF8F2), Color(0xFFEA580C), Color(0xFFD97706))
    ThemePalette.PURPLE -> listOf(Color(0xFFF8F5FF), Color(0xFF7C3AED), Color(0xFFC026D3))
}
