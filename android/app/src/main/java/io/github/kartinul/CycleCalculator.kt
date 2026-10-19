package io.github.kartinul

import java.util.Calendar

const val MINUTE = 60_000L
const val CYCLE = 90 * MINUTE
private const val FALL_ASLEEP = 10 * MINUTE
private const val LATE_SLEEP_NAP = 30 * MINUTE

fun checkTime(wake: Long) = wake - CYCLE

/** When to ring for [wake], given the latest counted sleep time. Never later than [wake]. */
fun ringTime(wake: Long, sleep: Long?, now: Long): Long {
    if (sleep == null || now < checkTime(wake)) return wake
    if (sleep >= checkTime(wake)) return minOf(sleep + LATE_SLEEP_NAP, wake)
    val asleep = sleep + FALL_ASLEEP
    val cycles = (wake - asleep) / CYCLE
    return if (cycles >= 1) asleep + cycles * CYCLE else wake
}

/** The last [sleepCutoff] ("HH:mm") before [wake]; earlier sleep events don't count. */
fun cutoffBefore(wake: Long, sleepCutoff: String): Long {
    val (hour, minute) = sleepCutoff.split(":").map(String::toInt)
    return Calendar.getInstance().apply {
        timeInMillis = wake
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis >= wake) add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
}
