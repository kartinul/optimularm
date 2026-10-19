//
//  MenuBarView.swift
//  optimularm
//

import SwiftUI

struct MenuBarView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        if state.paired {
            Text("Wake up \(state.settings.wakeTime)")
            Text(state.settings.alarmOn ? "Alarm on" : "Alarm off")
            Text("Last sleep: \(state.lastSleep?.formatted(date: .abbreviated, time: .shortened) ?? "none")")
        } else {
            Text("Not paired: open Settings")
        }

        Divider()

        #if DEBUG
        if let pairId = state.pairId {
            Button("Test: Write Sleep Event") {
                writeSleepEvent(pairId: pairId)
            }
            Button("Test: Sleep Event 3h Ago") {
                writeSleepEvent(pairId: pairId, at: Date().addingTimeInterval(-3 * 3600))
            }
            Divider()
        }
        #endif

        SettingsLink {
            Text("Settings…")
        }
        .keyboardShortcut(",")

        Divider()

        Button("Quit") {
            NSApp.terminate(nil)
        }
        .keyboardShortcut("q")
    }
}
