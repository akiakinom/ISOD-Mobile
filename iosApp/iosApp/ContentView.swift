import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var initialTab: MainTab? = nil
    var initialDayOfWeek: Int32? = nil
    var initialNewsHash: String? = nil

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            initialTab: initialTab,
            initialDayOfWeek: initialDayOfWeek,
            initialNewsHash: initialNewsHash
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State var initialTab: MainTab? = nil
    @State var initialDayOfWeek: Int32? = nil
    @State var initialNewsHash: String? = nil

    var body: some View {
        ComposeView(
            initialTab: initialTab,
            initialDayOfWeek: initialDayOfWeek,
            initialNewsHash: initialNewsHash
        )
        .ignoresSafeArea(.all)
        .onOpenURL { url in
            // Handle deep links if needed
        }
    }
}
