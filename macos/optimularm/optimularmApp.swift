//
//  optimularmApp.swift
//  optimularm
//
//  Created by Kartik Sharma on 10/08/26.
//

import os
import SwiftUI
import FirebaseCore

@Observable
final class AppState {
    // Symbol name for the menu bar icon; update this from anywhere to reflect in the menu
    var menuBarSystemImage: String = "timer.circle.fill"
    // Optional title if you want to change the label text too
    var menuBarTitle: String = "optimularm"
}

class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        FirebaseApp.configure()

        // Hide from Dock and Cmd+Tab by default — menu bar only
        NSApp.setActivationPolicy(.accessory)

        NSWorkspace.shared.notificationCenter.addObserver(
            forName: NSWorkspace.screensDidSleepNotification,
            object: nil,
            queue: .main
        ) { _ in
            logger.info("Display went to sleep")
            writeSleepEvent()
        }
    }
}

@main
struct optimularmApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @State private var appState = AppState()

    var body: some Scene {
        MenuBarExtra {
            #if DEBUG
            Button("Test: Write Sleep Event") {
                writeSleepEvent()
            }
            Button("Test: Sleep Event 3h Ago") {
                writeSleepEvent(at: Date().addingTimeInterval(-3 * 3600))
            }
            Divider()
            #endif

            Button("Settings") {
                NSApp.setActivationPolicy(.regular)
            }

            Divider()

            Button("Quit") {
                NSApp.terminate(nil)
            }
        } label: {
            Label(appState.menuBarTitle, systemImage: appState.menuBarSystemImage)
        }
        .environment(appState)
    }
}
