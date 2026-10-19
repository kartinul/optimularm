//
//  AppDelegate.swift
//  optimularm
//

import AppKit
import FirebaseCore
import os

class AppDelegate: NSObject, NSApplicationDelegate {
    let state = AppState()

    func applicationDidFinishLaunching(_ notification: Notification) {
        FirebaseApp.configure()

        // Hide from Dock and Cmd+Tab by default — menu bar only
        NSApp.setActivationPolicy(.accessory)

        watchDisplaySleep(state)
        Task { @MainActor in
            do {
                _ = try await signIn()
            } catch {
                logger.error("Sign-in failed: \(error.localizedDescription)")
            }
            // listeners start after sign-in so the security rules see our uid
            state.listen()
        }
    }
}
