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
    private val checkPassed = at(20, 6, 30)

    @Test
    fun noSleepEventRingsAtWake() = assertEquals(wake, ringTime(wake, null, checkPassed))

    @Test
    fun beforeCheckRingsAtWake() = assertEquals(wake, ringTime(wake, at(19, 23, 50), at(20, 6, 0)))

    @Test
    fun sleepBeforeMidnightRings15AfterLastBoundary() =
        assertEquals(at(20, 7, 50), ringTime(wake, at(19, 23, 50), checkPassed))

    @Test
    fun earlyMorningSleepFitsOneCycle() =
        assertEquals(wake, ringTime(wake, at(20, 6, 0), checkPassed))

    @Test
    fun boundaryExactlyAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 15), checkPassed))

    @Test
    fun boundary15AfterWakeRingsAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 30), checkPassed))

    @Test
    fun boundary20AfterWakeIsTooFar() = assertEquals(at(20, 7, 5), ringTime(wake, at(20, 0, 35), checkPassed))

    @Test
    fun boundary15BeforeWakeRingsAtWake() = assertEquals(wake, ringTime(wake, at(20, 0, 0), checkPassed))

    @Test
    fun lateSleepNaps30() = assertEquals(at(20, 7, 30), ringTime(wake, at(20, 7, 0), at(20, 7, 1)))

    @Test
    fun lateSleepCappedAtWake() = assertEquals(wake, ringTime(wake, at(20, 7, 45), at(20, 7, 46)))

    @Test
    fun customPrefs() = assertEquals(
        at(20, 6, 40),
        ringTime(wake, at(19, 23, 55), checkPassed, CalcPrefs(fallAsleep = 5, cycle = 100, feelGood = 0)),
    )

    @Test
    fun noCycleFitsWhenFallAsleepIsLong() = assertEquals(
        wake, ringTime(wake, at(20, 6, 25), checkPassed, CalcPrefs(fallAsleep = 45, feelGood = 0)),
    )

    @Test
    fun bedtimesRingAtWake() = assertEquals(
        listOf(at(20, 0, 30) to 5, at(20, 2, 0) to 4),
        bedtimes(wake).filter { it.second in 4..5 },
    )

    @Test
    fun cutoffIsPreviousDayWhenAfterWake() = assertEquals(at(19, 22, 0), cutoffBefore(wake, "22:00"))

    @Test
    fun cutoffIsSameDayWhenBeforeWake() = assertEquals(at(20, 2, 0), cutoffBefore(wake, "02:00"))
}
