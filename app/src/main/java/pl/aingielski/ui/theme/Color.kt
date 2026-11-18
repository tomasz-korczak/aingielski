package pl.aingielski.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Tonal roles derived from seed #2B4C9B (primary), #D9434F (secondary) and #F2A93B (tertiary), research R3.

val LightColors = lightColorScheme(
    primary = Color(0xFF2B4C9B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    inversePrimary = Color(0xFFB3C5FF),
    secondary = Color(0xFFB3263A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD8),
    onSecondaryContainer = Color(0xFF410009),
    tertiary = Color(0xFF7C5800),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceTint = Color(0xFF2B4C9B),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF8FF),
    surfaceDim = Color(0xFFDAD9E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF4),
    surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE3E2E9),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFB3C5FF),
    onPrimary = Color(0xFF002B75),
    primaryContainer = Color(0xFF1F3F8D),
    onPrimaryContainer = Color(0xFFDBE1FF),
    inversePrimary = Color(0xFF2B4C9B),
    secondary = Color(0xFFFFB3B0),
    onSecondary = Color(0xFF680014),
    secondaryContainer = Color(0xFF920A25),
    onSecondaryContainer = Color(0xFFFFDAD8),
    tertiary = Color(0xFFF7BD48),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5E4200),
    onTertiaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    surfaceTint = Color(0xFFB3C5FF),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF2F3036),
    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF44464F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF38393F),
    surfaceDim = Color(0xFF121318),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)

/** Semantic quiz-result colors (research R3). Always paired with an icon and a text label. */
@Immutable
data class ResultColors(
    val correct: Color,
    val incorrect: Color,
    val skipped: Color,
)

val LightResultColors = ResultColors(
    correct = Color(0xFF2E7D32),
    incorrect = Color(0xFFC62828),
    skipped = Color(0xFFEF6C00),
)

val DarkResultColors = ResultColors(
    correct = Color(0xFF81C784),
    incorrect = Color(0xFFE57373),
    skipped = Color(0xFFFFB74D),
)

val LocalResultColors = staticCompositionLocalOf { LightResultColors }
