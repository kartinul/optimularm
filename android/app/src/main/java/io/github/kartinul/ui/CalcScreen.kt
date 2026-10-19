package io.github.kartinul.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kartinul.MINUTE
import io.github.kartinul.bedtimes
import io.github.kartinul.checkTime
import io.github.kartinul.cutoffBefore
import io.github.kartinul.data.CalcPrefs
import io.github.kartinul.data.SleepEvent
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.loadCalcPrefs
import io.github.kartinul.data.saveCalcPrefs
import io.github.kartinul.nextOccurrence
import io.github.kartinul.ringTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import io.github.kartinul.R
import java.util.Calendar
import androidx.compose.foundation.layout.aspectRatio

private const val SPAN = 12 * 60 // the scrubber reaches this many minutes before wake
private const val STEP = 5

@Composable
fun CalcScreen(
    settings: WakeSettings,
    latestSleep: SleepEvent?,
    onCalcChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val now = System.currentTimeMillis()
    var calc by remember { mutableStateOf(loadCalcPrefs(context)) }
    val wake = remember(settings.wakeTime) { nextOccurrence(settings.wakeTime).timeInMillis }
    val start = wake - SPAN * MINUTE
    fun minutesOf(time: Long) = ((time.coerceIn(start, wake) - start) / MINUTE).toInt() / STEP * STEP
    // same rule as sync(): only sleep after tonight's cutoff counts
    val realSleep = latestSleep?.sleepTime?.takeIf { it in maxOf(start, cutoffBefore(wake, settings.sleepCutoff))..wake }
    val bestBedtime = bedtimes(wake, calc).firstOrNull { it.first > now }?.first
    var sleepMinutes by remember(wake, realSleep) { mutableIntStateOf(minutesOf(realSleep ?: bestBedtime ?: now)) }
    val sleep = start + sleepMinutes * MINUTE
    // now = wake: show the alarm as it will be after the check
    val ring = ringTime(wake, sleep, now = wake, calc)
    val format = { time: Long -> DateFormat.getTimeFormat(context).format(time) }

    fun save() {
        saveCalcPrefs(context, calc)
        onCalcChanged()
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section {
            CycleDial(sleep, ring, wake, calc, format)
            Spacer(Modifier.height(12.dp))
            Slider(
                value = sleepMinutes.toFloat(),
                onValueChange = {
                    val snapped = (it / STEP).roundToInt() * STEP
                    if (snapped != sleepMinutes) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        sleepMinutes = snapped
                    }
                },
                valueRange = 0f..SPAN.toFloat(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Screen off at", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(format(sleep), style = MaterialTheme.typography.titleLarge, color = colors.primary)
            }
            Text(
                if (realSleep != null && sleepMinutes == minutesOf(realSleep)) "your last sleep, from the Mac"
                else "drag to try another bedtime",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Section("Bedtimes for ${format(wake)}") {
            bedtimes(wake, calc).forEach { (time, cycles) ->
                BedtimeRow(
                    time = format(time),
                    cycles = if (time == bestBedtime) "$cycles cycles · best" else "$cycles cycles",
                    slept = "${duration(cycles * calc.cycle - calc.feelGood)} of sleep",
                    best = time == bestBedtime,
                    past = time <= now,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        sleepMinutes = minutesOf(time)
                    },
                )
            }
        }

        Section(
            "Tuning",
            action = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        calc = CalcPrefs()
                        save()
                    },
                    enabled = calc != CalcPrefs(),
                ) { Text("Reset") }
            },
        ) {
            Tune("Fall asleep", "after the screen goes off", calc.fallAsleep, 0..45, ::save) {
                calc = calc.copy(fallAsleep = it)
            }
            Tune("Sleep cycle", "one light → deep → REM loop", calc.cycle, 70..120, ::save) {
                calc = calc.copy(cycle = it)
            }
            Tune("Feel-good window", "waking this close to a cycle end is fine", calc.feelGood, 0..30, ::save, "± ") {
                calc = calc.copy(feelGood = it)
            }
            Tune("Late-sleep nap", "if you fall asleep less than a cycle before wake", calc.lateNap, 10..60, ::save) {
                calc = calc.copy(lateNap = it)
            }
        }
    }
}

private fun subtitle(sleep: Long, ring: Long, wake: Long, calc: CalcPrefs): String {
    val asleep = if (sleep >= checkTime(wake, calc)) 0 else calc.fallAsleep
    return "${duration(((ring - sleep) / MINUTE).toInt() - asleep)} of sleep"
}

/** 345 → "5 h 45 min". */
private fun duration(minutes: Int) = listOfNotNull(
    (minutes / 60).takeIf { it > 0 }?.let { "$it h" },
    (minutes % 60).takeIf { it > 0 || minutes == 0 }?.let { "$it min" },
).joinToString(" ")

@Composable
private fun Section(
    title: String? = null,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
    ) {
        if (title != null) Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f),
            )
            action()
        }
        content()
    }
}

private val DIAL_TRACK = 30.dp

/** Minutes on a 12-hour face → degrees, 12 o'clock at the top. */
private fun dialAngle(time: Long): Float = Calendar.getInstance().run {
    timeInMillis = time
    (get(Calendar.HOUR) * 60 + get(Calendar.MINUTE)) / 2f - 90f
}

/** The night on a 12-hour clock: screen off → numbered cycles → the alarm, then any time left until wake. */
@Composable
private fun CycleDial(sleep: Long, ring: Long, wake: Long, calc: CalcPrefs, format: (Long) -> String) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val numberStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onPrimary)
    val hourStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val bedIcon = painterResource(R.drawable.ic_bedtime)
    val alarmIcon = painterResource(R.drawable.ic_alarm)
    val late = sleep >= checkTime(wake, calc)
    val asleep = if (late) sleep else sleep + calc.fallAsleep * MINUTE
    // a cycle cut short or overrun by up to feelGood still counts as one
    val cycles = buildList {
        var left = ((ring - asleep) / MINUTE).toInt()
        while (left > 0) {
            val size = if (left - calc.cycle <= calc.feelGood) left else calc.cycle
            add(size)
            left -= size
        }
    }

    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val track = DIAL_TRACK.toPx()
            val radius = size.minDimension / 2 - track / 2
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            fun at(angle: Float, r: Float) = Offset(
                center.x + r * cos(angle * PI / 180).toFloat(),
                center.y + r * sin(angle * PI / 180).toFloat(),
            )
            fun arc(color: Color, start: Long, minutes: Int, gap: Float = 0f) = drawArc(
                color, dialAngle(start) + gap, minutes / 2f - 2 * gap, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(track),
            )

            drawCircle(colors.surfaceContainerHighest, radius, style = Stroke(track))
            for (hour in 1..12) {
                val layout = measurer.measure("$hour", hourStyle)
                val spot = at(hour * 30f - 90f, radius - track / 2 - 16.dp.toPx())
                drawText(layout, topLeft = spot - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }

            arc(colors.outlineVariant, sleep, ((asleep - sleep) / MINUTE).toInt())
            var start = asleep
            cycles.forEachIndexed { index, minutes ->
                arc(if (index % 2 == 0) colors.primary else colors.primary.copy(alpha = 0.75f), start, minutes, gap = 0.6f)
                val layout = measurer.measure(if (late) "nap" else "${index + 1}", numberStyle)
                val spot = at(dialAngle(start) + minutes / 4f, radius)
                drawText(layout, topLeft = spot - Offset(layout.size.width / 2f, layout.size.height / 2f))
                start += minutes * MINUTE
            }
            arc(colors.outline.copy(alpha = 0.4f), ring, ((wake - ring) / MINUTE).toInt())

            val wakeAngle = dialAngle(wake)
            drawLine(colors.onSurface, at(wakeAngle, radius - track / 2), at(wakeAngle, radius + track / 2), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            fun marker(time: Long, icon: Painter) {
                val spot = at(dialAngle(time), radius)
                val iconSize = 18.dp.toPx()
                drawCircle(colors.onSurface, radius = track / 2 + 2.dp.toPx(), center = spot)
                translate(spot.x - iconSize / 2, spot.y - iconSize / 2) {
                    with(icon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(colors.surface)) }
                }
            }
            marker(sleep, bedIcon)
            marker(ring, alarmIcon)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("rings at", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Text(format(ring), style = MaterialTheme.typography.displaySmall)
            Text(subtitle(sleep, ring, wake, calc), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun BedtimeRow(time: String, cycles: String, slept: String, best: Boolean, past: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (best) colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = when {
            best -> colors.onPrimaryContainer
            past -> colors.outline
            else -> colors.onSurface
        }
        Text(time, style = MaterialTheme.typography.titleLarge, color = text, modifier = Modifier.width(120.dp))
        Column(Modifier.weight(1f)) {
            Text(cycles, style = MaterialTheme.typography.bodyMedium, color = text)
            Text(slept, style = MaterialTheme.typography.bodySmall, color = text.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun Tune(
    label: String,
    caption: String,
    value: Int,
    range: IntRange,
    onDone: () -> Unit,
    prefix: String = "",
    onChange: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$prefix$value min", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                val snapped = (it / STEP).roundToInt() * STEP
                if (snapped != value) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    onChange(snapped)
                }
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            onValueChangeFinished = onDone,
        )
    }
}
