//
//  SleepEvent.swift
//  optimularm
//
//  Created by Kartik Sharma on 14/08/26.
//

import FirebaseFirestore
import os

struct SleepEvent: Codable {
    @DocumentID var id: String?
    let sleepTime: Date
}

private func sleepEvents(_ pairId: String) -> CollectionReference {
    Firestore.firestore().collection("pairings").document(pairId).collection("sleepEvents")
}

func writeSleepEvent(pairId: String, at sleepTime: Date = Date()) {
    do {
        try sleepEvents(pairId).addDocument(from: SleepEvent(sleepTime: sleepTime))
        logger.info("Sleep event written")
    } catch {
        logger.error("Failed to write: \(error.localizedDescription)")
    }
}

func listenLatestSleep(_ pairId: String, onChange: @escaping (Date?) -> Void) -> ListenerRegistration {
    sleepEvents(pairId).order(by: "sleepTime", descending: true).limit(to: 1).addSnapshotListener { snapshot, _ in
        onChange(try? snapshot?.documents.first?.data(as: SleepEvent.self).sleepTime)
    }
}
