//
//  SettingsRepository.swift
//  optimularm
//

import Foundation

struct WakeSettings: Equatable {
    var wakeTime = "07:00"
    /** Saturday and Sunday's wake time; nil = same as [wakeTime]. */
    var weekendWakeTime: String?
    var alarmOn = true
    var sleepCutoff = "00:00"
}

extension WakeSettings {
    init(_ fields: Fields) {
        let defaults = WakeSettings()
        wakeTime = fields["wakeTime"] as? String ?? defaults.wakeTime
        weekendWakeTime = fields["weekendWakeTime"] as? String
        alarmOn = fields["alarmOn"] as? Bool ?? defaults.alarmOn
        sleepCutoff = fields["sleepCutoff"] as? String ?? defaults.sleepCutoff
    }

    var fields: Fields {
        var fields: Fields = ["wakeTime": wakeTime, "alarmOn": alarmOn, "sleepCutoff": sleepCutoff, "lastUpdated": Date()]
        fields["weekendWakeTime"] = weekendWakeTime
        return fields
    }
}

func fetchSettings(_ pairId: String) async throws -> WakeSettings {
    WakeSettings(try await getDocument("settings/\(pairId)") ?? [:])
}

func saveSettings(_ pairId: String, _ settings: WakeSettings) async throws {
    try await setDocument("settings/\(pairId)", settings.fields, delete: settings.weekendWakeTime == nil ? ["weekendWakeTime"] : [])
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
