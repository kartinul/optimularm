//
//  SleepWatcher.swift
//  optimularm
//

import AppKit
import os

/** Writes a sleep event the moment the display sleeps; timers may not run once the Mac sleeps. */
func watchDisplaySleep(_ state: AppState) {
    NSWorkspace.shared.notificationCenter.addObserver(
        forName: NSWorkspace.screensDidSleepNotification,
        object: nil,
        queue: .main
    ) { _ in
        logger.info("Display went to sleep")
        if let pairId = state.pairId { writeSleepEvent(pairId: pairId) }
    }
}
