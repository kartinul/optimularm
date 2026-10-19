package io.github.kartinul.data

import android.content.Context
import androidx.core.content.edit

/** How the alarm rings. Phone-only, so it lives in SharedPreferences, not Firestore. */
data class AlarmPrefs(
    val ringtone: String? = null, // null = the phone's default alarm sound
    val volume: Float = 1f,
    val gradual: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val maxSnoozes: Int = 3,
)

val SNOOZE_LENGTHS = listOf(5, 10, 15)
val SNOOZE_LIMITS = listOf(1, 2, 3)

private fun prefs(context: Context) = context.getSharedPreferences("ring", Context.MODE_PRIVATE)

fun loadAlarmPrefs(context: Context): AlarmPrefs = prefs(context).let {
    val defaults = AlarmPrefs()
    AlarmPrefs(
        ringtone = it.getString("ringtone", defaults.ringtone),
        volume = it.getFloat("volume", defaults.volume),
        gradual = it.getBoolean("gradual", defaults.gradual),
        vibrate = it.getBoolean("vibrate", defaults.vibrate),
        // older builds offered other values; fall back to the default if a stored one is gone
        snoozeMinutes = it.getInt("snoozeMinutes", defaults.snoozeMinutes).takeIf(SNOOZE_LENGTHS::contains)
            ?: defaults.snoozeMinutes,
        maxSnoozes = it.getInt("maxSnoozes", defaults.maxSnoozes).takeIf(SNOOZE_LIMITS::contains) ?: defaults.maxSnoozes,
    )
}

fun saveAlarmPrefs(context: Context, alarm: AlarmPrefs) = prefs(context).edit {
    putString("ringtone", alarm.ringtone)
    putFloat("volume", alarm.volume)
    putBoolean("gradual", alarm.gradual)
    putBoolean("vibrate", alarm.vibrate)
    putInt("snoozeMinutes", alarm.snoozeMinutes)
    putInt("maxSnoozes", alarm.maxSnoozes)
}

/** Snoozes used since the alarm last rang for real. */
fun snoozesUsed(context: Context): Int = prefs(context).getInt("snoozesUsed", 0)

fun setSnoozesUsed(context: Context, count: Int) = prefs(context).edit { putInt("snoozesUsed", count) }

fun canSnooze(context: Context): Boolean = snoozesUsed(context) < loadAlarmPrefs(context).maxSnoozes
