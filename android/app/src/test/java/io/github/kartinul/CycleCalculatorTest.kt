package io.github.kartinul

import io.github.kartinul.data.CalcPrefs
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CycleCalculatorTest {
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private val wake = at(20, 8, 0)

    @Test
    fun noSleepEventRingsAtWake() = assertEquals(wake, ringTime(wake, null))

    @Test
    fun sleepBeforeMidnightRings15AfterLastBoundary() =
        assertEquals(at(20, 7, 45), ringTime(wake, at(19, 23, 50)))

    @Test
    fun earlyMorningSleepFitsOneCycle() =
        assertEquals(at(20, 7, 55), ringTime(wake, at(20, 6, 0)))

    @Test
    fun boundaryExactlyAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 20)))

    @Test
    fun boundary5AfterWakeRingsAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 25)))

    @Test
    fun boundary10AfterWakeIsTooFar() = assertEquals(at(20, 6, 55), ringTime(wake, at(20, 0, 30)))

    @Test
    fun boundary15BeforeWakeRingsAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 5)))

    @Test
    fun lateSleepNaps30() = assertEquals(at(20, 7, 30), ringTime(wake, at(20, 7, 0)))

    @Test
    fun lateSleepCappedAtWake() = assertEquals(wake, ringTime(wake, at(20, 7, 45)))

    @Test
    fun customPrefs() = assertEquals(
        at(20, 6, 40),
        ringTime(wake, at(19, 23, 55), CalcPrefs(fallAsleep = 5, cycle = 100, feelGoodBefore = 0, feelGoodAfter = 0)),
    )

    @Test
    fun noCycleFitsWhenFallAsleepIsLong() = assertEquals(
        wake, ringTime(wake, at(20, 6, 25), CalcPrefs(fallAsleep = 45, feelGoodBefore = 0, feelGoodAfter = 0)),
    )

    @Test
    fun bedtimesRingAtWake() = assertEquals(
        listOf(at(20, 0, 25) to 5, at(20, 1, 55) to 4),
        bedtimes(wake).filter { it.second in 4..5 },
    )

    @Test
    fun cutoffIsPreviousDayWhenAfterWake() = assertEquals(at(19, 22, 0), cutoffBefore(wake, "22:00"))

    @Test
    fun cutoffIsSameDayWhenBeforeWake() = assertEquals(at(20, 2, 0), cutoffBefore(wake, "02:00"))
}
