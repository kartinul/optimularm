package io.github.kartinul.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Something the alarm needs, and the settings screen that grants it. */
data class Permission(val name: String, val granted: Boolean, val fix: Intent)

fun checkPermissions(context: Context): List<Permission> {
    val packageName = context.packageName
    val packageUri = Uri.parse("package:$packageName")
    val alarms = context.getSystemService(AlarmManager::class.java)
    val notifications = context.getSystemService(NotificationManager::class.java)
    val power = context.getSystemService(PowerManager::class.java)
    return listOf(
        Permission(
            "Notifications",
            NotificationManagerCompat.from(context).areNotificationsEnabled(),
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        ),
        Permission(
            "Full-screen alarm",
            Build.VERSION.SDK_INT < 34 || notifications.canUseFullScreenIntent(),
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri),
        ),
        Permission(
            "Exact alarms",
            Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms(),
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri),
        ),
        // "Unrestricted" also keeps Samsung from putting the app in Sleeping apps
        Permission(
            "Battery unrestricted",
            power.isIgnoringBatteryOptimizations(packageName),
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
        ),
    )
}
