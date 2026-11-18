package pl.aingielski.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.testing.FakeSettingsRepository
import pl.aingielski.testing.FakeSpeechEngine
import pl.aingielski.testing.MainDispatcherRule

class SettingsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val speech = FakeSpeechEngine()

    @Test
    fun loadsSavedValuesAndVoices() {
        val repo = FakeSettingsRepository(AppSettings(EnglishVariant.US, "us-2", skipEnabled = false))
        val vm = SettingsViewModel(repo, speech)
        val state = vm.state.value
        assertTrue(state.loaded)
        assertEquals("us-2", state.draft.voiceId)
        assertFalse(state.draft.skipEnabled)
        assertEquals(listOf("us-1", "us-2"), state.voices.map { it.id })
    }

    @Test
    fun changingVariantReloadsVoicesAndResetsForeignVoice() {
        val vm = SettingsViewModel(FakeSettingsRepository(AppSettings(EnglishVariant.US, "us-1")), speech)
        vm.onVariantChange(EnglishVariant.GB)
        assertEquals(listOf("gb-1"), vm.state.value.voices.map { it.id })
        assertNull(vm.state.value.draft.voiceId)
    }

    @Test
    fun saveStoresEverySetting() {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo, speech)
        vm.onVariantChange(EnglishVariant.GB)
        vm.onVoiceChange("gb-1")
        vm.onSkipChange(false)
        vm.save()
        assertEquals(AppSettings(EnglishVariant.GB, "gb-1", skipEnabled = false), repo.flow.value)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun cancelDiscardsChanges() {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo, speech)
        vm.onSkipChange(false)
        vm.cancel()
        assertEquals(0, repo.saveCount)
        assertEquals(AppSettings(), repo.flow.value)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun missingVoicesAreReported() {
        speech.availabilityFlow.value = SpeechAvailability.MissingVoices(EnglishVariant.entries.toSet())
        speech.voices = emptyMap()
        val vm = SettingsViewModel(FakeSettingsRepository(), speech)
        assertTrue(vm.state.value.voicesMissing)
    }

    @Test
    fun previewSpeaksSampleWithSelectedVoice() {
        val vm = SettingsViewModel(FakeSettingsRepository(), speech)
        vm.preview("Hello")
        assertEquals(listOf("Hello"), speech.spoken)
    }
}
