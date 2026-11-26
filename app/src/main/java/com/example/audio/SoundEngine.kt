package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class MusicTrackMode {
    MENU,
    ENVIRONMENT,
    VICTORY,
    SILENT
}

class SoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var musicJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private var soundPool: SoundPool? = null
    private var sfxPulseId = 0
    private var sfxScatterId = 0
    private var sfxLanceId = 0
    private var sfxFootstepId = 0
    private var sfxJumpId = 0
    private var sfxPickupId = 0
    private var sfxHitId = 0
    private var sfxElimId = 0
    private var sfxReloadId = 0
    private var sfxUiClickId = 0
    private var sfxReady = false

    @Volatile
    var musicVolume: Float = 0.65f

    @Volatile
    var sfxVolume: Float = 0.85f

    @Volatile
    private var currentTrackMode: MusicTrackMode = MusicTrackMode.MENU

    @Volatile
    private var isPaused: Boolean = false

    fun initialize() {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()
        soundPool = pool

        scope.launch {
            val dir = File(context.cacheDir, "synth_sfx").apply { mkdirs() }
            sfxPulseId = loadSynth(pool, File(dir, "pulse.wav")) { t, dur ->
                val env = exp(-t * 28.0)
                val freq = 720.0 - 480.0 * (t / dur)
                (sin(2.0 * PI * freq * t) * 0.7 + (Random.nextDouble() - 0.5) * 0.35) * env
            }
            sfxScatterId = loadSynth(pool, File(dir, "scatter.wav"), 0.22) { t, dur ->
                val env = exp(-t * 16.0)
                val punch = sin(2.0 * PI * (190.0 - 120.0 * t / dur) * t)
                (punch * 0.5 + (Random.nextDouble() - 0.5) * 0.75) * env
            }
            sfxLanceId = loadSynth(pool, File(dir, "lance.wav"), 0.26) { t, dur ->
                val env = exp(-t * 14.0)
                val f = 1150.0 * exp(-t * 6.0)
                (sin(2.0 * PI * f * t) * 0.8 + sin(2.0 * PI * f * 1.5 * t) * 0.25) * env
            }
            sfxFootstepId = loadSynth(pool, File(dir, "step.wav"), 0.08) { t, _ ->
                val env = exp(-t * 45.0)
                ((Random.nextDouble() - 0.5) * 0.65 + sin(2.0 * PI * 95.0 * t) * 0.45) * env
            }
            sfxJumpId = loadSynth(pool, File(dir, "jump.wav"), 0.16) { t, dur ->
                val env = sin(PI * (t / dur))
                val f = 220.0 + 320.0 * (t / dur)
                sin(2.0 * PI * f * t) * 0.5 * env
            }
            sfxPickupId = loadSynth(pool, File(dir, "pickup.wav"), 0.22) { t, _ ->
                val f = if (t < 0.09) 660.0 else 990.0
                val env = exp(-t * 10.0)
                sin(2.0 * PI * f * t) * 0.6 * env
            }
            sfxHitId = loadSynth(pool, File(dir, "hit.wav"), 0.09) { t, _ ->
                val env = exp(-t * 38.0)
                sin(2.0 * PI * 1400.0 * t) * 0.7 * env
            }
            sfxElimId = loadSynth(pool, File(dir, "elim.wav"), 0.34) { t, _ ->
                val f = when {
                    t < 0.10 -> 523.25
                    t < 0.20 -> 659.25
                    else -> 783.99
                }
                val env = exp(-t * 6.5)
                (sin(2.0 * PI * f * t) + 0.3 * sin(2.0 * PI * f * 2.0 * t)) * 0.55 * env
            }
            sfxReloadId = loadSynth(pool, File(dir, "reload.wav"), 0.18) { t, _ ->
                val click1 = if (t < 0.05) exp(-t * 60.0) * sin(2.0 * PI * 900.0 * t) else 0.0
                val click2 = if (t > 0.10) exp(-(t - 0.10) * 60.0) * sin(2.0 * PI * 1250.0 * t) else 0.0
                (click1 + click2) * 0.65
            }
            sfxUiClickId = loadSynth(pool, File(dir, "click.wav"), 0.05) { t, _ ->
                exp(-t * 75.0) * sin(2.0 * PI * 980.0 * t) * 0.5
            }
            sfxReady = true
        }

        startMusicLoop()
    }

    private fun loadSynth(
        pool: SoundPool,
        file: File,
        durationSec: Double = 0.14,
        sampleFn: (t: Double, dur: Double) -> Double
    ): Int {
        val sampleRate = 22050
        val numSamples = (sampleRate * durationSec).toInt()
        val pcm = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val s = sampleFn(t, durationSec).coerceIn(-1.0, 1.0)
            val v = (s * 32767.0).toInt().toShort()
            pcm[i * 2] = (v.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        writeWavFile(file, pcm, sampleRate)
        return pool.load(file.absolutePath, 1)
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * 2
        val header = ByteArray(44)
        // RIFF
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1) // PCM
        writeShortLE(header, 22, 1) // Mono
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, 2)
        writeShortLE(header, 34, 16)
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcmData.size)

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmData)
        }
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, v: Int) {
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((v shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, v: Int) {
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
    }

    private fun startMusicLoop() {
        if (musicJob?.isActive == true) return
        musicJob = scope.launch {
            val sampleRate = 22050
            val chunkSamples = 2205 // 100ms chunks
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkSamples * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack = track
            track.play()

            val buffer = ShortArray(chunkSamples)
            var sampleIndex = 0L
            var windFilter = 0.0
            val menuChords = arrayOf(
                doubleArrayOf(110.0, 164.81, 220.0, 261.63), // Am7
                doubleArrayOf(87.31, 130.81, 174.61, 220.0), // Fmaj7
                doubleArrayOf(98.0, 146.83, 196.0, 246.94),  // G6
                doubleArrayOf(82.41, 123.47, 164.81, 220.0)  // Em
            )
            val victoryNotes = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 392.0, 523.25, 659.25, 783.99)

            while (isActive) {
                if (isPaused || musicVolume <= 0.01f || currentTrackMode == MusicTrackMode.SILENT) {
                    Thread.sleep(80)
                    continue
                }
                val vol = musicVolume
                val mode = currentTrackMode
                for (i in 0 until chunkSamples) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val raw = when (mode) {
                        MusicTrackMode.MENU -> {
                            val chordIdx = ((t / 2.5).toInt()) % menuChords.size
                            val chord = menuChords[chordIdx]
                            val beatPhase = (t % 0.625) / 0.625
                            val pulse = exp(-beatPhase * 4.5) * 0.25
                            var pad = 0.0
                            for (f in chord) {
                                pad += sin(2.0 * PI * f * t) * 0.14
                            }
                            val bass = sin(2.0 * PI * (chord[0] * 0.5) * t) * (0.28 + pulse)
                            (pad + bass) * 0.55
                        }
                        MusicTrackMode.ENVIRONMENT -> {
                            val white = Random.nextDouble() - 0.5
                            windFilter = windFilter * 0.96 + white * 0.04
                            val swell = 0.5 + 0.5 * sin(2.0 * PI * 0.14 * t)
                            val drone = sin(2.0 * PI * 65.41 * t) * 0.08
                            windFilter * 1.4 * swell + drone
                        }
                        MusicTrackMode.VICTORY -> {
                            val step = ((t / 0.28).toInt()) % victoryNotes.size
                            val localT = t % 0.28
                            val note = victoryNotes[step]
                            val env = exp(-localT * 5.0)
                            val chord = sin(2.0 * PI * 130.81 * t) * 0.18 + sin(2.0 * PI * 196.0 * t) * 0.14
                            (sin(2.0 * PI * note * t) * 0.35 * env + chord) * 0.75
                        }
                        MusicTrackMode.SILENT -> 0.0
                    }
                    buffer[i] = (raw * vol * 16000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    sampleIndex++
                }
                track.write(buffer, 0, chunkSamples)
            }
        }
    }

    fun setTrackMode(mode: MusicTrackMode) {
        currentTrackMode = mode
    }

    fun onPause() {
        isPaused = true
        try {
            audioTrack?.pause()
            soundPool?.autoPause()
        } catch (_: Throwable) {
        }
    }

    fun onResume() {
        isPaused = false
        try {
            audioTrack?.play()
            soundPool?.autoResume()
        } catch (_: Throwable) {
        }
    }

    fun release() {
        musicJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
        soundPool?.release()
        soundPool = null
    }

    private fun playSfx(soundId: Int, rate: Float = 1f, gain: Float = 1f) {
        if (!sfxReady || isPaused || sfxVolume <= 0.01f || soundId == 0) return
        val v = (sfxVolume * gain).coerceIn(0f, 1f)
        soundPool?.play(soundId, v, v, 1, 0, rate)
    }

    fun playWeaponShot(weaponOrdinal: Int) {
        when (weaponOrdinal) {
            1 -> playSfx(sfxScatterId, 0.95f + Random.nextFloat() * 0.1f, 0.95f)
            3 -> playSfx(sfxLanceId, 0.98f + Random.nextFloat() * 0.05f, 0.95f)
            else -> playSfx(sfxPulseId, 0.92f + Random.nextFloat() * 0.16f, 0.85f)
        }
    }

    fun playFootstep(isSprint: Boolean) {
        playSfx(sfxFootstepId, if (isSprint) 1.18f else 0.94f, 0.38f)
    }

    fun playJump() = playSfx(sfxJumpId, 1.0f, 0.65f)
    fun playPickup() = playSfx(sfxPickupId, 1.0f, 0.85f)
    fun playHitMarker() = playSfx(sfxHitId, 1.05f, 0.75f)
    fun playElimination() = playSfx(sfxElimId, 1.0f, 0.95f)
    fun playReload() = playSfx(sfxReloadId, 1.0f, 0.7f)
    fun playUiClick() = playSfx(sfxUiClickId, 1.0f, 0.55f)
}
