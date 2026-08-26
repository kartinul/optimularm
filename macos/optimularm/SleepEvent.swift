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
    let pairId: String
    let sleepTime: Date
    let isPostMidnight: Bool
    let wakeByCeiling: String
    var status: String
    var wokeAt: Date? = nil
    var actualWakeTime: Date? = nil
}

func writeSleepEvent() {
    let event = SleepEvent(
        pairId: "gaga",
        sleepTime: Date(),
        isPostMidnight: true,
        wakeByCeiling: "08:00",
        status: "active"
    )

    Task {
        do {
            try Firestore.firestore().collection("sleepEvents").addDocument(from: event)
            logger.info("Sleep event written")
        } catch {
            logger.error("Failed to write: \(error.localizedDescription)")
        }
    }
}

func markLatestEventInterrupted() {
    let db = Firestore.firestore()

    Task {
        do {
            let snapshot = try await db.collection("sleepEvents")
                .order(by: "sleepTime", descending: true)
                .limit(to: 1)
                .getDocuments()

            guard let doc = snapshot.documents.first else { return }

            try await doc.reference.updateData(["status": "interrupted"])
            logger.info("Marked latest event interrupted")
        } catch {
            logger.error("Failed to update: \(error.localizedDescription)")
        }
    }
}
