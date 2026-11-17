//
//  AppState.swift
//  optimularm
//

import Foundation
import os

@Observable
final class AppState {
    var pairId: String? = UserDefaults.standard.string(forKey: "pairId") {
        didSet {
            UserDefaults.standard.set(pairId, forKey: "pairId")
            pairing = Pairing()
            lastSleep = nil
            refresh()
        }
    }
    var pairing = Pairing()
    var paired: Bool { pairing.paired }
    var settings = WakeSettings()
    var lastSleep: Date?
    var error: String?
    /** A panel or window is showing, so poll fast enough to see the phone pair or change settings. */
    var visible = false {
        didSet { if visible != oldValue { startPolling() } }
    }

    private var timer: Timer?
    private var savedAt = Date.distantPast
    /** A display-sleep time not written yet; kept until a write lands. */
    private var unsentSleep: Date?

    /** shortcut: polling instead of live listeners (REST has none); ~1.5k reads/day idle, 3 s while a window is open. */
    func startPolling() {
        timer?.invalidate()
        timer = .scheduledTimer(withTimeInterval: visible ? 3 : 60, repeats: true) { [weak self] _ in self?.refresh() }
        refresh()
    }

    func refresh() {
        guard let pairId else { return }
        let started = Date.now
        Task {
            do {
                let pairing = try await fetchPairing(pairId)
                guard pairId == self.pairId else { return }
                if pairing.abandoned { return repair() }
                let settings = try await fetchSettings(pairId)
                let lastSleep = try await fetchLatestSleep(pairId)
                guard pairId == self.pairId else { return }
                self.pairing = pairing
                // a poll that started before a local save would undo it
                if started > savedAt { self.settings = settings }
                self.lastSleep = lastSleep
                error = nil
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    /** Applies [change] at once and saves it; the phone picks it up on its next sync. */
    func update(_ change: (inout WakeSettings) -> Void) {
        guard let pairId, paired else { return }
        change(&settings)
        let settings = settings
        savedAt = .distantFuture
        Task {
            do {
                try await saveSettings(pairId, settings)
            } catch {
                self.error = error.localizedDescription
            }
            savedAt = .now
        }
    }

    func toggleAlarm() {
        update { $0.alarmOn.toggle() }
    }

    func repair() {
        error = nil
        Task {
            do {
                pairId = try await createPairing()
            } catch {
                self.error = error.localizedDescription
                logger.error("Pairing failed: \(error.localizedDescription)")
            }
        }
    }

    func recordSleep() {
        unsentSleep = .now
        Task { await sendSleep() }
    }

    func sendSleep() async {
        guard let pairId, let sleep = unsentSleep else { return }
        do {
            try await writeSleepEvent(pairId, sleep)
            if unsentSleep == sleep { unsentSleep = nil }
            lastSleep = sleep
            logger.info("Sleep event written")
        } catch {
            logger.error("Sleep event failed: \(error.localizedDescription)")
        }
    }
}
