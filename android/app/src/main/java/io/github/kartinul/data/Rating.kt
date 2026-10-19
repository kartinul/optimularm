package io.github.kartinul.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

fun saveRating(rating: Int, eventId: String?) {
    val id = pairId ?: return
    Firebase.firestore.collection("ratings").add(
        mapOf("pairId" to id, "eventId" to eventId, "rating" to rating, "timestamp" to FieldValue.serverTimestamp())
    )
}
