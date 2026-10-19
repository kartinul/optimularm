package io.github.kartinul.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kartinul.data.PAIR_ID
import io.github.kartinul.data.markPaired

@Composable
fun SettingsScreen(pairStatus: String?, modifier: Modifier = Modifier) {
    val paired = pairStatus == "paired"

    Column(modifier) {
        Text(
            "Device",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text("Mac") },
            supportingContent = { Text(if (paired) "Paired · $PAIR_ID" else "Not paired") },
            trailingContent = {
                FilledTonalButton(onClick = ::markPaired, enabled = !paired) { Text(if (paired) "Paired" else "Pair") }
            },
        )
    }
}
