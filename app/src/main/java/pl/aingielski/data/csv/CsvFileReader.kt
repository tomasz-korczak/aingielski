package pl.aingielski.data.csv

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Raw file content read through the Storage Access Framework; [text] is null when unreadable. */
data class CsvFileContent(val text: String?, val displayName: String?)

/** Reads a picked document as strict UTF-8 text (research R9). No storage permission is needed. */
class CsvFileReader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun read(uri: Uri): CsvFileContent = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        val text = try {
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var tooBig = false
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > MAX_BYTES) {
                        tooBig = true
                        break
                    }
                }
                if (tooBig) null else out.toByteArray()
            }
            bytes?.let(::decodeUtf8)
        } catch (_: Exception) {
            null
        }
        CsvFileContent(text, name)
    }

    private fun decodeUtf8(bytes: ByteArray): String? = try {
        val decoded = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        if (decoded.contains('\u0000')) null else decoded
    } catch (_: CharacterCodingException) {
        null
    }

    private fun displayName(uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    } ?: uri.lastPathSegment?.substringAfterLast('/')

    private companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}
