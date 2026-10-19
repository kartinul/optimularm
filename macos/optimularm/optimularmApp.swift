//
//  optimularmApp.swift
//  optimularm
//
//  Created by Kartik Sharma on 10/08/26.
//

import SwiftUI

@main
struct optimularmApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    // the menu bar icon, its panel and the settings window live in AppDelegate; SwiftUI needs at least one scene
    var body: some Scene {
        Settings { EmptyView() }
    }
}
