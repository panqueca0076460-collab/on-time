package com.ontime.app.model

data class TimerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val totalSeconds: Long, // duração inicial escolhida
    val remainingSeconds: Long, // segundos restantes
    val isRunning: Boolean = false,
    val lastTickTimestamp: Long = 0L // para cálculo preciso mesmo com tela desligada
) {
    fun formattedTime(): String {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds % 3600) / 60
        val s = remainingSeconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}

