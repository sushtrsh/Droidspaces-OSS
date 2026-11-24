package com.droidspaces.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.util.AnimationUtils
import com.droidspaces.app.util.ContainerDiskUsageManager
import com.droidspaces.app.util.ContainerInfo
import com.droidspaces.app.util.ContainerOSInfoManager
import com.droidspaces.app.util.ContainerStatus
import com.droidspaces.app.util.ContainerUsersManager
import com.droidspaces.app.util.IconUtils
import com.droidspaces.app.util.ResourceLimits

data class ContainerCardActions(
    val onStart: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onEnter: () -> Unit = {},
    val onUninstall: () -> Unit = {},
    val onMigrate: () -> Unit = {},
    val onResize: () -> Unit = {},
    val onExport: () -> Unit = {},
    val onToggleExpand: () -> Unit = {},
    val onShowLogs: () -> Unit = {},
    val onTerminal: () -> Unit = {},
    val onServices: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContainerCard(
    container: ContainerInfo,
    actions: ContainerCardActions = ContainerCardActions(),
    isOperationRunning: Boolean = false,
    isExpanded: Boolean = false,
    osInfo: ContainerOSInfoManager.OSInfo? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(20.dp)

    val cacheVersion by ContainerOSInfoManager.iconCacheVersion
    val cachedOsInfo = remember(container.name, cacheVersion) {
        ContainerOSInfoManager.getCachedOSInfo(container.name, context)
    }
    val currentOsInfo = osInfo ?: cachedOsInfo

    var diskUsage by remember(container.rootfsPath) {
        mutableStateOf(
            if (container.useSparseImage) ContainerDiskUsageManager.getCached(container.rootfsPath) else null
        )
    }
    if (container.useSparseImage && diskUsage == null) {
        LaunchedEffect(container.rootfsPath) {
            ContainerDiskUsageManager.getUsage(container.rootfsPath)?.let { diskUsage = it }
        }
    }

    var users by remember(container.name) {
        mutableStateOf(ContainerUsersManager.getCachedUsers(container.name))
    }
    var initSystem by remember(container.name) {
        mutableStateOf(ContainerUsersManager.getCachedInitSystem(container.name))
    }

    if (container.isRunning && (users == null || initSystem == null)) {
        LaunchedEffect(container.name, container.isRunning) {
            val probe = ContainerUsersManager.probe(container.name)
            if (probe != null) {
                initSystem = probe.first
                users = probe.second
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = {
                    if (container.isRunning) actions.onEnter() else actions.onToggleExpand()
                },
                onLongClick = actions.onToggleExpand,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = AnimationUtils.mediumSpec())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Distro icon + Name + Subtitle (left), Modern Status Chip (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val distroPretty = currentOsInfo?.prettyName ?: currentOsInfo?.name
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = IconUtils.getDistroIcon(distroPretty),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (container.isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = container.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!distroPretty.isNullOrBlank()) {
                        Text(
                            text = "• $distroPretty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Modernized Status Pill
                val statusColor = if (container.isRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val statusLabel = if (container.isRunning) context.getString(R.string.status_running)
                else context.getString(R.string.status_stopped)

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        if (isOperationRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(6.dp),
                                color = statusColor,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = statusColor
                            ) {}
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            // Info Section in a subtle card
            val metaColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            val netModeLabel = when (container.netMode) {
                "nat" -> context.getString(R.string.network_mode_nat_short)
                "none" -> context.getString(R.string.network_mode_none_short)
                "gateway" -> context.getString(R.string.network_mode_gateway_short)
                "macvlan" -> context.getString(R.string.network_mode_macvlan_short)
                else -> context.getString(R.string.network_mode_host_short)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Top stats: IP/Net, User, Uptime/PID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Network & IP
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Public, null, modifier = Modifier.size(13.dp), tint = metaColor)
                            val ip = currentOsInfo?.ipAddress?.takeIf { it.isNotBlank() }
                            Text(
                                text = if (ip != null) "$netModeLabel • $ip" else netModeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Users
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(13.dp), tint = metaColor)
                            Text(
                                text = users?.firstOrNull() ?: if (users != null) "root" else "-",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Uptime or Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (container.isRunning) Icons.Default.Timer else Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = metaColor
                            )
                            val uptimeStr = if (container.isRunning) {
                                currentOsInfo?.uptime?.takeIf { it.isNotBlank() }
                                    ?: container.pid?.let { "PID $it" }
                                    ?: context.getString(R.string.status_running)
                            } else context.getString(R.string.status_stopped)
                            Text(
                                text = uptimeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = metaColor
                            )
                        }
                    }

                    // Bottom stats: Disk Usage & Live Resources
                    val usage = diskUsage
                    val hasSparse = container.useSparseImage
                    if (hasSparse || (container.isRunning && (currentOsInfo?.cpuUsage != null || currentOsInfo?.ramUsedKb != null))) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasSparse) {
                                val fraction = usage?.let {
                                    if (it.totalBytes > 0L) (it.usedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                                } ?: 0f
                                val animatedFraction by animateFloatAsState(
                                    targetValue = fraction,
                                    animationSpec = AnimationUtils.mediumSpec(),
                                    label = "disk_progress"
                                )
                                val diskUsedStr = usage?.let { ResourceLimits.formatBytes(it.usedBytes) } ?: "-"
                                val diskTotalStr = usage?.let { ResourceLimits.formatBytes(it.totalBytes) }
                                    ?: container.sparseImageSizeGB?.let { "${it}GB" } ?: "-"

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(painterResource(R.drawable.ic_disk), null, modifier = Modifier.size(13.dp), tint = metaColor)
                                    Text(
                                        text = "$diskUsedStr / $diskTotalStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = metaColor
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedFraction },
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }

                            if (container.isRunning) {
                                val cpuStr = currentOsInfo?.cpuUsage?.takeIf { it >= 0.0 }?.let { "${it.toInt()}%" }
                                val ramStr = currentOsInfo?.ramUsedKb?.let { ResourceLimits.formatMemoryUsage(context, it) }
                                val resourceList = listOfNotNull(cpuStr?.let { "CPU $it" }, ramStr?.let { "RAM $it" })
                                if (resourceList.isNotEmpty()) {
                                    Text(
                                        text = resourceList.joinToString(" • "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Capability Tags
            val badges = remember(container) {
                buildList {
                    if (container.enableGpuMode) add("GPU")
                    if (container.enableHwAccess) add("HW")
                    if (container.enableAndroidStorage) add("Storage")
                    if (container.privileged.isNotEmpty()) add("Privileged")
                    if (container.allowSandboxing) add("Sandboxed")
                    if (container.enableTermuxX11) add("X11")
                    if (container.enableAnland) add("Anland")
                    if (container.enableVirgl) add("VirGL")
                    if (container.enablePulseaudio) add("Audio")
                    if (container.runAtBoot) add("AutoBoot")
                    if (container.disableIPv6) add("No IPv6")
                }
            }

            if (badges.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    badges.take(5).forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (badges.size > 5) {
                        Text(
                            text = "+${badges.size - 5}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Actions: Clean icon buttons only
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (container.isRunning) {
                        // Left group: Stop & Restart
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            val isStopEnabled = !isOperationRunning
                            Surface(
                                onClick = actions.onStop,
                                enabled = isStopEnabled,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isStopEnabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isStopEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = context.getString(R.string.stop),
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isStopEnabled) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }

                            val isRestartEnabled = !isOperationRunning
                            Surface(
                                onClick = actions.onRestart,
                                enabled = isRestartEnabled,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isRestartEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isRestartEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = context.getString(R.string.restart),
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isRestartEnabled) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }

                        // Middle group: Terminal & Systemd
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = actions.onTerminal,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = context.getString(R.string.terminal),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val initName = when (initSystem?.lowercase()) {
                                "procd" -> context.getString(R.string.openwrt)
                                "openrc" -> context.getString(R.string.openrc)
                                "runit" -> context.getString(R.string.runit)
                                else -> context.getString(R.string.systemd)
                            }
                            Surface(
                                onClick = actions.onServices,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = initName,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    } else {
                        // Start Icon Button
                        val isStartEnabled = !isOperationRunning
                        Surface(
                            onClick = actions.onStart,
                            enabled = isStartEnabled,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isStartEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (isStartEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = context.getString(R.string.start),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isStartEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    // Right group: Logs & Expand
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = actions.onShowLogs,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Article,
                                    contentDescription = context.getString(R.string.view_logs),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = actions.onToggleExpand,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Drawer for Configuration, Resize/Migrate, Export, Uninstall
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = AnimationUtils.mediumSpec(),
                    expandFrom = Alignment.Top
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = AnimationUtils.mediumSpec(),
                    shrinkTowards = Alignment.Top
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))

                    ActionItem(
                        icon = rememberVectorPainter(Icons.Default.Edit),
                        label = context.getString(R.string.edit_container_configuration),
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = actions.onEdit
                    )

                    if (!container.useSparseImage) {
                        ActionItem(
                            icon = painterResource(R.drawable.ic_disk),
                            label = context.getString(R.string.migrate_to_sparse_image),
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = actions.onMigrate
                        )
                    } else {
                        ActionItem(
                            icon = painterResource(R.drawable.ic_disk),
                            label = context.getString(R.string.resize_sparse_image),
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = actions.onResize
                        )
                    }

                    ActionItem(
                        icon = rememberVectorPainter(Icons.Default.FileDownload),
                        label = context.getString(R.string.export_container),
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = actions.onExport
                    )

                    ActionItem(
                        icon = rememberVectorPainter(Icons.Default.Delete),
                        label = context.getString(R.string.uninstall_container_menu),
                        tint = MaterialTheme.colorScheme.error,
                        isBold = true,
                        onClick = actions.onUninstall
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: Painter,
    label: String,
    tint: Color,
    isBold: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isBold) tint else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
