package io.github.kartinul.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

private const val QR_PREFIX = "optimularm:"
private lateinit var prefs: SharedPreferences

fun initPairing(context: Context) {
    prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)
}

/** The Mac this phone is paired with, or null. */
var pairId: String?
    get() = prefs.getString("pairId", null)
    private set(value) = prefs.edit { putString("pairId", value) }

internal fun pairingDoc(id: String) = Firebase.firestore.collection("pairings").document(id)

suspend fun signIn(): String =
    Firebase.auth.currentUser?.uid ?: Firebase.auth.signInAnonymously().await().user!!.uid

/** Joins the pending pairing the Mac's QR points at. Returns false for a QR that isn't ours. */
suspend fun joinPairing(qr: String): Boolean {
    if (!qr.startsWith(QR_PREFIX)) return false
    val id = qr.removePrefix(QR_PREFIX)
    pairingDoc(id).update(
        mapOf("androidDeviceId" to signIn(), "status" to "paired", "pairedAt" to FieldValue.serverTimestamp())
    ).await()
    pairId = id
    return true
}

suspend fun unpair() {
    val id = pairId ?: return
    pairId = null
    pairingDoc(id).update(mapOf("status" to "pending", "androidDeviceId" to FieldValue.delete())).await()
}
