package io.github.kartinul

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val ACTION_CANCEL_SNOOZE = "io.github.kartinul.CANCEL_SNOOZE"
private const val SNOOZE_NOTIFICATION_ID = 2
private const val SNOOZE_CHANNEL = "snooze"

/** "Snoozed · rings 7:42 PM" with a live countdown and a Cancel snooze button. */
@SuppressLint("MissingPermission") // checked by areNotificationsEnabled()
fun showSnoozeNotification(context: Context, until: Long) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    manager.createNotificationChannel(
        NotificationChannelCompat.Builder(SNOOZE_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Snooze")
            .build()
    )
    val cancel = PendingIntent.getBroadcast(
        context, 3, Intent(context, AlarmReceiver::class.java).setAction(ACTION_CANCEL_SNOOZE), PendingIntent.FLAG_IMMUTABLE
    )
    val open = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, SNOOZE_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Snoozed")
        .setContentText("Rings at " + DateFormat.getTimeFormat(context).format(until))
        .setWhen(until)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setOngoing(true)
        .setContentIntent(open)
        .addAction(0, "Cancel snooze", cancel)
        .setTimeoutAfter(until - System.currentTimeMillis())
        .build()
    manager.notify(SNOOZE_NOTIFICATION_ID, notification)
}

fun hideSnoozeNotification(context: Context) = NotificationManagerCompat.from(context).cancel(SNOOZE_NOTIFICATION_ID)
