package com.droidspaces.app.util

/**
 * Immutable holder for the ~28 editable container-configuration fields that the
 * Create wizard ([com.droidspaces.app.ui.screen.ContainerConfigScreen]) and the
 * Edit screen ([com.droidspaces.app.ui.screen.EditContainerScreen]) both drive
 * through the shared [com.droidspaces.app.ui.component.ContainerConfigForm].
 *
 * Replaces the four hand-maintained copies of this field list that previously
 * lived as: editable `var`s, parallel `saved*` `var`s, a 28-arg `onNext`
 * callback, and a 28-arg `setConfig`. A single data class means `hasChanges`
 * is just `current != saved`, and config plumbing can never transpose fields.
 *
 * Note: `name`, `hostname`, `rootfsPath`, sparse-image and runtime fields are
 * NOT part of this state. They are owned by the surrounding screens/ViewModel.
 */
data class ContainerConfigState(
    val netMode: String = "nat",
    val disableIPv6: Boolean = false,
    val enableAndroidStorage: Boolean = false,
    val enableHwAccess: Boolean = false,
    val enableGpuMode: Boolean = false,
    val enableDevInput: Boolean = false,
    val enableDevUsb: Boolean = false,
    val enableDevSensors: Boolean = false,
    val enableDevSnd: Boolean = false,
    val enableDevVideo: Boolean = false,
    val enableDevBt: Boolean = false,
    val enableDevSerial: Boolean = false,
    val enableFirmware: Boolean = false,
    val devBlock: String = "",
    val enableTermuxX11: Boolean = false,
    val tx11ExtraFlags: String = "",
    val enableAnland: Boolean = false,
    val enableVirgl: Boolean = false,
    val virglExtraFlags: String = "",
    val enablePulseaudio: Boolean = false,
    val selinuxPermissive: Boolean = false,
    val allowSandboxing: Boolean = false,
    val volatileMode: Boolean = false,
    val bindMounts: List<BindMount> = emptyList(),
    val dnsServers: String = "",
    val runAtBoot: Boolean = false,
    val customInit: String = "",
    val staticNatIp: String = "",
    val forceCgroupv1: Boolean = false,
    val privileged: String = "",
    val privilegedNomask: Boolean = false,
    val privilegedNocaps: Boolean = false,
    val privilegedNoseccomp: Boolean = false,
    val privilegedShared: Boolean = false,
    val envFileContent: String = "",
    val upstreamInterfaces: List<String> = emptyList(),
    val portForwards: List<PortForward> = emptyList(),
    val gatewayContainer: String = "",
    val gatewayNet: String = "",
    val gatewayIface: String = "",
    val gatewayBridge: String = "",
    val macvlanParent: String = "",
    val macvlanMode: String = "",
    val memoryLimit: Long = 0,
    val cpuQuota: Long = 0,
    val pidsLimit: Long = 0,
    val allowSwap: Boolean = false,
)

/**
 * The tags in a privileged= value, read the way parse_privileged() in
 * src/config/parse.c reads them: comma separated, trimmed, case does not matter.
 */
fun privilegedTags(value: String): List<String> =
    value.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

fun formatPrivileged(nomask: Boolean, nocaps: Boolean, noseccomp: Boolean, shared: Boolean): String {
    if (nomask && nocaps && noseccomp && shared) return "full"
    val tags = mutableListOf<String>()
    if (nomask) tags.add("nomask")
    if (nocaps) tags.add("nocaps")
    if (noseccomp) tags.add("noseccomp")
    if (shared) tags.add("shared")
    return tags.joinToString(",")
}

fun ContainerConfigState.formatPrivileged(): String =
    if (privileged.isNotEmpty() && !privilegedNomask && !privilegedNocaps && !privilegedNoseccomp && !privilegedShared) {
        privileged
    } else {
        formatPrivileged(privilegedNomask, privilegedNocaps, privilegedNoseccomp, privilegedShared)
    }

fun ContainerConfigState.withPrivilegedFlag(flag: String, enabled: Boolean): ContainerConfigState {
    val nomask = if (flag == "nomask") enabled else privilegedNomask
    val nocaps = if (flag == "nocaps") enabled else privilegedNocaps
    val noseccomp = if (flag == "noseccomp") enabled else privilegedNoseccomp
    val shared = if (flag == "shared") enabled else privilegedShared
    return copy(
        privileged = formatPrivileged(nomask, nocaps, noseccomp, shared),
        privilegedNomask = nomask,
        privilegedNocaps = nocaps,
        privilegedNoseccomp = noseccomp,
        privilegedShared = shared
    )
}

fun ContainerConfigState.withPrivileged(tagsString: String): ContainerConfigState {
    val tags = privilegedTags(tagsString)
    val isFull = "full" in tags
    val nomask = isFull || "nomask" in tags
    val nocaps = isFull || "nocaps" in tags
    val noseccomp = isFull || "noseccomp" in tags
    val shared = isFull || "shared" in tags
    val formatted = if (isFull) "full" else formatPrivileged(nomask, nocaps, noseccomp, shared)
    return copy(
        privileged = formatted,
        privilegedNomask = nomask,
        privilegedNocaps = nocaps,
        privilegedNoseccomp = noseccomp,
        privilegedShared = shared
    )
}

/** Extract the editable config fields from an existing container. */
fun ContainerInfo.toConfigState(): ContainerConfigState {
    val tags = privilegedTags(privileged)
    val isFull = "full" in tags
    val nomask = isFull || "nomask" in tags
    val nocaps = isFull || "nocaps" in tags
    val noseccomp = isFull || "noseccomp" in tags
    val shared = isFull || "shared" in tags
    return ContainerConfigState(
        netMode = netMode,
        disableIPv6 = disableIPv6,
        enableAndroidStorage = enableAndroidStorage,
        enableHwAccess = enableHwAccess,
        enableGpuMode = enableGpuMode,
        enableDevInput = enableDevInput,
        enableDevUsb = enableDevUsb,
        enableDevSensors = enableDevSensors,
        enableDevSnd = enableDevSnd,
        enableDevVideo = enableDevVideo,
        enableDevBt = enableDevBt,
        enableDevSerial = enableDevSerial,
        enableFirmware = enableFirmware,
        devBlock = devBlock,
        enableTermuxX11 = enableTermuxX11,
        tx11ExtraFlags = tx11ExtraFlags,
        enableAnland = enableAnland,
        enableVirgl = enableVirgl,
        virglExtraFlags = virglExtraFlags,
        enablePulseaudio = enablePulseaudio,
        selinuxPermissive = selinuxPermissive,
        allowSandboxing = allowSandboxing,
        volatileMode = volatileMode,
        bindMounts = bindMounts,
        dnsServers = dnsServers,
        runAtBoot = runAtBoot,
        customInit = customInit,
        staticNatIp = staticNatIp,
        forceCgroupv1 = forceCgroupv1,
        privileged = privileged,
        privilegedNomask = nomask,
        privilegedNocaps = nocaps,
        privilegedNoseccomp = noseccomp,
        privilegedShared = shared,
        envFileContent = envFileContent ?: "",
        upstreamInterfaces = upstreamInterfaces,
        portForwards = portForwards,
        gatewayContainer = gatewayContainer,
        gatewayNet = gatewayNet,
        gatewayIface = gatewayIface,
        gatewayBridge = gatewayBridge,
        macvlanParent = macvlanParent,
        macvlanMode = macvlanMode,
        memoryLimit = memoryLimit,
        cpuQuota = cpuQuota,
        pidsLimit = pidsLimit,
        allowSwap = allowSwap,
    )
}

/**
 * Return a copy of this container with the editable config fields replaced by
 * [state]. Non-config fields (name, rootfsPath, status, sparse image,
 * runAtBootPriority, uuid, …) are preserved.
 */
fun ContainerInfo.withConfig(state: ContainerConfigState): ContainerInfo = copy(
    netMode = state.netMode,
    disableIPv6 = state.disableIPv6,
    enableAndroidStorage = state.enableAndroidStorage,
    enableHwAccess = state.enableHwAccess,
    enableGpuMode = state.enableGpuMode,
    enableDevInput = state.enableDevInput,
    enableDevUsb = state.enableDevUsb,
    enableDevSensors = state.enableDevSensors,
    enableDevSnd = state.enableDevSnd,
    enableDevVideo = state.enableDevVideo,
    enableDevBt = state.enableDevBt,
    enableDevSerial = state.enableDevSerial,
    enableFirmware = state.enableFirmware,
    devBlock = state.devBlock,
    enableTermuxX11 = state.enableTermuxX11,
    tx11ExtraFlags = state.tx11ExtraFlags,
    enableAnland = state.enableAnland,
    enableVirgl = state.enableVirgl,
    virglExtraFlags = state.virglExtraFlags,
    enablePulseaudio = state.enablePulseaudio,
    selinuxPermissive = state.selinuxPermissive,
    allowSandboxing = state.allowSandboxing,
    volatileMode = state.volatileMode,
    bindMounts = state.bindMounts,
    dnsServers = state.dnsServers,
    runAtBoot = state.runAtBoot,
    customInit = state.customInit,
    staticNatIp = state.staticNatIp,
    forceCgroupv1 = state.forceCgroupv1,
    privileged = state.formatPrivileged(),
    envFileContent = state.envFileContent.ifBlank { null },
    upstreamInterfaces = state.upstreamInterfaces,
    portForwards = state.portForwards,
    gatewayContainer = state.gatewayContainer,
    gatewayNet = state.gatewayNet,
    gatewayIface = state.gatewayIface,
    gatewayBridge = state.gatewayBridge,
    macvlanParent = state.macvlanParent,
    macvlanMode = state.macvlanMode,
    memoryLimit = state.memoryLimit,
    cpuQuota = state.cpuQuota,
    pidsLimit = state.pidsLimit,
    allowSwap = state.allowSwap,
)
