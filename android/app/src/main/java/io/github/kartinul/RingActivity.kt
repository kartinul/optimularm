package io.github.kartinul

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kartinul.data.canSnooze
import io.github.kartinul.data.loadAlarmPrefs
import io.github.kartinul.data.saveRating
import io.github.kartinul.ui.theme.OptimularmTheme

class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OptimularmTheme {
                var stopped by remember { mutableStateOf(false) }
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (stopped) Rating() else Ringing(onStop = {
                            stopRinging(this@RingActivity)
                            stopped = true
                        })
                    }
                }
            }
        }
    }

    @Composable
    private fun Ringing(onStop: () -> Unit) {
        Text(
            DateFormat.getTimeFormat(this).format(System.currentTimeMillis()),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Wake up",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(48.dp))
        FilledTonalButton(onClick = onStop, modifier = Modifier.size(width = 200.dp, height = 72.dp)) {
            Text("Stop", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        if (canSnooze(this)) OutlinedButton(onClick = {
            snoozeRinging(this)
            finish()
        }) {
            Text("Snooze ${loadAlarmPrefs(this@RingActivity).snoozeMinutes} min")
        }
    }

    @Composable
    private fun Rating() {
        Text("How did you sleep?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rating in 1..5) {
                FilledTonalButton(onClick = {
                    saveRating(rating, rungEventId(this@RingActivity))
                    finish()
                }) {
                    Text("$rating")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = ::finish) { Text("Skip") }
    }
}
