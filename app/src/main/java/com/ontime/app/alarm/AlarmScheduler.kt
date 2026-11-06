package com.ontime.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ontime.app.model.AlarmItem
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerTimeMs = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ontime.app.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Usamos setAlarmClock para máxima prioridade do Android, furando modo Doze e sem restrições
        val showIntent = Intent(context, com.ontime.app.MainActivity::class.java)
        val infoPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, infoPendingIntent)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Alarm ${alarm.id} agendado para $triggerTimeMs")
        } catch (e: SecurityException) {
            // Em caso raro de permissão SCHEDULE_EXACT_ALARM revogada no Android 12+
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun scheduleSnooze(alarmId: String, snoozeMinutes: Int = 10) {
        val triggerTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ontime.app.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarmId)
            putExtra("IS_SNOOZE", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.ontime.app.MainActivity::class.java)
        val infoPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode() + 9999,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, infoPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun cancelAlarm(alarmId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.ontime.app.ACTION_TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Também cancela qualquer soneca pendente
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 9999,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            // Se já passou a hora de hoje, joga para amanhã
            if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Caso tenha dias da semana selecionados (1=Dom no Calendar, adaptamos para o nosso modelo)
        // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sab, 7=Dom
        for (i in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val modelDay = when (calDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            if (daysOfWeek.contains(modelDay)) {
                if (candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback: amanhã no mesmo horário
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }
}

