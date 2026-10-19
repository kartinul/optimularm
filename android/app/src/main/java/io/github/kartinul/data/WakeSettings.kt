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

data class WakeSettings(
    val wakeTime: String = "07:00",
    val alarmOn: Boolean = true,
    val sleepCutoff: String = "00:00",
)

data class SleepEvent(val id: String, val sleepTime: Long)

private fun settingsDoc(id: String) = Firebase.firestore.collection("settings").document(id)
private fun latestSleepQuery(id: String) =
    pairingDoc(id).collection("sleepEvents").orderBy("sleepTime", Query.Direction.DESCENDING).limit(1)

private fun DocumentSnapshot.toWakeSettings() = WakeSettings().let { defaults ->
    WakeSettings(
        wakeTime = getString("wakeTime") ?: defaults.wakeTime,
        alarmOn = getBoolean("alarmOn") ?: defaults.alarmOn,
        sleepCutoff = getString("sleepCutoff") ?: defaults.sleepCutoff,
    )
}

private fun DocumentSnapshot.toSleepEvent() = getTimestamp("sleepTime")?.let { SleepEvent(id, it.toDate().time) }

fun wakeSettingsFlow(id: String): Flow<WakeSettings> = callbackFlow {
    val registration = settingsDoc(id).addSnapshotListener { snapshot, _ ->
        if (snapshot != null) trySend(snapshot.toWakeSettings())
    }
    awaitClose { registration.remove() }
}

suspend fun fetchWakeSettings(id: String): WakeSettings = settingsDoc(id).get().await().toWakeSettings()

/** Latest sleep event after [after] (millis), or null. */
suspend fun fetchLatestSleep(id: String, after: Long): SleepEvent? = latestSleepQuery(id)
    .whereGreaterThan("sleepTime", Timestamp(Date(after)))
    .get().await()
    .documents.firstOrNull()?.toSleepEvent()

fun latestSleepFlow(id: String): Flow<SleepEvent?> = callbackFlow {
    val registration = latestSleepQuery(id).addSnapshotListener { snapshot, _ ->
        if (snapshot != null) trySend(snapshot.documents.firstOrNull()?.toSleepEvent())
    }
    awaitClose { registration.remove() }
}

fun saveWakeSettings(vararg fields: Pair<String, Any?>) {
    val id = pairId ?: return
    settingsDoc(id).set(mapOf(*fields, "lastUpdated" to FieldValue.serverTimestamp()), SetOptions.merge())
}
