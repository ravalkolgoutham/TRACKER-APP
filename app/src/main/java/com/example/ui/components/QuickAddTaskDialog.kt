package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.ui.theme.CategoryFocus
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class WheelMode {
    TIME,
    DATE
}

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

    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0..11
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    val initHour24 = initialCal.get(Calendar.HOUR_OF_DAY)
    var selectedHour12 by remember {
        mutableIntStateOf(
            when {
                initHour24 == 0 -> 12
                initHour24 > 12 -> initHour24 - 12
                else -> initHour24
            }
        )
    }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(initHour24 < 12) }

    var wheelMode by remember { mutableStateOf(WheelMode.TIME) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedRepeatRule by remember { mutableStateOf(RepeatRule.NONE) }
    var snoozeReminderEnabled by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableIntStateOf(15) }
    var showRepeatOptions by remember { mutableStateOf(false) }

    val categories = listOf("Work", "Personal", "Focus", "Health", "Meeting", "Finance")
    val repeatRules = listOf(RepeatRule.NONE, RepeatRule.DAILY, RepeatRule.WEEKDAYS, RepeatRule.WEEKLY, RepeatRule.MONTHLY)
    val durationOptions = listOf(15, 30, 45, 60, 90, 120)
    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val years = remember { (2025..2032).toList() }

    val maxDaysInCurrentMonth = remember(selectedYear, selectedMonth) {
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(maxDaysInCurrentMonth) {
        if (selectedDay > maxDaysInCurrentMonth) {
            selectedDay = maxDaysInCurrentMonth
        }
    }

    val scheduledTimeMillis by remember(
        selectedYear,
        selectedMonth,
        selectedDay,
        selectedHour12,
        selectedMinute,
        isAm
    ) {
        derivedStateOf {
            val hour24 = when {
                isAm && selectedHour12 == 12 -> 0
                !isAm && selectedHour12 == 12 -> 12
                !isAm -> selectedHour12 + 12
                else -> selectedHour12
            }
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay.coerceAtMost(maxDaysInCurrentMonth))
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val formattedTime = remember(selectedHour12, selectedMinute, isAm) {
        String.format(Locale.getDefault(), "%d:%02d %s", selectedHour12, selectedMinute, if (isAm) "AM" else "PM")
    }

    fun applyQuickDateOffset(daysToAdd: Int) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
    }

    fun triggerSave() {
        val finalTitle = title.trim().ifEmpty { "Scheduled Task" }
        val durationMs = durationMinutes * 60 * 1000L
        onSave(
            finalTitle,
            description.trim(),
            scheduledTimeMillis,
            scheduledTimeMillis + durationMs,
            selectedPriority,
            selectedCategory,
            selectedRepeatRule,
            if (snoozeReminderEnabled) reminderMinutes else 0
        )
        onDismiss()
    }

    val accentAmber = OneUiSecondary
    val sheetBg = DarkSurface
    val cardBg = DarkSurfaceElevated
    val wheelBarBg = OneUiPrimary.copy(alpha = 0.28f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(top = 16.dp)
                .testTag("quick_add_task_dialog")
        ) {
            // 1. iOS-Style Top Action Bar: Cancel | Add Task / Event | Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentAmber,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("cancel_task_button")
                )

                Text(
                    text = "New Task or Event",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Save",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (title.isNotBlank()) accentAmber else accentAmber.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { triggerSave() }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag("top_save_task_button")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Mode Switcher Pill (Time Drum vs Date Drum)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isTimeMode = wheelMode == WheelMode.TIME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTimeMode) OneUiPrimary else Color.Transparent)
                        .clickable { wheelMode = WheelMode.TIME }
                        .padding(vertical = 8.dp)
                        .testTag("pick_specific_time_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isTimeMode) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTimeMode) Color.White else TextSecondary
                        )
                    }
                }

                val isDateMode = wheelMode == WheelMode.DATE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDateMode) OneUiPrimary else Color.Transparent)
                        .clickable { wheelMode = WheelMode.DATE }
                        .padding(vertical = 8.dp)
                        .testTag("pick_specific_date_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isDateMode) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dateFormat.format(Date(scheduledTimeMillis)),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDateMode) Color.White else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Drum / Wheel Picker Area (matches reference image with center highlight bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center Selection Pill Highlight Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(wheelBarBg)
                )

                if (wheelMode == WheelMode.TIME) {
                    // 3-Column Drum Time Picker: Hour (1..12) | Minute (00..59) | AM/PM
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Wheel (1..12, circular)
                        val hours = remember { (1..12).toList() }
                        CircularWheelColumn(
                            items = hours,
                            selectedItem = selectedHour12,
                            onItemSelected = { selectedHour12 = it },
                            formatLabel = { "$it" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wheel_hour_column"),
                            textAlign = TextAlign.End
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        // Minute Wheel (00..59, circular)
                        val minutes = remember { (0..59).toList() }
                        CircularWheelColumn(
                            items = minutes,
                            selectedItem = selectedMinute,
                            onItemSelected = { selectedMinute = it },
                            formatLabel = { String.format(Locale.getDefault(), "%02d", it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wheel_minute_column"),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        // AM / PM Wheel (Bounded 2 items)
                        BoundedWheelColumn(
                            items = listOf("AM", "PM"),
                            selectedIndex = if (isAm) 0 else 1,
                            onIndexSelected = { idx -> isAm = (idx == 0) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wheel_ampm_column"),
                            textAlign = TextAlign.Start
                        )
                    }
                } else {
                    // 3-Column Drum Date Picker: Month (Jan..Dec) | Day (1..31) | Year (2025..2032)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Wheel
                        val monthIndices = remember { (0..11).toList() }
                        CircularWheelColumn(
                            items = monthIndices,
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            formatLabel = { monthNames[it] },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("wheel_month_column"),
                            textAlign = TextAlign.End
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Day Wheel
                        val days = remember(maxDaysInCurrentMonth) { (1..maxDaysInCurrentMonth).toList() }
                        CircularWheelColumn(
                            items = days,
                            selectedItem = selectedDay.coerceIn(1, maxDaysInCurrentMonth),
                            onItemSelected = { selectedDay = it },
                            formatLabel = { "$it" },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("wheel_day_column"),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Year Wheel
                        BoundedWheelColumn(
                            items = years.map { "$it" },
                            selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                            onIndexSelected = { idx -> selectedYear = years[idx] },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("wheel_year_column"),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                // Top & Bottom Cylindrical Fade Gradients
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(sheetBg, sheetBg.copy(alpha = 0f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(sheetBg.copy(alpha = 0f), sheetBg)
                            )
                        )
                )
            }

            // Quick Date Shortcut Chips when in Date Mode
            AnimatedVisibility(visible = wheelMode == WheelMode.DATE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDateChip("Today", onClick = { applyQuickDateOffset(0) }, modifier = Modifier.weight(1f))
                    QuickDateChip("Tomorrow", onClick = { applyQuickDateOffset(1) }, modifier = Modifier.weight(1f))
                    QuickDateChip("+2 Days", onClick = { applyQuickDateOffset(2) }, modifier = Modifier.weight(1f))
                    QuickDateChip("Next Week", onClick = { applyQuickDateOffset(7) }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Scrollable Grouped Settings Card (Matches Reference Image)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Row 1: Label (Task Title)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Label",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.width(90.dp)
                            )
                            BasicTextField(
                                value = title,
                                onValueChange = { title = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End
                                ),
                                cursorBrush = SolidColor(accentAmber),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("task_title_input"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (title.isEmpty()) {
                                            Text(
                                                text = "Morning Alarm / Sprint Task",
                                                color = TextTertiary,
                                                fontSize = 15.sp,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 16.dp))

                        // Row 2: Notes / Description
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.width(90.dp)
                            )
                            BasicTextField(
                                value = description,
                                onValueChange = { description = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.End
                                ),
                                cursorBrush = SolidColor(accentAmber),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("task_description_input"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (description.isEmpty()) {
                                            Text(
                                                text = "Optional agenda or link",
                                                color = TextTertiary,
                                                fontSize = 14.5.sp,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 16.dp))

                        // Row 3: Date Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    wheelMode = if (wheelMode == WheelMode.DATE) WheelMode.TIME else WheelMode.DATE
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dateFormat.format(Date(scheduledTimeMillis)),
                                    fontSize = 15.sp,
                                    color = if (wheelMode == WheelMode.DATE) accentAmber else TextSecondary,
                                    fontWeight = if (wheelMode == WheelMode.DATE) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 16.dp))

                        // Row 4: Repeat Selector Row (Matches Screenshot "Repeat   Weekdays >")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRepeatOptions = !showRepeatOptions }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedRepeatRule.label,
                                    fontSize = 15.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showRepeatOptions) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeatRules.forEach { rule ->
                                    val isSelected = selectedRepeatRule == rule
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedRepeatRule = rule
                                                showRepeatOptions = false
                                            },
                                        color = if (isSelected) OneUiPrimary else DarkSurfaceElevated,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = rule.label,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 16.dp))

                        // Row 5: Snooze & Reminder Toggle (Matches Screenshot green toggle switch)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Snooze & Reminder",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = if (snoozeReminderEnabled) "Notify 15 mins before event" else "Notifications off",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = snoozeReminderEnabled,
                                onCheckedChange = { snoozeReminderEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF34C759),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Vibrant Category, Duration & Priority Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Event Duration / Timer Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Duration Timer",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = if (durationMinutes < 60) "${durationMinutes}m" else "${durationMinutes / 60}h" + if (durationMinutes % 60 != 0) " ${durationMinutes % 60}m" else "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            durationOptions.forEach { mins ->
                                val isSelected = durationMinutes == mins
                                val label = if (mins < 60) "${mins}m" else "${mins / 60}h" + if (mins % 60 != 0) " ${mins % 60}m" else ""
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) OneUiSecondary else DarkBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { durationMinutes = mins }
                                        .testTag("duration_chip_$mins"),
                                    color = if (isSelected) OneUiSecondary.copy(alpha = 0.2f) else DarkSurface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) OneUiSecondary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vibrant Category Chips
                        Text(
                            text = "Category",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                val catColor = when (cat) {
                                    "Work" -> CategoryWork
                                    "Personal" -> CategoryPersonal
                                    "Focus" -> CategoryFocus
                                    "Health" -> CategoryHealth
                                    "Meeting" -> CategoryMeeting
                                    else -> OneUiPrimary
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) catColor else DarkBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .testTag("cat_chip_$cat"),
                                    color = if (isSelected) catColor.copy(alpha = 0.25f) else DarkSurface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) catColor else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vibrant Priority Selector
                        Text(
                            text = "Priority",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

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
                                    Priority.LOW -> OneUiSuccess
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) prioColor else DarkBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedPriority = prio },
                                    color = if (isSelected) prioColor.copy(alpha = 0.22f) else DarkSurface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = prio.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) prioColor else TextSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Save Button
                Button(
                    onClick = { triggerSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_task_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentAmber,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Save Task ($formattedTime)",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> CircularWheelColumn(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    formatLabel: (T) -> String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    textAlign: TextAlign = TextAlign.Center
) {
    if (items.isEmpty()) return
    val coroutineScope = rememberCoroutineScope()
    val cycleMultiplier = 80
    val totalCount = items.size * cycleMultiplier
    val middleBase = (cycleMultiplier / 2) * items.size
    val selectedIdxInItems = items.indexOf(selectedItem).coerceAtLeast(0)
    val initialIndex = middleBase + selectedIdxInItems

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                initialIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visible.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: initialIndex
            }
        }
    }

    val currentItems by rememberUpdatedState(items)
    val currentSelectedItem by rememberUpdatedState(selectedItem)
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val list = currentItems
                if (list.isNotEmpty()) {
                    val actualItem = list[idx % list.size]
                    if (actualItem != currentSelectedItem) {
                        currentOnItemSelected(actualItem)
                    }
                }
            }
    }

    LaunchedEffect(selectedItem, items.size) {
        val currentCenteredItem = items[centeredIndex % items.size]
        if (currentCenteredItem != selectedItem && !listState.isScrollInProgress) {
            val targetOffset = items.indexOf(selectedItem).coerceAtLeast(0)
            val currentCycleBase = (centeredIndex / items.size) * items.size
            listState.scrollToItem(currentCycleBase + targetOffset)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        modifier = modifier.height(itemHeight * 5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(count = totalCount, key = { it }) { index ->
            val item = items[index % items.size]
            val distance = abs(index - centeredIndex)
            val isCenter = distance == 0

            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.58f
                2 -> 0.30f
                else -> 0.12f
            }
            val scale = when (distance) {
                0 -> 1.08f
                1 -> 0.92f
                else -> 0.80f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        onItemSelected(item)
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    },
                contentAlignment = when (textAlign) {
                    TextAlign.End -> Alignment.CenterEnd
                    TextAlign.Start -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                Text(
                    text = formatLabel(item),
                    fontSize = if (isCenter) 23.sp else 19.sp,
                    fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCenter) Color.White else Color(0xFF9E9EA8),
                    textAlign = textAlign,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoundedWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    textAlign: TextAlign = TextAlign.Center
) {
    if (items.isEmpty()) return
    val coroutineScope = rememberCoroutineScope()
    val safeInitial = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                safeInitial
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visible.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: safeInitial
            }
        }
    }

    val currentItems by rememberUpdatedState(items)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnIndexSelected by rememberUpdatedState(onIndexSelected)

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val list = currentItems
                if (list.isNotEmpty()) {
                    val clamped = idx.coerceIn(0, list.lastIndex)
                    if (clamped != currentSelectedIndex) {
                        currentOnIndexSelected(clamped)
                    }
                }
            }
    }

    LaunchedEffect(selectedIndex) {
        val clamped = selectedIndex.coerceIn(0, items.lastIndex)
        if (centeredIndex != clamped && !listState.isScrollInProgress) {
            listState.animateScrollToItem(clamped)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        modifier = modifier.height(itemHeight * 5),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(count = items.size, key = { it }) { index ->
            val distance = abs(index - centeredIndex)
            val isCenter = distance == 0
            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.58f
                else -> 0.28f
            }
            val scale = if (isCenter) 1.06f else 0.90f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        onIndexSelected(index)
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                    },
                contentAlignment = when (textAlign) {
                    TextAlign.End -> Alignment.CenterEnd
                    TextAlign.Start -> Alignment.CenterStart
                    else -> Alignment.Center
                }
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isCenter) 21.sp else 18.sp,
                    fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCenter) Color.White else Color(0xFF9E9EA8),
                    textAlign = textAlign,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickDateChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, OneUiPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = OneUiPrimary.copy(alpha = 0.16f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = OneUiPrimaryLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
        )
    }
}
