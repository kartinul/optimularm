//
//  SleepWatcher.swift
//  optimularm
//

import AppKit
import IOKit.pwr_mgt
import os

// iokit_common_msg() macros, which Swift doesn't import
private let canSystemSleep: natural_t = 0xE000_0270
private let systemWillSleep: natural_t = 0xE000_0280

private var rootPort: io_connect_t = 0
private var beforeSleep: ((@escaping () -> Void) -> Void)?

/**
 Writes a sleep event the moment the display sleeps. Closing the lid sleeps the whole Mac right after,
 so system sleep is held (macOS allows up to 30 s) until that write lands, and it's retried on wake.
 */
func watchDisplaySleep(_ state: AppState) {
    let center = NSWorkspace.shared.notificationCenter
    center.addObserver(forName: NSWorkspace.screensDidSleepNotification, object: nil, queue: .main) { _ in
        logger.info("Display went to sleep")
        state.recordSleep()
    }
    center.addObserver(forName: NSWorkspace.didWakeNotification, object: nil, queue: .main) { _ in
        Task { await state.sendSleep() }
    }
    holdSystemSleep { done in
        Task {
            await state.sendSleep()
            done()
        }
    }
}

private func holdSystemSleep(_ work: @escaping (@escaping () -> Void) -> Void) {
    beforeSleep = work
    var port: IONotificationPortRef?
    var notifier: io_object_t = 0
    rootPort = IORegisterForSystemPower(nil, &port, { _, _, message, argument in
        let id = Int(bitPattern: argument)
        MainActor.assumeIsolated {
            switch message {
            case canSystemSleep: IOAllowPowerChange(rootPort, id)
            case systemWillSleep: beforeSleep? { IOAllowPowerChange(rootPort, id) }
            default: break
            }
        }
    }, &notifier)
    IONotificationPortSetDispatchQueue(port, .main)
}
