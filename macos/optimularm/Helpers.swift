//
//  Helpers.swift
//  optimularm
//
//  Created by Kartik Sharma on 26/08/26.
//

import Foundation

func isPastMidnight() -> Bool {
    let hour = Calendar.current.component(.hour, from: Date())
    return hour < 12  // rough: midnight to noon counts as "post-midnight" window
}
