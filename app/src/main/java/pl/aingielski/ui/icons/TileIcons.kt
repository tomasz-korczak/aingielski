package pl.aingielski.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.aingielski.ui.theme.AIngielskiTheme

/*
 * Two-tone tile illustrations drawn in code (research R4): XML drawables cannot follow the
 * in-app light/dark color scheme, so the colors are passed in from the current theme.
 */

/** Headphones with sound-wave bars and an "A" speech bubble. */
fun listenTileIcon(primary: Color, accent: Color, onAccent: Color): ImageVector =
    ImageVector.Builder(
        name = "ListenTile",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 64f,
        viewportHeight = 64f,
    ).apply {
        // Head band
        path(stroke = SolidColor(primary), strokeLineWidth = 5f, strokeLineCap = StrokeCap.Round) {
            moveTo(13f, 38f)
            arcTo(19f, 19f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 51f, y1 = 38f)
        }
        // Ear cups
        path(fill = SolidColor(primary)) {
            moveTo(13f, 32f)
            lineTo(16f, 32f)
            arcTo(4f, 4f, 0f, false, true, 20f, 36f)
            lineTo(20f, 49f)
            arcTo(4f, 4f, 0f, false, true, 16f, 53f)
            lineTo(13f, 53f)
            arcTo(5f, 5f, 0f, false, true, 8f, 48f)
            lineTo(8f, 37f)
            arcTo(5f, 5f, 0f, false, true, 13f, 32f)
            close()
            moveTo(48f, 32f)
            lineTo(51f, 32f)
            arcTo(5f, 5f, 0f, false, true, 56f, 37f)
            lineTo(56f, 48f)
            arcTo(5f, 5f, 0f, false, true, 51f, 53f)
            lineTo(48f, 53f)
            arcTo(4f, 4f, 0f, false, true, 44f, 49f)
            lineTo(44f, 36f)
            arcTo(4f, 4f, 0f, false, true, 48f, 32f)
            close()
        }
        // Sound-wave bars between the cups
        path(stroke = SolidColor(accent), strokeLineWidth = 3.5f, strokeLineCap = StrokeCap.Round) {
            moveTo(26f, 39f); lineTo(26f, 47f)
            moveTo(32f, 34f); lineTo(32f, 52f)
            moveTo(38f, 39f); lineTo(38f, 47f)
        }
        // "A" speech bubble
        path(fill = SolidColor(accent)) {
            moveTo(50f, 3f)
            arcTo(9f, 9f, 0f, true, true, 43.6f, 18.4f)
            lineTo(40f, 22f)
            lineTo(41.4f, 14.5f)
            arcTo(9f, 9f, 0f, false, true, 50f, 3f)
            close()
        }
        path(
            stroke = SolidColor(onAccent),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(46.5f, 16f); lineTo(50f, 7f); lineTo(53.5f, 16f)
            moveTo(47.8f, 13f); lineTo(52.2f, 13f)
        }
    }.build()

/** A rising trend line over bars, with a trend dot. */
fun statsTileIcon(primary: Color, accent: Color): ImageVector =
    ImageVector.Builder(
        name = "StatsTile",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 64f,
        viewportHeight = 64f,
    ).apply {
        path(fill = SolidColor(primary), fillAlpha = 0.35f) {
            moveTo(10f, 40f); lineTo(18f, 40f); lineTo(18f, 54f); lineTo(10f, 54f); close()
            moveTo(24f, 32f); lineTo(32f, 32f); lineTo(32f, 54f); lineTo(24f, 54f); close()
            moveTo(38f, 36f); lineTo(46f, 36f); lineTo(46f, 54f); lineTo(38f, 54f); close()
        }
        path(stroke = SolidColor(primary), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
            moveTo(7f, 57f); lineTo(57f, 57f)
        }
        path(
            stroke = SolidColor(accent),
            strokeLineWidth = 4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(10f, 34f); lineTo(26f, 22f); lineTo(38f, 28f); lineTo(53f, 12f)
        }
        path(fill = SolidColor(accent)) {
            moveTo(53f, 7f)
            arcTo(5f, 5f, 0f, true, true, 52.99f, 7f)
            close()
        }
    }.build()

@Composable
fun rememberListenTileIcon(): ImageVector {
    val colors = MaterialTheme.colorScheme
    return remember(colors.primary, colors.secondary, colors.onSecondary) {
        listenTileIcon(colors.primary, colors.secondary, colors.onSecondary)
    }
}

@Composable
fun rememberStatsTileIcon(): ImageVector {
    val colors = MaterialTheme.colorScheme
    return remember(colors.primary, colors.secondary) { statsTileIcon(colors.primary, colors.secondary) }
}

@Composable
private fun TileIconsPreviewContent() {
    Surface {
        Row(Modifier.padding(16.dp)) {
            Image(rememberListenTileIcon(), contentDescription = null, modifier = Modifier.size(96.dp))
            Image(rememberStatsTileIcon(), contentDescription = null, modifier = Modifier.size(96.dp))
        }
    }
}

@Preview(name = "Light")
@Composable
private fun TileIconsLightPreview() {
    AIngielskiTheme(darkTheme = false) { TileIconsPreviewContent() }
}

@Preview(name = "Dark")
@Composable
private fun TileIconsDarkPreview() {
    AIngielskiTheme(darkTheme = true) { TileIconsPreviewContent() }
}
