package com.ontime.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ontime.app.model.AlarmItem
import com.ontime.app.ui.dialogs.AlarmEditDialog
import com.ontime.app.ui.theme.CardBackground
import com.ontime.app.ui.theme.DarkBackground
import com.ontime.app.ui.theme.ThemePalette

@Composable
fun AlarmsTabContent(
    isPuzzleTab: Boolean,
    alarms: List<AlarmItem>,
    universalVolume: Int,
    theme: ThemePalette,
    onVolumeChange: (Int) -> Unit,
    onToggleAlarm: (AlarmItem, Boolean) -> Unit,
    onSaveAlarm: (AlarmItem) -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val filteredAlarms = remember(alarms, isPuzzleTab) {
        alarms.filter { it.isPuzzle == isPuzzleTab }
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
            // Cartão de Volume Universal (Não depende do volume do celular!)
            item {
                UniversalVolumeCard(
                    volume = universalVolume,
                    theme = theme,
                    onVolumeChange = onVolumeChange
                )
            }

            if (isPuzzleTab) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B182B))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = theme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Despertador com Quebra-Cabeça",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Exige resolver o mini puzzle 3x3 para poder desligar ou adiar.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            if (filteredAlarms.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isPuzzleTab) Icons.Default.Extension else Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPuzzleTab) "Nenhum despertador puzzle ativo" else "Nenhum despertador cadastrado",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toque no botão + abaixo para agendar um horário",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                items(filteredAlarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        theme = theme,
                        onToggle = { isChecked -> onToggleAlarm(alarm, isChecked) },
                        onClick = { editingAlarm = alarm }
                    )
                }
            }
        }

        // Botão Flutuante para Adicionar Novo Alarme
        FloatingActionButton(
            onClick = { isCreatingNew = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = theme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar Alarme",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Modal de Edição / Criação
    if (isCreatingNew) {
        AlarmEditDialog(
            initialAlarm = null,
            isPuzzleDefault = isPuzzleTab,
            theme = theme,
            onDismiss = { isCreatingNew = false },
            onSave = { newAlarm ->
                onSaveAlarm(newAlarm)
                isCreatingNew = false
            }
        )
    }

    if (editingAlarm != null) {
        AlarmEditDialog(
            initialAlarm = editingAlarm,
            isPuzzleDefault = isPuzzleTab,
            theme = theme,
            onDismiss = { editingAlarm = null },
            onSave = { updated ->
                onSaveAlarm(updated)
                editingAlarm = null
            },
            onDelete = { id ->
                onDeleteAlarm(id)
                editingAlarm = null
            }
        )
    }
}

@Composable
fun UniversalVolumeCard(
    volume: Int,
    theme: ThemePalette,
    onVolumeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = theme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Volume Universal do Alarme",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "$volume%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.secondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toca exatamente nesta altura, mesmo se o celular estiver no silencioso ou vibrar.",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = volume.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.primary,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = Color(0xFF282838)
                )
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    theme: ThemePalette,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Horário bem nítido
                Text(
                    text = alarm.formattedTime(),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (alarm.isEnabled) Color.White else Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarm.label,
                    fontSize = 14.sp,
                    color = if (alarm.isEnabled) theme.secondary else Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alarm.vibrate) {
                        Text(
                            text = "📳 Vibra",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    if (alarm.customAudioUri != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = theme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = alarm.customAudioName ?: "Áudio custom",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                        }
                    }

                    if (alarm.daysOfWeek.isNotEmpty()) {
                        Text(
                            text = "Repete (${alarm.daysOfWeek.size} dias)",
                            fontSize = 11.sp,
                            color = theme.primary
                        )
                    }
                }
            }

            // Interruptor Ativar / Desativar
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = theme.primary,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF262635)
                )
            )
        }
    }
}

