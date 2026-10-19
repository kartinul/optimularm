package io.github.kartinul

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import io.github.kartinul.data.loadAlarmPrefs
import io.github.kartinul.data.setSnoozesUsed
import io.github.kartinul.data.snoozesUsed
import java.util.Calendar

private const val SNOOZE = "snooze"
private const val RING_AT = "ringAt"
private const val WAKE = "wake"
private const val DONE_WAKE = "doneWake"
private const val EVENT_ID = "eventId"
private const val RUNG_EVENT_ID = "rungEventId"

fun ownAlarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context, 0, Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE
)

private fun alarmPrefs(context: Context) = context.getSharedPreferences("alarm", Context.MODE_PRIVATE)

fun ringAt(context: Context): Long? = alarmPrefs(context).getLong(RING_AT, 0).takeIf { it > 0 }

/** The wake time whose alarm already rang, so sync() moves on to the next day. */
fun doneWake(context: Context): Long = alarmPrefs(context).getLong(DONE_WAKE, 0)

/** The sleep event behind the alarm that rang last, for the rating. */
fun rungEventId(context: Context): String? = alarmPrefs(context).getString(RUNG_EVENT_ID, null)

fun nextOccurrence(time: String, now: Calendar = Calendar.getInstance()): Calendar {
    val (hour, minute) = time.split(":").map(String::toInt)
    return (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
    }
}

fun scheduleAlarm(context: Context, millis: Long, wake: Long, eventId: String?) {
    val showIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    context.getSystemService(AlarmManager::class.java)
        .setAlarmClock(AlarmManager.AlarmClockInfo(millis, showIntent), ownAlarmIntent(context))
    alarmPrefs(context).edit { putLong(RING_AT, millis).putLong(WAKE, wake).putString(EVENT_ID, eventId) }
}

/** A separate alarm (request code 2), so sync() never overwrites a snooze. */
fun snooze(context: Context) {
    val intent = PendingIntent.getBroadcast(
        context, 2, Intent(context, AlarmReceiver::class.java).putExtra(SNOOZE, true), PendingIntent.FLAG_IMMUTABLE
    )
    val showIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    val millis = System.currentTimeMillis() + loadAlarmPrefs(context).snoozeMinutes * MINUTE
    setSnoozesUsed(context, snoozesUsed(context) + 1)
    context.getSystemService(AlarmManager::class.java)
        .setAlarmClock(AlarmManager.AlarmClockInfo(millis, showIntent), intent)
}

fun cancelAlarm(context: Context) {
    context.getSystemService(AlarmManager::class.java).cancel(ownAlarmIntent(context))
    alarmPrefs(context).edit { remove(RING_AT) }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.getBooleanExtra(SNOOZE, false)) {
            val prefs = alarmPrefs(context)
            prefs.edit {
                putLong(DONE_WAKE, prefs.getLong(WAKE, 0))
                putString(RUNG_EVENT_ID, prefs.getString(EVENT_ID, null))
                remove(RING_AT)
            }
            setSnoozesUsed(context, 0)
            syncAsync(context)
        }
        startRinging(context)
    }
}
