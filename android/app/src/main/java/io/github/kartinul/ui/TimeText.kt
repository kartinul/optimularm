package io.github.kartinul.ui

import android.content.Context
import android.text.format.DateFormat
import io.github.kartinul.MINUTE
import io.github.kartinul.nextOccurrence

/** "08:00" → "8:00 AM" or "08:00", following the phone's 12/24h setting. */
fun displayTime(context: Context, time: String): String =
    DateFormat.getTimeFormat(context).format(nextOccurrence(time).time)

/** "in 7 h 20 min", rounded up so the last minute reads "in 1 min", never "0 min". */
fun timeUntil(millis: Long): String {
    val minutes = ((millis + MINUTE - 1) / MINUTE).coerceAtLeast(1)
    val parts = listOfNotNull(
        (minutes / 60).takeIf { it > 0 }?.let { "$it h" },
        (minutes % 60).takeIf { it > 0 }?.let { "$it min" },
    )
    return "in " + parts.joinToString(" ")
}
