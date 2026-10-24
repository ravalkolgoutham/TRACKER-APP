package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TaskItem
import com.example.ui.components.OneUiHeader
import com.example.ui.components.TaskCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    tasks: List<TaskItem>,
    isSyncing: Boolean,
    isGoogleConnected: Boolean,
    onSyncClick: () -> Unit,
    onToggleComplete: (String) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onDelete: (String) -> Unit,
    onStartFocus: (taskId: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = remember {
        (0..13).map { offset ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            cal.time
        }
    }

    var selectedDate by remember { mutableStateOf(dates[0]) }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val dateYearDayFormat = SimpleDateFormat("yyyy-DDD", Locale.getDefault())
    val selectedDayKey = dateYearDayFormat.format(selectedDate)

    val dayTasks = tasks.filter { task ->
        val taskDayKey = dateYearDayFormat.format(Date(task.scheduledTime))
        taskDayKey == selectedDayKey
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        OneUiHeader(
            title = "Google Calendar Sync",
            subtitle = "${tasks.count { it.googleEventId != null }} events linked • Real-time conflict resolution",
            isSyncing = isSyncing,
            isGoogleConnected = isGoogleConnected,
            onSyncClick = onSyncClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Month and Year label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthYearFormat.format(selectedDate),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = OneUiSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OAuth 2.0 Connected",
                    fontSize = 12.5.sp,
                    color = OneUiSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Date Strip with generous breathing room
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dates.forEach { date ->
                val isSelected = dateYearDayFormat.format(date) == selectedDayKey
                val dayKey = dateYearDayFormat.format(date)
                val hasEvents = tasks.any { dateYearDayFormat.format(Date(it.scheduledTime)) == dayKey }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (isSelected) Color.White else DarkBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedDate = date }
                        .testTag("date_chip_${dayNumFormat.format(date)}"),
                    color = if (isSelected) DarkSurfaceVariant else DarkSurfaceElevated,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayFormat.format(date).uppercase(Locale.getDefault()),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayNumFormat.format(date),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Event indicator dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasEvents) {
                                        if (isSelected) Color.White else OneUiSecondary
                                    } else Color.Transparent
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selected Day Agenda Header
        val selectedHeaderFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedHeaderFormat.format(selectedDate),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Text(
                text = "${dayTasks.size} ${if (dayTasks.size == 1) "Event" else "Events"}",
                fontSize = 13.5.sp,
                color = OneUiSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (dayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_empty_tasks_illustration),
                        contentDescription = null,
                        modifier = Modifier.size(130.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No events scheduled for this day",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tasks added with dates or synced from Google Calendar will show here automatically.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = dayTasks,
                    key = { it.id }
                ) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleComplete(task.id) },
                        onSnooze = { onSnooze(task.id, 10) },
                        onDelete = { onDelete(task.id) },
                        onStartFocus = { onStartFocus(task.id, task.title) },
                        onClick = { /* Detail */ }
                    )
                }
            }
        }
    }
}
