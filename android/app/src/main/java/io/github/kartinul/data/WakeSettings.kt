package io.github.kartinul.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// shortcut: hardcoded until QR pairing exists
const val PAIR_ID = "gaga"

data class WakeSettings(
    val wakeTime: String = "08:00",
    val alarmOn: Boolean = true,
    val sleepCutoff: String = "12:00",
)

private val settingsDoc get() = Firebase.firestore.collection("settings").document(PAIR_ID)
private val pairingDoc get() = Firebase.firestore.collection("pairings").document(PAIR_ID)

fun wakeSettingsFlow(): Flow<WakeSettings> = callbackFlow {
    val registration = settingsDoc.addSnapshotListener { snapshot, _ ->
        if (snapshot == null) return@addSnapshotListener
        val defaults = WakeSettings()
        trySend(
            WakeSettings(
                wakeTime = snapshot.getString("wakeTime") ?: defaults.wakeTime,
                alarmOn = snapshot.getBoolean("alarmOn") ?: defaults.alarmOn,
                sleepCutoff = snapshot.getString("sleepCutoff") ?: defaults.sleepCutoff,
            )
        )
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
