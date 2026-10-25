package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeOptionsBottomSheet(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSnoozeSelected: (minutes: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag("snooze_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        tint = OneUiWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Snooze Reminder",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Text(
                text = "Reschedule 15m notification for '${task.title}'",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            SnoozeOptionRow(
                icon = Icons.Default.Alarm,
                title = "10 Minutes",
                subtitle = "Quick pause to finish current thought",
                onClick = { onSnoozeSelected(10) },
                tag = "snooze_10m"
            )

            SnoozeOptionRow(
                icon = Icons.Default.AccessTime,
                title = "1 Hour",
                subtitle = "Later today after focus session",
                onClick = { onSnoozeSelected(60) },
                tag = "snooze_1h"
            )

            SnoozeOptionRow(
                icon = Icons.Default.NightlightRound,
                title = "This Evening (6:00 PM)",
                subtitle = "Wind down and wrap up",
                onClick = {
                    val now = System.currentTimeMillis()
                    val eveningCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = now
                        set(java.util.Calendar.HOUR_OF_DAY, 18)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                        if (timeInMillis <= now + 5 * 60_000L) {
                            add(java.util.Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    val minsUntilEvening = ((eveningCal.timeInMillis - now) / 60_000L).toInt().coerceAtLeast(15)
                    onSnoozeSelected(minsUntilEvening)
                },
                tag = "snooze_evening"
            )

            SnoozeOptionRow(
                icon = Icons.Default.DateRange,
                title = "Tomorrow Morning (9:00 AM)",
                subtitle = "Reschedule alarm for tomorrow",
                onClick = {
                    val now = System.currentTimeMillis()
                    val tomorrowCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = now
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 9)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val minsUntilTomorrow = ((tomorrowCal.timeInMillis - now) / 60_000L).toInt().coerceAtLeast(60)
                    onSnoozeSelected(minsUntilTomorrow)
                },
                tag = "snooze_tomorrow"
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SnoozeOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OneUiWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = OneUiWarning, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
