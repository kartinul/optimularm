# Firestore schema

```jsonc
// pairings/{pairId}                        — Mac creates (pending), phone joins (paired)
{ "pairId", "macDeviceId": "<anon uid>", "androidDeviceId": "<anon uid>",
  "status": "pending|paired", "createdAt", "pairedAt",
  "ringAt" }                                // phone writes its next computed alarm, the Mac shows it

// settings/{pairId}                        — both read and write, live, last write wins
{ "wakeTime": "07:00", "weekendWakeTime": "09:30" /* optional, Sat & Sun */, "alarmOn": true,
  "phoneSleep": false, "autoTune": false,   // opt-in, toggled from either app
  "sleepCutoff": "00:00", "lastUpdated" }

// pairings/{pairId}/sleepEvents/{eventId}  — Mac writes on display sleep; eventId = sleepTime in epoch seconds
{ "sleepTime" }

// ratings/{ratingId}                       — phone writes after the alarm
{ "pairId", "eventId", "rating": 1-5, "timestamp" }
```

A pairing back at `pending` with a `pairedAt` means the phone left: the Mac drops it and shows a fresh QR.
The Mac talks to Firebase over REST (no SDK), so it runs unsigned; it polls instead of listening.

The QR on the Mac encodes `optimularm:<pairId>`.
Rules: `firestore.rules`. Only the pair's two uids can read or write. Deploy with `firebase deploy --only firestore:rules`.
