package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.ConflictLog
import com.example.model.SyncQueueItem
import com.example.ui.components.OneUiHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
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
    modifier: Modifier = Modifier,
    syncQueueLogs: List<SyncQueueItem> = emptyList(),
    onRunSimulatedSync: (() -> Unit)? = null,
    onPurgeDuplicates: (() -> Unit)? = null,
    onExportBackupJson: (((String) -> Unit) -> Unit)? = null,
    onImportBackupJson: ((String, (Boolean) -> Unit) -> Unit)? = null
) {
    var jsonBackupBuffer by remember { mutableStateOf("") }
    var showJsonPanel by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf<String?>(null) }

    com.example.ui.theme.AeroGlassBackdrop(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OneUiHeader(
                title = "Settings & Theme Studio",
                subtitle = "Glassmorphism Engine • Custom Themes • Google Calendar Sync",
                isSyncing = isSyncing,
                isGoogleConnected = isGoogleConnected,
                onSyncClick = onTriggerSync,
                syncQueueLogs = syncQueueLogs
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 0. Theme & Glassmorphism Customization Studio
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Theme & Glassmorphism Studio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OneUiSecondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(com.example.ui.theme.glassSurfaceBrush(1.02f))
                            .border(
                                width = 1.dp,
                                brush = com.example.ui.theme.glassSpecularBorderBrush(OneUiSecondary),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                            .testTag("theme_customization_studio_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Select App Color & Glass Palette",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Customize the entire app's frosted glass surfaces, glowing squircle buttons, and diagonal geometric wing in real time.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )

                            // 5 Interactive Theme Preset Cards
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.example.ui.theme.AppThemePreset.values().forEach { preset ->
                                    val isSelected = com.example.ui.theme.ThemeManager.currentPreset == preset
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) preset.primary.copy(alpha = 0.26f) else preset.surface.copy(alpha = 0.78f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) preset.secondary else preset.border.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { com.example.ui.theme.ThemeManager.applyPreset(preset) }
                                            .padding(horizontal = 14.dp, vertical = 11.dp)
                                            .testTag("settings_theme_preset_${preset.id}"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                                            listOf(preset.secondary, preset.primary)
                                                        )
                                                    )
                                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(11.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = preset.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = preset.subtitle,
                                                    fontSize = 11.5.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(preset.secondary.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = preset.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Frosted Glass Transparency Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Frosted Glass Transparency",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${(com.example.ui.theme.ThemeManager.glassAlpha * 100).toInt()}% Opacity",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUiSecondary
                                    )
                                }
                                androidx.compose.material3.Slider(
                                    value = com.example.ui.theme.ThemeManager.glassAlpha,
                                    onValueChange = { com.example.ui.theme.ThemeManager.glassAlpha = it },
                                    valueRange = 0.45f..0.95f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = OneUiSecondary,
                                        activeTrackColor = OneUiPrimary,
                                        inactiveTrackColor = DarkBorder
                                    ),
                                    modifier = Modifier.testTag("settings_glass_opacity_slider")
                                )
                            }

                            // Diagonal Cobalt Wing Backdrop Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Diagonal Geometric Wing Backdrop",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Renders the angled electric cyan-to-cobalt backdrop slash",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = com.example.ui.theme.ThemeManager.showDiagonalBackdrop,
                                    onCheckedChange = { com.example.ui.theme.ThemeManager.showDiagonalBackdrop = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OneUiSecondary
                                    ),
                                    modifier = Modifier.testTag("toggle_diagonal_backdrop_switch")
                                )
                            }

                            // Specular Rim Lighting Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3D Specular Glass Rim Lighting",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Luminous top-left glass edge highlights on cards & squircles",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = com.example.ui.theme.ThemeManager.enableSpecularRim,
                                    onCheckedChange = { com.example.ui.theme.ThemeManager.enableSpecularRim = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OneUiPrimary
                                    ),
                                    modifier = Modifier.testTag("toggle_specular_rim_switch")
                                )
                            }
                        }
                    }
                }

                // Visual Sync Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(com.example.ui.theme.glassSurfaceBrush())
                        .border(1.dp, com.example.ui.theme.glassSpecularBorderBrush(), RoundedCornerShape(24.dp))
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
                                text = "Two-way synchronizer connects Room DB with Google Calendar with ETag & timestamp conflict resolution.",
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

            // 1. Google Calendar OAuth & Simulated Two-Way Sync Section
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
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = authEmail,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isGoogleConnected) "OAuth 2.0 Connected" else "Offline — Queuing Locally",
                                        fontSize = 12.sp,
                                        color = if (isGoogleConnected) OneUiSuccess else OneUiWarning,
                                        maxLines = 1
                                    )
                                }
                            }

                            Switch(
                                checked = isGoogleConnected,
                                onCheckedChange = { onToggleAccountConnection() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OneUiSuccess,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = DarkSurface
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
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                maxLines = 2
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
                                Text(
                                    text = "Sync Now",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (onPurgeDuplicates != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onPurgeDuplicates,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, OneUiWarning),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("purge_duplicates_button")
                            ) {
                                Text(
                                    text = "Clean Up Duplicate & Holiday Events",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiWarning
                                )
                            }
                        }

                        if (onRunSimulatedSync != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onRunSimulatedSync,
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, OneUiSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulated_google_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = OneUiSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simulated Google Calendar Sync",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSecondary
                                )
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

            // 2. Data Portability: JSON Export & Import Backup
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Data Backup & Portability (JSON)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Export or Restore Full Room DB Snapshot",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Export all tasks, subtasks, Pomodoro stats, and streaks as portable JSON or restore from a backup.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onExportBackupJson?.invoke { exported ->
                                        jsonBackupBuffer = exported
                                        showJsonPanel = true
                                        backupStatusText = "Exported Room DB snapshot to JSON below."
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_json_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUiPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    showJsonPanel = !showJsonPanel
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_json_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, OneUiSecondary)
                            ) {
                                Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = OneUiSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import / Restore", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OneUiSecondary)
                            }
                        }

                        AnimatedVisibility(visible = showJsonPanel) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                OutlinedTextField(
                                    value = jsonBackupBuffer,
                                    onValueChange = { jsonBackupBuffer = it },
                                    label = { Text("Backup JSON Payload", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .testTag("backup_json_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OneUiSecondary,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = backupStatusText ?: "Paste JSON snapshot and tap Restore",
                                        fontSize = 11.5.sp,
                                        color = OneUiSuccess,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (jsonBackupBuffer.isNotBlank()) {
                                                onImportBackupJson?.invoke(jsonBackupBuffer) { ok ->
                                                    backupStatusText = if (ok) "Backup restored successfully!" else "Invalid JSON format"
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OneUiSuccess)
                                    ) {
                                        Text("Restore Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Offline Sync & Conflict Resolution Inspector
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
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = OneUiSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Timestamp & ETag Conflict Resolution",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "When reconnecting after offline edits, SyncTask compares local Room entity timestamps and ETag hashes with Google Calendar 'updated' RFC3339 timestamps. Offline mutations are queued and replayed deterministically.",
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

            // 4. System & Hardware Architecture
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
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureCheckRow(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Adaptive Multitasking & Split-Screen",
                            subtitle = "Multi-window split screen, responsive navigation rail, vibrant AMOLED dark theme"
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
