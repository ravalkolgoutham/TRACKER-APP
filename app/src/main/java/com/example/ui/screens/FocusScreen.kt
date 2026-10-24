package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FocusSession
import com.example.service.FocusTimerService
import com.example.ui.components.OneUiHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiError
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    var selectedPresetMinutes by remember { mutableIntStateOf(25) }

    val presets = listOf(
        15 to "Sprint (15m)",
        25 to "Pomodoro (25m)",
        45 to "Deep Work (45m)",
        60 to "Flow State (60m)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OneUiHeader(
            title = "Focus Timer",
            subtitle = "$totalFocusMinutes mins total completed • Background Foreground Service Active",
            isSyncing = false,
            isGoogleConnected = true,
            onSyncClick = {}
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Circular Timer Canvas
        val progress = if (totalSeconds > 0) {
            remainingSeconds.toFloat() / totalSeconds.toFloat()
        } else 0f
        val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

        Box(
            modifier = Modifier
                .size(270.dp)
                .testTag("circular_focus_timer"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

                // Track background circle
                drawCircle(
                    color = DarkBorder,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Progress sweep arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(OneUiPrimary, OneUiSecondary, OneUiPrimary)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inside Timer Content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val formattedTime = String.format("%02d:%02d", mins, secs)

                Text(
                    text = formattedTime,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentTaskTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = OneUiSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (timerStatus) {
                    FocusTimerService.TimerStatus.RUNNING -> "Active • Foreground Service"
                    FocusTimerService.TimerStatus.PAUSED -> "Paused"
                    FocusTimerService.TimerStatus.COMPLETED -> "Finished 🎉"
                    FocusTimerService.TimerStatus.IDLE -> "Ready to Focus"
                }
                val statusColor = when (timerStatus) {
                    FocusTimerService.TimerStatus.RUNNING -> OneUiSuccess
                    FocusTimerService.TimerStatus.PAUSED -> OneUiWarning
                    FocusTimerService.TimerStatus.COMPLETED -> OneUiPrimary
                    FocusTimerService.TimerStatus.IDLE -> TextSecondary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controls: Play / Pause / Resume / Stop with generous size and touch targets
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (timerStatus) {
                FocusTimerService.TimerStatus.IDLE, FocusTimerService.TimerStatus.COMPLETED -> {
                    Button(
                        onClick = {
                            onStartTimer(selectedPresetMinutes, "Deep Work ($selectedPresetMinutes mins)")
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .width(190.dp)
                            .testTag("start_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Start Focus", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiWarning)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Pause", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

                FocusTimerService.TimerStatus.PAUSED -> {
                    Button(
                        onClick = onResumeTimer,
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp)
                            .testTag("resume_focus_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Resume", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

        Spacer(modifier = Modifier.height(32.dp))

        // Preset Duration Chips
        if (timerStatus == FocusTimerService.TimerStatus.IDLE || timerStatus == FocusTimerService.TimerStatus.COMPLETED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Select Session Length",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                                    if (isSelected) OneUiPrimary else DarkBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPresetMinutes = mins },
                            color = if (isSelected) OneUiPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) OneUiPrimary else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label.substringBefore(" "),
                                    fontSize = 11.sp,
                                    color = if (isSelected) OneUiPrimary else TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Completed Sessions Card with Zen Hero Art
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Focus Momentum",
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
                            text = "Start a focus sprint to enter deep work and log sessions here automatically.",
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
