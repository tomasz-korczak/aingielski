package pl.aingielski

import android.app.Application
import kotlinx.coroutines.launch

class AIngielskiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Warm up the speech engine early so the first quiz word plays quickly (SC-002).
        container.speechEngine
        // A quiz cut off by the system is kept like an abort and never resumed (FR-028a, R8).
        container.appScope.launch { container.sessionRepository.finalizeAllStale() }
    }
}
