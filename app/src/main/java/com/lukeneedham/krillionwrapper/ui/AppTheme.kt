package com.lukeneedham.krillionwrapper.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.lukeneedham.common.ui.DebugTheme

/** Colours and typeface of the wrapper's own screens, matched to the game's deep-sea look. */
@Immutable
data class AppThemeValues(
    val background: Color = Color(0xFF050A14),
    val onBackground: Color = Color(0xFFE8F1FF),
    val surface: Color = Color(0xFF101D33),
    val onSurface: Color = Color(0xFFE8F1FF),
    val onSurfaceMuted: Color = Color(0xFF8CA0BF),
    val primary: Color = Color(0xFFFF5D8F),
    val onPrimary: Color = Color(0xFF050A14),
    val outline: Color = Color(0xFF3A4A66),
    val error: Color = Color(0xFFFF6B6B),
    val warning: Color = Color(0xFFFFC857),
    val scrim: Color = Color(0x99000000),
    val fontFamily: FontFamily = FontFamily.Monospace,
)

private val LocalAppTheme = staticCompositionLocalOf { AppThemeValues() }

object AppTheme {
    val current: AppThemeValues
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current
}

@Composable
fun AppTheme(values: AppThemeValues = AppThemeValues(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppTheme provides values, content = content)
}

/** The shared library's debug pages take the app's colours and typeface through this one mapping. */
fun AppThemeValues.toDebugTheme() = DebugTheme(
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    primary = primary,
    onPrimary = onPrimary,
    outline = outline,
    error = error,
    warning = warning,
    fontFamily = fontFamily,
)
