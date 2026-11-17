# TODO

## Flow

1. On the Mac you set a **wake time** (e.g. 08:00). It goes to `settings/{pairId}`.
2. Android listens to it. As soon as it changes, Android **must**:
   - set its own **default alarm** at the wake time
   - schedule an internal check at **wake − 90 min** (`AlarmManager.setExactAndAllowWhileIdle`)
3. When the Mac display sleeps, the Mac writes a **sleep event that holds only `sleepTime`**.
4. On every sync (new sleep event, the wake − 90 check, the 15-min worker), Android reads the latest sleep event:
   - one arrived → calculate the alarm (from `sleepTime + 10min`, step in 90-min cycles, pick the last boundary at or before the wake time) and move the alarm there
   - none arrived → do nothing, the default alarm rings
5. **Late sleep** (the Mac sleeps after wake − 90): wake the user 30 min after `sleepTime`.

The schema is in `CLAUDE.md`.

## Later

- [ ] Notification with a manual "cancel alarm" action
- [ ] Rating tags and notes
- [x] Stats / history view (Calculations tab, phone-local)
