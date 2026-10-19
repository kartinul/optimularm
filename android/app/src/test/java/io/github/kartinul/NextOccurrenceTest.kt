package io.github.kartinul

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextOccurrenceTest {
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }

    @Test
    fun laterToday() = assertEquals(at(19, 8, 0), nextOccurrence("08:00", at(19, 6, 30)))

    @Test
    fun alreadyPassedMeansTomorrow() = assertEquals(at(20, 8, 0), nextOccurrence("08:00", at(19, 23, 10)))

    @Test
    fun exactlyNowMeansTomorrow() = assertEquals(at(20, 8, 0), nextOccurrence("08:00", at(19, 8, 0)))
}
