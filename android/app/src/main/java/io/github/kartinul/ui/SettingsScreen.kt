package io.github.kartinul.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import com.google.firebase.firestore.FieldValue
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.saveWakeSettings
import io.github.kartinul.data.unpair
import io.github.kartinul.ui.theme.saveWallpaperColors
import io.github.kartinul.ui.theme.wallpaperColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    pairId: String?,
    settings: WakeSettings?,
    permissions: List<Permission>,
    onPairChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun unpairNow() {
        unpair()
        onPairChanged()
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        SectionTitle("Alarm")
        if (settings != null) {
            WeekendWakeTime(settings)
            PhoneSleepSwitch(settings, usageAccess = permissions.any { it.name == USAGE_ACCESS && it.granted })
        }
        AlarmSettings()
        // wallpaper colors need Android 12+; older phones always get the app palette
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SectionTitle("Appearance")
            ListItem(
                headlineContent = { Text("Wallpaper colors") },
                supportingContent = { Text("Match your phone's Material You theme") },
                trailingContent = { Switch(checked = wallpaperColors, onCheckedChange = ::saveWallpaperColors) },
                modifier = Modifier.clickable { saveWallpaperColors(!wallpaperColors) },
            )
        }
        SectionTitle("Device")
        ListItem(
            headlineContent = { Text("Mac") },
            supportingContent = { Text(if (pairId != null) "Paired" else "Not paired") },
            trailingContent = {
                if (pairId != null) FilledTonalButton(onClick = ::unpairNow) { Text("Unpair") }
                else FilledTonalButton(onClick = { scanAndPair(context, scope, onPairChanged) }) { Text("Pair") }
            },
        )
        SectionTitle("Permissions")
        permissions.forEach { permission ->
            ListItem(
                headlineContent = { Text(permission.name) },
                supportingContent = {
                    Text(when {
                        permission.granted -> "OK"
                        permission.required -> "Needed for the alarm"
                        else -> "Optional: lets the phone tell when you went to bed"
                    })
                },
                trailingContent = {
                    if (!permission.granted) FilledTonalButton(onClick = { context.startActivity(permission.fix) }) {
                        Text("Fix")
                    }
                },
            )
        }
    }
}

/** Turning it on without usage access also opens the screen that grants it. */
@Composable
private fun PhoneSleepSwitch(settings: WakeSettings, usageAccess: Boolean) {
    val context = LocalContext.current
    fun set(on: Boolean) {
        saveWakeSettings("phoneSleep" to on)
        if (on && !usageAccess) context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    ListItem(
        headlineContent = { Text("Phone screen as bedtime") },
        supportingContent = {
            Text(if (settings.phoneSleep && !usageAccess) "Needs usage access" else "Counts when you put the phone down too, not just the Mac")
        },
        trailingContent = { Switch(checked = settings.phoneSleep, onCheckedChange = ::set) },
        modifier = Modifier.clickable { set(!settings.phoneSleep) },
    )
}

/** Off = weekends ring at the usual wake time; on = their own time, picked with the system time picker. */
@Composable
private fun WeekendWakeTime(settings: WakeSettings) {
    val context = LocalContext.current
    val weekend = settings.weekendWakeTime
    fun pick(time: String) {
        val (hour, minute) = time.split(":").map(String::toInt)
        TimePickerDialog(context, { _, h, m -> saveWakeSettings("weekendWakeTime" to "%02d:%02d".format(h, m)) },
            hour, minute, DateFormat.is24HourFormat(context)).show()
    }
    ListItem(
        headlineContent = { Text("Weekend wake time") },
        supportingContent = { Text(weekend?.let { "Sat & Sun at " + displayTime(context, it) } ?: "Same as weekdays") },
        trailingContent = {
            Switch(
                checked = weekend != null,
                onCheckedChange = { on -> if (on) pick(settings.wakeTime) else saveWakeSettings("weekendWakeTime" to FieldValue.delete()) },
            )
        },
        modifier = Modifier.clickable { pick(weekend ?: settings.wakeTime) },
    )
}

@Composable
private fun SectionTitle(text: String) = Text(
    text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
)
