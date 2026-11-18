package pl.aingielski.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** Material 3 type scale; all sizes are in sp so system font scaling applies (constitution V). */
val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val AnswerTextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
