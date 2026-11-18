package pl.aingielski.ui.components

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import pl.aingielski.R
import pl.aingielski.domain.model.WordResult
import pl.aingielski.ui.theme.LocalResultColors

/**
 * Quiz result: icon + text + color, so meaning never depends on color alone (constitution V).
 * A polite live region, so TalkBack announces it when it appears (FR-027a).
 */
@Composable
fun ResultBadge(result: WordResult, modifier: Modifier = Modifier) {
    val colors = LocalResultColors.current
    val (color, icon, text) = when (result) {
        WordResult.CORRECT -> Triple(colors.correct, R.drawable.ic_check_circle, R.string.result_correct)
        WordResult.INCORRECT -> Triple(colors.incorrect, R.drawable.ic_cancel, R.string.result_incorrect)
        else -> Triple(colors.skipped, R.drawable.ic_skip, R.string.result_skipped)
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(stringResource(text), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
