# TODO

The locked decisions and the Firestore schema are in `CLAUDE.md`.

## macOS — fix what exists

- [ ] Wake observer uses `didWakeNotification` (system wake), but sleep is display-based → switch to `screensDidWakeNotification` so a display-only wake also interrupts
- [ ] `pairId` is hardcoded `"gaga"` → use the real paired id
- [ ] `isPostMidnight` is hardcoded `true` → compute it from `sleepTime` (`isPastMidnight()` exists but is unused; the hour < 12 cutoff still needs confirming)
- [ ] `wakeByCeiling` is hardcoded `"08:00"` → read it from `settings/{pairId}`
- [ ] `markLatestEventInterrupted()`: only interrupt the latest `active` event for this `pairId`, and set `wokeAt`
- [ ] Logger subsystem mismatch: `optimularmApp.swift` uses `com.kartik.optimularm`, `Logging.swift` uses `com.kartinul.optimularm`
- [ ] `scheduleNextMidnightCheck()` is never called; the icon update is still a TODO; re-sync it on wake
- [ ] Hide the test buttons behind `#if DEBUG`
- [ ] Remove the unused template `ContentView.swift`

## macOS — build

- [ ] Split files: `AppDelegate.swift`, `AppState.swift`, `SleepWatcher.swift`, `MidnightScheduler.swift`, `Data/*Repository.swift`, `UI/MenuBarView.swift`, `UI/SettingsView.swift`, `UI/PairingView.swift`
- [ ] `AppState` owned by `AppDelegate` and observed by the `App`; holds the icon and the alarm display
- [ ] Anonymous Auth sign-in on launch
- [ ] Pairing:
  - [ ] generate a `pairId`
  - [ ] write `pairings/{pairId}` with `status: "pending"` and `macDeviceId`
  - [ ] show a QR code (`CIFilter.qrCodeGenerator()`)
  - [ ] listen for `status: "paired"`
  - [ ] persist `pairId` in `UserDefaults`
- [ ] Settings window:
  - [ ] `wakeByCeiling` time picker → `settings/{pairId}`
  - [ ] launch at login (`SMAppService.mainApp`)
  - [ ] pairing status and a Re-pair button
  - [ ] switch to `.regular` on open and back to `.accessory` on close
- [ ] Menu dropdown: live listener on `alarmState/{pairId}`. Show "No alarm yet" / "Alarm: 06:42 (adaptive)" and the last sleep time

## Android

- [ ] Gradle:
  - [ ] add Navigation-compose, Firebase Auth/Firestore/Messaging, WorkManager, CameraX + ML Kit barcode, Accompanist permissions, Material icons extended
  - [ ] remove Crashlytics
  - [ ] remove Analytics if not wanted
- [ ] Manifest:
  - [ ] remove the duplicate permissions
  - [ ] add `USE_EXACT_ALARM` and `CAMERA`
  - [ ] register `AlarmReceiver`, `BootReceiver`, `OptimularmMessagingService`
- [ ] `OptimularmApplication`: Firebase init and Anonymous Auth
- [ ] Onboarding checklist with deep links: notifications, exact alarms, battery unrestricted, camera
- [ ] `PairingScreen`:
  - [ ] scan the QR
  - [ ] write `androidDeviceId`, the FCM token and `status: "paired"`
  - [ ] store `pairId` in DataStore
- [ ] `HomeScreen`: paired status, last sync, permission health, current `alarmState`, last sleep event (replace the empty `ui/HomeScreen.java` with Kotlin)
- [ ] `SleepEventRepository`: live listener on the latest `sleepEvents` for this `pairId`
- [ ] `CycleCalculator`: a pure function plus unit tests
  - [ ] ceiling before the first full cycle
  - [ ] ceiling on the next day
  - [ ] pre-midnight sleep
- [ ] `AlarmScheduler`:
  - [ ] new active event → schedule the internal exact alarm at wake − 90 min
  - [ ] `interrupted` → cancel it
  - [ ] write `alarmState` with `status: "scheduled"`
- [ ] `AlarmReceiver`:
  - [ ] re-check the event status
  - [ ] `ACTION_SET_ALARM` with `EXTRA_SKIP_UI`
  - [ ] event → `confirmed`, `alarmState` → `set`
- [ ] `BootReceiver`: reschedule a pending internal alarm from Firestore
- [ ] WorkManager 15-minute fallback poll
- [ ] `OptimularmMessagingService`: triggers the same sync (backup path)
- [ ] `RatingScreen`: 1–5 rating after the alarm (notification → screen) → `ratings`
- [ ] **On the real Samsung:** does `EXTRA_SKIP_UI` set the alarm silently, or does Samsung Clock open?

## Firebase

- [ ] Replace the open rules with `pairId`-scoped rules: only the pair's two uids can read/write, and only the Mac uid can write `settings`
- [ ] Composite index on `sleepEvents`: `pairId` + `sleepTime` desc (+ `status` if filtered)
- [ ] Create `/shared/firestore-schema.md` and `/shared/firestore.rules`

## Later

- [ ] Adaptive learning from `ratings` (bias which 90-min boundary gets picked; stays on Android)
- [ ] Move learning to Cloud Functions if it gets heavy
- [ ] Real sleep-stage sensing (accelerometer / mic)
- [ ] Notification with a manual "cancel alarm" action
- [ ] Rating tags and notes
- [ ] Stats / history view
- [ ] Foreground service, only if alarms prove unreliable
