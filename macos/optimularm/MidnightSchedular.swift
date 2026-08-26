//
//  MidnightSchedular.swift
//  optimularm
//
//  Created by Kartik Sharma on 26/08/26.
//

import Foundation

func scheduleNextMidnightCheck() {
    let calendar = Calendar.current
    let now = Date()
    
    // Get midnight of tomorrow
    guard let nextMidnight = calendar.nextDate(
        after: now,
        matching: DateComponents(hour: 0, minute: 0, second: 0),
        matchingPolicy: .nextTime
    ) else { return }
    
    let secondsUntilMidnight = nextMidnight.timeIntervalSince(now)
    
    Timer.scheduledTimer(withTimeInterval: secondsUntilMidnight, repeats: false) { _ in
        logger.info("Hit midnight, updating icon")
        // update icon here
        scheduleNextMidnightCheck()
    }
}
