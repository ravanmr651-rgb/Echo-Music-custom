package echo.music.iad1tya.ui.screens.equalizer.axion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.dsp.controller.DspController
import echo.music.dsp.core.models.FilterType
import echo.music.dsp.core.models.ParametricEQBand
import echo.music.dsp.core.models.SavedEQProfile
import echo.music.dsp.data.EQProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AxionEqViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val dspController: DspController,
  private val eqProfileRepository: EQProfileRepository
) : ViewModel() {

  private val prefs = context.getSharedPreferences("echo_eq_prefs", Context.MODE_PRIVATE)

  private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
  val enabled = _enabled.asStateFlow()

  private val bandFrequencies =
    doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

  private val _bandGains = MutableStateFlow(FloatArray(10) { prefs.getFloat("band_$it", 0f) })
  val bandGains = _bandGains.asStateFlow()

  private val _mode = MutableStateFlow(prefs.getInt("mode", 0))
  val mode = _mode.asStateFlow()

  private val _isDirty = MutableStateFlow(false)
  val isDirty = _isDirty.asStateFlow()

  // Slider drags can emit dozens of values per second. Coalesce DSP/profile writes.
  private var pendingApply: Job? = null

  val customProfiles =
    eqProfileRepository.profiles
      .map { profiles -> profiles.filter { it.isCustom && it.id != "echo_tuning" } }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  init {
    if (_enabled.value) {
      applyToService()
    }
  }

  fun setEnabled(enabled: Boolean) {
    _enabled.value = enabled
    prefs.edit().putBoolean("enabled", enabled).apply()
    if (enabled) {
      applyToService()
    } else {
      pendingApply?.cancel()
      pendingApply = null
      viewModelScope.launch { eqProfileRepository.setActiveProfile(null) }
      dspController.disableEqualizer()
    }
  }

  fun setMode(mode: Int) {
    _mode.value = mode
    prefs.edit().putInt("mode", mode).apply()
    _isDirty.value = false
  }

  fun setBandGain(index: Int, gain: Float) {
    if (index !in bandFrequencies.indices || !gain.isFinite()) return

    val safeGain = gain.coerceIn(-600f, 600f)
    val newGains = _bandGains.value.copyOf()
    newGains[index] = safeGain
    _bandGains.value = newGains
    prefs.edit().putFloat("band_$index", safeGain).apply()
    _isDirty.value = true
    if (_enabled.value) {
      applyToService()
    }
  }

  fun setBandsGains(gains: FloatArray, fromUser: Boolean = false) {
    // The UI stores gains in 1/50 dB steps, so +/-600 means +/-12.0 dB.
    val safeGains =
      FloatArray(bandFrequencies.size) { index ->
        gains.getOrNull(index)?.takeIf { it.isFinite() }?.coerceIn(-600f, 600f) ?: 0f
      }
    _bandGains.value = safeGains
    val editor = prefs.edit()
    safeGains.forEachIndexed { index, gain -> editor.putFloat("band_$index", gain) }
    editor.apply()
    _isDirty.value = fromUser
    if (_enabled.value) {
      applyToService()
    }
  }

  fun reset() {
    val flat = FloatArray(10) { 0f }
    setBandsGains(flat)
  }

  fun saveCustomProfile(name: String) {
    // Do not let a queued live-tuning update overwrite the profile being saved.
    pendingApply?.cancel()
    pendingApply = null
    viewModelScope.launch {
      val gainsSnapshot = _bandGains.value.copyOf()
      val bands =
        gainsSnapshot.mapIndexed { index, gain ->
          ParametricEQBand(
            frequency = bandFrequencies[index],
            gain = gain.toDouble() / 50.0,
            q = 1.41,
            filterType = FilterType.PK,
            enabled = true
          )
        }
      val headroomDb = -(bands.maxOfOrNull { it.gain } ?: 0.0).coerceAtLeast(0.0)

      val id = "custom_${System.currentTimeMillis()}"
      val profile =
        SavedEQProfile(
          id = id,
          name = name,
          deviceModel = "Equalizer",
          bands = bands,
          preamp = headroomDb,
          isCustom = true,
          isActive = true
        )

      eqProfileRepository.saveProfile(profile)
      eqProfileRepository.setActiveProfile(profile.id)
      if (_enabled.value) dspController.applyProfile(profile)
      _isDirty.value = false
    }
  }

  fun deleteProfiles(ids: List<String>) {
    viewModelScope.launch { ids.forEach { id -> eqProfileRepository.deleteProfile(id) } }
  }

  private fun applyToService() {
    val gainsSnapshot = _bandGains.value.copyOf()
    pendingApply?.cancel()
    pendingApply =
      viewModelScope.launch {
        // Coalesce rapid slider events so the audio thread and profile store are not spammed.
        delay(80L)
        if (!_enabled.value) return@launch

        val bands =
          gainsSnapshot.mapIndexed { index, gain ->
            ParametricEQBand(
              frequency = bandFrequencies[index],
              gain = gain.toDouble() / 50.0,
              q = 1.41,
              filterType = FilterType.PK,
              enabled = true
            )
          }
        // Keep digital peaks below 0 dBFS when positive bands are boosted.
        val headroomDb = -(bands.maxOfOrNull { it.gain } ?: 0.0).coerceAtLeast(0.0)

        val profile =
          SavedEQProfile(
            id = "echo_tuning",
            name = "AURA Tuning",
            deviceModel = "Equalizer",
            bands = bands,
            preamp = headroomDb,
            isCustom = false,
            isActive = true
          )

        eqProfileRepository.saveProfile(profile)
        if (!_enabled.value) return@launch
        eqProfileRepository.setActiveProfile(profile.id)
        if (!_enabled.value) return@launch
        dspController.applyProfile(profile)
      }
  }
}
