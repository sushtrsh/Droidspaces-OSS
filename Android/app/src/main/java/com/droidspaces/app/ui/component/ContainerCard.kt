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
import com.droidspaces.app.R
import com.droidspaces.app.util.AnimationUtils
import com.droidspaces.app.util.ContainerDiskUsageManager
import com.droidspaces.app.util.ContainerInfo
import com.droidspaces.app.util.ContainerOSInfoManager
import com.droidspaces.app.util.ContainerStatus
import com.droidspaces.app.util.ContainerUsersManager
import com.droidspaces.app.util.IconUtils
import com.droidspaces.app.util.ResourceLimits

/**
 * Per-item callbacks for [ContainerCard], grouped so the call site passes one
 * stable object instead of 13 separate lambdas.
 */
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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

    // Cached OS info for instant presentation
    val cacheVersion by ContainerOSInfoManager.iconCacheVersion
    val cachedOsInfo = remember(container.name, cacheVersion) {
        ContainerOSInfoManager.getCachedOSInfo(container.name, context)
    }
    val currentOsInfo = osInfo ?: cachedOsInfo

    // Live disk usage snapshot for sparse rootfs
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

    // Available users & init system
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
                .padding(CardContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CardHeaderHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val distroPretty = currentOsInfo?.prettyName ?: currentOsInfo?.name
                    Icon(
                        painter = IconUtils.getDistroIcon(distroPretty),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (container.isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = container.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!distroPretty.isNullOrBlank()) {
                            Text(
                                text = distroPretty,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val (statusText, statusColor) = when (container.status) {
                    ContainerStatus.RUNNING -> context.getString(R.string.status_running) to MaterialTheme.colorScheme.primary
                    ContainerStatus.STOPPED -> context.getString(R.string.status_stopped) to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }

                StatusPill(
                    label = statusText.uppercase(),
                    color = statusColor,
                    busy = isOperationRunning
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            val netModeLabel = when (container.netMode) {
                "nat" -> context.getString(R.string.network_mode_nat_short)
                "none" -> context.getString(R.string.network_mode_none_short)
                "gateway" -> context.getString(R.string.network_mode_gateway_short)
                "macvlan" -> context.getString(R.string.network_mode_macvlan_short)
                else -> context.getString(R.string.network_mode_host_short)
            }

            val metaColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val ipText = currentOsInfo?.ipAddress?.takeIf { it.isNotBlank() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = metaColor
                    )
                    Text(
                        text = if (ipText != null) "$netModeLabel • $ipText" else netModeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (container.isRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = metaColor
                        )
                        val uptimeStr = currentOsInfo?.uptime?.takeIf { it.isNotBlank() } ?: context.getString(R.string.status_running)
                        val pidStr = container.pid?.let { " • PID $it" } ?: ""
                        Text(
                            text = "$uptimeStr$pidStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val userList = users
                if (userList != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = metaColor
                        )
                        val displayUsers = if (userList.isNotEmpty()) userList.joinToString(", ") else "root"
                        Text(
                            text = displayUsers,
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val cpuPercent = currentOsInfo?.cpuUsage ?: -1.0
                val ramLabel = currentOsInfo?.ramLabel(context)
                if (container.isRunning && (cpuPercent >= 0.0 || ramLabel != null)) {
                    if (cpuPercent >= 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = context.getString(R.string.cpu_percent_label, cpuPercent),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (ramLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = ramLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Disk usage
            if (container.useSparseImage) {
                val usage = diskUsage
                val fraction = usage?.let {
                    if (it.totalBytes > 0L) (it.usedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                } ?: 0f
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = AnimationUtils.mediumSpec(),
                    label = "disk_progress"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_disk),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = metaColor
                            )
                            Text(
                                text = context.getString(R.string.disk_usage),
                                style = MaterialTheme.typography.bodySmall,
                                color = metaColor
                            )
                        }
                        val usageText = usage?.let {
                            context.getString(
                                R.string.disk_usage_used_of_total,
                                ResourceLimits.formatBytes(it.usedBytes),
                                ResourceLimits.formatBytes(it.totalBytes)
                            )
                        } ?: container.sparseImageSizeGB?.let {
                            context.getString(R.string.gb_size, it)
                        } ?: context.getString(R.string.disk_usage_calculating)

                        Text(
                            text = usageText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Active capability tags
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badges.forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Actions
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (container.isRunning) {
                        val isStopEnabled = !isOperationRunning
                        Surface(
                            onClick = actions.onStop,
                            enabled = isStopEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isStopEnabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (isStopEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isStopEnabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = context.getString(R.string.stop),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isStopEnabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        val isRestartEnabled = !isOperationRunning
                        Surface(
                            onClick = actions.onRestart,
                            enabled = isRestartEnabled,
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(16.dp),
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

                        Surface(
                            onClick = actions.onTerminal,
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
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
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
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
                    } else {
                        val isStartEnabled = !isOperationRunning
                        Surface(
                            onClick = actions.onStart,
                            enabled = isStartEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isStartEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (isStartEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isStartEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = context.getString(R.string.start),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isStartEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = actions.onShowLogs,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(16.dp),
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
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(16.dp),
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
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))

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
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = if (isBold) tint else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
