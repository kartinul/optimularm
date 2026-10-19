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
import io.github.kartinul.data.unpair
import io.github.kartinul.ui.theme.saveWallpaperColors
import io.github.kartinul.ui.theme.wallpaperColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    pairId: String?,
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
                supportingContent = { Text(if (permission.granted) "OK" else "Needed for the alarm") },
                trailingContent = {
                    if (!permission.granted) FilledTonalButton(onClick = { context.startActivity(permission.fix) }) {
                        Text("Fix")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(
    text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
)
