# Firestore schema

```jsonc
// pairings/{pairId}                        — Mac creates (pending), phone joins (paired)
{ "pairId", "macDeviceId": "<anon uid>", "androidDeviceId": "<anon uid>",
  "status": "pending|paired", "createdAt", "pairedAt" }

// settings/{pairId}                        — both read and write, live, last write wins
{ "wakeTime": "07:00", "alarmOn": true, "sleepCutoff": "00:00", "lastUpdated" }

// pairings/{pairId}/sleepEvents/{eventId}  — Mac writes on display sleep
{ "sleepTime" }

// ratings/{ratingId}                       — phone writes after the alarm
{ "pairId", "eventId", "rating": 1-5, "timestamp" }
```

The QR on the Mac encodes `optimularm:<pairId>`.
Rules: `firestore.rules`. Only the pair's two uids can read or write. Deploy with `firebase deploy --only firestore:rules`.
