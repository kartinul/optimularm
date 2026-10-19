package io.github.kartinul

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.kartinul.data.loadAlarmPrefs
import io.github.kartinul.data.setSnoozesUsed
import io.github.kartinul.data.snoozesUsed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar

private const val SNOOZE = "snooze"
private const val SNOOZED_UNTIL = "snoozedUntil"
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
private fun snoozeIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context, 2, Intent(context, AlarmReceiver::class.java).putExtra(SNOOZE, true), PendingIntent.FLAG_IMMUTABLE
)

fun snooze(context: Context) {
    val intent = snoozeIntent(context)
    val showIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    val millis = System.currentTimeMillis() + loadAlarmPrefs(context).snoozeMinutes * MINUTE
    setSnoozesUsed(context, snoozesUsed(context) + 1)
    alarmPrefs(context).edit { putLong(SNOOZED_UNTIL, millis) }
    context.getSystemService(AlarmManager::class.java)
        .setAlarmClock(AlarmManager.AlarmClockInfo(millis, showIntent), intent)
}

/** When a pending snooze will ring, or null. */
fun snoozedUntil(context: Context): Long? =
    alarmPrefs(context).getLong(SNOOZED_UNTIL, 0).takeIf { it > System.currentTimeMillis() }

/** snoozedUntil(), updated live whenever the alarm prefs change (snooze, cancel, ring). */
fun snoozedUntilFlow(context: Context): Flow<Long?> = callbackFlow {
    val prefs = alarmPrefs(context)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(snoozedUntil(context)) }
    trySend(snoozedUntil(context))
    prefs.registerOnSharedPreferenceChangeListener(listener)
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
}

/** Stops any ringing and drops a pending snooze. The next regular alarm stays. */
fun cancelSnooze(context: Context) {
    stopRinging(context)
    context.getSystemService(AlarmManager::class.java).cancel(snoozeIntent(context))
    setSnoozesUsed(context, 0)
    alarmPrefs(context).edit { remove(SNOOZED_UNTIL) }
}

fun cancelAlarm(context: Context) {
    context.getSystemService(AlarmManager::class.java).cancel(ownAlarmIntent(context))
    alarmPrefs(context).edit { remove(RING_AT) }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        alarmPrefs(context).edit { remove(SNOOZED_UNTIL) }
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
