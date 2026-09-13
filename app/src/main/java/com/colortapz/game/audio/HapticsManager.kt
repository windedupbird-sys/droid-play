package com.colortapz.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HapticsManager {
    val isHapticsEnabled: StateFlow<Boolean>
    fun setHapticsEnabled(enabled: Boolean)
    fun perform(effect: HapticEffect)
}

class AndroidHapticsManager(
    context: Context
) : HapticsManager {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isHapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    override val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    private val hasVibrator: Boolean = vibrator?.hasVibrator() == true
    private val hasAmplitudeControl: Boolean = vibrator?.hasAmplitudeControl() == true

    override fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    override fun perform(effect: HapticEffect) {
        if (!_isHapticsEnabled.value || !hasVibrator || vibrator == null) return

        try {
            val vibrationEffect = createVibrationEffect(effect)
            vibrator.vibrate(vibrationEffect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createVibrationEffect(effect: HapticEffect): VibrationEffect {
        return when (effect) {
            HapticEffect.LIGHT_TICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                } else {
                    val amplitude = if (hasAmplitudeControl) 60 else VibrationEffect.DEFAULT_AMPLITUDE
                    VibrationEffect.createOneShot(10L, amplitude)
                }
            }

            HapticEffect.CRISP_CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    val amplitude = if (hasAmplitudeControl) 160 else VibrationEffect.DEFAULT_AMPLITUDE
                    VibrationEffect.createOneShot(20L, amplitude)
                }
            }

            HapticEffect.HEAVY_THUD -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    val amplitude = if (hasAmplitudeControl) 255 else VibrationEffect.DEFAULT_AMPLITUDE
                    VibrationEffect.createOneShot(65L, amplitude)
                }
            }

            HapticEffect.DOUBLE_PULSE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                } else {
                    val timings = longArrayOf(0L, 30L, 50L, 30L)
                    val amplitudes = if (hasAmplitudeControl) intArrayOf(0, 200, 0, 200) else null
                    if (amplitudes != null) {
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                }
            }

            HapticEffect.FREEZE_SHIVER -> {
                val timings = longArrayOf(0L, 12L, 20L, 12L, 20L, 12L)
                val amplitudes = if (hasAmplitudeControl) intArrayOf(0, 100, 0, 140, 0, 180) else null
                if (amplitudes != null) {
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "colortapz_haptics_prefs"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
    }
}
