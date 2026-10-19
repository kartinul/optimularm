//
//  AppDelegate.swift
//  optimularm
//

import AppKit
import FirebaseCore
import ServiceManagement
import SwiftUI
import os

class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    let state = AppState()
    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
    private let popover = NSPopover()
    private var settingsWindow: NSWindow?

    func applicationDidFinishLaunching(_ notification: Notification) {
        FirebaseApp.configure()

        // Hide from Dock and Cmd+Tab by default — menu bar only
        NSApp.setActivationPolicy(.accessory)

        popover.behavior = .transient
        popover.contentViewController = NSHostingController(
            rootView: MenuBarView(openSettings: showSettings).environment(state)
        )
        statusItem.button?.action = #selector(statusItemClicked)
        statusItem.button?.target = self
        statusItem.button?.sendAction(on: [.leftMouseUp, .rightMouseUp])
        updateIcon()

        #if !DEBUG
        enableLaunchAtLoginOnce()
        #endif

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

    /** Left click opens the popover, right click flips the alarm on/off. */
    @objc private func statusItemClicked() {
        if NSApp.currentEvent?.type == .rightMouseUp {
            state.toggleAlarm()
        } else if popover.isShown {
            popover.close()
        } else if let button = statusItem.button {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            popover.contentViewController?.view.window?.makeKey()
        }
    }

    /** Re-runs whenever the state it reads changes (Observation framework). */
    private func updateIcon() {
        withObservationTracking {
            let on = state.paired && state.settings.alarmOn
            let image = NSImage(systemSymbolName: on ? "alarm.fill" : "alarm", accessibilityDescription: "optimularm")
            image?.isTemplate = true
            statusItem.button?.image = image
            statusItem.button?.appearsDisabled = !on
        } onChange: {
            DispatchQueue.main.async { self.updateIcon() }
        }
    }

    private func showSettings() {
        popover.close()
        if settingsWindow == nil {
            let window = NSWindow(contentViewController: NSHostingController(rootView: SettingsView().environment(state)))
            window.title = "optimularm Settings"
            window.styleMask = [.titled, .closable]
            window.isReleasedWhenClosed = false
            window.delegate = self
            window.center()
            settingsWindow = window
        }
        NSApp.setActivationPolicy(.regular)
        NSApp.activate()
        settingsWindow?.makeKeyAndOrderFront(nil)
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
    }

    private func enableLaunchAtLoginOnce() {
        guard !UserDefaults.standard.bool(forKey: "launchAtLoginSet") else { return }
        try? SMAppService.mainApp.register()
        UserDefaults.standard.set(true, forKey: "launchAtLoginSet")
    }
}
