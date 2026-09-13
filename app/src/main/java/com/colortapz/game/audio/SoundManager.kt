package com.colortapz.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

interface SoundManager {
    val isSoundEnabled: StateFlow<Boolean>
    fun setSoundEnabled(enabled: Boolean)
    fun play(effect: SoundEffect, rate: Float = 1.0f)
    fun release()
}

class SynthesizedSoundManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : SoundManager {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    override val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIdMap = ConcurrentHashMap<SoundEffect, Int>()
    @Volatile private var isInitialized = false

    init {
        scope.launch {
            initializeSynthesizer()
        }
    }

    private suspend fun initializeSynthesizer() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "synth_audio").apply { if (!exists()) mkdirs() }

            for (effect in SoundEffect.values()) {
                val pcm = SynthAudioGenerator.generatePcm(effect)
                val wavBytes = SynthAudioGenerator.pcmToWav(pcm)

                val tempWav = File(cacheDir, "${effect.name.lowercase()}.wav")
                FileOutputStream(tempWav).use { it.write(wavBytes) }

                val soundId = soundPool.load(tempWav.absolutePath, 1)
                soundIdMap[effect] = soundId
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    override fun play(effect: SoundEffect, rate: Float) {
        if (!_isSoundEnabled.value || !isInitialized) return

        val soundId = soundIdMap[effect] ?: return
        soundPool.play(
            soundId,
            /* leftVolume = */ 1.0f,
            /* rightVolume = */ 1.0f,
            /* priority = */ 1,
            /* loop = */ 0,
            /* rate = */ rate.coerceIn(0.5f, 2.0f)
        )
    }

    override fun release() {
        soundIdMap.clear()
        soundPool.release()
        scope.cancel()
    }

    companion object {
        private const val PREFS_NAME = "colortapz_audio_prefs"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
    }
}
