package io.github.kartinul

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CycleCalculatorTest {
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private val wake = at(20, 8, 0)
    private val checkPassed = at(20, 6, 30)

    @Test
    fun noSleepEventRingsAtWake() = assertEquals(wake, ringTime(wake, null, checkPassed))

    @Test
    fun beforeCheckRingsAtWake() = assertEquals(wake, ringTime(wake, at(19, 23, 50), at(20, 6, 0)))

    @Test
    fun sleepBeforeMidnightPicksLastBoundary() =
        assertEquals(at(20, 7, 30), ringTime(wake, at(19, 23, 50), checkPassed))

    @Test
    fun earlyMorningSleepFitsOneCycle() =
        assertEquals(at(20, 7, 40), ringTime(wake, at(20, 6, 0), checkPassed))

    @Test
    fun boundaryExactlyAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 20), checkPassed))

    @Test
    fun noFullCycleFitsRingsAtWake() = assertEquals(wake, ringTime(wake, at(20, 6, 29), checkPassed))

    @Test
    fun lateSleepNaps30() = assertEquals(at(20, 7, 30), ringTime(wake, at(20, 7, 0), at(20, 7, 1)))

    @Test
    fun lateSleepCappedAtWake() = assertEquals(wake, ringTime(wake, at(20, 7, 45), at(20, 7, 46)))

    @Test
    fun cutoffIsPreviousDayWhenAfterWake() = assertEquals(at(19, 22, 0), cutoffBefore(wake, "22:00"))

    @Test
    fun cutoffIsSameDayWhenBeforeWake() = assertEquals(at(20, 2, 0), cutoffBefore(wake, "02:00"))
}
