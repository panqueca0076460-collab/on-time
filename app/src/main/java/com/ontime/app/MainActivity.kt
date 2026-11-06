package com.ontime.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ontime.app.alarm.AlarmScheduler
import com.ontime.app.data.PreferencesManager
import com.ontime.app.model.AlarmItem
import com.ontime.app.ui.dialogs.PermissionsHelperDialog
import com.ontime.app.ui.dialogs.ThemeSelectorDialog
import com.ontime.app.ui.screens.AlarmsTabContent
import com.ontime.app.ui.screens.StopwatchTabContent
import com.ontime.app.ui.screens.TimerTabContent
import com.ontime.app.ui.theme.DarkBackground
import com.ontime.app.ui.theme.ThemePalette

enum class AppTab(val title: String) {
    ALARMS("Alarmes"),
    PUZZLE("Puzzle"),
    STOPWATCH("Cronômetro"),
    TIMER("Timer")
}

class MainActivity : ComponentActivity() {
    private lateinit var prefs: PreferencesManager
    private lateinit var scheduler: AlarmScheduler

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(this)
        scheduler = AlarmScheduler(this)

        // Pedir permissão de Notificação no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MainScreen(prefs = prefs, scheduler = scheduler)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: PreferencesManager,
    scheduler: AlarmScheduler
) {
    var selectedTab by remember { mutableStateOf(AppTab.ALARMS) }
    var currentTheme by remember { mutableStateOf(prefs.selectedTheme) }
    var alarmsList by remember { mutableStateOf(prefs.getAlarms()) }
    var universalVolume by remember { mutableIntStateOf(prefs.universalVolume) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    fun refreshAlarms() {
        alarmsList = prefs.getAlarms()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "On Time",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(currentTheme.primary)
                        )
                    }
                },
                actions = {
                    // Botão de Segurança e Autonomia (Permissões de segundo plano)
                    IconButton(onClick = { showPermissionsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Autonomia Total",
                            tint = currentTheme.secondary
                        )
                    }

                    // Botão de Trocar Paleta de Cores
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Cores do Tema",
                            tint = currentTheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF101016),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.ALARMS,
                    onClick = { selectedTab = AppTab.ALARMS },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarmes") },
                    label = { Text("Alarmes", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.primary,
                        selectedTextColor = currentTheme.primary,
                        indicatorColor = currentTheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.PUZZLE,
                    onClick = { selectedTab = AppTab.PUZZLE },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Puzzle") },
                    label = { Text("Puzzle", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.primary,
                        selectedTextColor = currentTheme.primary,
                        indicatorColor = currentTheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.STOPWATCH,
                    onClick = { selectedTab = AppTab.STOPWATCH },
                    icon = { Icon(Icons.Default.Timelapse, contentDescription = "Cronômetro") },
                    label = { Text("Cronômetro", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.primary,
                        selectedTextColor = currentTheme.primary,
                        indicatorColor = currentTheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.TIMER,
                    onClick = { selectedTab = AppTab.TIMER },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer") },
                    label = { Text("Timer", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.primary,
                        selectedTextColor = currentTheme.primary,
                        indicatorColor = currentTheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    AppTab.ALARMS -> {
                        AlarmsTabContent(
                            isPuzzleTab = false,
                            alarms = alarmsList,
                            universalVolume = universalVolume,
                            theme = currentTheme,
                            onVolumeChange = { vol ->
                                universalVolume = vol
                                prefs.universalVolume = vol
                            },
                            onToggleAlarm = { alarm, isChecked ->
                                val updated = alarm.copy(isEnabled = isChecked)
                                prefs.updateAlarm(updated)
                                scheduler.scheduleAlarm(updated)
                                refreshAlarms()
                            },
                            onSaveAlarm = { alarm ->
                                prefs.updateAlarm(alarm)
                                scheduler.scheduleAlarm(alarm)
                                refreshAlarms()
                            },
                            onDeleteAlarm = { id ->
                                scheduler.cancelAlarm(id)
                                prefs.deleteAlarm(id)
                                refreshAlarms()
                            }
                        )
                    }

                    AppTab.PUZZLE -> {
                        AlarmsTabContent(
                            isPuzzleTab = true,
                            alarms = alarmsList,
                            universalVolume = universalVolume,
                            theme = currentTheme,
                            onVolumeChange = { vol ->
                                universalVolume = vol
                                prefs.universalVolume = vol
                            },
                            onToggleAlarm = { alarm, isChecked ->
                                val updated = alarm.copy(isEnabled = isChecked)
                                prefs.updateAlarm(updated)
                                scheduler.scheduleAlarm(updated)
                                refreshAlarms()
                            },
                            onSaveAlarm = { alarm ->
                                prefs.updateAlarm(alarm)
                                scheduler.scheduleAlarm(alarm)
                                refreshAlarms()
                            },
                            onDeleteAlarm = { id ->
                                scheduler.cancelAlarm(id)
                                prefs.deleteAlarm(id)
                                refreshAlarms()
                            }
                        )
                    }

                    AppTab.STOPWATCH -> {
                        StopwatchTabContent(theme = currentTheme)
                    }

                    AppTab.TIMER -> {
                        TimerTabContent(theme = currentTheme)
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = currentTheme,
            onThemeSelected = { newTheme ->
                currentTheme = newTheme
                prefs.selectedTheme = newTheme
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showPermissionsDialog) {
        PermissionsHelperDialog(
            theme = currentTheme,
            onDismiss = { showPermissionsDialog = false }
        )
    }
}

