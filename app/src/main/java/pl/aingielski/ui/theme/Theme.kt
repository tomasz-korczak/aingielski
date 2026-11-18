package pl.aingielski.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** App theme. Dynamic (wallpaper) color is intentionally disabled so brand colors and icons match (R3). */
@Composable
fun AIngielskiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val resultColors = if (darkTheme) DarkResultColors else LightResultColors
    CompositionLocalProvider(LocalResultColors provides resultColors) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
