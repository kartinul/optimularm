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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.saveWakeSettings

@Composable
fun ClockScreen(settings: WakeSettings, ringAt: Long?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // the settings field whose time dialog is open: "sleepCutoff", "wakeTime" or null
    var picking by remember { mutableStateOf<String?>(null) }

    val window = (settings.wakeTime.toMinutes() - settings.sleepCutoff.toMinutes() + DAY_MINUTES) % DAY_MINUTES
    val ringText = if (!settings.alarmOn || ringAt == null) "Off"
    else "Rings " + DateFormat.getTimeFormat(context).format(ringAt) + " · " +
        DateUtils.getRelativeTimeSpanString(ringAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)

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
        }
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
