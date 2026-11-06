package com.ontime.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ontime.app.data.PreferencesManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "onReceive acionado com action: $action")

        val prefs = PreferencesManager(context)
        val scheduler = AlarmScheduler(context)

        // Se reiniciou o aparelho ou atualizou o app, reagenda todos os alarmes ativos
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            val alarms = prefs.getAlarms()
            for (alarm in alarms) {
                if (alarm.isEnabled) {
                    scheduler.scheduleAlarm(alarm)
                }
            }
            return
        }

        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)

        val alarm = prefs.getAlarmById(alarmId)
        if (alarm == null && !isSnooze) {
            return
        }

        // 1. Inicia o Serviço de Alarme em Primeiro Plano (garante execução e áudio mesmo com sistema matando apps)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("IS_PUZZLE", alarm?.isPuzzle ?: false)
            putExtra("VIBRATE", alarm?.vibrate ?: true)
            putExtra("AUDIO_URI", alarm?.customAudioUri)
            putExtra("LABEL", alarm?.label ?: "Alarme")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2. Abre imediatamente a Activity de tela cheia que sobrepõe qualquer bloqueio
        val activityIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("IS_PUZZLE", alarm?.isPuzzle ?: false)
            putExtra("LABEL", alarm?.label ?: "Alarme")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                     Intent.FLAG_ACTIVITY_CLEAR_TOP or
                     Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(activityIntent)

        // Se o alarme não repete dias da semana e não foi soneca, desativa para a próxima
        if (!isSnooze && alarm != null && alarm.daysOfWeek.isEmpty()) {
            val updated = alarm.copy(isEnabled = false)
            prefs.updateAlarm(updated)
        } else if (!isSnooze && alarm != null && alarm.daysOfWeek.isNotEmpty()) {
            // Reagenda para a próxima ocorrência
            scheduler.scheduleAlarm(alarm)
        }
    }
}

