package com.ontime.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ontime.app.OnTimeApp
import com.ontime.app.data.PreferencesManager

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_STOP_ALARM = "com.ontime.app.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.ontime.app.ACTION_SNOOZE_ALARM"
        var isRinging = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_SNOOZE_ALARM) {
            val alarmId = intent.getStringExtra("ALARM_ID")
            if (alarmId != null) {
                AlarmScheduler(this).scheduleSnooze(alarmId, 10)
            }
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getStringExtra("ALARM_ID") ?: ""
        val isPuzzle = intent?.getBooleanExtra("IS_PUZZLE", false) ?: false
        val vibrate = intent?.getBooleanExtra("VIBRATE", true) ?: true
        val audioUriStr = intent?.getStringExtra("AUDIO_URI")
        val label = intent?.getStringExtra("LABEL") ?: "Alarme"

        startForegroundWithNotification(alarmId, isPuzzle, label)
        acquireWakeLock()
        startPlaying(audioUriStr, vibrate)
        isRinging = true

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "OnTime:AlarmWakeLock"
            )
            wakeLock?.acquire(15 * 60 * 1000L) // Máximo 15 minutos de toque contínuo
        }
    }

    private fun startForegroundWithNotification(alarmId: String, isPuzzle: Boolean, label: String) {
        val fullScreenIntent = Intent(this, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("IS_PUZZLE", isPuzzle)
            putExtra("LABEL", label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.hashCode() + 500,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Botão Desligar na notificação caso a tela demore
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            alarmId.hashCode() + 501,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Botão Soneca
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra("ALARM_ID", alarmId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId.hashCode() + 502,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, OnTimeApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("On Time: $label")
            .setContentText("Despertador tocando!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        if (!isPuzzle) {
            builder.addAction(android.R.drawable.ic_media_pause, "Soneca (+10m)", snoozePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desligar", stopPendingIntent)
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                OnTimeApp.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(OnTimeApp.NOTIFICATION_ID, notification)
        }
    }

    private fun startPlaying(customAudioUri: String?, shouldVibrate: Boolean) {
        val prefs = PreferencesManager(this)
        val universalVolume = prefs.universalVolume // 10 a 100

        // Configuração do AudioManager para forçar o volume especificado no STREAM_ALARM
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVol = ((universalVolume / 100f) * maxVol).toInt().coerceIn(1, maxVol)

        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
        } catch (e: Exception) {
            Log.e("AlarmService", "Erro ao ajustar volume do sistema: ${e.message}")
        }

        // Tocar áudio
        try {
            val audioUri = if (!customAudioUri.isNullOrBlank()) {
                Uri.parse(customAudioUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, audioUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                // Volume universal direto no próprio MediaPlayer
                val volRatio = universalVolume / 100f
                setVolume(volRatio, volRatio)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Falha ao reproduzir áudio personalizado, tentando padrão: ${e.message}")
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(this, fallbackUri).apply {
                    val volRatio = universalVolume / 100f
                    setVolume(volRatio, volRatio)
                    isLooping = true
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Vibração
        if (shouldVibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopAlarm() {
        isRinging = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

