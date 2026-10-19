package io.github.kartinul

import io.github.kartinul.ui.snapMinutes
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapMinutesTest {
    @Test
    fun fiveMinuteStepsBetweenQuarters() = assertEquals(10, snapMinutes(9.0, current = 5))

    @Test
    fun quarterPullsInFromThreeMinutes() = assertEquals(15, snapMinutes(12.5, current = 10))

    @Test
    fun quarterHoldsUntilSixMinutesAway() = assertEquals(15, snapMinutes(20.0, current = 15))

    @Test
    fun leavesQuarterPastSixMinutes() = assertEquals(20, snapMinutes(21.5, current = 15))

    @Test
    fun midnightIsStickyAcrossTheWrap() = assertEquals(0, snapMinutes(1436.0, current = 0))
}
