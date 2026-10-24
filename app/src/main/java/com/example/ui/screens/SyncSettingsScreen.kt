package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.ConflictLog
import com.example.ui.components.OneUiHeader
import com.example.ui.theme.DarkBackground
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
import java.util.Date
import java.util.Locale

@Composable
fun SyncSettingsScreen(
    authEmail: String,
    isGoogleConnected: Boolean,
    isSyncing: Boolean,
    syncMessage: String?,
    lastSyncTime: Long,
    conflictLogs: List<ConflictLog>,
    onToggleAccountConnection: () -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        OneUiHeader(
            title = "Sync & Ecosystem",
            subtitle = "Google Calendar API • Room Local Persistence • Cloud Sync",
            isSyncing = isSyncing,
            isGoogleConnected = isGoogleConnected,
            onSyncClick = onTriggerSync
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Visual Sync Hero Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud & Local Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Two-way synchronizer connects Room DB with Google Calendar with conflict resolution.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_sync_cloud_illustration),
                        contentDescription = "Sync Cloud Illustration",
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // 1. Google Calendar OAuth Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Google Calendar Integration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_account_card"),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
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
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(OneUiPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = OneUiPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = authEmail,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (isGoogleConnected) "OAuth 2.0 Connected" else "Disconnected",
                                        fontSize = 12.5.sp,
                                        color = if (isGoogleConnected) OneUiSuccess else OneUiWarning
                                    )
                                }
                            }

                            Switch(
                                checked = isGoogleConnected,
                                onCheckedChange = { onToggleAccountConnection() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = Color(0xFF1E1E1E)
                                ),
                                modifier = Modifier.testTag("google_sync_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val syncDateFormat = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
                        val lastSyncStr = if (lastSyncTime > 0) syncDateFormat.format(Date(lastSyncTime)) else "Never synced"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last sync: $lastSyncStr",
                                fontSize = 12.5.sp,
                                color = TextSecondary
                            )

                            Button(
                                onClick = onTriggerSync,
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUiPrimary),
                                modifier = Modifier.testTag("manual_sync_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(text = "Sync Now", fontSize = 13.5.sp)
                            }
                        }

                        if (!syncMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = syncMessage,
                                fontSize = 12.5.sp,
                                color = OneUiSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // 2. Offline Sync & Conflict Resolution Inspector
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Offline Sync & Conflict Resolution",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = OneUiSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Timestamp-Based Conflict Resolution",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "When reconnecting after offline edits, SyncTask compares local Room entity timestamps with Google Calendar 'updated' RFC3339 timestamps. If local changes were made while offline, they are pushed without loss. If calendar was modified remotely, local database reconciles cleanly.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Recent Conflict Resolution Logs:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUiPrimaryLight
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (conflictLogs.isEmpty()) {
                            Text(
                                text = "No merge conflicts detected. All local and remote versions in harmony.",
                                fontSize = 12.5.sp,
                                color = TextTertiary
                            )
                        } else {
                            conflictLogs.take(3).forEach { log ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = log.taskTitle, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = log.resolutionReason, fontSize = 11.5.sp, color = OneUiSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. System & Alarm Diagnostics
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "System & Hardware Architecture",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureCheckRow(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Adaptive Multitasking & Split-Screen",
                            subtitle = "Multi-window split screen, responsive navigation rail, AMOLED deep dark theme"
                        )

                        FeatureCheckRow(
                            icon = Icons.Default.Alarm,
                            title = "Exact AlarmManager Reminders",
                            subtitle = "Triggers precisely 15 minutes before tasks with sound and vibration"
                        )

                        FeatureCheckRow(
                            icon = Icons.Default.RestartAlt,
                            title = "Reboot Alarm Restoration (BootReceiver)",
                            subtitle = "Restores all pending task alarms after device restart or timezone change"
                        )

                        FeatureCheckRow(
                            icon = Icons.Default.CloudSync,
                            title = "WorkManager Background Sync",
                            subtitle = "Periodic background synchronizer with connected network constraints"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun FeatureCheckRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OneUiPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = OneUiPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, fontSize = 12.5.sp, color = TextSecondary, lineHeight = 17.sp)
        }
    }
}
