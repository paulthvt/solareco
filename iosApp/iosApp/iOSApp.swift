import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init(){
        Platform_iosKt.doInitLogger()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}