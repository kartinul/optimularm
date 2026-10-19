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
- [ ] Late sleep: if `sleepTime + 30min` is after the wake time, skip it and let the default alarm ring?
- [ ] Does sleeping before midnight still mean "alarm = wake time exactly", or does the cycle maths apply every time?

The schema is in `CLAUDE.md`.

## macOS — fix what exists

- [x] Drop `isPostMidnight` from `SleepEvent`
- [ ] Shrink `SleepEvent` to `sleepTime` only: remove `pairId`, `wakeByCeiling`, `status`, `wokeAt`, `actualWakeTime`
- [ ] Write sleep events to `pairings/{pairId}/sleepEvents` (still `"gaga"` until pairing exists)
- [ ] Delete `markLatestEventInterrupted()`, the `didWakeNotification` observer and the "Mark Interrupted" test button
- [ ] Delete `Helpers.swift` (`isPastMidnight()` is unused)
- [ ] Logger subsystem mismatch: `optimularmApp.swift` uses `com.kartik.optimularm`, `Logging.swift` uses `com.kartinul.optimularm`
- [ ] `scheduleNextMidnightCheck()` is never called and the icon update is still a TODO. Keep it or delete it?
- [ ] Hide the test buttons behind `#if DEBUG`
- [ ] Remove the unused template `ContentView.swift`

## macOS — build

- [ ] Settings window:
  - [ ] live listener on `settings/{pairId}` (same doc the phone's Clock dial writes; last write wins)
  - [ ] wake time, bedtime/sleep cutoff and alarm on/off pickers → `settings/{pairId}` (`"HH:mm"` strings, `alarmOn` bool, merge writes)
  - [ ] launch at login (`SMAppService.mainApp`)
  - [ ] pairing status and a Re-pair button
  - [ ] switch to `.regular` on open and back to `.accessory` on close
- [ ] Anonymous Auth sign-in on launch
- [ ] Pairing:
  - [ ] generate a `pairId`
  - [ ] write `pairings/{pairId}` with `status: "pending"` and `macDeviceId`
  - [ ] show a QR code (`CIFilter.qrCodeGenerator()`)
  - [ ] listen for `status: "paired"`
  - [ ] persist `pairId` in `UserDefaults`
- [ ] Split files: `AppDelegate.swift`, `AppState.swift`, `SleepWatcher.swift`, `Data/*Repository.swift`, `UI/MenuBarView.swift`, `UI/SettingsView.swift`, `UI/PairingView.swift`
- [ ] Menu dropdown: wake time, alarm on/off and the last sleep time (from the `settings` listener)

## Android

- [x] Own alarm works on the S23: `setAlarmClock` → `AlarmReceiver` → full-screen notification → `RingActivity` (sound + Stop), and it cancels silently. Samsung Clock was dropped because it can't remove alarms silently.
- [ ] `CycleCalculator`: a pure function plus unit tests
  - [ ] normal case (pick the last boundary ≤ wake time)
  - [ ] no full cycle fits → wake time
  - [ ] late sleep → `sleepTime + 30min`
  - [ ] sleep before midnight, wake time next day
- [x] Listen to `settings/{pairId}` (`data/WakeSettings.kt`) and set the default alarm on change (`MainActivity`, only while the app is open)
- [ ] Schedule the check at wake − 90 on every settings change
- [ ] Reschedule tomorrow's alarm after it rings (`AlarmReceiver`), not only when the app is opened
- [ ] `CheckReceiver` (the check): read the latest sleep event → `CycleCalculator` (ignore events before `sleepCutoff`) → move the alarm with `scheduleAlarm`
- [ ] Late-sleep path: a listener on `sleepEvents` that handles events arriving after wake − 90
- [ ] Gradle:
  - [ ] add WorkManager, CameraX + ML Kit barcode (tabs and icons need no extra deps)
  - [ ] remove Crashlytics
  - [ ] remove Analytics if not wanted
- [ ] `.gitignore`: `android/build/` and `android/.gradle/` show up as untracked
- [ ] Manifest:
  - [ ] remove the duplicate `POST_NOTIFICATIONS`
  - [ ] add `CAMERA`
  - [ ] register `CheckReceiver`, `BootReceiver`, `OptimularmMessagingService`
- [ ] `OptimularmApplication`: Firebase init and Anonymous Auth
- [ ] Onboarding checklist with deep links: notifications, exact alarms, battery unrestricted, camera
- [ ] `PairingScreen`:
  - [ ] scan the QR
  - [ ] write `androidDeviceId`, the FCM token and `status: "paired"`
  - [ ] store `pairId` in DataStore
- [x] Runs on the S23; phone writes reach Firestore (checked `settings/gaga` and `pairings/gaga`)
- [x] UI: Clock tab (24h dial: bedtime handle = sleep cutoff, alarm handle = wake time; on/off; real ring time) and Settings tab (pair button), live-synced with `settings/{pairId}`
- [x] Ring screen: centered Wake up + Stop
- [ ] Clock tab: last sleep time, permission health
- [ ] Clock tab: the "in 14 hours" text only refreshes on settings change; tick it every minute
- [ ] Settings tab: Unpair button (today Pair disables itself and you can't undo it). Unpair = set `status: "pending"` / clear the pair; once real pairing exists, also forget the stored `pairId`
- [ ] Default bedtime 12:00 draws a 20 h arc on first launch; consider 22:00
- [ ] `BootReceiver`: reschedule the check from `settings`
- [ ] WorkManager 15-minute fallback poll
- [ ] `OptimularmMessagingService`: triggers the same sync (backup path)
- [ ] Own alarm, finish it: snooze, alarm volume/vibration, reschedule after reboot, Samsung battery "sleeping apps" exemption
- [ ] `RatingScreen`: 1–5 rating after the alarm (notification → screen) → `ratings`

## Firebase

- [ ] Replace the open rules with `pairId`-scoped rules: only the pair's two uids can read/write (both may write `settings`)
- [ ] Index on `pairings/{pairId}/sleepEvents` by `sleepTime` desc (a single-field index, created automatically)
- [ ] Create `/shared/firestore-schema.md` and `/shared/firestore.rules`

## Later

- [ ] Adaptive learning from `ratings` (bias which 90-min boundary gets picked; stays on Android)
- [ ] Real sleep-stage sensing (accelerometer / mic)
- [ ] Notification with a manual "cancel alarm" action
- [ ] Rating tags and notes
- [ ] Stats / history view
- [ ] Foreground service, only if alarms prove unreliable
