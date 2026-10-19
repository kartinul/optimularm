package io.github.kartinul.ui

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.kartinul.MINUTE
import io.github.kartinul.R
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.saveWakeSettings
import kotlinx.coroutines.delay

@Composable
fun ClockScreen(
    settings: WakeSettings,
    ringAt: Long?,
    missingPermissions: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // the settings field whose time dialog is open: "sleepCutoff", "wakeTime" or null
    var picking by remember { mutableStateOf<String?>(null) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            // tick on the minute boundary so the countdown flips with the clock
            delay(MINUTE - System.currentTimeMillis() % MINUTE)
            now = System.currentTimeMillis()
        }
    }
    val ringing = settings.alarmOn && ringAt != null

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (ringing) DateFormat.getTimeFormat(context).format(ringAt) else "Alarm off",
                style = MaterialTheme.typography.displaySmall,
                color = if (ringing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ringing) Text(
                timeUntil(ringAt!! - now),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SleepDial(
            bedtime = settings.sleepCutoff,
            wakeTime = settings.wakeTime,
            onBedtimeChange = { saveWakeSettings("sleepCutoff" to it) },
            onWakeTimeChange = { saveWakeSettings("wakeTime" to it) },
            modifier = Modifier.fillMaxWidth(0.85f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeTile("Bedtime", settings.sleepCutoff, R.drawable.ic_bedtime, Modifier.weight(1f)) {
                picking = "sleepCutoff"
            }
            TimeTile("Wake up", settings.wakeTime, R.drawable.ic_alarm, Modifier.weight(1f)) {
                picking = "wakeTime"
            }
        }
        AlarmToggle(settings.alarmOn) { saveWakeSettings("alarmOn" to it) }
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

@Composable
private fun TimeTile(label: String, time: String, @DrawableRes icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(icon), contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Text(displayTime(LocalContext.current, time), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        }
    }
}

/** The whole card is the switch: tinted when on, with the alarm icon in the thumb. */
@Composable
private fun AlarmToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        checked = on,
        onCheckedChange = onChange,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (on) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (on) colors.onPrimaryContainer else colors.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Smart alarm", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (on) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = on,
                onCheckedChange = onChange,
                thumbContent = {
                    Icon(
                        painterResource(R.drawable.ic_alarm), contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
                modifier = Modifier.scale(1.25f),
            )
        }
    }
}
