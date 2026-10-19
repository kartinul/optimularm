package io.github.kartinul.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.kartinul.nextOccurrence

/** "08:00" → "8:00 AM" or "08:00", following the phone's 12/24h setting. */
fun displayTime(context: Context, time: String): String =
    DateFormat.getTimeFormat(context).format(nextOccurrence(time).time)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    time: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val state = rememberTimePickerState(time.toMinutes() / 60, time.toMinutes() % 60)
    var displayMode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { TimePickerDialogDefaults.Title(displayMode) },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) TimePickerDisplayMode.Input
                    else TimePickerDisplayMode.Picker
                },
                displayMode = displayMode,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = { onConfirm((state.hour * 60 + state.minute).toTime()) }) {
                Text("OK")
            }
        },
    ) {
        if (displayMode == TimePickerDisplayMode.Picker) TimePicker(state) else TimeInput(state)
    }
}
