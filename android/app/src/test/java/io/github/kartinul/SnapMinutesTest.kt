package io.github.kartinul

import io.github.kartinul.ui.snapMinutes
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapMinutesTest {
    @Test
    fun roundsUpToQuarter() = assertEquals(15, snapMinutes(9.0))

    @Test
    fun roundsDownToQuarter() = assertEquals(30, snapMinutes(37.0))

    @Test
    fun nearMidnightWrapsToZero() = assertEquals(0, snapMinutes(1435.0))
}
