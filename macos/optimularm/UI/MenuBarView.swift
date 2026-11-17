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
            if state.paired {
                AlarmPanel()
            } else {
                PairPanel()
            }
            if let error = state.error {
                Label(error, systemImage: "exclamationmark.triangle.fill")
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
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
                    QuarterStepper(time: settings.wakeTime) { time in state.update { $0.wakeTime = time } }
                }
                Divider()
                Row(icon: "calendar", title: "Weekends") {
                    if let weekend = settings.weekendWakeTime {
                        QuarterStepper(time: weekend) { time in state.update { $0.weekendWakeTime = time } }
                        Button("Same as weekdays", systemImage: "xmark.circle.fill") {
                            state.update { $0.weekendWakeTime = nil }
                        }
                        .labelStyle(.iconOnly)
                        .buttonStyle(.borderless)
                        .foregroundStyle(.secondary)
                    } else {
                        Button("Same") { state.update { $0.weekendWakeTime = settings.wakeTime } }
                            .buttonStyle(.borderless)
                            .foregroundStyle(.secondary)
                    }
                }
                Divider()
                Row(icon: "bed.double", title: "Bedtime") {
                    QuarterStepper(time: settings.sleepCutoff) { time in state.update { $0.sleepCutoff = time } }
                }
                Divider()
                Row(icon: "moon.zzz", title: "Last sleep") {
                    Text(state.lastSleep?.formatted(.relative(presentation: .named)) ?? "—")
                        .foregroundStyle(.secondary)
                }
                Divider()
                Row(icon: "bell", title: "Phone rings") {
                    Text(state.pairing.ringAt.map { $0.formatted(.dateTime.weekday().hour().minute()) } ?? "—")
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .background(.quinary, in: RoundedRectangle(cornerRadius: 10))
        }
        .padding(16)
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

/** "8:15 AM" plus a stepper that moves in quarter hours: :00 :15 :30 :45. */
private struct QuarterStepper: View {
    let time: String
    let onChange: (String) -> Void

    var body: some View {
        HStack(spacing: 6) {
            Text(timeDate(time), style: .time)
                .monospacedDigit()
            Stepper("", onIncrement: { step(+1) }, onDecrement: { step(-1) })
                .labelsHidden()
        }
    }

    private func step(_ direction: Int) {
        let minutes = Calendar.current.dateComponents([.hour, .minute], from: timeDate(time))
        let total = (minutes.hour ?? 0) * 60 + (minutes.minute ?? 0)
        // from an off-quarter time like 8:10, the first step lands on 8:15 (or 8:00)
        let quarter = direction > 0 ? total / 15 + 1 : (total + 14) / 15 - 1
        let next = (quarter * 15 + 24 * 60) % (24 * 60)
        NSHapticFeedbackManager.defaultPerformer.perform(.alignment, performanceTime: .now)
        onChange(String(format: "%02d:%02d", next / 60, next % 60))
    }
}
