package io.github.kartinul

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.util.Calendar

const val ALARM_NOTIFICATION_ID = 1
private const val ALARM_CHANNEL = "ring"
private const val SNOOZE = "snooze"
private const val SNOOZE_MINUTES = 9
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
    val millis = System.currentTimeMillis() + SNOOZE_MINUTES * MINUTE
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
            syncAsync(context)
        }

        val canNotify = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!canNotify) return

        val manager = NotificationManagerCompat.from(context)
        // the notification itself rings, so it is loud even when the phone is in use and no full screen shows
        manager.deleteNotificationChannel("alarm")
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ALARM_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Alarm")
                .setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
                .setVibrationEnabled(true)
                .setVibrationPattern(longArrayOf(0, 800, 800))
                .build()
        )

        val ringIntent = PendingIntent.getActivity(
            context, 0, Intent(context, RingActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(ringIntent, true)
            .setContentIntent(ringIntent)
            .setOngoing(true)
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }
        manager.notify(ALARM_NOTIFICATION_ID, notification)
    }
}
