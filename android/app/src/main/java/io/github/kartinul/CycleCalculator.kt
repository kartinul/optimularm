package io.github.kartinul

import io.github.kartinul.data.CalcPrefs
import java.util.Calendar

const val MINUTE = 60_000L
const val CYCLE = 90 * MINUTE

fun checkTime(wake: Long, calc: CalcPrefs = CalcPrefs()) = wake - calc.cycle * MINUTE

/** When to ring for [wake]: feelGood after the last cycle end within feelGood of [wake]. Never later than [wake]. */
fun ringTime(wake: Long, sleep: Long?, now: Long, calc: CalcPrefs = CalcPrefs()): Long {
    val check = checkTime(wake, calc)
    if (sleep == null || now < check) return wake
    if (sleep >= check) return minOf(sleep + calc.lateNap * MINUTE, wake)
    val asleep = sleep + calc.fallAsleep * MINUTE
    val cycle = calc.cycle * MINUTE
    val feelGood = calc.feelGood * MINUTE
    val cycles = (wake + feelGood - asleep) / cycle
    // only possible when fallAsleep > feelGood: not even one cycle fits
    if (cycles < 1) return wake
    return minOf(wake, asleep + cycles * cycle + feelGood)
}

/** For 6 down to 3 cycles: the latest bedtime that still rings right at [wake]. */
fun bedtimes(wake: Long, calc: CalcPrefs = CalcPrefs()): List<Pair<Long, Int>> = (6 downTo 3).map { cycles ->
    wake + (calc.feelGood - calc.fallAsleep - cycles * calc.cycle) * MINUTE to cycles
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
