//
//  SettingsRepository.swift
//  optimularm
//

import Foundation

struct WakeSettings: Equatable {
    var wakeTime = "07:00"
    var alarmOn = true
    var sleepCutoff = "00:00"
    /** Phone also counts its own screen-off as bedtime. */
    var phoneSleep = false
    /** Morning ratings tune the phone's "wake after cycle end". */
    var autoTune = false
}

extension WakeSettings {
    init(_ fields: Fields) {
        let defaults = WakeSettings()
        wakeTime = fields["wakeTime"] as? String ?? defaults.wakeTime
        alarmOn = fields["alarmOn"] as? Bool ?? defaults.alarmOn
        sleepCutoff = fields["sleepCutoff"] as? String ?? defaults.sleepCutoff
        phoneSleep = fields["phoneSleep"] as? Bool ?? defaults.phoneSleep
        autoTune = fields["autoTune"] as? Bool ?? defaults.autoTune
    }

    var fields: Fields {
        [
            "wakeTime": wakeTime, "alarmOn": alarmOn, "sleepCutoff": sleepCutoff,
            "phoneSleep": phoneSleep, "autoTune": autoTune, "lastUpdated": Date(),
        ]
    }
}

func fetchSettings(_ pairId: String) async throws -> WakeSettings {
    WakeSettings(try await getDocument("settings/\(pairId)") ?? [:])
}

func saveSettings(_ pairId: String, _ settings: WakeSettings) async throws {
    try await setDocument("settings/\(pairId)", settings.fields)
}

private let timeFormat: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "HH:mm"
    return formatter
}()

/** "08:00" ⇄ Date, for pickers and display. */
func timeDate(_ time: String) -> Date { timeFormat.date(from: time) ?? .now }
func timeString(_ date: Date) -> String { timeFormat.string(from: date) }
