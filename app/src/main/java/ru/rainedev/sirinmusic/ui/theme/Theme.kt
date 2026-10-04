package ru.rainedev.sirinmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ru.rainedev.sirinmusic.data.*

private fun paletteColors(palette: Palette, dark: Boolean): ColorScheme {
    val (light, night, container) = when (palette) {
        Palette.SIRIN -> Triple(0xFF9B4220, 0xFFFFB695, 0xFFFFDBCF)
        Palette.FOREST -> Triple(0xFF386A20, 0xFF9CD67D, 0xFFB7F397)
        Palette.OCEAN -> Triple(0xFF00658E, 0xFF85CFFF, 0xFFC7E7FF)
        Palette.SUNSET -> Triple(0xFF904B40, 0xFFFFB4A5, 0xFFFFDAD2)
    }
    return if (dark) darkColorScheme(primary = Color(night), onPrimary = Color(0xFF201A25),
        primaryContainer = Color(light), onPrimaryContainer = Color.White)
    else lightColorScheme(primary = Color(light), onPrimary = Color.White,
        primaryContainer = Color(container), onPrimaryContainer = Color(0xFF201A25))
}

@Composable
fun SirinMusicTheme(appearance: Appearance = Appearance(), content: @Composable () -> Unit) {
    val dark = when (appearance.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = if (appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else paletteColors(appearance.palette, dark)
    MaterialTheme(colorScheme = colors, content = content)
}
