package io.github.kartinul

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.kartinul.data.fetchLatestSleep
import io.github.kartinul.data.loadCalcPrefs
import io.github.kartinul.data.fetchWakeSettings
import io.github.kartinul.data.pairId
import io.github.kartinul.data.reportRingAt
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

const val TAG = "optimularm"

/** Reads settings and the latest sleep event, then (re)sets the alarm and the wake − 90 check. */
suspend fun sync(context: Context) {
    val id = pairId
    val settings = id?.let { fetchWakeSettings(it) }
    if (id == null || settings?.alarmOn != true) {
        if (id != null && ringAt(context) != null) reportRingAt(id, null)
        cancelAlarm(context)
        cancelCheck(context)
        return
    }
    val now = System.currentTimeMillis()
    // an alarm rings at most a cycle before its wake time, so a later "done" mark is stale; ignore it
    val done = doneWake(context).takeIf { it <= now + CYCLE } ?: 0
    val from = Calendar.getInstance().apply { timeInMillis = maxOf(now, done) }
    val upcoming = nextWake(settings, from)
    val wake = (if (upcoming.timeInMillis == skippedWake(context)) nextWake(settings, upcoming) else upcoming).timeInMillis
    val cutoff = cutoffBefore(wake, settings.sleepCutoff)
    val macSleep = try {
        fetchLatestSleep(id, cutoff, Source.SERVER)
    } catch (e: FirebaseFirestoreException) {
        // Doze often cuts the network at the wake − 90 check: use the cache now, sync again once online
        Log.w(TAG, "offline, using cached sleep", e)
        retrySyncWhenOnline(context)
        fetchLatestSleep(id, cutoff, Source.CACHE)
    }
    // whichever device was put down last says when you actually went to bed
    val sleep = listOfNotNull(macSleep?.sleepTime, phoneSleep(context, cutoff)).maxOrNull()?.takeIf { it < wake }
    val calc = loadCalcPrefs(context)
    val ring = ringTime(wake, sleep, calc)
    if (ringAt(context) != ring) reportRingAt(id, ring)
    scheduleAlarm(context, ring, wake, sleep, macSleep?.id?.takeIf { sleep == macSleep.sleepTime })
    if (now < checkTime(wake, calc)) scheduleCheck(context, checkTime(wake, calc)) else cancelCheck(context)
    Log.i(TAG, "sync: wake=${Date(wake)} sleep=${sleep?.let(::Date)} ring=${Date(ring)}")
}

private fun checkIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context, 1, Intent(context, CheckReceiver::class.java), PendingIntent.FLAG_IMMUTABLE
)

private fun scheduleCheck(context: Context, millis: Long) = context.getSystemService(AlarmManager::class.java)
    .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, checkIntent(context))

private fun cancelCheck(context: Context) =
    context.getSystemService(AlarmManager::class.java).cancel(checkIntent(context))

/** Runs sync() off the main thread; goAsync() keeps the receiver alive until it's done. */
fun BroadcastReceiver.syncAsync(context: Context) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // receivers get ~10 s; if Firestore is slower, retry once the network is back
            withTimeoutOrNull(9_000) { sync(context) } ?: retrySyncWhenOnline(context)
        } catch (e: Exception) {
            Log.e(TAG, "sync failed", e)
        } finally {
            pending.finish()
        }
    }
}

class CheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = syncAsync(context)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = syncAsync(context)
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        sync(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "worker sync failed", e)
        Result.retry()
    }
}

fun retrySyncWhenOnline(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(
    "retry", ExistingWorkPolicy.REPLACE,
    OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED)).build(),
)

fun startSyncWorker(context: Context) = WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "sync", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
)
