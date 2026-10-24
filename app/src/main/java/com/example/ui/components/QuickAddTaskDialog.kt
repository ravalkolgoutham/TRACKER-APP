package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Priority
import com.example.model.RepeatRule
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        scheduledTime: Long,
        endTime: Long,
        priority: Priority,
        category: String,
        repeatRule: RepeatRule,
        reminderMinutes: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val defaultScheduledTime = now + (60 * 60 * 1000L) // +1 hour default
    var scheduledTime by remember { mutableLongStateOf(defaultScheduledTime) }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedRepeatRule by remember { mutableStateOf(RepeatRule.NONE) }
    var reminderMinutes by remember { mutableIntStateOf(15) }

    val categories = listOf("Work", "Personal", "Focus", "Health", "Meeting")
    val repeatRules = listOf(RepeatRule.NONE, RepeatRule.DAILY, RepeatRule.WEEKDAYS, RepeatRule.WEEKLY, RepeatRule.MONTHLY)

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
                .verticalScroll(rememberScrollState())
                .testTag("quick_add_task_dialog")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Task or Event",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g., Code Review & Architecture") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OneUiPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Details (Optional)") },
                placeholder = { Text("Add agenda, links or context") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input"),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OneUiPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Time & Schedule Quick Presets
            Text(
                text = "Schedule Time (15m reminder automatic)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())

                PresetTimeChip(
                    label = "In 1 Hour",
                    time = sdf.format(Date(now + 3600000)),
                    isSelected = scheduledTime == (now + 3600000),
                    onClick = { scheduledTime = now + 3600000 }
                )

                // Today 6 PM
                val eveningCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 18)
                    set(Calendar.MINUTE, 0)
                }
                PresetTimeChip(
                    label = "This Evening",
                    time = "6:00 PM",
                    isSelected = scheduledTime == eveningCal.timeInMillis,
                    onClick = { scheduledTime = eveningCal.timeInMillis }
                )

                // Tomorrow 9 AM
                val tomorrowCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
                PresetTimeChip(
                    label = "Tomorrow",
                    time = "9:00 AM",
                    isSelected = scheduledTime == tomorrowCal.timeInMillis,
                    onClick = { scheduledTime = tomorrowCal.timeInMillis }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(text = "Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in categories) {
                    val isSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat },
                        color = if (isSelected) OneUiPrimary else DarkSurfaceElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(text = "Priority", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.values().forEach { prio ->
                    val isSelected = selectedPriority == prio
                    val prioColor = when (prio) {
                        Priority.URGENT -> OneUiError
                        Priority.HIGH -> OneUiWarning
                        Priority.MEDIUM -> OneUiPrimary
                        Priority.LOW -> OneUiSecondary
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, if (isSelected) prioColor else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedPriority = prio },
                        color = if (isSelected) prioColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = prio.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) prioColor else TextSecondary,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat Rule Selector
            Text(text = "Repeat Rule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rule in repeatRules) {
                    val isSelected = selectedRepeatRule == rule
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedRepeatRule = rule },
                        color = if (isSelected) OneUiSecondary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, OneUiSecondary) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rule.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) OneUiSecondary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val duration = 60 * 60 * 1000L
                        onSave(
                            title,
                            description,
                            scheduledTime,
                            scheduledTime + duration,
                            selectedPriority,
                            selectedCategory,
                            selectedRepeatRule,
                            reminderMinutes
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUiPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Save Task & Sync",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PresetTimeChip(
    label: String,
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isSelected) OneUiPrimary else DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) OneUiPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = label, fontSize = 11.sp, color = if (isSelected) OneUiPrimary else TextSecondary)
            Text(text = time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) OneUiPrimary else TextPrimary)
        }
    }
}
