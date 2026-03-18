import WidgetKit
import SwiftUI
import ComposeApp

struct NextClassProvider: TimelineProvider {
    let provider = TimetableWidgetProvider()

    func placeholder(in context: Context) -> NextClassEntry {
        NextClassEntry(date: Date(), items: [])
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> ()) {
        provider.getNextClasses { items in
            completion(NextClassEntry(date: Date(), items: items))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextClassEntry>) -> ()) {
        provider.getNextClasses { items in
            let timeline = Timeline(entries: [NextClassEntry(date: Date(), items: items)], policy: .after(Date().addingTimeInterval(900)))
            completion(timeline)
        }
    }
}

struct NextClassEntry: TimelineEntry {
    let date: Date
    let items: [TimetableEntry]
}

struct NextClassWidgetEntryView : View {
    var entry: NextClassProvider.Entry
    let provider = TimetableWidgetProvider()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("next_classes")
                .font(.headline)
            
            if entry.items.isEmpty {
                Spacer()
                Text("no_upcoming_classes")
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer()
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(entry.items, id: \.id) { item in
                        NextClassItem(item: item, 
                                     now: provider.getCurrentTime(), 
                                     today: provider.getTodayDayOfWeek())
                    }
                }
            }
        }
        .padding()
    }
}

struct NextClassItem: View {
    let item: TimetableEntry
    let now: String
    let today: Int32

    var body: some View {
        let isCurrent = Int32(item.dayOfWeek) == today && item.startTime <= now && item.endTime > now
        
        VStack(alignment: .leading) {
            HStack {
                if isCurrent {
                    Text(String(format: NSLocalizedString("now_format", comment: ""), item.endTime))
                        .font(.system(size: 12, weight: .medium))
                } else {
                    Text("\(item.startTime) - \(item.endTime)")
                        .font(.system(size: 12, weight: .medium))
                }
                Spacer()
                Text(String(format: NSLocalizedString("day_format", comment: ""), item.dayOfWeek))
                    .font(.system(size: 10))
            }
            Text(item.courseNameShort)
                .font(.system(size: 14, weight: .bold))
            Text("\(item.location)")
                .font(.system(size: 12))
        }
        .padding(.vertical, 2)
    }
}

struct NextClassWidget: Widget {
    let kind: String = "NextClassWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: NextClassProvider()) { entry in
            if #available(iOS 17.0, *) {
                NextClassWidgetEntryView(entry: entry)
                    .containerBackground(.fill.tertiary, for: .widget)
            } else {
                NextClassWidgetEntryView(entry: entry)
                    .background()
            }
        }
        .configurationDisplayName(LocalizedStringKey("next_classes"))
        .description(LocalizedStringKey("widget_next_classes_description"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
