package com.ontime.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ontime.app.ui.theme.CardBackground
import com.ontime.app.ui.theme.DarkBackground
import com.ontime.app.ui.theme.ThemePalette
import kotlinx.coroutines.delay

@Composable
fun StopwatchTabContent(theme: ThemePalette) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var lastStartTimestamp by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            lastStartTimestamp = System.currentTimeMillis() - elapsedTimeMs
            while (isRunning) {
                elapsedTimeMs = System.currentTimeMillis() - lastStartTimestamp
                delay(16) // ~60fps para precisão nos centésimos
            }
        }
    }

    val totalSeconds = elapsedTimeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (elapsedTimeMs % 1000) / 10

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Mostrador Circular Estiloso do Cronômetro
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Progresso animado dos segundos
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    drawCircle(
                        color = Color(0xFF1E1E28),
                        style = Stroke(strokeWidth)
                    )
                    val sweepAngle = ((elapsedTimeMs % 60000L) / 60000f) * 360f
                    drawArc(
                        color = theme.primary,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = String.format(".%02d", millis),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = theme.secondary,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = if (isRunning) "EM EXECUÇÃO" else "PARADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isRunning) theme.primary else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CONTROLES PEDIDOS ESPECIFICAMENTE:
            // "iniciar com aquele triângulo de lado, pausar com o quadrado e reiniciar com a seta girando"
            // "detalhe: a opção de iniciar se torna a de pausar quando clicada e vice-versa"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Reiniciar (Seta girando)
                IconButton(
                    onClick = {
                        isRunning = false
                        elapsedTimeMs = 0L
                        laps.clear()
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF232332))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Botão Principal (Triângulo de Iniciar vira Quadrado de Pausar e vice-versa)
                IconButton(
                    onClick = {
                        isRunning = !isRunning
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(theme.primary)
                ) {
                    if (isRunning) {
                        // Quadrado para pausar
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Pausar",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        // Triângulo de lado para iniciar
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Botão Volta / Lap
                IconButton(
                    onClick = {
                        if (elapsedTimeMs > 0) {
                            laps.add(0, elapsedTimeMs)
                        }
                    },
                    enabled = isRunning,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF232332) else Color(0xFF181822))
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Volta",
                        tint = if (isRunning) theme.secondary else Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de Voltas (Laps)
            if (laps.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(laps) { index, lapTime ->
                        val lapTotalSec = lapTime / 1000
                        val lapMin = lapTotalSec / 60
                        val lapSec = lapTotalSec % 60
                        val lapMil = (lapTime % 1000) / 10

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Volta ${laps.size - index}",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format("%02d:%02d.%02d", lapMin, lapSec, lapMil),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp
                            )
                        }
                        if (index < laps.size - 1) {
                            Divider(color = Color(0xFF222230), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

