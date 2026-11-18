package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.FontChoice
import com.example.data.model.ThemePalette

@Composable
fun LevelUpTheme(
    themePalette: ThemePalette = ThemePalette.LIGHT,
    fontChoice: FontChoice = FontChoice.PLUS_JAKARTA,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeForPalette(themePalette)
    val typography = buildAppTypography(fontChoice)
    val isDark = themePalette == ThemePalette.DARK

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
