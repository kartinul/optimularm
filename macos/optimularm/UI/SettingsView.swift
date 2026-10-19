//
//  SettingsView.swift
//  optimularm
//

import ServiceManagement
import SwiftUI

struct SettingsView: View {
    @Environment(AppState.self) private var state
    @State private var launchAtLogin = SMAppService.mainApp.status == .enabled

    var body: some View {
        Form {
            Section {
                Toggle("Launch at login", isOn: $launchAtLogin)
                    .onChange(of: launchAtLogin) { _, enabled in
                        try? enabled ? SMAppService.mainApp.register() : SMAppService.mainApp.unregister()
                        launchAtLogin = SMAppService.mainApp.status == .enabled
                    }
            }

            Section("Phone") {
                LabeledContent("Status") {
                    Label(state.paired ? "Paired" : "Not paired",
                          systemImage: state.paired ? "checkmark.circle.fill" : "circle.dashed")
                        .foregroundStyle(state.paired ? .green : .secondary)
                }
                if let pairId = state.pairId, !state.paired {
                    PairingView(pairId: pairId)
                }
                Button(state.pairId == nil ? "Pair" : "Re-pair", action: state.repair)
            }
        }
        .formStyle(.grouped)
        .frame(width: 380)
        .fixedSize()
    }
}
