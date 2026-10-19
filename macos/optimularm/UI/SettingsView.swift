//
//  SettingsView.swift
//  optimularm
//

import ServiceManagement
import SwiftUI

private let timeFormat: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "HH:mm"
    return formatter
}()

struct SettingsView: View {
    @Environment(AppState.self) private var state
    @State private var launchAtLogin = SMAppService.mainApp.status == .enabled

    var body: some View {
        Form {
            if let pairId = state.pairId, state.paired {
                Section("Alarm") {
                    Toggle("Alarm on", isOn: Binding(
                        get: { state.settings.alarmOn },
                        set: { saveSettings(pairId, ["alarmOn": $0]) }
                    ))
                    DatePicker("Wake up", selection: timeBinding(pairId, "wakeTime", state.settings.wakeTime),
                               displayedComponents: .hourAndMinute)
                    DatePicker("Bedtime", selection: timeBinding(pairId, "sleepCutoff", state.settings.sleepCutoff),
                               displayedComponents: .hourAndMinute)
                }
            }

            Section("General") {
                Toggle("Launch at login", isOn: $launchAtLogin)
                    .onChange(of: launchAtLogin) { _, enabled in
                        try? enabled ? SMAppService.mainApp.register() : SMAppService.mainApp.unregister()
                        launchAtLogin = SMAppService.mainApp.status == .enabled
                    }
            }

            Section("Phone") {
                if state.paired {
                    LabeledContent("Status", value: "Paired")
                } else if let pairId = state.pairId {
                    PairingView(pairId: pairId)
                }
                Button(state.pairId == nil ? "Pair" : "Re-pair", action: state.repair)
            }
        }
        .formStyle(.grouped)
        .frame(width: 380)
        .fixedSize()
        .onAppear {
            NSApp.setActivationPolicy(.regular)
            NSApp.activate()
        }
        .onDisappear {
            NSApp.setActivationPolicy(.accessory)
        }
    }

    private func timeBinding(_ pairId: String, _ field: String, _ time: String) -> Binding<Date> {
        Binding(
            get: { timeFormat.date(from: time) ?? .now },
            set: { saveSettings(pairId, [field: timeFormat.string(from: $0)]) }
        )
    }
}
