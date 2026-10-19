# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## How to work here: no vibe coding

The user is learning Swift and Kotlin and wrote the Mac code by hand. The goal is for them to understand every line, not just to have working code.

- Make small, single-purpose changes, one TODO item at a time. Do not do sweeping rewrites or build several features in one go.
- Before editing, say what you will change and why. After editing, explain what the new code does.
- When you introduce a new concept (a property wrapper, a coroutine, `AlarmManager`, Firestore listeners, etc.), explain it in a few plain sentences.
- Keep the user's existing files, naming and structure unless the task is to change them.
- Where it helps learning, offer the user the chance to write a piece themselves: describe the approach and let them try.
- Ask before changing anything listed under "Locked decisions".
- Tick items in `TODO.md` as they are finished.

## What this is

optimularm wakes you at the optimal point in your sleep cycle before a set wake-up time. Two apps share one Firebase project:

- `macos/`: a SwiftUI menu-bar app that detects display sleep and writes a `sleepEvents` doc to Firestore. It is also the only place `wakeByCeiling` is set.
- `android/`: a Jetpack Compose app for the user's Samsung phone. It listens for sleep events, calculates the wake time and sets a real alarm in Samsung Clock. It is still the template scaffold.

`TODO.md` lists the current state and the plan.

## Commands

Android (run from `android/`):
```sh
./gradlew assembleDebug
./gradlew installDebug
./gradlew test                                   # JVM unit tests
./gradlew connectedAndroidTest                   # instrumented, needs device/emulator
./gradlew test --tests io.github.kartinul.ExampleUnitTest
```

macOS (scheme `optimularm`, latest macOS only, Firebase via SwiftPM):
```sh
xcodebuild -project macos/optimularm.xcodeproj -scheme optimularm build
```

## Locked decisions

- No Cloud Functions. Both apps use Firestore directly with live listeners. All calculation happens on Android.
- Sleep is display-based: `NSWorkspace.screensDidSleepNotification` counts as "user went to sleep", and `screensDidWakeNotification` counts as waking. The Mac writes the event immediately, because timers may not run once the system sleeps.
- Assume the user falls asleep 10 minutes after the display sleeps.
- Wake time:
  - **Adaptive** (sleep after midnight): from `sleepTime + 10min`, step in 90-minute cycles and pick the last boundary at or before `wakeByCeiling`.
  - **Ceiling** (sleep before midnight): the alarm is exactly `wakeByCeiling`.
- Samsung Clock has no public cancel API, so Android sets an internal `AlarmManager.setExactAndAllowWhileIdle` alarm at wake − 90 min. When it fires, it re-checks the event:
  - `active` → `AlarmClock.ACTION_SET_ALARM`, event → `confirmed`, write `alarmState`.
  - `interrupted` → do nothing.
- If the Mac wakes before T−90, it marks the latest active event `interrupted` and Android cancels the internal alarm. A new sleep creates a new event.
- Reliability layers: the Firestore listener (primary), a battery-optimization exemption, `SCHEDULE_EXACT_ALARM`, a `BootReceiver`, and a WorkManager 15-minute fallback poll. No foreground service yet.
- The Mac app is menu-bar only (`.accessory`). It switches to `.regular` only while Settings is open, and uses `SMAppService` for launch at login.
- Auth is Firebase Anonymous Auth on both devices. The devices are linked by a `pairId` via QR: the Mac shows the code and the phone scans it.
- Ratings (1–5) always sync to Firestore.
- Namespaces: Android `io.github.kartinul`, macOS bundle `io.github.kartinul.optimularm`, logger subsystem `com.kartinul.optimularm`.

## Firestore schema (target)

```jsonc
// pairings/{pairId}
{ "pairId", "macDeviceId": "<anon uid>", "androidDeviceId": "<anon uid>",
  "androidFcmToken", "status": "pending|paired", "createdAt", "pairedAt" }

// settings/{pairId}        — Mac writes only
{ "pairId", "wakeByCeiling": "08:00", "lastUpdated" }

// sleepEvents/{eventId}    — Mac creates / marks interrupted, Android marks confirmed
{ "pairId", "sleepTime", "isPostMidnight", "wakeByCeiling": "HH:mm",
  "status": "active|interrupted|confirmed|completed", "wokeAt", "actualWakeTime" }

// alarmState/{pairId}      — Android writes, Mac displays
{ "pairId", "sourceEventId", "alarmTime": "06:42", "mode": "adaptive|ceiling",
  "status": "scheduled|set|cancelled", "lastUpdated" }

// ratings/{ratingId}       — Android writes
{ "pairId", "eventId", "rating": 1-5, "timestamp" }
```

The Swift model is `SleepEvent` in `macos/optimularm/SleepEvent.swift`. If you change a shape, update both apps (and `/shared` once it exists).

## Current macOS code

`AppDelegate` (in `optimularmApp.swift`) configures Firebase and observes sleep and wake, calling `writeSleepEvent()` and `markLatestEventInterrupted()`. `AppState` (`@Observable`) holds the menu-bar icon and title. Log through the shared `logger` in `Logging.swift`. Firestore rules are currently wide open.

The Firebase config files are committed: `android/app/google-services.json` and `macos/optimularm/GoogleService-Info.plist`.
