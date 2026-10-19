//
//  SettingsView.swift
//  optimularm
//

import ServiceManagement
import SwiftUI

struct GeneralTab: View {
    @State private var launchAtLogin = SMAppService.mainApp.status == .enabled

    var body: some View {
        Form {
            Toggle("Launch at login", isOn: $launchAtLogin)
                .onChange(of: launchAtLogin) { _, enabled in
                    try? enabled ? SMAppService.mainApp.register() : SMAppService.mainApp.unregister()
                    launchAtLogin = SMAppService.mainApp.status == .enabled
                }
            LabeledContent("Right-click the menu bar icon", value: "Turns the alarm on or off")
        }
        .formStyle(.grouped)
        .fixedSize(horizontal: false, vertical: true)
    }
}

struct PhoneTab: View {
    @Environment(AppState.self) private var state
    @State private var confirmRepair = false

    var body: some View {
        Form {
            Section {
                LabeledContent("Status") {
                    Label(state.paired ? "Paired" : "Waiting for phone",
                          systemImage: state.paired ? "checkmark.circle.fill" : "hourglass")
                        .foregroundStyle(state.paired ? .green : .secondary)
                }
                if let pairId = state.pairId, !state.paired {
                    PairingView(pairId: pairId)
                        .padding(.vertical, 8)
                }
            }
            Section {
                Button(state.pairId == nil ? "Pair a phone" : "Re-pair…") {
                    if state.paired { confirmRepair = true } else { state.repair() }
                }
            } footer: {
                Text("Re-pairing shows a new code; the current phone stops receiving sleep events until it scans it.")
                    .foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
        .fixedSize(horizontal: false, vertical: true)
        .confirmationDialog("Re-pair with a new phone?", isPresented: $confirmRepair) {
            Button("Re-pair", role: .destructive, action: state.repair)
        }
    }
}
