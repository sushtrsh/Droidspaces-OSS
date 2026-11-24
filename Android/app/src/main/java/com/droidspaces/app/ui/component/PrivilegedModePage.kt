package com.droidspaces.app.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.JetBrainsMono

/**
 * The tags in a privileged= value, read the way parse_privileged() in
 * src/config/parse.c reads them: comma separated, trimmed, case does not matter.
 */
fun privilegedTags(value: String): List<String> =
    value.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

val PRIVILEGED_FLAGS = listOf("nomask", "nocaps", "noseccomp", "shared")

fun flagDescription(flag: String) = when (flag) {
    "nomask" -> R.string.privileged_nomask_desc
    "nocaps" -> R.string.privileged_nocaps_desc
    "noseccomp" -> R.string.privileged_noseccomp_desc
    else -> R.string.privileged_shared_desc
}

@Composable
fun PrivilegedModePage(initial: String, onApply: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val initialTags = privilegedTags(initial)
    val initialOn = if ("full" in initialTags) PRIVILEGED_FLAGS else initialTags.filter { it in PRIVILEGED_FLAGS }
    var on by rememberSaveable { mutableStateOf(initialOn) }

    val full = on.size == PRIVILEGED_FLAGS.size
    val tags = if (full) "full" else PRIVILEGED_FLAGS.filter { it in on }.joinToString(",")
    val dirty = on.toSet() != initialOn.toSet()

    DraftPageScaffold(
        title = context.getString(R.string.privileged_mode),
        dirty = dirty,
        canSave = true,
        onSave = { onApply(tags) },
        onClose = onClose,
        confirmLabel = context.getString(R.string.done)
    ) {
        DangerousWarningCard(
            title = context.getString(R.string.privileged_warning_title),
            text = context.getString(R.string.privileged_disclaimer),
            modifier = Modifier.padding(horizontal = screenGutter(), vertical = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            SwitchItem(
                title = "full",
                titleFontFamily = JetBrainsMono,
                summary = context.getString(R.string.privileged_full_desc),
                checked = full,
                onCheckedChange = { on = if (it) PRIVILEGED_FLAGS else emptyList() }
            )
        }
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            PRIVILEGED_FLAGS.forEachIndexed { i, flag ->
                if (i > 0) GroupDivider()
                SwitchItem(
                    title = flag,
                    titleFontFamily = JetBrainsMono,
                    summary = context.getString(flagDescription(flag)),
                    checked = flag in on,
                    onCheckedChange = { checked -> on = if (checked) on + flag else on - flag }
                )
            }
        }
    }
}
