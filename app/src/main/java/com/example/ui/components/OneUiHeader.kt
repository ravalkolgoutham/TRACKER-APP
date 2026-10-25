package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SyncQueueItem
import com.example.ui.theme.AeroSquircleButton
import com.example.ui.theme.AppThemePreset
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
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.aeroActionBrush
import com.example.ui.theme.glassSpecularBorderBrush
import com.example.ui.theme.glassSurfaceBrush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OneUiHeader(
    title: String,
    subtitle: String,
    isSyncing: Boolean,
    isGoogleConnected: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
    pendingQueueCount: Int = 0,
    syncQueueLogs: List<SyncQueueItem> = emptyList()
) {
    var showSyncDrawer by remember { mutableStateOf(false) }
    var showThemePopup by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .border(
                width = 1.dp,
                brush = glassSpecularBorderBrush(OneUiSecondary),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            DarkSurfaceElevated.copy(alpha = (ThemeManager.glassAlpha + 0.1f).coerceAtMost(0.95f)),
                            DarkSurface.copy(alpha = ThemeManager.glassAlpha),
                            OneUiPrimary.copy(alpha = 0.22f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Action Strip: Sync Queue Drawer + Quick Theme Pop-up + Live Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sync Queue Log Drawer Squircle Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(glassSurfaceBrush())
                            .border(1.dp, glassSpecularBorderBrush(), RoundedCornerShape(12.dp))
                            .clickable { showSyncDrawer = !showSyncDrawer }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_sync_log_drawer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Sync Log Drawer",
                            tint = OneUiSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Queue (${syncQueueLogs.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Interactive Glass Theme Customizer Pop-up Trigger
                    AeroSquircleButton(
                        onClick = { showThemePopup = true },
                        isActive = true,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 2.dp)
                            .testTag("header_theme_popup_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Customize Glass Theme",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Glass UI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Live Connectivity & Sync Status Pill
                val statusLabel = when {
                    isSyncing -> "Syncing..."
                    !isGoogleConnected || pendingQueueCount > 0 ->
                        if (pendingQueueCount > 0) "Queued ($pendingQueueCount)" else "Offline"
                    else -> "Synced"
                }
                val badgeColor = if (isGoogleConnected && pendingQueueCount == 0) OneUiSuccess else OneUiWarning

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = OneUiSecondary
                        )
                    } else {
                        Icon(
                            imageVector = if (isGoogleConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = "Sync Status",
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("header_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync now",
                            tint = OneUiSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Collapsible Sync Log Drawer showing recent Room DB transactions
            AnimatedVisibility(visible = showSyncDrawer) {
                val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassSurfaceBrush(0.95f))
                        .border(1.dp, glassSpecularBorderBrush(OneUiSecondary), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                        .testTag("sync_log_drawer_content"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Optimistic Mutation & Room Sync Queue",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUiSecondary
                        )
                        Text(
                            text = "<16ms UI Write",
                            fontSize = 10.5.sp,
                            color = OneUiSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (syncQueueLogs.isEmpty()) {
                        Text(
                            text = "No recent mutations queued. All local Room records are in sync.",
                            fontSize = 11.5.sp,
                            color = TextTertiary
                        )
                    } else {
                        syncQueueLogs.take(5).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.syncStatus == "synced") OneUiSuccess else OneUiWarning
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${item.entityType}:${item.action}] ${item.payloadSummary}",
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = timeFmt.format(Date(item.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }

    if (showThemePopup) {
        QuickGlassThemePopupDialog(onDismiss = { showThemePopup = false })
    }
}

@Composable
fun QuickGlassThemePopupDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DarkSurfaceElevated.copy(alpha = 0.96f),
                            DarkSurface.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(1.5.dp, glassSpecularBorderBrush(OneUiSecondary), RoundedCornerShape(28.dp))
                .padding(20.dp)
                .testTag("quick_glass_theme_popup")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AeroSquircleButton(
                            onClick = onDismiss,
                            isActive = true,
                            cornerRadius = 12.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Glass & Theme Studio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Live interactive glass & palette switcher",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Preset Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemePreset.values().forEach { preset ->
                        val isSelected = ThemeManager.currentPreset == preset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) preset.primary.copy(alpha = 0.25f) else preset.surface.copy(alpha = 0.85f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) preset.secondary else preset.border.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { ThemeManager.applyPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Dual-tone gradient swatch
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(preset.secondary, preset.primary)
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = preset.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Glass Frosted Transparency Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Glass Opacity / Frosted Blur",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(ThemeManager.glassAlpha * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUiSecondary
                        )
                    }
                    Slider(
                        value = ThemeManager.glassAlpha,
                        onValueChange = { ThemeManager.glassAlpha = it },
                        valueRange = 0.45f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = OneUiSecondary,
                            activeTrackColor = OneUiPrimary,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                }

                // Diagonal Cobalt Wing Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagonal Geometric Wing Backdrop",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = ThemeManager.showDiagonalBackdrop,
                        onCheckedChange = { ThemeManager.showDiagonalBackdrop = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OneUiSecondary
                        )
                    )
                }

                AeroSquircleButton(
                    onClick = onDismiss,
                    isActive = true,
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Apply & Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
