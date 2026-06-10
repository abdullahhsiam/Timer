package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isRinging = false

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

    @Synchronized
    fun startAlarm() {
        if (isRinging) return
        isRinging = true
        Log.i("AlarmController", "Starting alarm sound and vibration")

        // 1. Play Sound
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
            Log.e("AlarmController", "Failed starting alarm via MediaPlayer, trying ringtone fallback", e)
            triggerRingtoneFallback()
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

    @Synchronized
    fun stopAlarm() {
        if (!isRinging) return
        isRinging = false
        Log.i("AlarmController", "Stopping alarm and vibration")

        // 1. Stop audio players
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmController", "Error releasing MediaPlayer", e)
        }

        try {
            fallbackRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            fallbackRingtone = null
        } catch (e: Exception) {
            Log.e("AlarmController", "Error releasing Ringtone", e)
        }

        // 2. Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmController", "Error canceling vibration", e)
        }
    }
}
