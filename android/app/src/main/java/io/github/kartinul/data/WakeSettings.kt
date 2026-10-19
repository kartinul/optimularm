package io.github.kartinul.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

// shortcut: hardcoded until QR pairing exists
const val PAIR_ID = "gaga"

data class WakeSettings(
    val wakeTime: String = "08:00",
    val alarmOn: Boolean = true,
    val sleepCutoff: String = "12:00",
)

private val settingsDoc get() = Firebase.firestore.collection("settings").document(PAIR_ID)
private val pairingDoc get() = Firebase.firestore.collection("pairings").document(PAIR_ID)
private val latestSleepQuery get() =
    pairingDoc.collection("sleepEvents").orderBy("sleepTime", Query.Direction.DESCENDING).limit(1)

private fun DocumentSnapshot.toWakeSettings() = WakeSettings().let { defaults ->
    WakeSettings(
        wakeTime = getString("wakeTime") ?: defaults.wakeTime,
        alarmOn = getBoolean("alarmOn") ?: defaults.alarmOn,
        sleepCutoff = getString("sleepCutoff") ?: defaults.sleepCutoff,
    )
}

fun wakeSettingsFlow(): Flow<WakeSettings> = callbackFlow {
    val registration = settingsDoc.addSnapshotListener { snapshot, _ ->
        if (snapshot != null) trySend(snapshot.toWakeSettings())
    }
    awaitClose { registration.remove() }
}

suspend fun fetchWakeSettings(): WakeSettings = settingsDoc.get().await().toWakeSettings()

/** Latest sleep time after [after] (millis), or null. */
suspend fun fetchLatestSleep(after: Long): Long? = latestSleepQuery
    .whereGreaterThan("sleepTime", Timestamp(Date(after)))
    .get().await()
    .documents.firstOrNull()?.getTimestamp("sleepTime")?.toDate()?.time

fun latestSleepFlow(): Flow<Long?> = callbackFlow {
    val registration = latestSleepQuery.addSnapshotListener { snapshot, _ ->
        if (snapshot != null) trySend(snapshot.documents.firstOrNull()?.getTimestamp("sleepTime")?.toDate()?.time)
    }
    awaitClose { registration.remove() }
}

fun saveWakeSettings(vararg fields: Pair<String, Any?>) {
    settingsDoc.set(mapOf(*fields, "lastUpdated" to FieldValue.serverTimestamp()), SetOptions.merge())
}

fun pairStatusFlow(): Flow<String?> = callbackFlow {
    val registration = pairingDoc.addSnapshotListener { snapshot, _ -> trySend(snapshot?.getString("status")) }
    awaitClose { registration.remove() }
}

fun markPaired() {
    pairingDoc.set(
        mapOf("pairId" to PAIR_ID, "status" to "paired", "pairedAt" to FieldValue.serverTimestamp()),
        SetOptions.merge()
    )
}
