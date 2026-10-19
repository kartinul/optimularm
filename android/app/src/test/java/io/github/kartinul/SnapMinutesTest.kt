package io.github.kartinul

import io.github.kartinul.ui.snapMinutes
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapMinutesTest {
    @Test
    fun nearQuarterSnapsToQuarter() = assertEquals(15, snapMinutes(12.0))

    @Test
    fun justPastQuarterSnapsBack() = assertEquals(30, snapMinutes(33.9))

    @Test
    fun betweenQuartersUsesFiveMinuteSteps() = assertEquals(10, snapMinutes(9.0))

    @Test
    fun nearMidnightWrapsToZero() = assertEquals(0, snapMinutes(1438.0))
}
