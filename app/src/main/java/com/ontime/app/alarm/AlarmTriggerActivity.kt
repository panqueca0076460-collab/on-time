package com.ontime.app.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ontime.app.data.PreferencesManager
import com.ontime.app.ui.theme.ThemePalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class AlarmTriggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração para abrir mesmo com tela bloqueada, desligada ou não perturbe
        turnScreenOnAndKeyguard()

        val alarmId = intent.getStringExtra("ALARM_ID") ?: ""
        val isPuzzle = intent.getBooleanExtra("IS_PUZZLE", false)
        val label = intent.getStringExtra("LABEL") ?: "Alarme"
        val prefs = PreferencesManager(this)
        val theme = prefs.selectedTheme

        setContent {
            AlarmScreen(
                alarmId = alarmId,
                isPuzzle = isPuzzle,
                label = label,
                theme = theme,
                onDismiss = {
                    stopAlarmService()
                    finish()
                },
                onSnooze = {
                    snoozeAlarmService(alarmId)
                    finish()
                }
            )
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(intent)
    }

    private fun snoozeAlarmService(alarmId: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE_ALARM
            putExtra("ALARM_ID", alarmId)
        }
        startService(intent)
    }
}

@Composable
fun AlarmScreen(
    alarmId: String,
    isPuzzle: Boolean,
    label: String,
    theme: ThemePalette,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var currentSecondsStr by remember {
        mutableStateOf(SimpleDateFormat("ss", Locale.getDefault()).format(Date()))
    }
    var currentDateStr by remember {
        mutableStateOf(SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date()))
    }

    // Atualiza o relógio a cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentSecondsStr = SimpleDateFormat("ss", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    var puzzleSolved by remember { mutableStateOf(!isPuzzle) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Topo: Indicador do Alarme e Tema
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = theme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label.uppercase(Locale.ROOT),
                        color = theme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentDateStr.replaceFirstChar { it.uppercase() },
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }

            // Centro: HORÁRIO BEM GRANDE NO MEIO (como pedido especificamente)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTimeStr,
                        fontSize = 82.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = ":$currentSecondsStr",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.secondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }

                if (isPuzzle && !puzzleSolved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🧠 Resolva o mini quebra-cabeça para liberar as opções!",
                        color = theme.secondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quebra cabeça 3x3 de emoji
                    EmojiPuzzle3x3(
                        accentColor = theme.primary,
                        onSolved = {
                            puzzleSolved = true
                        }
                    )
                }
            }

            // Abaixo: OPÇÕES DE SONECA E DESLIGAR
            if (puzzleSolved) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Botão Soneca (+10m)
                        Button(
                            onClick = onSnooze,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262630)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = "Soneca",
                                    tint = theme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Soneca",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "+10 minutos",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        // Botão Desligar
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlarmOff,
                                    contentDescription = "Desligar",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Desligar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "🔒 Bloqueado até resolver o quebra-cabeça",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// Mini Quebra-cabeça 3x3 deslizante de emojis
@Composable
fun EmojiPuzzle3x3(
    accentColor: Color,
    onSolved: () -> Unit
) {
    // Escolhe um conjunto divertido de emojis temáticos
    val targetOrder = remember { listOf("🚀", "⭐", "🌙", "🪐", "☀️", "🌍", "🛸", "👾", "") }
    var board by remember {
        val initial = ArrayList(targetOrder)
        // Garante que está embaralhado mas solucionável
        do {
            Collections.shuffle(initial)
        } while (initial == targetOrder)
        mutableStateOf(initial.toList())
    }

    fun isSolved(current: List<String>): Boolean {
        return current == targetOrder
    }

    fun moveTile(index: Int) {
        val emptyIndex = board.indexOf("")
        val row = index / 3
        val col = index % 3
        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3

        val isAdjacent = (kotlin.math.abs(row - emptyRow) + kotlin.math.abs(col - emptyCol)) == 1
        if (isAdjacent) {
            val mutable = board.toMutableList()
            mutable[emptyIndex] = board[index]
            mutable[index] = ""
            board = mutable
            if (isSolved(mutable)) {
                onSolved()
            }
        }
    }

    Column(
        modifier = Modifier
            .size(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161622))
            .border(2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until 3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until 3) {
                    val idx = r * 3 + c
                    val item = board[idx]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (item.isNotEmpty()) Color(0xFF2A2A38) else Color.Transparent
                            )
                            .clickable(enabled = item.isNotEmpty()) {
                                moveTile(idx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isNotEmpty()) {
                            Text(
                                text = item,
                                fontSize = 28.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

