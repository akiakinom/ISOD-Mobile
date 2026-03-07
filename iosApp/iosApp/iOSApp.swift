import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        IosAppKt.doInitApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
