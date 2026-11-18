package io.github.kartinul

import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kartinul.data.canSnooze
import io.github.kartinul.data.loadAlarmPrefs
import io.github.kartinul.data.Night
import io.github.kartinul.data.loadCalcPrefs
import io.github.kartinul.data.recordNight
import io.github.kartinul.data.saveRating
import io.github.kartinul.ui.theme.OptimularmTheme

class RingActivity : ComponentActivity() {
    private var stopped by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OptimularmTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (stopped) Rating() else Ringing(onStop = ::stop)
                    }
                }
            }
        }
    }

    private fun stop() {
        stopRinging(this)
        stopped = true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volumeKey || stopped) return super.onKeyDown(keyCode, event)
        stop()
        return true
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
                    val (ring, sleep) = rungAlarm(this@RingActivity)
                    val calc = loadCalcPrefs(this@RingActivity)
                    val wake = doneWake(this@RingActivity)
                    // a late-sleep nap, the default alarm or one capped at wake says nothing about cycle ends
                    val optimized = sleep != null && sleep < checkTime(wake, calc) && ring < wake
                    recordNight(this@RingActivity, Night(sleep, ring, rating, calc.feelGoodAfter, optimized))
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
