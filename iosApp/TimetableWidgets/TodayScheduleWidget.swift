import WidgetKit
import SwiftUI
import ComposeApp

struct TodayScheduleProvider: TimelineProvider {
    let provider = TimetableWidgetProvider()

    func placeholder(in context: Context) -> TodayScheduleEntry {
        TodayScheduleEntry(date: Date(), items: [])
    }

    func getSnapshot(in context: Context, completion: @escaping (TodayScheduleEntry) -> ()) {
        provider.getTodaySchedule { items in
            completion(TodayScheduleEntry(date: Date(), items: items))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodayScheduleEntry>) -> ()) {
        provider.getTodaySchedule { items in
            let timeline = Timeline(entries: [TodayScheduleEntry(date: Date(), items: items)], policy: .after(Date().addingTimeInterval(3600)))
            completion(timeline)
        }
    }
}

struct TodayScheduleEntry: TimelineEntry {
    let date: Date
    let items: [TimetableEntry]
}

struct TodayScheduleWidgetEntryView : View {
    var entry: TodayScheduleProvider.Entry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("today_schedule")
                .font(.headline)
            
            if entry.items.isEmpty {
                Spacer()
                Text("no_classes_today")
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer()
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(entry.items, id: \.id) { item in
                        VStack(alignment: .leading) {
                            HStack {
                                Text("\(item.startTime) - \(item.endTime)")
                                    .font(.system(size: 12, weight: .medium))
                                Spacer()
                                Text(item.courseType)
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
            }
        }
        .padding()
    }
}

struct TodayScheduleWidget: Widget {
    let kind: String = "TodayScheduleWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TodayScheduleProvider()) { entry in
            if #available(iOS 17.0, *) {
                TodayScheduleWidgetEntryView(entry: entry)
                    .containerBackground(.fill.tertiary, for: .widget)
            } else {
                TodayScheduleWidgetEntryView(entry: entry)
                    .background()
            }
        }
        .configurationDisplayName(LocalizedStringKey("today_schedule"))
        .description(LocalizedStringKey("widget_today_schedule_description"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
