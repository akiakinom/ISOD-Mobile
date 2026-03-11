import WidgetKit
import SwiftUI

@main
struct TimetableWidgetsBundle: WidgetBundle {
    var body: some Widget {
        TodayScheduleWidget()
        NextClassWidget()
        
        // ControlWidget requires iOS 18.0+
        if #available(iOS 18.0, *) {
            TimetableWidgetsControl()
        }
    }
}
