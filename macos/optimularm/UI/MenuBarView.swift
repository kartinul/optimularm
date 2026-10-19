//
//  MenuBarView.swift
//  optimularm
//

import SwiftUI

/** The panel under the menu bar icon. */
struct MenuBarView: View {
    @Environment(AppState.self) private var state
    let close: () -> Void
    let openSettings: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            if let pairId = state.pairId, state.paired {
                AlarmPanel(pairId: pairId)
            } else {
                PairPanel()
            }
            Divider()
            HStack {
                Button(action: openSettings) {
                    Label("Settings…", systemImage: "gearshape")
                }
                Spacer()
                Button("Quit") { NSApp.terminate(nil) }
            }
            .buttonStyle(.borderless)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .frame(width: 300)
        .focusEffectDisabled()
        .onExitCommand(perform: close)
    }
}

private struct AlarmPanel: View {
    @Environment(AppState.self) private var state
    let pairId: String

    var body: some View {
        let settings = state.settings
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Wake up by")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(timeDate(settings.wakeTime), style: .time)
                        .font(.system(size: 46, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(settings.alarmOn ? .primary : .tertiary)
                }
                Spacer()
                Toggle("Alarm", isOn: Binding(get: { settings.alarmOn }, set: { _ in state.toggleAlarm() }))
                    .toggleStyle(.switch)
                    .labelsHidden()
            }

            VStack(spacing: 0) {
                Row(icon: "alarm", title: "Wake up") {
                    DatePicker("", selection: timeBinding("wakeTime", settings.wakeTime), displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                Divider()
                Row(icon: "bed.double", title: "Bedtime") {
                    DatePicker("", selection: timeBinding("sleepCutoff", settings.sleepCutoff), displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                Divider()
                Row(icon: "moon.zzz", title: "Last sleep") {
                    Text(state.lastSleep?.formatted(.relative(presentation: .named)) ?? "—")
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .background(.quinary, in: RoundedRectangle(cornerRadius: 10))
        }
        .padding(16)
    }

    private func timeBinding(_ field: String, _ time: String) -> Binding<Date> {
        Binding(get: { timeDate(time) }, set: { saveSettings(pairId, [field: timeString($0)]) })
    }
}

private struct Row<Content: View>: View {
    let icon: String
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        HStack {
            Label(title, systemImage: icon)
            Spacer()
            content
        }
        .frame(minHeight: 36)
    }
}

private struct PairPanel: View {
    @Environment(AppState.self) private var state

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "iphone.radiowaves.left.and.right")
                .font(.system(size: 32))
                .foregroundStyle(.tint)
            Text("Pair your phone")
                .font(.title3.weight(.semibold))
            if let pairId = state.pairId {
                PairingView(pairId: pairId)
            } else {
                Button("Show pairing code", action: state.repair)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
    }
}
