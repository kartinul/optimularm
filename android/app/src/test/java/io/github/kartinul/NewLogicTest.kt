package io.github.kartinul

import io.github.kartinul.data.Night
import io.github.kartinul.data.tunedFeelGoodAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
