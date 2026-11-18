package pl.aingielski

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.aingielski.speech.VoiceInstallAction
import pl.aingielski.ui.navigation.AppNavHost
import pl.aingielski.ui.theme.AIngielskiTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as AIngielskiApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // Voices may have been installed while the user was in system settings (R6).
                lifecycleScope.launch { container.speechEngine.refresh() }
            }
        })
        setContent {
            AIngielskiTheme {
                AppNavHost(onOpenVoiceInstall = ::openVoiceInstall)
            }
        }
    }

    /** Resolves [VoiceInstallAction] into the first system screen that exists (research R6). */
    private fun openVoiceInstall() {
        val action = container.speechEngine.voiceInstallAction()
        if (action !is VoiceInstallAction.OpenSystemScreen) return
        val candidates = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next screen.
            } catch (_: SecurityException) {
                // Try the next screen.
            }
        }
    }
}
