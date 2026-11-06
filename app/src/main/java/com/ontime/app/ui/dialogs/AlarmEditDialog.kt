package com.ontime.app.ui.dialogs

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ontime.app.model.AlarmItem
import com.ontime.app.ui.theme.CardBackgroundElevated
import com.ontime.app.ui.theme.ThemePalette

@Composable
fun AlarmEditDialog(
    initialAlarm: AlarmItem?,
    isPuzzleDefault: Boolean = false,
    theme: ThemePalette,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Alarme") }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    val isPuzzle by remember { mutableStateOf(initialAlarm?.isPuzzle ?: isPuzzleDefault) }
    var customAudioUri by remember { mutableStateOf(initialAlarm?.customAudioUri) }
    var customAudioName by remember { mutableStateOf(initialAlarm?.customAudioName ?: "Toque Padrão do Sistema") }
    var daysOfWeek by remember { mutableStateOf(initialAlarm?.daysOfWeek ?: emptySet()) }

    // Seletor de arquivo de áudio do dispositivo
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Manter permissão persistente de leitura do arquivo
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            customAudioUri = uri.toString()
            customAudioName = getFileNameFromUri(context, uri) ?: "Áudio Personalizado"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundElevated),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (initialAlarm == null) {
                        if (isPuzzle) "Novo Alarme com Puzzle 🧩" else "Novo Despertador"
                    } else {
                        "Editar Despertador"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Seleção de Hora e Minuto moderna
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NumberPickerBox(
                        value = hour,
                        range = 0..23,
                        accentColor = theme.primary,
                        onValueChange = { hour = it }
                    )
                    Text(
                        text = ":",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    NumberPickerBox(
                        value = minute,
                        range = 0..59,
                        accentColor = theme.primary,
                        onValueChange = { minute = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nome / Etiqueta
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nome do Alarme") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = Color(0xFF333344),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dias da semana
                Text(
                    text = "Repetir nos dias:",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("S" to 1, "T" to 2, "Q" to 3, "Q" to 4, "S" to 5, "S" to 6, "D" to 7)
                    days.forEach { (dName, dVal) ->
                        val selected = daysOfWeek.contains(dVal)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) theme.primary else Color(0xFF232330))
                                .clickable {
                                    daysOfWeek = if (selected) {
                                        daysOfWeek - dVal
                                    } else {
                                        daysOfWeek + dVal
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dName,
                                color = if (selected) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Selecionar Áudio / Toque personalizado
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF20202B))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = theme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Toque do Alarme",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = customAudioName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        if (customAudioUri != null) {
                            IconButton(onClick = {
                                customAudioUri = null
                                customAudioName = "Toque Padrão do Sistema"
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Limpar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Opção de Vibrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF20202B))
                        .clickable { vibrate = !vibrate }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = theme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Vibrar ao tocar",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = theme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botões de Ação Salvar / Cancelar / Excluir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialAlarm != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(initialAlarm.id)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF351B20))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val result = AlarmItem(
                                id = initialAlarm?.id ?: java.util.UUID.randomUUID().toString(),
                                hour = hour,
                                minute = minute,
                                label = label.ifBlank { "Alarme" },
                                isEnabled = true,
                                vibrate = vibrate,
                                isPuzzle = isPuzzle,
                                customAudioUri = customAudioUri,
                                customAudioName = customAudioName,
                                daysOfWeek = daysOfWeek
                            )
                            onSave(result)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPickerBox(
    value: Int,
    range: IntRange,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF22222F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
                onClick = {
                    val prev = if (value > range.first) value - 1 else range.last
                    onValueChange(prev)
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2B2B3A))
            ) {
                Text("-", fontSize = 18.sp, color = Color.White)
            }

            FilledTonalButton(
                onClick = {
                    val next = if (value < range.last) value + 1 else range.first
                    onValueChange(next)
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2B2B3A))
            ) {
                Text("+", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

