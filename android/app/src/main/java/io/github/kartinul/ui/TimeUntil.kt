package io.github.kartinul.ui

import io.github.kartinul.MINUTE

/** "in 7 h 20 min", rounded up so the last minute reads "in 1 min", never "0 min". */
fun timeUntil(millis: Long): String {
    val minutes = ((millis + MINUTE - 1) / MINUTE).coerceAtLeast(1)
    val parts = listOfNotNull(
        (minutes / 60).takeIf { it > 0 }?.let { "$it h" },
        (minutes % 60).takeIf { it > 0 }?.let { "$it min" },
    )
    return "in " + parts.joinToString(" ")
}
