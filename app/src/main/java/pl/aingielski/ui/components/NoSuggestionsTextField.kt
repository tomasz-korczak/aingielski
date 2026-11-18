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
 * Plain-text answer field. The typed text is always visible; nothing is masked.
 *
 * It wraps an Android [EditText] because Compose offers no reliable way to turn off keyboard
 * suggestions (FR-022a). `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` is only a hint that makes Gboard and
 * Samsung Keyboard disable suggestions and autocorrect; this field is NOT a password, must never show
 * dots, and is excluded from autofill so no "save password" prompt appears. Up to 200 characters,
 * any characters allowed (FR-022b). TalkBack reads the hint/label and the typed text normally.
 */
@Composable
fun NoSuggestionsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 200,
    focusKey: Any? = null,
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnDone by rememberUpdatedState(onDone)
    val colors = MaterialTheme.colorScheme
    var editText by remember { mutableStateOf<EditText?>(null) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(2.dp, colors.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        factory = { context ->
            EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                maxLines = 1
                setHorizontallyScrolling(true)
                filters = arrayOf(InputFilter.LengthFilter(maxLength))
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                background = null
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                hint = label
                doAfterTextChanged { currentOnChange(it?.toString().orEmpty()) }
                setOnEditorActionListener { _, actionId, event ->
                    val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                    if (actionId == EditorInfo.IME_ACTION_DONE || enter) {
                        currentOnDone()
                        true
                    } else {
                        false
                    }
                }
                editText = this
            }
        },
        update = { view ->
            if (view.text.toString() != value) {
                view.setText(value)
                view.setSelection(view.text.length)
            }
            if (view.hint?.toString() != label) view.hint = label
            view.setTextColor(colors.onSurface.toArgb())
            view.setHintTextColor(colors.onSurfaceVariant.toArgb())
        },
    )

    LaunchedEffect(focusKey, editText) {
        val view = editText ?: return@LaunchedEffect
        if (focusKey == null) return@LaunchedEffect
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.post { imm.showSoftInput(view, 0) }
    }
}
