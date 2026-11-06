package com.ontime.app.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ontime.app.model.TimerItem
import com.ontime.app.ui.dialogs.NumberPickerBox
import com.ontime.app.ui.theme.CardBackground
import com.ontime.app.ui.theme.CardBackgroundElevated
import com.ontime.app.ui.theme.DarkBackground
import com.ontime.app.ui.theme.ThemePalette
import kotlinx.coroutines.delay

@Composable
fun TimerTabContent(theme: ThemePalette) {
    val context = LocalContext.current
    val timers = remember {
        mutableStateListOf(
            TimerItem(totalSeconds = 300, remainingSeconds = 300), // 5 min
            TimerItem(totalSeconds = 600, remainingSeconds = 600)  // 10 min
        )
    }

    var isAddingNew by remember { mutableStateOf(false) }

    // Motor de contagem regressiva em tempo real para todos os timers ativos
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            for (i in timers.indices) {
                val t = timers[i]
                if (t.isRunning && t.remainingSeconds > 0) {
                    val nextSec = t.remainingSeconds - 1
                    timers[i] = t.copy(remainingSeconds = nextSec)
                    if (nextSec == 0L) {
                        // Avisar que o timer acabou
                        playTimerFinishedBeep(context)
                        timers[i] = timers[i].copy(isRunning = false)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (timers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum timer ativo",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(timers, key = { it.id }) { timer ->
                    TimerCard(
                        timer = timer,
                        theme = theme,
                        onToggle = {
                            val idx = timers.indexOfFirst { it.id == timer.id }
                            if (idx >= 0) {
                                timers[idx] = timer.copy(isRunning = !timer.isRunning)
                            }
                        },
                        onReset = {
                            val idx = timers.indexOfFirst { it.id == timer.id }
                            if (idx >= 0) {
                                timers[idx] = timer.copy(
                                    isRunning = false,
                                    remainingSeconds = timer.totalSeconds
                                )
                            }
                        },
                        onAddOneMinute = {
                            val idx = timers.indexOfFirst { it.id == timer.id }
                            if (idx >= 0) {
                                timers[idx] = timer.copy(
                                    remainingSeconds = timer.remainingSeconds + 60,
                                    totalSeconds = timer.totalSeconds + 60
                                )
                            }
                        },
                        onDelete = {
                            timers.removeAll { it.id == timer.id }
                        }
                    )
                }
            }
        }

        // FAB para adicionar novo timer
        FloatingActionButton(
            onClick = { isAddingNew = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = theme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar Timer",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (isAddingNew) {
        NewTimerDialog(
            theme = theme,
            onDismiss = { isAddingNew = false },
            onAdd = { minutes, seconds ->
                val total = (minutes * 60L) + seconds
                if (total > 0) {
                    timers.add(TimerItem(totalSeconds = total, remainingSeconds = total))
                }
                isAddingNew = false
            }
        )
    }
}

@Composable
fun TimerCard(
    timer: TimerItem,
    theme: ThemePalette,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAddOneMinute: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timer.formattedTime(),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (timer.remainingSeconds == 0L) Color(0xFFEF4444) else Color.White
                )

                // Botão de Excluir Timer
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de controles do Timer:
            // Iniciar/Pausar (triângulo / quadrado), Reiniciar (seta girando) e Adicionar 1 minuto (+1 min)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Iniciar / Pausar
                Button(
                    onClick = onToggle,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timer.isRunning) Color(0xFF2E2E3E) else theme.primary
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (timer.isRunning) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Pausar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Iniciar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iniciar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Reiniciar (seta girando)
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF222230))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Adicionar 1 minuto (+1 min)
                Button(
                    onClick = onAddOneMinute,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222230)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("+1 min", color = theme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun NewTimerDialog(
    theme: ThemePalette,
    onDismiss: () -> Unit,
    onAdd: (minutes: Int, seconds: Int) -> Unit
) {
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Novo Timer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberPickerBox(
                        value = minutes,
                        range = 0..99,
                        accentColor = theme.primary,
                        onValueChange = { minutes = it }
                    )
                    Text(
                        text = "m",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NumberPickerBox(
                        value = seconds,
                        range = 0..59,
                        accentColor = theme.primary,
                        onValueChange = { seconds = it }
                    )
                    Text(
                        text = "s",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }

                    Button(
                        onClick = { onAdd(minutes, seconds) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                    ) {
                        Text("Criar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

fun playTimerFinishedBeep(context: Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.play()

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

