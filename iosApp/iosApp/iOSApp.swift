import SwiftUI
import ComposeApp
import UserNotifications

@main
struct iOSApp: App {
    @StateObject private var delegate = NotificationDelegate()

    init() {
        IosAppKt.doInitApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(initialNewsHash: delegate.selectedNewsHash)
        }
    }
}

class NotificationDelegate: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published var selectedNewsHash: String?

    override init() {
        super.init()
        UNUserNotificationCenter.currentNotificationCenter().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let newsHash = response.notification.request.content.userInfo["newsHash"] as? String {
            selectedNewsHash = newsHash
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}
