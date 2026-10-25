package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FocusSession
import com.example.model.SyncQueueItem
import com.example.model.TaskItem
import com.example.service.FocusTimerService
import com.example.ui.components.OneUiHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiAccent
import com.example.ui.theme.OneUiError
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FocusModeType(val label: String, val defaultMins: Int, val tag: String) {
    POMODORO("Focus", 25, "Deep Work"),
    SHORT_BREAK("Short Break", 5, "Short Break"),
    LONG_BREAK("Long Break", 15, "Long Break")
}

data class BreakTimerPreset(
    val minutes: Int,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val mode: FocusModeType,
    val accentColor: Color
)

@Composable
fun FocusScreen(
    timerStatus: FocusTimerService.TimerStatus,
    remainingSeconds: Int,
    totalSeconds: Int,
    currentTaskTitle: String,
    totalFocusMinutes: Int,
    focusSessions: List<FocusSession>,
    onStartTimer: (minutes: Int, taskTitle: String) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier,
    tasks: List<TaskItem> = emptyList(),
    syncQueueLogs: List<SyncQueueItem> = emptyList(),
    onStartTimerWithTask: ((minutes: Int, taskId: String?, taskTitle: String, tag: String) -> Unit)? = null
) {
    var selectedMode by remember { mutableStateOf(FocusModeType.POMODORO) }
    var selectedPresetMinutes by remember { mutableIntStateOf(25) }
    var selectedBreakLabel by remember { mutableStateOf<String?>(null) }

    // Custom Break Cadence Builder state
    var configuredShortBreakMins by remember { mutableIntStateOf(5) }
    var configuredLongBreakMins by remember { mutableIntStateOf(15) }
    var configuredCyclesBeforeLongBreak by remember { mutableIntStateOf(4) }

    val activeTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    var selectedLinkedTask by remember(activeTasks) { mutableStateOf(activeTasks.firstOrNull()) }

    val modeAccentColor = when (selectedMode) {
        FocusModeType.POMODORO -> OneUiPrimary
        FocusModeType.SHORT_BREAK -> OneUiSecondary
        FocusModeType.LONG_BREAK -> OneUiAccent
    }

    val modeSecondaryColor = when (selectedMode) {
        FocusModeType.POMODORO -> OneUiSecondary
        FocusModeType.SHORT_BREAK -> OneUiSuccess
        FocusModeType.LONG_BREAK -> OneUiWarning
    }

    val presets = when (selectedMode) {
        FocusModeType.POMODORO -> listOf(
            15 to "Sprint",
            25 to "Pomodoro",
            45 to "Deep Work",
            60 to "Flow"
        )
        FocusModeType.SHORT_BREAK -> listOf(
            2 to "Eye Rest",
            5 to "Quick",
            10 to "Stretch",
            15 to "Recharge"
        )
        FocusModeType.LONG_BREAK -> listOf(
            15 to "Reset",
            20 to "Power Nap",
            30 to "Walk",
            45 to "Meal"
        )
    }

    val dedicatedBreakTimers = remember {
        listOf(
            BreakTimerPreset(2, "Micro Eye Rest", "20-20-20 screen relief", "👀", FocusModeType.SHORT_BREAK, OneUiSecondary),
            BreakTimerPreset(5, "Coffee & Water", "Quick hydration breather", "☕", FocusModeType.SHORT_BREAK, OneUiSuccess),
            BreakTimerPreset(10, "Desk Stretch", "Posture & mobility reset", "🧘", FocusModeType.SHORT_BREAK, OneUiPrimaryLight),
            BreakTimerPreset(15, "Power Recharge", "Step away & clear mind", "⚡", FocusModeType.LONG_BREAK, OneUiWarning),
            BreakTimerPreset(20, "Power Nap", "20-min alertness boost", "😴", FocusModeType.LONG_BREAK, OneUiAccent),
            BreakTimerPreset(30, "Walk & Meal Break", "Full outdoor recharge", "🚶", FocusModeType.LONG_BREAK, OneUiPrimary)
        )
    }

    fun startSelectedSession(minutes: Int, customTitle: String? = null, mode: FocusModeType = selectedMode) {
        val titleToUse = customTitle
            ?: if (mode == FocusModeType.POMODORO) {
                selectedLinkedTask?.title ?: "Focus ($minutes mins)"
            } else {
                selectedBreakLabel ?: "${mode.label} ($minutes mins)"
            }
        val taskIdToUse = if (mode == FocusModeType.POMODORO) selectedLinkedTask?.id else null
        if (onStartTimerWithTask != null) {
            onStartTimerWithTask(minutes, taskIdToUse, titleToUse, mode.tag)
        } else {
            onStartTimer(minutes, titleToUse)
        }
    }

    val isTimerIdle = timerStatus == FocusTimerService.TimerStatus.IDLE ||
            timerStatus == FocusTimerService.TimerStatus.COMPLETED

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OneUiHeader(
            title = "Focus & Break Timers",
            subtitle = "$totalFocusMinutes mins logged • Custom Focus & Break Engine",
            isSyncing = false,
            isGoogleConnected = true,
            onSyncClick = {},
            syncQueueLogs = syncQueueLogs
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Focus / Short Break / Long Break Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusModeType.values().forEach { mode ->
                val isSelected = selectedMode == mode
                val tabColor = when (mode) {
                    FocusModeType.POMODORO -> OneUiPrimary
                    FocusModeType.SHORT_BREAK -> OneUiSecondary
                    FocusModeType.LONG_BREAK -> OneUiAccent
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedMode = mode
                            selectedPresetMinutes = when (mode) {
                                FocusModeType.POMODORO -> 25
                                FocusModeType.SHORT_BREAK -> configuredShortBreakMins
                                FocusModeType.LONG_BREAK -> configuredLongBreakMins
                            }
                            selectedBreakLabel = if (mode == FocusModeType.POMODORO) null else "${mode.label} (${selectedPresetMinutes}m)"
                        }
                        .testTag("focus_mode_${mode.name.lowercase()}"),
                    color = if (isSelected) tabColor else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.5f) else DarkBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task-Linked Focus Selector Strip (shown when in Focus mode & idle)
        if (selectedMode == FocusModeType.POMODORO && activeTasks.isNotEmpty() && isTimerIdle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Link Focus Session to Active Task (🍅 Pomodoro Counter)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUiSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeTasks.forEach { task ->
                        val isLinked = selectedLinkedTask?.id == task.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedLinkedTask = task }
                                .testTag("focus_link_task_${task.id}"),
                            color = if (isLinked) OneUiSecondary.copy(alpha = 0.22f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isLinked) OneUiSecondary else DarkBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLinked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLinked) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiWarning
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Circular Timer Canvas - dynamically updates when selecting presets/breaks in IDLE mode
        val displaySeconds = if (isTimerIdle) selectedPresetMinutes * 60 else remainingSeconds
        val displayTotalSeconds = if (isTimerIdle) (selectedPresetMinutes * 60).coerceAtLeast(1) else totalSeconds.coerceAtLeast(1)

        val progress = if (isTimerIdle) {
            1f
        } else {
            displaySeconds.toFloat() / displayTotalSeconds.toFloat()
        }
        val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

        Box(
            modifier = Modifier
                .size(265.dp)
                .testTag("circular_focus_timer"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = DarkBorder,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(modeAccentColor, modeSecondaryColor, modeAccentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = selectedMode.label.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = modeAccentColor,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val mins = displaySeconds / 60
                val secs = displaySeconds % 60
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                Text(
                    text = formattedTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val activeSubtitle = if (!isTimerIdle) {
                    currentTaskTitle
                } else if (selectedMode == FocusModeType.POMODORO) {
                    selectedLinkedTask?.title ?: "Deep Work Session"
                } else {
                    selectedBreakLabel ?: "${selectedMode.label} (${selectedPresetMinutes}m)"
                }

                Text(
                    text = activeSubtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = modeSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (timerStatus) {
                    FocusTimerService.TimerStatus.RUNNING -> "Active • Timestamp Engine"
                    FocusTimerService.TimerStatus.PAUSED -> "Paused"
                    FocusTimerService.TimerStatus.COMPLETED -> "Completed 🎉 (+50 XP)"
                    FocusTimerService.TimerStatus.IDLE -> "Ready • ${selectedPresetMinutes}m Selected"
                }
                val statusColor = when (timerStatus) {
                    FocusTimerService.TimerStatus.RUNNING -> OneUiSuccess
                    FocusTimerService.TimerStatus.PAUSED -> OneUiWarning
                    FocusTimerService.TimerStatus.COMPLETED -> OneUiPrimary
                    FocusTimerService.TimerStatus.IDLE -> modeAccentColor
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Fine-Tune Duration Stepper (-5m, -1m, +1m, +5m) when IDLE
        if (isTimerIdle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(-5, -1, +1, +5).forEach { delta ->
                    val sign = if (delta > 0) "+$delta" else "$delta"
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPresetMinutes = (selectedPresetMinutes + delta).coerceIn(1, 120)
                                if (selectedMode != FocusModeType.POMODORO) {
                                    selectedBreakLabel = "${selectedMode.label} (${selectedPresetMinutes}m)"
                                }
                            }
                            .testTag("adjust_timer_${delta}m"),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${sign}m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Controls: Play / Pause / Resume / Stop + Quick Break Trigger
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            when (timerStatus) {
                FocusTimerService.TimerStatus.IDLE, FocusTimerService.TimerStatus.COMPLETED -> {
                    Button(
                        onClick = {
                            startSelectedSession(selectedPresetMinutes)
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f)
                            .testTag("start_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = modeAccentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start ${selectedMode.label} (${selectedPresetMinutes}m)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedMode == FocusModeType.POMODORO) {
                        Surface(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedMode = FocusModeType.SHORT_BREAK
                                    selectedPresetMinutes = configuredShortBreakMins
                                    selectedBreakLabel = "Short Break (${configuredShortBreakMins}m)"
                                    startSelectedSession(
                                        minutes = configuredShortBreakMins,
                                        customTitle = "☕ Short Break (${configuredShortBreakMins}m)",
                                        mode = FocusModeType.SHORT_BREAK
                                    )
                                }
                                .testTag("quick_start_break_button"),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, OneUiSecondary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Coffee,
                                    contentDescription = "Start Break",
                                    tint = OneUiSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${configuredShortBreakMins}m Break",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSecondary
                                )
                            }
                        }
                    }
                }

                FocusTimerService.TimerStatus.RUNNING -> {
                    Button(
                        onClick = onPauseTimer,
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp)
                            .testTag("pause_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = OneUiWarning, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Pause", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStopTimer,
                        modifier = Modifier
                            .height(56.dp)
                            .width(130.dp)
                            .testTag("stop_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = OneUiError
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = OneUiError, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "End", color = OneUiError, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FocusTimerService.TimerStatus.PAUSED -> {
                    Button(
                        onClick = onResumeTimer,
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp)
                            .testTag("resume_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = modeAccentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Resume", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStopTimer,
                        modifier = Modifier
                            .height(56.dp)
                            .width(130.dp)
                            .testTag("stop_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = OneUiError, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "End", color = OneUiError, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preset Duration Chips for Current Mode
        if (isTimerIdle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Quick ${selectedMode.label} Presets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presets.forEach { (mins, label) ->
                        val isSelected = selectedPresetMinutes == mins
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) modeAccentColor else DarkBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedPresetMinutes = mins
                                    if (selectedMode != FocusModeType.POMODORO) {
                                        selectedBreakLabel = "$label Break (${mins}m)"
                                    }
                                }
                                .testTag("preset_chip_${mins}m"),
                            color = if (isSelected) modeAccentColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) modeSecondaryColor else TextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dedicated Break Timers Hub (Multiple Break Timers with 1-Tap Set or Start)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(OneUiSecondary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = OneUiSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Break Timers Hub",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Choose different break lengths or launch immediately",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid of 6 Break Timer Cards (2 rows of 3 or 3 rows of 2)
                        dedicatedBreakTimers.chunked(2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPresets.forEach { breakPreset ->
                                    val isBreakActiveSelection =
                                        selectedMode == breakPreset.mode && selectedPresetMinutes == breakPreset.minutes
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedMode = breakPreset.mode
                                                selectedPresetMinutes = breakPreset.minutes
                                                selectedBreakLabel = "${breakPreset.emoji} ${breakPreset.title}"
                                            }
                                            .testTag("break_timer_card_${breakPreset.minutes}m"),
                                        color = if (isBreakActiveSelection) {
                                            breakPreset.accentColor.copy(alpha = 0.22f)
                                        } else {
                                            DarkSurfaceElevated
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isBreakActiveSelection) breakPreset.accentColor else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${breakPreset.emoji} ${breakPreset.minutes}m",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isBreakActiveSelection) Color.White else breakPreset.accentColor
                                                )

                                                // Instant Play mini-button on each Break Timer card
                                                Surface(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            selectedMode = breakPreset.mode
                                                            selectedPresetMinutes = breakPreset.minutes
                                                            selectedBreakLabel = "${breakPreset.emoji} ${breakPreset.title}"
                                                            startSelectedSession(
                                                                minutes = breakPreset.minutes,
                                                                customTitle = "${breakPreset.emoji} ${breakPreset.title} (${breakPreset.minutes}m)",
                                                                mode = breakPreset.mode
                                                            )
                                                        }
                                                        .testTag("instant_start_break_${breakPreset.minutes}m"),
                                                    color = breakPreset.accentColor.copy(alpha = 0.25f),
                                                    shape = CircleShape
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Start ${breakPreset.title}",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = breakPreset.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = breakPreset.subtitle,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Break Cadence Configurator (Short Break, Long Break, Cycles)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Custom Break Schedule Settings",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Configure your default Short Break, Long Break & Pomodoro interval:",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Short Break Default Selector
                                BreakDurationRow(
                                    label = "Short Break Timer",
                                    valueMinutes = configuredShortBreakMins,
                                    options = listOf(2, 5, 10, 15),
                                    accentColor = OneUiSecondary,
                                    onSelect = { mins ->
                                        configuredShortBreakMins = mins
                                        if (selectedMode == FocusModeType.SHORT_BREAK) {
                                            selectedPresetMinutes = mins
                                            selectedBreakLabel = "Short Break (${mins}m)"
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Long Break Default Selector
                                BreakDurationRow(
                                    label = "Long Break Timer",
                                    valueMinutes = configuredLongBreakMins,
                                    options = listOf(15, 20, 30, 45),
                                    accentColor = OneUiAccent,
                                    onSelect = { mins ->
                                        configuredLongBreakMins = mins
                                        if (selectedMode == FocusModeType.LONG_BREAK) {
                                            selectedPresetMinutes = mins
                                            selectedBreakLabel = "Long Break (${mins}m)"
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Custom Slider for any exact break duration (1m - 60m)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Custom Timer Dial",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "$selectedPresetMinutes mins",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modeAccentColor
                                    )
                                }

                                Slider(
                                    value = selectedPresetMinutes.toFloat().coerceIn(1f, 90f),
                                    onValueChange = { newVal ->
                                        selectedPresetMinutes = newVal.toInt().coerceAtLeast(1)
                                        if (selectedMode != FocusModeType.POMODORO) {
                                            selectedBreakLabel = "Custom ${selectedMode.label} (${selectedPresetMinutes}m)"
                                        }
                                    },
                                    valueRange = 1f..90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = modeAccentColor,
                                        activeTrackColor = modeAccentColor,
                                        inactiveTrackColor = DarkBorder
                                    ),
                                    modifier = Modifier.testTag("custom_duration_slider")
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Completed Sessions Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Focus & Break History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "$totalFocusMinutes mins logged • ${focusSessions.size} sessions",
                                fontSize = 12.5.sp,
                                color = TextSecondary
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_focus_zen),
                            contentDescription = "Zen focus illustration",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (focusSessions.isEmpty()) {
                        Text(
                            text = "Start a focus sprint or timed recharge break above. Completed focus sessions increment task Pomodoros and award +50 XP.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    } else {
                        focusSessions.take(4).forEach { session ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(OneUiSuccess.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = OneUiSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = session.taskTitle ?: "Deep Work",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            val timeFormat = SimpleDateFormat("h:mm a • MMM d", Locale.getDefault())
                                            Text(
                                                text = timeFormat.format(Date(session.completedAt)),
                                                fontSize = 11.5.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "+${session.durationMinutes}m",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUiSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun BreakDurationRow(
    label: String,
    valueMinutes: Int,
    options: List<Int>,
    accentColor: Color,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { mins ->
                val isSelected = valueMinutes == mins
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(mins) },
                    color = if (isSelected) accentColor else DarkSurface,
                    border = BorderStroke(1.dp, if (isSelected) accentColor else DarkBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${mins}m",
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
