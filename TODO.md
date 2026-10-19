# TODO

## Flow

1. On the Mac you set a **wake time** (e.g. 08:00). It goes to `settings/{pairId}`.
2. Android listens to it. As soon as it changes, Android **must**:
   - set its own **default alarm** at the wake time
   - schedule an internal check at **wake − 90 min** (`AlarmManager.setExactAndAllowWhileIdle`)
3. When the Mac display sleeps, the Mac writes a **sleep event that holds only `sleepTime`**.
4. At the check (wake − 90), Android reads the latest sleep event:
   - one arrived → calculate the alarm (from `sleepTime + 10min`, step in 90-min cycles, pick the last boundary at or before the wake time) and move the alarm there
   - none arrived → do nothing, the default alarm rings
5. **Late sleep** (the Mac sleeps after wake − 90): wake the user 30 min after `sleepTime`.

### Open questions

- [x] How old can the "latest" sleep event be before it doesn't count? → only events after `settings.sleepCutoff` (default 12:00, set with the bedtime handle of the Clock dial)
- [x] Late sleep: if `sleepTime + 30min` is after the wake time, skip it and let the default alarm ring? → yes, ring at the wake time
- [x] Does sleeping before midnight still mean "alarm = wake time exactly", or does the cycle maths apply every time? → cycle maths every time

The schema is in `CLAUDE.md`.

## macOS — fix what exists

- [x] Drop `isPostMidnight` from `SleepEvent`
- [x] Shrink `SleepEvent` to `sleepTime` only: remove `pairId`, `wakeByCeiling`, `status`, `wokeAt`, `actualWakeTime`
- [x] Write sleep events to `pairings/{pairId}/sleepEvents` (still `"gaga"` until pairing exists)
- [x] Delete `markLatestEventInterrupted()`, the `didWakeNotification` observer and the "Mark Interrupted" test button
- [x] Delete `Helpers.swift` (`isPastMidnight()` is unused)
- [x] Logger subsystem mismatch: `optimularmApp.swift` uses `com.kartik.optimularm`, `Logging.swift` uses `com.kartinul.optimularm`
- [x] `scheduleNextMidnightCheck()` is never called and the icon update is still a TODO. Keep it or delete it?
- [x] Hide the test buttons behind `#if DEBUG`
- [x] Remove the unused template `ContentView.swift`

## macOS — build

- [x] Settings window:
  - [x] live listener on `settings/{pairId}` (same doc the phone's Clock dial writes; last write wins)
  - [x] wake time, bedtime/sleep cutoff and alarm on/off pickers → `settings/{pairId}` (`"HH:mm"` strings, `alarmOn` bool, merge writes)
  - [x] launch at login (`SMAppService.mainApp`)
  - [x] pairing status and a Re-pair button
  - [x] switch to `.regular` on open and back to `.accessory` on close
- [x] Anonymous Auth sign-in on launch
- [x] Pairing:
  - [x] generate a `pairId`
  - [x] write `pairings/{pairId}` with `status: "pending"` and `macDeviceId`
  - [x] show a QR code (`CIFilter.qrCodeGenerator()`)
  - [x] listen for `status: "paired"`
  - [x] persist `pairId` in `UserDefaults`
- [x] Split files: `AppDelegate.swift`, `AppState.swift`, `SleepWatcher.swift`, `Data/*Repository.swift`, `UI/MenuBarView.swift`, `UI/SettingsView.swift`, `UI/PairingView.swift`
- [x] Menu dropdown: wake time, alarm on/off and the last sleep time (from the `settings` listener)

## Android

- [x] Own alarm works on the S23: `setAlarmClock` → `AlarmReceiver` → full-screen notification → `RingActivity` (sound + Stop), and it cancels silently. Samsung Clock was dropped because it can't remove alarms silently.
- [x] `CycleCalculator`: a pure function plus unit tests
  - [x] normal case (pick the last boundary ≤ wake time)
  - [x] no full cycle fits → wake time
  - [x] late sleep → `sleepTime + 30min`
  - [x] sleep before midnight, wake time next day
- [x] Listen to `settings/{pairId}` (`data/WakeSettings.kt`) and set the default alarm on change (`MainActivity`, only while the app is open)
- [x] Schedule the check at wake − 90 on every settings change
- [x] Reschedule tomorrow's alarm after it rings (`AlarmReceiver`), not only when the app is opened
- [x] `CheckReceiver` (the check): read the latest sleep event → `CycleCalculator` (ignore events before `sleepCutoff`) → move the alarm with `scheduleAlarm`
- [x] Late-sleep path: a listener on `sleepEvents` that handles events arriving after wake − 90
- [x] Gradle:
  - [x] add WorkManager, Google code scanner (no CameraX, no CAMERA permission) (tabs and icons need no extra deps)
  - [x] remove Crashlytics
  - [x] remove Analytics if not wanted
- [x] `.gitignore`: `android/build/` and `android/.gradle/` show up as untracked
- [x] Manifest:
  - [x] remove the duplicate `POST_NOTIFICATIONS`
  - [x] register `CheckReceiver`, `BootReceiver`
- [x] `OptimularmApplication`: Firebase init and Anonymous Auth
- [x] Onboarding checklist with deep links: notifications, full-screen, exact alarms, battery unrestricted (Settings tab → Permissions)
- [x] `PairingScreen`:
  - [x] scan the QR
  - [x] write `androidDeviceId` and `status: "paired"` (no FCM)
  - [x] store `pairId` (SharedPreferences, no DataStore dep)
- [x] Runs on the S23; phone writes reach Firestore (checked `settings/gaga` and `pairings/gaga`)
- [x] UI: Clock tab (24h dial: bedtime handle = sleep cutoff, alarm handle = wake time; on/off; real ring time) and Settings tab (pair button), live-synced with `settings/{pairId}`
- [x] Ring screen: centered Wake up + Stop
- [x] Clock tab: last sleep time, permission health
- [x] Clock tab: the "in 14 hours" text only refreshes on settings change; tick it every minute
- [x] Settings tab: Unpair button (today Pair disables itself and you can't undo it). Unpair = set `status: "pending"` / clear the pair; once real pairing exists, also forget the stored `pairId`
- [x] Default bedtime 12:00 draws a 20 h arc on first launch → defaults are now 00:00 → 07:00
- [x] `BootReceiver`: reschedule the check from `settings`
- [x] WorkManager 15-minute fallback poll
- [x] Own alarm, finish it: snooze, alarm volume/vibration, reschedule after reboot, Samsung battery "sleeping apps" exemption
- [x] Rating: 1–5 on the ring screen after Stop → `ratings`

## Firebase

- [x] Replace the open rules with `pairId`-scoped rules: only the pair's two uids can read/write (both may write `settings`)
- [x] Index on `pairings/{pairId}/sleepEvents` by `sleepTime` desc (a single-field index, created automatically)
- [x] Create `/shared/firestore-schema.md` and `/shared/firestore.rules`

## Android — alarm

- [x] `RingService` foreground service plays the alarm (ringtone, volume, gradual, vibrate, auto-silence after 10 min)
- [x] Alarm settings: sound, volume, gradual, vibrate, snooze length, snoozes allowed, test ring

## Later

- [ ] Notification with a manual "cancel alarm" action
- [ ] Rating tags and notes
- [ ] Stats / history view
