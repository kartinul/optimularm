package io.github.kartinul

import io.github.kartinul.data.Night
import io.github.kartinul.data.WakeSettings
import io.github.kartinul.data.tunedFeelGoodAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class NextWakeTest {
    // 2026-10-16 is a Friday
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }

    private val settings = WakeSettings(wakeTime = "07:00", weekendWakeTime = "09:30")

    @Test
    fun fridayNightWakesAtWeekendTime() = assertEquals(at(17, 9, 30), nextWake(settings, at(16, 23, 0)))

    @Test
    fun sundayNightWakesAtWeekdayTime() = assertEquals(at(19, 7, 0), nextWake(settings, at(18, 23, 0)))

    @Test
    fun saturdayAfterWeekendTimeMeansSunday() = assertEquals(at(18, 9, 30), nextWake(settings, at(17, 10, 0)))

    @Test
    fun noWeekendTimeMatchesNextOccurrence() =
        assertEquals(nextOccurrence("07:00", at(16, 23, 0)), nextWake(WakeSettings(wakeTime = "07:00"), at(16, 23, 0)))
}

class LastScreenOffTest {
    private val m = MINUTE

    @Test
    fun screenStillOnMeansAwake() = assertNull(lastScreenOff(listOf(false to 0L, true to 10 * m)))

    @Test
    fun lastLongSessionCounts() = assertEquals(60 * m, lastScreenOff(listOf(false to 0L, true to 30 * m, false to 60 * m)))

    @Test
    fun shortGlanceIsIgnored() = assertEquals(0L, lastScreenOff(listOf(false to 0L, true to 120 * m, false to 122 * m)))

    @Test
    fun firstOffCountsWithoutKnownOn() = assertEquals(5 * m, lastScreenOff(listOf(false to 5 * m)))

    @Test
    fun nothingMeansUnknown() = assertNull(lastScreenOff(emptyList()))
}

class TuneTest {
    private fun nights(feelGoodAfter: Int, vararg ratings: Int) =
        ratings.map { Night(sleep = 0, ring = 0, rating = it, feelGoodAfter = feelGoodAfter, optimized = true) }

    @Test
    fun staysUntilTrialIsDone() = assertEquals(15, tunedFeelGoodAfter(nights(15, 2, 2), 15))

    @Test
    fun triesUntriedNeighbourAfterTrial() = assertEquals(10, tunedFeelGoodAfter(nights(15, 3, 3, 3), 15))

    @Test
    fun settlesOnBestRated() =
        assertEquals(10, tunedFeelGoodAfter(nights(15, 3, 3, 3) + nights(10, 5, 4, 5) + nights(20, 2, 2, 2), 15))

    @Test
    fun ignoresDefaultAlarms() = assertEquals(
        15,
        tunedFeelGoodAfter(nights(15, 3, 3) + Night(null, 0, 1, 15, optimized = false), 15),
    )
}
