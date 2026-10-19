//
//  SleepEvent.swift
//  optimularm
//
//  Created by Kartik Sharma on 14/08/26.
//

import FirebaseFirestore
import os

// shortcut: hardcoded until QR pairing exists
let pairId = "gaga"

struct SleepEvent: Codable {
    @DocumentID var id: String?
    let sleepTime: Date
}

func writeSleepEvent(at sleepTime: Date = Date()) {
    do {
        try Firestore.firestore()
            .collection("pairings").document(pairId).collection("sleepEvents")
            .addDocument(from: SleepEvent(sleepTime: sleepTime))
        logger.info("Sleep event written")
    } catch {
        logger.error("Failed to write: \(error.localizedDescription)")
    }
}
