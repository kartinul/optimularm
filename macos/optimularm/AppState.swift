//
//  AppState.swift
//  optimularm
//

import FirebaseFirestore
import Foundation
import os

@Observable
final class AppState {
    // Symbol name for the menu bar icon; update this from anywhere to reflect in the menu
    var menuBarSystemImage: String = "timer.circle.fill"
    // Optional title if you want to change the label text too
    var menuBarTitle: String = "optimularm"

    var pairId: String? = UserDefaults.standard.string(forKey: "pairId") {
        didSet {
            UserDefaults.standard.set(pairId, forKey: "pairId")
            listen()
        }
    }
    var paired = false
    var settings = WakeSettings()
    var lastSleep: Date?

    private var listeners: [ListenerRegistration] = []

    /** Restarts the live listeners for the current pairId. */
    func listen() {
        listeners.forEach { $0.remove() }
        listeners = []
        paired = false
        lastSleep = nil
        guard let pairId else { return }
        listeners = [
            listenPairStatus(pairId) { [weak self] in self?.paired = $0 == "paired" },
            listenSettings(pairId) { [weak self] in self?.settings = $0 },
            listenLatestSleep(pairId) { [weak self] in self?.lastSleep = $0 },
        ]
    }

    func repair() {
        Task { @MainActor in
            do {
                pairId = try await createPairing()
            } catch {
                logger.error("Pairing failed: \(error.localizedDescription)")
            }
        }
    }
}
