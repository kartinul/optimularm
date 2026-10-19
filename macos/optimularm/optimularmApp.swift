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

    var body: some Scene {
        MenuBarExtra {
            MenuBarView()
                .environment(appDelegate.state)
        } label: {
            Label(appDelegate.state.menuBarTitle, systemImage: appDelegate.state.menuBarSystemImage)
        }

        Settings {
            SettingsView()
                .environment(appDelegate.state)
        }
    }
}
