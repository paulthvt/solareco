import SwiftUI
import ComposeApp
import WidgetKit
import BackgroundTasks

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    
    private static let backgroundTaskId = "net.thevenot.comwatt.widget.refresh"
    
    init() {
        registerBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in handleDeepLink(url) }
                .onAppear { updateWidgetData() }
        }
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .active:
                updateWidgetData()
                WidgetCenter.shared.reloadTimelines(ofKind: "ConsumptionWidget")
            case .background:
                updateWidgetData()
                WidgetCenter.shared.reloadTimelines(ofKind: "ConsumptionWidget")
                scheduleBackgroundRefresh()
            default:
                break
            }
        }
    }
    
    private func registerBackgroundTasks() {
        #if !targetEnvironment(simulator)
        BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.backgroundTaskId, using: nil) { task in
            self.handleBackgroundRefresh(task: task as! BGAppRefreshTask)
        }
        #endif
    }
    
    private func scheduleBackgroundRefresh() {
        #if !targetEnvironment(simulator)
        let request = BGAppRefreshTaskRequest(identifier: Self.backgroundTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
        #endif
    }
    
    private func handleBackgroundRefresh(task: BGAppRefreshTask) {
        scheduleBackgroundRefresh()
        
        let fetchTask = Task {
            updateWidgetData()
            WidgetCenter.shared.reloadTimelines(ofKind: "ConsumptionWidget")
            task.setTaskCompleted(success: true)
        }
        
        task.expirationHandler = {
            fetchTask.cancel()
            task.setTaskCompleted(success: false)
        }
    }
    
    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "comwatt", url.host == "refresh" else { return }
        updateWidgetData()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            WidgetCenter.shared.reloadTimelines(ofKind: "ConsumptionWidget")
        }
    }
    
    private func updateWidgetData() {
        let factory = Factory()
        IosWidgetHelperKt.updateWidgetData(dataRepository: factory.dataRepository)
    }
}
