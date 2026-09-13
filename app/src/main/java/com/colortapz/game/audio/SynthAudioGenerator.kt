package com.colortapz.game.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
    TAP,
    PERFECT_TAP,
    COMBO_UP,
    HEART,
    FREEZE,
    BOMB_EXPLOSION,
    FEVER_START,
    SHIELD_CRACK,
    GAME_OVER,
    BUTTON_CLICK
}

enum class HapticEffect {
    LIGHT_TICK,
    CRISP_CLICK,
    HEAVY_THUD,
    DOUBLE_PULSE,
    FREEZE_SHIVER
}

object SynthAudioGenerator {
    const val SAMPLE_RATE = 44100

    fun generatePcm(effect: SoundEffect): ShortArray {
        return when (effect) {
            SoundEffect.BUTTON_CLICK -> generateChirp(
                startFreq = 800.0,
                endFreq = 400.0,
                durationMs = 15,
                volume = 0.5f
            )

            SoundEffect.TAP -> generateChirp(
                startFreq = 440.0,
                endFreq = 880.0,
                durationMs = 45,
                volume = 0.75f
            )

            SoundEffect.PERFECT_TAP -> generateHarmonicTone(
                baseFreq = 880.0,
                durationMs = 85,
                volume = 0.85f
            )

            SoundEffect.COMBO_UP -> generateArpeggio(
                frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50),
                noteDurationMs = 30,
                volume = 0.8f
            )

            SoundEffect.HEART -> generateHarmonicBell(
                freq1 = 587.33,
                freq2 = 880.0,
                durationMs = 160,
                volume = 0.8f
            )

            SoundEffect.FREEZE -> generateShimmer(
                frequencies = doubleArrayOf(1200.0, 1600.0, 2000.0, 2400.0),
                durationMs = 220,
                volume = 0.65f
            )

            SoundEffect.SHIELD_CRACK -> generateChirp(
                startFreq = 300.0,
                endFreq = 600.0,
                durationMs = 60,
                volume = 0.8f
            )

            SoundEffect.FEVER_START -> generateArpeggio(
                frequencies = doubleArrayOf(440.0, 554.37, 659.25, 880.0, 1108.73),
                noteDurationMs = 35,
                volume = 0.85f
            )

            SoundEffect.BOMB_EXPLOSION -> generateExplosion(
                durationMs = 240,
                volume = 0.9f
            )

            SoundEffect.GAME_OVER -> generateGameOverSlide(
                startFreq = 400.0,
                endFreq = 120.0,
                durationMs = 350,
                volume = 0.85f
            )
        }
    }

    private fun generateChirp(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val phaseIncrement = 2.0 * PI * currentFreq / SAMPLE_RATE
            phase += phaseIncrement

            val envelope = (1.0 - progress) * exp(-progress * 2.0)
            val sample = sin(phase) * envelope * volume
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateHarmonicTone(
        baseFreq: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = if (progress < 0.1) progress / 0.1 else (1.0 - progress) / 0.9

            val wave1 = sin(2.0 * PI * baseFreq * t)
            val wave2 = sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.4
            val sample = (wave1 + wave2) * envelope * volume * 0.7
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateArpeggio(
        frequencies: DoubleArray,
        noteDurationMs: Int,
        volume: Float
    ): ShortArray {
        val samplesPerNote = (SAMPLE_RATE * (noteDurationMs / 1000.0)).toInt()
        val totalSamples = samplesPerNote * frequencies.size
        val buffer = ShortArray(totalSamples)

        for (n in frequencies.indices) {
            val freq = frequencies[n]
            for (i in 0 until samplesPerNote) {
                val index = n * samplesPerNote + i
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / samplesPerNote
                val envelope = 1.0 - progress
                val sample = sin(2.0 * PI * freq * t) * envelope * volume
                buffer[index] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return buffer
    }

    private fun generateHarmonicBell(
        freq1: Double,
        freq2: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-progress * 3.5)
            val sample = (sin(2.0 * PI * freq1 * t) * 0.6 + sin(2.0 * PI * freq2 * t) * 0.4) * envelope * volume
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateShimmer(
        frequencies: DoubleArray,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = (1.0 - progress) * (sin(2.0 * PI * 20.0 * t) * 0.2 + 0.8)
            var sum = 0.0
            for (freq in frequencies) {
                sum += sin(2.0 * PI * freq * t)
            }
            val sample = (sum / frequencies.size) * envelope * volume
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateExplosion(durationMs: Int, volume: Float): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var lowPass = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-progress * 4.0)

            val rumble = sin(2.0 * PI * (80.0 * (1.0 - progress * 0.5)) * t) * 0.5
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.5
            val raw = rumble + noise

            lowPass += 0.25 * (raw - lowPass)
            val sample = lowPass * envelope * volume
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateGameOverSlide(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq - (startFreq - endFreq) * (progress * progress)
            phase += 2.0 * PI * currentFreq / SAMPLE_RATE

            val vibrato = sin(2.0 * PI * 6.0 * (i.toDouble() / SAMPLE_RATE)) * 0.05
            val envelope = (1.0 - progress)
            val sample = sin(phase) * (envelope + vibrato).coerceIn(0.0, 1.0) * volume
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun pcmToWav(pcmData: ShortArray): ByteArray {
        val byteDataLength = pcmData.size * 2
        val totalDataLen = byteDataLength + 36
        val sampleRate = SAMPLE_RATE
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val bos = ByteArrayOutputStream(44 + byteDataLength)
        val dos = DataOutputStream(bos)

        dos.writeBytes("RIFF")
        dos.writeInt(Integer.reverseBytes(totalDataLen))
        dos.writeBytes("WAVE")

        dos.writeBytes("fmt ")
        dos.writeInt(Integer.reverseBytes(16))
        dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
        dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        dos.writeInt(Integer.reverseBytes(sampleRate))
        dos.writeInt(Integer.reverseBytes(byteRate))
        dos.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt())
        dos.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())

        dos.writeBytes("data")
        dos.writeInt(Integer.reverseBytes(byteDataLength))

        val byteBuffer = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }
        dos.write(byteBuffer.array())
        dos.flush()

        return bos.toByteArray()
    }
}
