import WidgetKit
import SwiftUI
import ComposeApp

struct ConsumptionWidgetEntry: TimelineEntry {
    let date: Date
    let timestamps: [Int64]
    let consumptions: [Double]
    let lastUpdateTime: Int64
    let maxConsumption: Double
    let averageConsumption: Double
    
    static func empty() -> ConsumptionWidgetEntry {
        return ConsumptionWidgetEntry(
            date: Date(),
            timestamps: [],
            consumptions: [],
            lastUpdateTime: 0,
            maxConsumption: 0.0,
            averageConsumption: 0.0
        )
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> ConsumptionWidgetEntry {
        ConsumptionWidgetEntry.empty()
    }

    func getSnapshot(in context: Context, completion: @escaping (ConsumptionWidgetEntry) -> ()) {
        let entry = loadWidgetData()
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let currentDate = Date()
        let entry = loadWidgetData()
        
        // Update every 15 minutes
        let nextUpdateDate = Calendar.current.date(byAdding: .minute, value: 15, to: currentDate)!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdateDate))
        
        completion(timeline)
    }
    
    private func loadWidgetData() -> ConsumptionWidgetEntry {
        // Load widget data from shared UserDefaults
        let sharedDefaults = UserDefaults(suiteName: "group.net.thevenot.comwatt.widget")
        
        guard let jsonString = sharedDefaults?.string(forKey: "widget_consumption_data"),
              let jsonData = jsonString.data(using: .utf8) else {
            return ConsumptionWidgetEntry.empty()
        }
        
        do {
            let decoder = JSONDecoder()
            let data = try decoder.decode(WidgetDataModel.self, from: jsonData)
            
            return ConsumptionWidgetEntry(
                date: Date(),
                timestamps: data.timestamps,
                consumptions: data.consumptions,
                lastUpdateTime: data.lastUpdateTime,
                maxConsumption: data.maxConsumption,
                averageConsumption: data.averageConsumption
            )
        } catch {
            print("Error decoding widget data: \(error)")
            return ConsumptionWidgetEntry.empty()
        }
    }
}

struct WidgetDataModel: Codable {
    let timestamps: [Int64]
    let consumptions: [Double]
    let lastUpdateTime: Int64
    let maxConsumption: Double
    let averageConsumption: Double
}

struct ConsumptionWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var widgetFamily
    
    private func loadChartImage() -> UIImage? {
        let sharedDefaults = UserDefaults(suiteName: "group.net.thevenot.comwatt.widget")
        guard let imageData = sharedDefaults?.data(forKey: "widget_chart_image") else {
            return nil
        }
        return UIImage(data: imageData)
    }

    var body: some View {
        ZStack {
            Color(red: 0.12, green: 0.12, blue: 0.12)
            
            VStack(alignment: .leading, spacing: 8) {
                // Header
                HStack(spacing: 8) {
                    Text("⚡")
                        .font(.system(size: 20, weight: .bold))
                    
                    Text("Consumption")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                    
                    Spacer()
                }
                
                if !entry.consumptions.isEmpty {
                    // Statistics
                    HStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Current")
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 0.73, green: 0.73, blue: 0.73))
                            
                            Text("\(Int(entry.consumptions.last ?? 0)) W")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(red: 0.30, green: 0.69, blue: 0.31))
                        }
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Average")
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 0.73, green: 0.73, blue: 0.73))
                            
                            Text("\(Int(entry.averageConsumption)) W")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        }
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Peak")
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 0.73, green: 0.73, blue: 0.73))
                            
                            Text("\(Int(entry.maxConsumption)) W")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(red: 1.0, green: 0.60, blue: 0.0))
                        }
                        
                        Spacer()
                    }
                    
                    // Chart
                    ChartView(consumptions: entry.consumptions, maxValue: entry.maxConsumption)
                        .frame(height: 60)
                    
                    // Last update
                    if entry.lastUpdateTime > 0 {
                        let date = Date(timeIntervalSince1970: Double(entry.lastUpdateTime) / 1000.0)
                        let formatter = DateFormatter()
                        formatter.dateFormat = "HH:mm"
                        
                        Text("Updated: \(formatter.string(from: date))")
                            .font(.system(size: 10))
                            .foregroundColor(Color(red: 0.53, green: 0.53, blue: 0.53))
                    }
                } else {
                    Spacer()
                    
                    VStack(spacing: 4) {
                        Text("No data available")
                            .font(.system(size: 14))
                            .foregroundColor(Color(red: 0.53, green: 0.53, blue: 0.53))
                        
                        Text("Widget will update automatically")
                            .font(.system(size: 12))
                            .foregroundColor(Color(red: 0.40, green: 0.40, blue: 0.40))
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    
                    Spacer()
                }
            }
            .padding(16)
        }
    }
}

struct ChartView: View {
    let consumptions: [Double]
    let maxValue: Double
    
    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let height = geometry.size.height
            let dataCount = min(consumptions.count, 40)
            let data = Array(consumptions.suffix(dataCount))
            
            if !data.isEmpty && maxValue > 0 {
                Path { path in
                    let stepX = width / CGFloat(max(1, data.count - 1))
                    
                    for (index, value) in data.enumerated() {
                        let x = CGFloat(index) * stepX
                        let normalizedValue = CGFloat(value / maxValue)
                        let y = height - (normalizedValue * height)
                        
                        if index == 0 {
                            path.move(to: CGPoint(x: x, y: y))
                        } else {
                            path.addLine(to: CGPoint(x: x, y: y))
                        }
                    }
                }
                .stroke(Color(red: 0.30, green: 0.69, blue: 0.31), lineWidth: 2)
            }
        }
    }
}

struct ConsumptionWidget: Widget {
    let kind: String = "ConsumptionWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            ConsumptionWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Energy Consumption")
        .description("Shows your last hour energy consumption.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

@main
struct ConsumptionWidgetBundle: WidgetBundle {
    var body: some Widget {
        ConsumptionWidget()
    }
}
