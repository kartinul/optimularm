package io.github.kartinul

import io.github.kartinul.ui.timeUntil
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUntilTest {
    @Test
    fun underAMinuteRoundsUp() = assertEquals("in 1 min", timeUntil(20_000))

    @Test
    fun partialMinuteRoundsUp() = assertEquals("in 3 min", timeUntil(2 * MINUTE + 1))

    @Test
    fun hoursAndMinutes() = assertEquals("in 7 h 20 min", timeUntil(440 * MINUTE))

    @Test
    fun wholeHours() = assertEquals("in 2 h", timeUntil(120 * MINUTE))
}
