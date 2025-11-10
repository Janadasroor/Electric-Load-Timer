package com.example.loadtimeresp.presentation.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loadtimeresp.presentation.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Orange accent color palette
private val OrangeAccent = Color(0xFFFF8A00)
private val OrangeDark = Color(0xFFE67700)
private val OrangeLight = Color(0xFFFFAD42)
private val BackgroundDark = Color(0xFF1A1A1A)
private val SurfaceDark = Color(0xFF2D2D2D)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSecondary = Color(0xFFB0B0B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESP8266ControlScreen(viewModel: MainViewModel) {
    val status by viewModel.status.collectAsState()
    val schedule by viewModel.schedule.collectAsState()

    var showScheduleDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        //viewModel.syncTime()
        viewModel.getStatus()
        viewModel.getSchedule()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ESP8266 Controller",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark
            ),
            actions = {
                IconButton(onClick = {
                    viewModel.getStatus()
                    viewModel.getSchedule()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = OrangeAccent
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(status)

            // Schedule Card
            ScheduleCard(
                schedule = schedule,
                onSetSchedule = { showScheduleDialog = true },
                onClearSchedule = { viewModel.clearSchedule() }
            )

            // Control Buttons
            ControlButtons(
                onSyncTime = { showSyncDialog = true },
                onRefresh = {
                    viewModel.getStatus()
                    viewModel.getSchedule()
                }
            )

            // Improved Log Window
            ImprovedLogWindow(viewModel)
        }
    }

    // Dialogs
    if (showScheduleDialog) {
        ScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { start, end, startPeriod, endPeriod ->
                viewModel.setSchedule(
                    start.first, start.second,
                    end.first, end.second,
                    startPeriod, endPeriod
                )
                showScheduleDialog = false
            }
        )
    }

    if (showSyncDialog) {
        SyncTimeDialog(
            onDismiss = { showSyncDialog = false },
            onConfirm = { hour, min, sec ->
                viewModel.syncTime(hour, min, sec)
                showSyncDialog = false
            }
        )
    }
}

@Composable
fun StatusCard(status: com.example.loadtimeresp.data.api.StatusResponse?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (status != null) {
                StatusRow("LED State", if (status.ledState) "ON" else "OFF", status.ledState)
                StatusRow("Schedule", if (status.scheduleValid) "Active" else "Inactive", status.scheduleValid)
                StatusRow("Time Init", if (status.timeInitialized) "Yes" else "No", status.timeInitialized)

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Time: %02d:%02d".format(status.currentHour, status.currentMinute),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAccent
                    )
                }
            } else {
                Text(
                    text = "Loading...",
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) OrangeAccent else Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: com.example.loadtimeresp.data.api.ScheduleResponse?,
    onSetSchedule: () -> Unit,
    onClearSchedule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (schedule?.valid == true) {
                    TextButton(onClick = onClearSchedule) {
                        Text("Clear", color = Color(0xFFFF5252))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (schedule?.valid == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeDisplay("Start", "${schedule.startHour}:${"%02d".format(schedule.startMinute)} ${schedule.startPeriod}")
                    TimeDisplay("End", "${schedule.endHour}:${"%02d".format(schedule.endMinute)} ${schedule.endPeriod}")
                }
            } else {
                Text(
                    text = "No schedule set",
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSetSchedule,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (schedule?.valid == true) "Update Schedule" else "Set Schedule")
            }
        }
    }
}

@Composable
fun TimeDisplay(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            color = OrangeAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ControlButtons(onSyncTime: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSyncTime,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OrangeAccent
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Time")
        }

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OrangeAccent
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh")
        }
    }
}

@Composable
fun ImprovedLogWindow(viewModel: MainViewModel) {
    val logs by viewModel.logList.collectAsState()
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Activity Log",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = OrangeAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${logs.size} entries",
                        color = OrangeAccent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Log content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, Color(0xFF2D2D2D), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No logs yet...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "[${timeFormat.format(Date())}]",
                                    color = OrangeLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = line,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Pair<Int, Int>, Pair<Int, Int>, String, String) -> Unit
) {
    var startHour by remember { mutableStateOf(9) }
    var startMin by remember { mutableStateOf(0) }
    var startPeriod by remember { mutableStateOf("AM") }

    var endHour by remember { mutableStateOf(5) }
    var endMin by remember { mutableStateOf(0) }
    var endPeriod by remember { mutableStateOf("PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Set Schedule", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Start Time", color = OrangeAccent, fontWeight = FontWeight.Bold)
                TimePickerRow(startHour, startMin, startPeriod,
                    onHourChange = { startHour = it },
                    onMinChange = { startMin = it },
                    onPeriodChange = { startPeriod = it }
                )

                Divider(color = TextSecondary.copy(alpha = 0.3f))

                Text("End Time", color = OrangeAccent, fontWeight = FontWeight.Bold)
                TimePickerRow(endHour, endMin, endPeriod,
                    onHourChange = { endHour = it },
                    onMinChange = { endMin = it },
                    onPeriodChange = { endPeriod = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Pair(startHour, startMin),
                        Pair(endHour, endMin),
                        startPeriod, endPeriod
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncTimeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var min by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var sec by remember { mutableStateOf(calendar.get(Calendar.SECOND)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Sync Time", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Current device time: %02d:%02d:%02d".format(hour, min, sec),
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val now = Calendar.getInstance()
                        hour = now.get(Calendar.HOUR_OF_DAY)
                        min = now.get(Calendar.MINUTE)
                        sec = now.get(Calendar.SECOND)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("Use Current Time")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, min, sec) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun TimePickerRow(
    hour: Int,
    min: Int,
    period: String,
    onHourChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit,
    onPeriodChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberPicker(hour, 1..12, onHourChange)
        Text(":", color = TextPrimary, fontSize = 24.sp)
        NumberPicker(min, 0..59, onMinChange)

        Column {
            TextButton(
                onClick = { onPeriodChange("AM") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (period == "AM") OrangeAccent else TextSecondary
                )
            ) {
                Text("AM", fontWeight = if (period == "AM") FontWeight.Bold else FontWeight.Normal)
            }
            TextButton(
                onClick = { onPeriodChange("PM") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (period == "PM") OrangeAccent else TextSecondary
                )
            ) {
                Text("PM", fontWeight = if (period == "PM") FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun NumberPicker(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            val newVal = if (value + 1 > range.last) range.first else value + 1
            onValueChange(newVal)
        }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = OrangeAccent)
        }
        Text(
            text = "%02d".format(value),
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            val newVal = if (value - 1 < range.first) range.last else value - 1
            onValueChange(newVal)
        }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = OrangeAccent)
        }
    }
}