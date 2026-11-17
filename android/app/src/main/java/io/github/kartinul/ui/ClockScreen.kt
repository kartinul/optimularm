package io.github.kartinul.ui

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.kartinul.MINUTE
import io.github.kartinul.R
import io.github.kartinul.cutoffBefore
import io.github.kartinul.data.SleepEvent
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.loadCalcPrefs
import io.github.kartinul.nextWake
import io.github.kartinul.ringTime
import io.github.kartinul.data.saveWakeSettings
import kotlinx.coroutines.delay

@Composable
fun ClockScreen(
    settings: WakeSettings,
    ringAt: Long?,
    latestSleep: SleepEvent?,
    missingPermissions: Int,
    snoozedUntil: Long?,
    skipped: Boolean,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            // tick on the minute boundary so the countdown flips with the clock
            delay(MINUTE - System.currentTimeMillis() % MINUTE)
            now = System.currentTimeMillis()
        }
    }
    // dragged (bedtime, wake), shown until sync lands a new ringAt or the saved settings move elsewhere
    var preview by remember(ringAt) { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(settings) {
        if (preview != settings.sleepCutoff to settings.wakeTime) preview = null
    }
    // same rule as sync(), without the already-dismissed check
    val previewRing = preview?.let { (bed, wake) ->
        val wakeAt = nextWake(settings.copy(wakeTime = wake)).timeInMillis
        val sleep = latestSleep?.sleepTime?.takeIf { it in cutoffBefore(wakeAt, bed)..wakeAt }
        ringTime(wakeAt, sleep, loadCalcPrefs(context))
    }
    // a pending snooze rings before the next regular alarm, so it takes the top spot
    val next = snoozedUntil ?: (previewRing ?: ringAt).takeIf { settings.alarmOn }
    val ringing = next != null
    val colors = MaterialTheme.colorScheme

    Column(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        // both lines always render, so toggling never changes the layout height
        Text(
            if (ringing) DateFormat.getTimeFormat(context).format(next) else displayTime(context, preview?.second ?: settings.wakeTime),
            style = MaterialTheme.typography.displayMedium,
            color = if (ringing) colors.onSurface else colors.outline,
        )
        Text(
            when {
                snoozedUntil != null -> "snoozed · " + timeUntil(snoozedUntil - now)
                next != null && skipped -> "skipped · " + timeUntil(next - now)
                next != null -> timeUntil(next - now)
                else -> "off"
            },
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        SleepDial(
            bedtime = settings.sleepCutoff,
            wakeTime = settings.wakeTime,
            onBedtimeChange = { saveWakeSettings("sleepCutoff" to it) },
            onWakeTimeChange = { saveWakeSettings("wakeTime" to it) },
            onDrag = { bed, wake -> preview = bed to wake },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BigToggle(settings.alarmOn) { saveWakeSettings("alarmOn" to it) }
            CancelButton(
                enabled = snoozedUntil != null || settings.alarmOn,
                active = snoozedUntil != null || skipped,
                label = when {
                    snoozedUntil != null -> "Cancel snooze"
                    skipped -> "Unskip next alarm"
                    else -> "Skip next alarm"
                },
                onClick = onCancel,
            )
        }
        Box(Modifier.weight(1f).padding(top = 16.dp)) {
            if (missingPermissions > 0) Text(
                "$missingPermissions permission(s) missing: fix them in Settings",
                color = colors.error,
            )
        }
    }
}

private val TRACK_WIDTH = 176.dp
private val TRACK_HEIGHT = 88.dp
private val THUMB = 72.dp

/** A large pill switch: the thumb slides across and the track fills with color. */
@Composable
private fun BigToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val padding = (TRACK_HEIGHT - THUMB) / 2
    val thumbX by animateDpAsState(if (on) TRACK_WIDTH - THUMB - padding else padding, label = "thumb")
    val track by animateColorAsState(if (on) colors.primary else colors.surfaceContainerHighest, label = "track")
    val thumb by animateColorAsState(if (on) colors.onPrimary else colors.outline, label = "thumbColor")
    val icon by animateColorAsState(if (on) colors.primary else colors.surfaceContainerHighest, label = "icon")

    Box(
        Modifier
            .size(TRACK_WIDTH, TRACK_HEIGHT)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = on, role = Role.Switch) {
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onChange(it)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = thumbX).size(THUMB).clip(CircleShape).background(thumb),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_alarm), contentDescription = "Alarm", tint = icon, modifier = Modifier.size(32.dp))
        }
    }
}

/** Round ✕, as tall as the toggle: cancels a snooze, or skips/unskips the next alarm. Red while active. */
@Composable
private fun CancelButton(enabled: Boolean, active: Boolean, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(TRACK_HEIGHT)
            .clip(CircleShape)
            .background(if (active) colors.errorContainer else colors.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_close), contentDescription = label,
            tint = when {
                active -> colors.onErrorContainer
                enabled -> colors.onSurfaceVariant
                else -> colors.outline
            },
            modifier = Modifier.size(36.dp),
        )
    }
}
