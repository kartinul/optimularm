//
//  SettingsRepository.swift
//  optimularm
//

import FirebaseFirestore

struct WakeSettings {
    var wakeTime = "08:00"
    var alarmOn = true
    var sleepCutoff = "12:00"
}

private func settingsDoc(_ pairId: String) -> DocumentReference {
    Firestore.firestore().collection("settings").document(pairId)
}

func listenSettings(_ pairId: String, onChange: @escaping (WakeSettings) -> Void) -> ListenerRegistration {
    settingsDoc(pairId).addSnapshotListener { snapshot, _ in
        guard let snapshot else { return }
        let data = snapshot.data() ?? [:]
        let defaults = WakeSettings()
        onChange(WakeSettings(
            wakeTime: data["wakeTime"] as? String ?? defaults.wakeTime,
            alarmOn: data["alarmOn"] as? Bool ?? defaults.alarmOn,
            sleepCutoff: data["sleepCutoff"] as? String ?? defaults.sleepCutoff
        ))
    }
}

func saveSettings(_ pairId: String, _ fields: [String: Any]) {
    settingsDoc(pairId).setData(fields.merging(["lastUpdated": FieldValue.serverTimestamp()]) { $1 }, merge: true)
}

func createDefaultSettings(_ pairId: String) async throws {
    let defaults = WakeSettings()
    try await settingsDoc(pairId).setData([
        "wakeTime": defaults.wakeTime,
        "alarmOn": defaults.alarmOn,
        "sleepCutoff": defaults.sleepCutoff,
        "lastUpdated": FieldValue.serverTimestamp(),
    ])
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
