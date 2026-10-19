package io.github.kartinul

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.kartinul.data.canSnooze
import io.github.kartinul.data.loadAlarmPrefs

const val ALARM_NOTIFICATION_ID = 1
private const val RING_CHANNEL = "ringing"
private const val ACTION_STOP = "stop"
private const val ACTION_SNOOZE = "snooze"
private const val RAMP_STEPS = 30 // gradual volume: one step per second
private const val SILENCE_AFTER = 10 * MINUTE

fun startRinging(context: Context) =
    ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))

fun stopRinging(context: Context) = context.startService(ringIntent(context, ACTION_STOP))

fun snoozeRinging(context: Context) = context.startService(ringIntent(context, ACTION_SNOOZE))

private fun ringIntent(context: Context, action: String) = Intent(context, RingService::class.java).setAction(action)

private val alarmAudio = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Plays the alarm in the foreground, so it rings whether the phone is locked, idle or in use. */
class RingService : Service() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator get() = getSystemService(Vibrator::class.java)

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stop()
            ACTION_SNOOZE -> {
                snooze(this)
                stop()
            }
            else -> ring()
        }
        return START_NOT_STICKY
    }

    private fun ring() {
        ServiceCompat.startForeground(
            this, ALARM_NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        if (player != null) return
        val alarm = loadAlarmPrefs(this)
        val sound = alarm.ringtone?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(alarmAudio)
            setDataSource(this@RingService, sound)
            isLooping = true
            prepare()
            start()
        }
        if (alarm.gradual) ramp(alarm.volume, 1) else player?.setVolume(alarm.volume, alarm.volume)
        if (alarm.vibrate) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0), alarmAudio)
        handler.postDelayed(::stop, SILENCE_AFTER)
    }

    private fun ramp(target: Float, step: Int) {
        val volume = target * step / RAMP_STEPS
        player?.setVolume(volume, volume)
        if (step < RAMP_STEPS) handler.postDelayed({ ramp(target, step + 1) }, 1000)
    }

    private fun notification() = NotificationCompat.Builder(this, RING_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Wake up")
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setFullScreenIntent(ringScreen(), true)
        .setContentIntent(ringScreen())
        .setOngoing(true)
        .apply { if (canSnooze(this@RingService)) addAction(0, "Snooze", serviceAction(ACTION_SNOOZE)) }
        .addAction(0, "Stop", serviceAction(ACTION_STOP))
        .build()
        .also { createChannel() }

    private fun ringScreen() = PendingIntent.getActivity(
        this, 0, Intent(this, RingActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceAction(action: String) =
        PendingIntent.getService(this, action.hashCode(), ringIntent(this, action), PendingIntent.FLAG_IMMUTABLE)

    // silent channel: the service plays the sound itself, at the chosen volume
    private fun createChannel() = NotificationManagerCompat.from(this).run {
        deleteNotificationChannel("alarm")
        deleteNotificationChannel("ring")
        createNotificationChannel(
            NotificationChannelCompat.Builder(RING_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Alarm")
                .setSound(null, null)
                .setVibrationEnabled(false)
                .build()
        )
    }

    private fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        vibrator.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}
