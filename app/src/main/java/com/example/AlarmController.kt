package com.example

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.exp
import kotlin.math.sin

enum class AlarmSoundPreset(val id: String, val displayName: String, val isSynth: Boolean, val frequency: Double) {
    SYSTEM_ALARM("system_alarm", "System Default Alarm", false, 0.0),
    SYSTEM_RINGTONE("system_ringtone", "System Default Ringtone", false, 0.0),
    SYSTEM_NOTIFICATION("system_notification", "System Default Notification", false, 0.0),
    ETHEREAL_CHIME("ethereal_chime", "Ethereal Chime (528Hz)", true, 528.0),
    ZEN_RESONANCE("zen_resonance", "Zen Harmony (432Hz)", true, 432.0),
    MINIMAL_PULSE("minimal_pulse", "Minimal Pulse (880Hz)", true, 880.0)
}

class AlarmController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isRinging = false

    // Synthesizer variables
    private var synthTrack: AudioTrack? = null
    private var isSynthPlaying = false
    private var synthThread: Thread? = null

    private val prefs: SharedPreferences = context.getSharedPreferences("focus_sound_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PREF_SOUND = "selected_alarm_sound_id"
    }

    init {
        // Initialize appropriate vibrator service based on API level
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun getSelectedSound(): AlarmSoundPreset {
        val savedId = prefs.getString(KEY_PREF_SOUND, AlarmSoundPreset.ETHEREAL_CHIME.id)
        return AlarmSoundPreset.values().firstOrNull { it.id == savedId } ?: AlarmSoundPreset.ETHEREAL_CHIME
    }

    fun saveSelectedSound(preset: AlarmSoundPreset) {
        prefs.edit().putString(KEY_PREF_SOUND, preset.id).apply()
    }

    @Synchronized
    fun startAlarm() {
        if (isRinging) return
        isRinging = true
        Log.i("AlarmController", "Starting alarm sound and vibration")

        val currentPreset = getSelectedSound()

        if (currentPreset.isSynth) {
            startSynthesizer(currentPreset)
        } else {
            startSystemSound(currentPreset)
        }

        // 2. Vibrate
        try {
            val pattern = longArrayOf(0, 800, 800) // Vibrate 800ms, sleep 800ms
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 1)) // 1 is index to repeat
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Vibration starting error", e)
        }
    }

    private fun startSystemSound(preset: AlarmSoundPreset) {
        try {
            var alarmUri: Uri? = when (preset) {
                AlarmSoundPreset.SYSTEM_ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                AlarmSoundPreset.SYSTEM_RINGTONE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                AlarmSoundPreset.SYSTEM_NOTIFICATION -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (alarmUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                triggerRingtoneFallback()
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed starting system sound, trying ringtone fallback", e)
            triggerRingtoneFallback()
        }
    }

    private fun triggerRingtoneFallback() {
        try {
            val toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (toneUri != null) {
                fallbackRingtone = RingtoneManager.getRingtone(context, toneUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Ringtone fallback failed entirely", e)
        }
    }

    /**
     * Synthesizes and streams sine wave therapeutic tones with organic decay profiles.
     */
    private fun startSynthesizer(preset: AlarmSoundPreset) {
        stopSynthesizer()
        isSynthPlaying = true

        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(sampleRate)

        try {
            @Suppress("DEPRECATION")
            synthTrack = AudioTrack(
                AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            synthThread = Thread {
                val samples = ShortArray(1024)
                var globalSampleIndex = 0L

                // Envelope config: chime repetition rate
                val cycleSamples = (sampleRate * 2.0).toInt() // Repeats every 2 seconds

                synthTrack?.let { track ->
                    try {
                        track.play()
                    } catch (e: Exception) {
                        Log.e("AlarmController", "Failed starting audio track playback", e)
                    }

                    while (isSynthPlaying) {
                        for (i in samples.indices) {
                            val localSample = (globalSampleIndex % cycleSamples).toInt()
                            val timeInSec = localSample.toDouble() / sampleRate

                            // Elegant decay chime profile
                            val envelope = if (preset == AlarmSoundPreset.MINIMAL_PULSE) {
                                // Pulsing beep: 200ms on, 200ms off
                                val pulseIndex = (globalSampleIndex % (sampleRate * 0.4)).toInt()
                                if (pulseIndex < sampleRate * 0.2) 0.8 else 0.0
                            } else {
                                // Organic resonant attack & decay envelope
                                if (timeInSec < 0.04) {
                                    timeInSec / 0.04 // Fast attack linear gradient
                                } else {
                                    exp(-2.2 * (timeInSec - 0.04)) // Smooth acoustic mechanical exponential decay
                                }
                            }

                            // Calculate sine wave sample
                            val angle = 2.0 * java.lang.Math.PI * preset.frequency * globalSampleIndex / sampleRate
                            val rawValue = sin(angle) * Short.MAX_VALUE * envelope

                            samples[i] = rawValue.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            globalSampleIndex++
                        }
                        if (isSynthPlaying) {
                            track.write(samples, 0, samples.size)
                        }
                    }
                }
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed initializing audio track for preset synth play", e)
        }
    }

    private fun stopSynthesizer() {
        isSynthPlaying = false
        synthThread?.interrupt()
        synthThread = null
        try {
            synthTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        stop()
                    } catch (e: Exception) {}
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Error releasing AudioTrack", e)
        }
        synthTrack = null
    }

    /**
     * Plays a brief 2-second preview of the sound for selection menu feedback.
     */
    fun playPreview(preset: AlarmSoundPreset) {
        stopAlarm()
        isRinging = true
        Log.i("AlarmController", "Playing sounding preview for preset ${preset.displayName}")

        if (preset.isSynth) {
            startSynthesizer(preset)
        } else {
            startSystemSound(preset)
        }

        // Automatically scheduling audio teardown after 2.5s for crisp feedback previews
        Thread {
            try {
                Thread.sleep(2500)
                if (isRinging && getSelectedSound().id == prefs.getString(KEY_PREF_SOUND, "")) {
                    // Stop ONLY if user has not already started a timer or clicked something else
                    stopAlarm()
                } else {
                    // Always guarantee preview stop on safe teardown triggers
                    stopAudio()
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun stopAudio() {
        stopSynthesizer()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {}

        try {
            fallbackRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            fallbackRingtone = null
        } catch (e: Exception) {}
    }

    @Synchronized
    fun stopAlarm() {
        if (!isRinging) return
        isRinging = false
        Log.i("AlarmController", "Stopping alarm and vibration")

        stopAudio()

        // 2. Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmController", "Error canceling vibration", e)
        }
    }
}
