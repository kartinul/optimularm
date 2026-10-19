package io.github.kartinul.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.kartinul.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

const val DAY_MINUTES = 24 * 60
private const val SNAP_MINUTES = 5
private const val QUARTER = 15
private const val MAGNET = 3.0 // a quarter pulls the handle in from this many minutes
private const val STICKY = 6.0 // once on a quarter, the handle stays until dragged this far

/** Minutes between two times on the 24h ring, the short way round. */
private fun ringDistance(a: Double, b: Int): Double = abs(a - b).let { minOf(it, DAY_MINUTES - it) }

/** 5-minute steps, with :00 :15 :30 :45 magnetic (pull in from ±3) and sticky (hold until ±6). */
fun snapMinutes(raw: Double, current: Int): Int {
    if (current % QUARTER == 0 && ringDistance(raw, current) < STICKY) return current
    val quarter = (raw / QUARTER).roundToInt() * QUARTER % DAY_MINUTES
    if (ringDistance(raw, quarter) <= MAGNET) return quarter
    return (raw / SNAP_MINUTES).roundToInt() * SNAP_MINUTES % DAY_MINUTES
}

fun String.toMinutes(): Int = split(":").map(String::toInt).let { (hour, minute) -> hour * 60 + minute }
fun Int.toTime(): String = String.format(Locale.ROOT, "%02d:%02d", this / 60, this % 60)

/** Midnight at the top, clockwise. Returns radians from 3 o'clock, the way Canvas measures angles. */
private fun Int.toRadians() = Math.toRadians(this / 4.0 - 90)

private fun pointOnRing(center: Offset, radius: Float, minutes: Int) = Offset(
    center.x + radius * cos(minutes.toRadians()).toFloat(),
    center.y + radius * sin(minutes.toRadians()).toFloat(),
)

private fun minutesAt(center: Offset, point: Offset, current: Int): Int {
    val degrees = Math.toDegrees(atan2((point.x - center.x).toDouble(), (center.y - point.y).toDouble()))
    return snapMinutes((degrees + 360) % 360 * 4, current)
}

private enum class Handle { Bedtime, Wake }

/** 24h ring with a draggable bedtime and wake handle. Changes are reported once the finger lifts. */
@Composable
fun SleepDial(
    bedtime: String,
    wakeTime: String,
    onBedtimeChange: (String) -> Unit,
    onWakeTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    var bed by remember(bedtime) { mutableIntStateOf(bedtime.toMinutes()) }
    var wake by remember(wakeTime) { mutableIntStateOf(wakeTime.toMinutes()) }
    var dragged by remember { mutableStateOf<Handle?>(null) }

    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurfaceVariant)
    val bedIcon = painterResource(R.drawable.ic_bedtime)
    val alarmIcon = painterResource(R.drawable.ic_alarm)

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(bedtime, wakeTime) {
                    val stroke = 40.dp.toPx()
                    val ringCenter = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - stroke / 2
                    detectDragGestures(
                        onDragStart = { start ->
                            val toBed = (start - pointOnRing(ringCenter, radius, bed)).getDistance()
                            val toWake = (start - pointOnRing(ringCenter, radius, wake)).getDistance()
                            dragged = when {
                                minOf(toBed, toWake) > stroke -> null
                                toBed < toWake -> Handle.Bedtime
                                else -> Handle.Wake
                            }
                        },
                        onDragEnd = {
                            when (dragged) {
                                Handle.Bedtime -> onBedtimeChange(bed.toTime())
                                Handle.Wake -> onWakeTimeChange(wake.toTime())
                                null -> {}
                            }
                            dragged = null
                        },
                        onDragCancel = { dragged = null },
                    ) { change, _ ->
                        val current = if (dragged == Handle.Bedtime) bed else wake
                        val minutes = minutesAt(ringCenter, change.position, current)
                        val moved = when (dragged) {
                            Handle.Bedtime -> (bed != minutes).also { bed = minutes }
                            Handle.Wake -> (wake != minutes).also { wake = minutes }
                            null -> false
                        }
                        if (moved) haptics.performHapticFeedback(
                            // a strong click on every quarter, a light tick on the 5-minute steps between
                            if (minutes % QUARTER == 0) HapticFeedbackType.GestureThresholdActivate
                            else HapticFeedbackType.SegmentFrequentTick
                        )
                    }
                }
        ) {
            val stroke = 40.dp.toPx()
            val radius = size.minDimension / 2 - stroke / 2
            val sweep = ((wake - bed + DAY_MINUTES) % DAY_MINUTES) / 4f

            drawCircle(colors.surfaceContainerHighest, radius, style = Stroke(stroke))
            drawArc(
                colors.primaryContainer, bed / 4f - 90, sweep, useCenter = false,
                topLeft = center - Offset(radius, radius), size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )

            val tickOuter = radius - stroke / 2 - 8.dp.toPx()
            for (quarterHour in 0 until 24 * 4) {
                val isHour = quarterHour % 4 == 0
                val minutes = quarterHour * 15
                drawLine(
                    if (isHour) colors.outline else colors.outlineVariant,
                    pointOnRing(center, tickOuter, minutes),
                    pointOnRing(center, tickOuter - if (isHour) 8.dp.toPx() else 4.dp.toPx(), minutes),
                    strokeWidth = if (isHour) 2.dp.toPx() else 1.dp.toPx(),
                )
            }
            for (hour in listOf(0, 6, 12, 18)) {
                val layout = textMeasurer.measure("$hour", labelStyle)
                val at = pointOnRing(center, tickOuter - 24.dp.toPx(), hour * 60)
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }

            fun drawHandle(minutes: Int, icon: Painter) {
                val at = pointOnRing(center, radius, minutes)
                val iconSize = 24.dp.toPx()
                drawCircle(colors.primary, stroke / 2 - 4.dp.toPx(), at)
                translate(at.x - iconSize / 2, at.y - iconSize / 2) {
                    with(icon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(colors.onPrimary)) }
                }
            }
            drawHandle(bed, bedIcon)
            drawHandle(wake, alarmIcon)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DialTime(displayTime(context, bed.toTime()), bedIcon, colors.onSurfaceVariant)
            DialTime(displayTime(context, wake.toTime()), alarmIcon, colors.onSurface)
        }
    }
}

@Composable
private fun DialTime(time: String, icon: Painter, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 6.dp))
        Text(time, style = MaterialTheme.typography.headlineMedium, color = color)
    }
}
