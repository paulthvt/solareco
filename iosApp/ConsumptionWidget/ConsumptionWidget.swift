import WidgetKit
import SwiftUI
import AppIntents

private let appGroupId = "group.net.thevenot.comwatt.widget"
private let widgetDataKey = "widget_consumption_data"

// MARK: - Intent

@available(iOS 17.0, *)
struct RefreshWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "Refresh Widget"
    static var openAppWhenRun: Bool = false
    
    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

// MARK: - Models

struct ConsumptionWidgetEntry: TimelineEntry {
    let date: Date
    let timestamps: [Int64]
    let consumptions: [Double]
    let productions: [Double]
    let lastUpdateTime: Int64
    let maxConsumption: Double
    let averageConsumption: Double
    let maxProduction: Double
    let averageProduction: Double
    
    static let empty = ConsumptionWidgetEntry(
        date: Date(),
        timestamps: [],
        consumptions: [],
        productions: [],
        lastUpdateTime: 0,
        maxConsumption: 0,
        averageConsumption: 0,
        maxProduction: 0,
        averageProduction: 0
    )
    
    var hasData: Bool { !consumptions.isEmpty || !productions.isEmpty }
    var currentConsumption: Int { Int(consumptions.last ?? 0) }
    var currentProduction: Int { Int(productions.last ?? 0) }
}

struct WidgetDataModel: Codable {
    let timestamps: [Int64]
    let consumptions: [Double]
    let productions: [Double]
    let lastUpdateTime: Int64
    let maxConsumption: Double
    let averageConsumption: Double
    let maxProduction: Double
    let averageProduction: Double
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        timestamps = try container.decodeIfPresent([Int64].self, forKey: .timestamps) ?? []
        consumptions = try container.decodeIfPresent([Double].self, forKey: .consumptions) ?? []
        productions = try container.decodeIfPresent([Double].self, forKey: .productions) ?? []
        lastUpdateTime = try container.decodeIfPresent(Int64.self, forKey: .lastUpdateTime) ?? 0
        maxConsumption = try container.decodeIfPresent(Double.self, forKey: .maxConsumption) ?? 0
        averageConsumption = try container.decodeIfPresent(Double.self, forKey: .averageConsumption) ?? 0
        maxProduction = try container.decodeIfPresent(Double.self, forKey: .maxProduction) ?? 0
        averageProduction = try container.decodeIfPresent(Double.self, forKey: .averageProduction) ?? 0
    }
}

// MARK: - Provider

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> ConsumptionWidgetEntry { .empty }
    
    func getSnapshot(in context: Context, completion: @escaping (ConsumptionWidgetEntry) -> ()) {
        completion(loadWidgetData())
    }
    
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let entry = loadWidgetData()
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date())!
        completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
    }
    
    private func loadWidgetData() -> ConsumptionWidgetEntry {
        guard let sharedDefaults = UserDefaults(suiteName: appGroupId),
              let jsonString = sharedDefaults.string(forKey: widgetDataKey),
              let jsonData = jsonString.data(using: .utf8) else {
            return .empty
        }
        
        do {
            let data = try JSONDecoder().decode(WidgetDataModel.self, from: jsonData)
            return ConsumptionWidgetEntry(
                date: Date(),
                timestamps: data.timestamps,
                consumptions: data.consumptions,
                productions: data.productions,
                lastUpdateTime: data.lastUpdateTime,
                maxConsumption: data.maxConsumption,
                averageConsumption: data.averageConsumption,
                maxProduction: data.maxProduction,
                averageProduction: data.averageProduction
            )
        } catch {
            return .empty
        }
    }
}

// MARK: - Colors

extension Color {
    static let powerConsumption = Color(red: 1.0, green: 0.70, blue: 0.0)
    static let powerProduction = Color(red: 0.40, green: 0.73, blue: 0.42)
    static let boltYellow = Color(red: 1.0, green: 0.76, blue: 0.03)
}

// MARK: - Views

struct ConsumptionWidgetEntryView: View {
    var entry: Provider.Entry
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            header
            if entry.hasData {
                content
            } else {
                emptyState
            }
        }
        .padding(12)
        .widgetBackground(colorScheme == .dark ? Color(red: 0.12, green: 0.12, blue: 0.12) : Color(UIColor.systemBackground))
    }
    
    private var header: some View {
        HStack(spacing: 4) {
            Image(systemName: "bolt.fill")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.boltYellow)
            
            Text("Energy Overview")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(Color(UIColor.label))
            
            Spacer()
            
            refreshButton
        }
    }
    
    @ViewBuilder
    private var refreshButton: some View {
        if #available(iOS 17.0, *) {
            Button(intent: RefreshWidgetIntent()) {
                Image(systemName: "arrow.clockwise")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(colorScheme == .dark ? .white : Color(UIColor.label))
                    .frame(width: 28, height: 28)
                    .background(Circle().fill(colorScheme == .dark ? Color(white: 0.2) : Color(UIColor.tertiarySystemBackground)))
            }
            .buttonStyle(.plain)
        }
    }
    
    private var content: some View {
        VStack(alignment: .leading, spacing: 6) {
            statsRow
            ChartView(entry: entry).frame(maxWidth: .infinity).frame(height: 50)
            HStack { Spacer(); lastUpdateText }
        }
    }
    
    private var statsRow: some View {
        HStack(spacing: 12) {
            if !entry.consumptions.isEmpty {
                PowerStat(iconName: "arrow.down", value: entry.currentConsumption, color: .powerConsumption)
            }
            if !entry.productions.isEmpty {
                PowerStat(iconName: "arrow.up", value: entry.currentProduction, color: .powerProduction)
            }
            Spacer()
        }
    }
    
    private var lastUpdateText: some View {
        Group {
            if entry.lastUpdateTime > 0 {
                let date = Date(timeIntervalSince1970: Double(entry.lastUpdateTime) / 1000.0)
                let formatter = DateFormatter()
                let _ = formatter.dateFormat = "HH:mm"
                Text("Last update: \(formatter.string(from: date))")
            } else {
                Text("No data")
            }
        }
        .font(.system(size: 9))
        .foregroundColor(Color(UIColor.secondaryLabel))
    }
    
    private var emptyState: some View {
        VStack(spacing: 4) {
            Spacer()
            Text("No data available").font(.system(size: 14)).foregroundColor(Color(UIColor.secondaryLabel))
            Text("Open app to refresh").font(.system(size: 12)).foregroundColor(Color(UIColor.tertiaryLabel))
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

struct PowerStat: View {
    let iconName: String
    let value: Int
    let color: Color
    
    var body: some View {
        HStack(spacing: 2) {
            Image(systemName: iconName).font(.system(size: 10, weight: .bold)).foregroundColor(color)
            Text("\(value)W").font(.system(size: 12, weight: .bold)).foregroundColor(Color(UIColor.label))
        }
    }
}

struct ChartView: View {
    let entry: ConsumptionWidgetEntry
    
    var body: some View {
        GeometryReader { geo in
            let maxValue = max(entry.maxConsumption, entry.maxProduction)
            ZStack {
                if !entry.productions.isEmpty && entry.maxProduction > 0 {
                    ChartLine(data: entry.productions, maxValue: maxValue, size: geo.size, color: .powerProduction)
                }
                if !entry.consumptions.isEmpty && entry.maxConsumption > 0 {
                    ChartLine(data: entry.consumptions, maxValue: maxValue, size: geo.size, color: .powerConsumption)
                }
            }
        }
    }
}

struct ChartLine: View {
    let data: [Double]
    let maxValue: Double
    let size: CGSize
    let color: Color
    
    var body: some View {
        let displayData = Array(data.suffix(40))
        if !displayData.isEmpty && maxValue > 0 {
            Path { path in
                let stepX = size.width / CGFloat(max(1, displayData.count - 1))
                for (i, value) in displayData.enumerated() {
                    let x = CGFloat(i) * stepX
                    let y = size.height - (CGFloat(value / maxValue) * size.height * 0.9) - (size.height * 0.05)
                    if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                    else { path.addLine(to: CGPoint(x: x, y: y)) }
                }
            }
            .stroke(color, lineWidth: 2)
        }
    }
}

extension View {
    @ViewBuilder
    func widgetBackground(_ color: Color) -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(color, for: .widget)
        } else {
            background(color)
        }
    }
}

// MARK: - Widget

struct ConsumptionWidget: Widget {
    let kind = "ConsumptionWidget"
    
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            if #available(iOS 17.0, *) {
                ConsumptionWidgetEntryView(entry: entry).invalidatableContent()
            } else {
                ConsumptionWidgetEntryView(entry: entry)
            }
        }
        .configurationDisplayName("Energy Overview")
        .description("Shows your consumption and production data")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

@main
struct ConsumptionWidgetBundle: WidgetBundle {
    var body: some Widget { ConsumptionWidget() }
}