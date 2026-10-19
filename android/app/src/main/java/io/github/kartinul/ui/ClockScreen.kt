package io.github.kartinul.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.saveWakeSettings
import io.github.kartinul.MINUTE
import kotlinx.coroutines.delay

@Composable
fun ClockScreen(
    settings: WakeSettings,
    ringAt: Long?,
    lastSleep: Long?,
    missingPermissions: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // the settings field whose time dialog is open: "sleepCutoff", "wakeTime" or null
    var picking by remember { mutableStateOf<String?>(null) }

    val window = (settings.wakeTime.toMinutes() - settings.sleepCutoff.toMinutes() + DAY_MINUTES) % DAY_MINUTES
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE)
            now = System.currentTimeMillis()
        }
    }
    val ringText = if (!settings.alarmOn || ringAt == null) "Off"
    else "Rings " + DateFormat.getTimeFormat(context).format(ringAt) + " · " +
        DateUtils.getRelativeTimeSpanString(ringAt, now, DateUtils.MINUTE_IN_MILLIS)
    val sleepText = lastSleep?.let {
        DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY)
    } ?: "No sleep yet"

    Column(
        modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SleepDial(
            bedtime = settings.sleepCutoff,
            wakeTime = settings.wakeTime,
            onBedtimeChange = { saveWakeSettings("sleepCutoff" to it) },
            onWakeTimeChange = { saveWakeSettings("wakeTime" to it) },
            onBedtimeClick = { picking = "sleepCutoff" },
            onWakeTimeClick = { picking = "wakeTime" },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Sleep window: ${window / 60} h ${window % 60} min",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Wake-up alarm") },
                supportingContent = { Text(ringText) },
                trailingContent = {
                    Switch(checked = settings.alarmOn, onCheckedChange = { saveWakeSettings("alarmOn" to it) })
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            ListItem(
                headlineContent = { Text("Mac went to sleep") },
                supportingContent = { Text(sleepText) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
        if (missingPermissions > 0) Text(
            "$missingPermissions permission(s) missing: fix them in Settings",
            color = MaterialTheme.colorScheme.error,
        )
    }

    picking?.let { field ->
        TimeDialog(
            time = if (field == "wakeTime") settings.wakeTime else settings.sleepCutoff,
            onDismiss = { picking = null },
            onConfirm = {
                saveWakeSettings(field to it)
                picking = null
            },
        )
    }
}
