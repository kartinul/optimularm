package io.github.kartinul

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

private const val GLANCE = 5 * MINUTE

/** When the phone's screen went off for good after [after] (needs usage access), or null if it's on or unknown. */
fun phoneSleep(context: Context, after: Long): Long? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(after, System.currentTimeMillis())
    val event = UsageEvents.Event()
    val screen = buildList {
        while (events.getNextEvent(event)) when (event.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> add(true to event.timeStamp)
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> add(false to event.timeStamp)
        }
    }
    return lastScreenOff(screen)
}

/** From (screen on?, time) events: the last screen-off, skipping glances shorter than [GLANCE]; null while the screen is on. */
fun lastScreenOff(screen: List<Pair<Boolean, Long>>): Long? {
    var off: Long? = null
    var on: Long? = null
    for ((interactive, time) in screen) {
        if (interactive) on = time
        else {
            if (off == null || on == null || time - on >= GLANCE) off = time
            on = null
        }
    }
    return off.takeIf { on == null }
}
