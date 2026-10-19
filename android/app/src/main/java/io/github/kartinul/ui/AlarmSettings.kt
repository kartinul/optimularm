package io.github.kartinul.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import io.github.kartinul.data.AlarmPrefs
import io.github.kartinul.data.SNOOZE_LENGTHS
import io.github.kartinul.data.SNOOZE_LIMITS
import io.github.kartinul.data.loadAlarmPrefs
import io.github.kartinul.data.saveAlarmPrefs
import io.github.kartinul.data.setSnoozesUsed
import io.github.kartinul.startRinging
import kotlin.math.roundToInt


/** Sound, volume, vibration and snooze settings, stored on the phone. */
@Composable
fun AlarmSettings() {
    val context = LocalContext.current
    var alarm by remember { mutableStateOf(loadAlarmPrefs(context)) }
    fun update(changed: AlarmPrefs) {
        alarm = changed
        saveAlarmPrefs(context, changed)
    }

    val sound = alarm.ringtone?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    val soundName = RingtoneManager.getRingtone(context, sound)?.getTitle(context) ?: "Default"
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        update(alarm.copy(ringtone = picked?.toString()))
    }

    ListItem(
        headlineContent = { Text("Sound") },
        supportingContent = { Text(soundName) },
        modifier = Modifier.clickable {
            picker.launch(
                Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            )
        },
    )
    ListItem(
        headlineContent = { Text("Volume ${(alarm.volume * 100).roundToInt()}%") },
        supportingContent = {
            Slider(value = alarm.volume, onValueChange = { update(alarm.copy(volume = it)) }, valueRange = 0.1f..1f)
        },
    )
    SwitchItem("Gradually increase volume", alarm.gradual) { update(alarm.copy(gradual = it)) }
    SwitchItem("Vibrate", alarm.vibrate) { update(alarm.copy(vibrate = it)) }
    ChoiceItem("Snooze length", SNOOZE_LENGTHS, alarm.snoozeMinutes, { "$it min" }) {
        update(alarm.copy(snoozeMinutes = it))
    }
    ChoiceItem("Snoozes allowed", SNOOZE_LIMITS, alarm.maxSnoozes, { "$it" }) {
        update(alarm.copy(maxSnoozes = it))
    }
    ListItem(
        headlineContent = { Text("Test alarm") },
        supportingContent = { Text("Rings now with these settings") },
        trailingContent = { TextButton(onClick = {
            setSnoozesUsed(context, 0)
            startRinging(context)
        }) { Text("Ring") } },
    )
}

@Composable
private fun SwitchItem(title: String, checked: Boolean, onChange: (Boolean) -> Unit) = ListItem(
    headlineContent = { Text(title) },
    trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    modifier = Modifier.clickable { onChange(!checked) },
)

@Composable
private fun ChoiceItem(title: String, options: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) =
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, modifier = Modifier.padding(bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label(option)) }
            }
        }
    }
