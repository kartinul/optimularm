//
//  AppDelegate.swift
//  optimularm
//

import AppKit
import FirebaseCore
import ServiceManagement
import SwiftUI
import os

/** A borderless panel can't take keyboard focus by default; the time pickers need it. */
private final class MenuPanel: NSPanel {
    override var canBecomeKey: Bool { true }
}

class AppDelegate: NSObject, NSApplicationDelegate, NSWindowDelegate {
    let state = AppState()
    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
    private let panel = MenuPanel(
        contentRect: .zero, styleMask: [.borderless, .nonactivatingPanel], backing: .buffered, defer: true
    )
    private var outsideClickMonitor: Any?
    private var settingsWindow: NSWindow?

    func applicationDidFinishLaunching(_ notification: Notification) {
        FirebaseApp.configure()

        // Hide from Dock and Cmd+Tab by default — menu bar only
        NSApp.setActivationPolicy(.accessory)

        setUpPanel()
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

    /** Left click opens the panel, right click flips the alarm on/off. */
    @objc private func statusItemClicked() {
        if NSApp.currentEvent?.type == .rightMouseUp {
            state.toggleAlarm()
        } else if panel.isVisible {
            closePanel()
        } else {
            openPanel()
        }
    }

    private func setUpPanel() {
        // the system menu material with rounded corners, like Control Center
        let background = NSVisualEffectView()
        background.material = .menu
        background.state = .active
        background.maskImage = roundedMask(radius: 12)

        let content = NSHostingView(rootView: MenuBarView(close: closePanel, openSettings: showSettings).environment(state))
        content.translatesAutoresizingMaskIntoConstraints = false
        background.addSubview(content)
        NSLayoutConstraint.activate([
            content.leadingAnchor.constraint(equalTo: background.leadingAnchor),
            content.trailingAnchor.constraint(equalTo: background.trailingAnchor),
            content.topAnchor.constraint(equalTo: background.topAnchor),
            content.bottomAnchor.constraint(equalTo: background.bottomAnchor),
        ])

        panel.contentView = background
        panel.isOpaque = false
        panel.backgroundColor = .clear
        panel.hasShadow = true
        panel.level = .statusBar
        panel.animationBehavior = .none
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
    }

    private func openPanel() {
        guard let button = statusItem.button, let buttonFrame = button.window?.convertToScreen(button.frame) else { return }
        let size = panel.contentView?.fittingSize ?? .zero
        let screen = button.window?.screen?.visibleFrame ?? .zero
        let x = min(buttonFrame.midX - size.width / 2, screen.maxX - size.width - 8)
        panel.setFrame(NSRect(x: x, y: buttonFrame.minY - size.height - 6, width: size.width, height: size.height), display: true)
        panel.makeKeyAndOrderFront(nil)
        panel.invalidateShadow() // shadow follows the rounded mask, not the square frame
        button.highlight(true)

        // any click in another app closes the panel, like a real menu
        outsideClickMonitor = NSEvent.addGlobalMonitorForEvents(matching: [.leftMouseDown, .rightMouseDown]) { [weak self] _ in
            self?.closePanel()
        }
    }

    /** A stretchable rounded rect: clips the blur material and shapes the window shadow. */
    private func roundedMask(radius: CGFloat) -> NSImage {
        let side = radius * 2 + 1
        let image = NSImage(size: NSSize(width: side, height: side), flipped: false) { rect in
            NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
            return true
        }
        image.capInsets = NSEdgeInsets(top: radius, left: radius, bottom: radius, right: radius)
        image.resizingMode = .stretch
        return image
    }

    private func closePanel() {
        panel.orderOut(nil)
        statusItem.button?.highlight(false)
        if let outsideClickMonitor { NSEvent.removeMonitor(outsideClickMonitor) }
        outsideClickMonitor = nil
    }

    /** The standard settings window: toolbar tabs, each hosting a SwiftUI form. */
    private func showSettings() {
        closePanel()
        if settingsWindow == nil {
            let tabs = NSTabViewController()
            tabs.tabStyle = .toolbar
            tabs.addTabViewItem(settingsTab("General", "gearshape", GeneralTab()))
            tabs.addTabViewItem(settingsTab("Phone", "iphone", PhoneTab().environment(state)))
            let window = NSWindow(contentViewController: tabs)
            window.styleMask = [.titled, .closable]
            window.toolbarStyle = .preference
            window.isReleasedWhenClosed = false
            window.delegate = self
            window.center()
            settingsWindow = window
        }
        NSApp.setActivationPolicy(.regular)
        NSApp.activate()
        settingsWindow?.makeKeyAndOrderFront(nil)
    }

    private func settingsTab(_ title: String, _ symbol: String, _ view: some View) -> NSTabViewItem {
        let controller = NSHostingController(rootView: view.frame(width: 440))
        controller.sizingOptions = .preferredContentSize
        controller.title = title // the window shows the selected tab's title
        let item = NSTabViewItem(viewController: controller)
        item.label = title
        item.image = NSImage(systemSymbolName: symbol, accessibilityDescription: title)
        return item
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
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

    private func enableLaunchAtLoginOnce() {
        guard !UserDefaults.standard.bool(forKey: "launchAtLoginSet") else { return }
        try? SMAppService.mainApp.register()
        UserDefaults.standard.set(true, forKey: "launchAtLoginSet")
    }
}
