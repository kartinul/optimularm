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

- `macos/`: a SwiftUI menu-bar app that detects display sleep and writes a sleep event to Firestore. It is also the only place the wake time is set.
- `android/`: a Jetpack Compose app for the user's Samsung phone. It listens for the wake time and sleep events, and rings its own alarm. It is still a tester screen.

`TODO.md` lists the current state and the plan.

## Commands

From the repo root: `make install` (Release Mac app → /Applications + phone), `make test`, `make rules`, `make logs`.

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
- Sleep is display-based: `NSWorkspace.screensDidSleepNotification` counts as "user went to sleep". The Mac writes the event immediately, because timers may not run once the system sleeps.
- Assume the user falls asleep 10 minutes after the display sleeps.
- The user sets the **wake time** on either device (`settings/{pairId}.wakeTime`). Both apps read and write `settings` with live listeners; last write wins.
- Only sleep events after `settings.sleepCutoff` (default `"12:00"`) count for tonight's alarm. On the phone it is the bedtime handle of the Clock dial.
- As soon as Android receives a wake time, it **must** set its own **default alarm** at that time (`AlarmManager.setAlarmClock` → `AlarmReceiver` → full-screen notification → `RingActivity`), and schedule an internal check with `AlarmManager.setExactAndAllowWhileIdle` at wake − 90 min.
- A sleep event holds only `sleepTime`. The Mac does nothing when the display wakes; sleeping again just writes a newer event.
- At the check, Android reads the latest sleep event:
  - one arrived → from `sleepTime + 10min`, step in 90-minute cycles, pick the last boundary at or before the wake time, and move the alarm there (cancel the default, set the new one).
  - none arrived → do nothing; the default alarm rings.
- **Late sleep** (the sleep event arrives after wake − 90): set the alarm at `sleepTime + 30min`.
- The app rings its own alarm, not Samsung Clock: Samsung Clock can't remove alarms silently (`ACTION_DISMISS_ALARM` opens its UI), while the app can cancel and move its own.
- Reliability layers: the Firestore listener (primary), a battery-optimization exemption, `SCHEDULE_EXACT_ALARM`, a `BootReceiver`, and a WorkManager 15-minute fallback poll. No foreground service yet.
- The Mac app is menu-bar only (`.accessory`). It switches to `.regular` only while Settings is open, and uses `SMAppService` for launch at login.
- No `alarmState` doc: only Android needs the real ring time, and it keeps that locally.
- Auth is Firebase Anonymous Auth on both devices. The devices are linked by a `pairId` via QR: the Mac shows the code and the phone scans it.
- Ratings (1–5) always sync to Firestore.
- Namespaces: Android `io.github.kartinul`, macOS bundle `io.github.kartinul.optimularm`, logger subsystem `com.kartinul.optimularm`.

## Firestore schema

Also in `shared/firestore-schema.md`; rules in `shared/firestore.rules`.

```jsonc
// pairings/{pairId}
{ "pairId", "macDeviceId": "<anon uid>", "androidDeviceId": "<anon uid>",
  "status": "pending|paired", "createdAt", "pairedAt" }

// settings/{pairId}                        — both read and write, live
{ "wakeTime": "08:00", "alarmOn": true, "sleepCutoff": "12:00", "lastUpdated" }

// pairings/{pairId}/sleepEvents/{eventId}  — Mac writes
{ "sleepTime" }

// ratings/{ratingId}       — Android writes
{ "pairId", "eventId", "rating": 1-5, "timestamp" }
```

The Swift model is `SleepEvent` in `macos/optimularm/SleepEvent.swift`. If you change a shape, update both apps (and `/shared` once it exists).

## Current macOS code

`AppDelegate` configures Firebase, signs in anonymously and owns the `NSStatusItem`: left click opens the `MenuBarView` popover, right click toggles the alarm. It also owns the settings window. `AppState` (`@Observable`) holds the pairing, settings and last sleep from live listeners. `SleepWatcher` writes a sleep event on display sleep. Firestore access lives in `Data/`. Log through the shared `logger` in `Logging.swift`. The app signs with the personal team because Firebase Auth needs a keychain access group.

The Firebase config files are committed: `android/app/google-services.json` and `macos/optimularm/GoogleService-Info.plist`.
