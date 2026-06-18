package com.example.loadtimeresp.presentation.screens


import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loadtimeresp.R
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
fun ESP8266ControlScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val status by viewModel.status.collectAsState()
    val schedule by viewModel.schedule.collectAsState()

    var showScheduleDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    // Auto sync time and fetch updates when the app loads
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)
        val sec = calendar.get(Calendar.SECOND)

        viewModel.syncTime(hour, min, sec)
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
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.esp_controller),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark
            ),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = OrangeAccent
                    )
                }
                IconButton(onClick = {
                    viewModel.getStatus()
                    viewModel.getSchedule()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh),
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

            // Brightness Control Card
            BrightnessCard(
                brightness = status?.brightness ?: 1023,
                onBrightnessChange = { viewModel.setBrightness(it) }
            )

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
fun BrightnessCard(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit
) {
    var sliderValue by remember(brightness) { mutableFloatStateOf(brightness.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.brightness),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(sliderValue / 10.23f).toInt()}%",
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onBrightnessChange(sliderValue.toInt())
                },
                valueRange = 0f..1023f,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeAccent,
                    activeTrackColor = OrangeAccent,
                    inactiveTrackColor = OrangeAccent.copy(alpha = 0.24f)
                )
            )
        }
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
                    text = stringResource(R.string.device_status),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (status != null) {
                StatusRow(
                    stringResource(R.string.led_state),
                    if (status.ledState) stringResource(R.string.on) else stringResource(R.string.off),
                    status.ledState
                )
                StatusRow(
                    stringResource(R.string.schedule),
                    if (status.scheduleValid) stringResource(R.string.active) else stringResource(R.string.inactive),
                    status.scheduleValid
                )
                StatusRow(
                    stringResource(R.string.time_init),
                    if (status.timeInitialized) stringResource(R.string.yes) else stringResource(R.string.no),
                    status.timeInitialized
                )

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
                        text = stringResource(R.string.current_time, status.currentHour, status.currentMinute),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAccent
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.loading),
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
                        text = stringResource(R.string.schedule),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (schedule?.valid == true) {
                    TextButton(onClick = onClearSchedule) {
                        Text(stringResource(R.string.clear), color = Color(0xFFFF5252))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (schedule?.valid == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeDisplay(
                        stringResource(R.string.start),
                        "${schedule.startHour}:${"%02d".format(schedule.startMinute)} ${schedule.startPeriod}"
                    )
                    TimeDisplay(
                        stringResource(R.string.end),
                        "${schedule.endHour}:${"%02d".format(schedule.endMinute)} ${schedule.endPeriod}"
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_schedule_set),
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
                Text(
                    if (schedule?.valid == true)
                        stringResource(R.string.update_schedule)
                    else
                        stringResource(R.string.set_schedule)
                )
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
            border = BorderStroke(1.dp, OrangeAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.sync_time))
        }

        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OrangeAccent
            ),
            border = BorderStroke(1.dp, OrangeAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.refresh))
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
                        text = stringResource(R.string.activity_log),
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
                        text = stringResource(R.string.entries, logs.size),
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
                        text = stringResource(R.string.no_logs_yet),
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
            Text(stringResource(R.string.set_schedule), color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.start_time), color = OrangeAccent, fontWeight = FontWeight.Bold)
                TimePickerRow(startHour, startMin, startPeriod,
                    onHourChange = { startHour = it },
                    onMinChange = { startMin = it },
                    onPeriodChange = { startPeriod = it }
                )

                Divider(color = TextSecondary.copy(alpha = 0.3f))

                Text(stringResource(R.string.end_time), color = OrangeAccent, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
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
            Text(stringResource(R.string.sync_time), color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.current_device_time, hour, min, sec),
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
                    Text(stringResource(R.string.use_current_time))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, min, sec) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text(stringResource(R.string.sync))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerRow(
    hour: Int,
    min: Int,
    period: String,
    onHourChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit,
    onPeriodChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hour Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Hour", color = TextSecondary, fontSize = 12.sp)
                Text(text = "$hour", color = OrangeAccent, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = hour.toFloat(),
                onValueChange = { onHourChange(it.toInt()) },
                valueRange = 1f..12f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeAccent,
                    activeTrackColor = OrangeAccent,
                    inactiveTrackColor = OrangeAccent.copy(alpha = 0.24f)
                )
            )
        }

        // Minute Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Minute", color = TextSecondary, fontSize = 12.sp)
                Text(text = "%02d".format(min), color = OrangeAccent, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = min.toFloat(),
                onValueChange = { onMinChange(it.toInt()) },
                valueRange = 0f..59f,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeAccent,
                    activeTrackColor = OrangeAccent,
                    inactiveTrackColor = OrangeAccent.copy(alpha = 0.24f)
                )
            )
        }

        // AM/PM Selection Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = period == "AM",
                onClick = { onPeriodChange("AM") },
                label = { Text("AM") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangeAccent.copy(alpha = 0.1f),
                    selectedLabelColor = OrangeAccent,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = period == "AM",
                    borderColor = TextSecondary.copy(alpha = 0.3f),
                    selectedBorderColor = OrangeAccent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
            FilterChip(
                selected = period == "PM",
                onClick = { onPeriodChange("PM") },
                label = { Text("PM") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangeAccent.copy(alpha = 0.1f),
                    selectedLabelColor = OrangeAccent,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = period == "PM",
                    borderColor = TextSecondary.copy(alpha = 0.3f),
                    selectedBorderColor = OrangeAccent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}
