package com.fanqieyu.deepfocus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DeepFocusPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundAccent: Color,
    val surfaceGlass: Color,
    val surfaceGlassStrong: Color,
    val borderTopHighlight: Color,
    val borderBottom: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val accentOn: Color,
    val coral: Color,
    val gold: Color,
    val hudGlass: Color,
    val hudInk: Color,
    val hudMuted: Color,
)

private val DarkPalette = DeepFocusPalette(
    isDark = true,
    background = Color(0xFF06131D),
    backgroundAccent = Color(0xFF0D2838),
    surfaceGlass = Color(0xB8143344),
    surfaceGlassStrong = Color(0xD9163A4D),
    borderTopHighlight = Color(0x66E8FFF9),
    borderBottom = Color(0x2278D9CC),
    ink = Color(0xFFF3F9F8),
    muted = Color(0xFFA7BFC2),
    accent = Color(0xFF8BEAD8),
    accentOn = Color(0xFF052028),
    coral = Color(0xFFFFA585),
    gold = Color(0xFFF9DC99),
    hudGlass = Color(0xB3102B3A),
    hudInk = Color(0xFFF4FAF9),
    hudMuted = Color(0xFFB3CACD),
)

private val LightPalette = DeepFocusPalette(
    isDark = false,
    background = Color(0xFFEAF5F8),
    backgroundAccent = Color(0xFFD6ECF2),
    surfaceGlass = Color(0xCCF7FCFD),
    surfaceGlassStrong = Color(0xEBFFFFFF),
    borderTopHighlight = Color(0xEEFFFFFF),
    borderBottom = Color(0x4D7AC6CF),
    ink = Color(0xFF0C2633),
    muted = Color(0xFF4F707D),
    accent = Color(0xFF0A8F8E),
    accentOn = Color(0xFFFFFFFF),
    coral = Color(0xFFE06646),
    gold = Color(0xFFB87914),
    hudGlass = Color(0xC7EEF8FB),
    hudInk = Color(0xFF0D2936),
    hudMuted = Color(0xFF426775),
)

val LocalDeepFocusPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun DeepFocusLiquidTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (useDark) DarkPalette else LightPalette
    val colorScheme = if (useDark) {
        darkColorScheme(
            background = palette.background,
            surface = palette.surfaceGlassStrong,
            primary = palette.accent,
            secondary = palette.coral,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onPrimary = palette.accentOn,
        )
    } else {
        lightColorScheme(
            background = palette.background,
            surface = palette.surfaceGlassStrong,
            primary = palette.accent,
            secondary = palette.coral,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onPrimary = palette.accentOn,
        )
    }
    CompositionLocalProvider(LocalDeepFocusPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isHud: Boolean = false,
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit,
) {
    val p = LocalDeepFocusPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseColor = if (isHud) p.hudGlass else p.surfaceGlass
    val sheenTop = if (p.isDark) Color(0x28FFFFFF) else Color(0xAAFFFFFF)
    val sheenBottom = if (p.isDark) Color(0x05FFFFFF) else Color(0x22FFFFFF)
    val borderBrush = Brush.verticalGradient(listOf(p.borderTopHighlight, p.borderBottom))

    Surface(
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier)
            .shadow(if (p.isDark) 14.dp else 10.dp, shape)
            .clip(shape)
            .border(BorderStroke(1.dp, borderBrush), shape),
        color = baseColor,
        shape = shape,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(sheenTop, Color.Transparent, sheenBottom)),
            ),
        ) {
            content()
        }
    }
}
