//
//  SleepEvent.swift
//  optimularm
//
//  Created by Kartik Sharma on 14/08/26.
//

import Foundation

/** The event id is the sleep time itself, so retrying a write overwrites instead of duplicating. */
func writeSleepEvent(_ pairId: String, _ sleepTime: Date) async throws {
    let id = Int(sleepTime.timeIntervalSince1970)
    try await setDocument("pairings/\(pairId)/sleepEvents/\(id)", ["sleepTime": sleepTime], replace: true)
}

func fetchLatestSleep(_ pairId: String) async throws -> Date? {
    try await latestDocument("pairings/\(pairId)", "sleepEvents", orderBy: "sleepTime")?["sleepTime"] as? Date
}
