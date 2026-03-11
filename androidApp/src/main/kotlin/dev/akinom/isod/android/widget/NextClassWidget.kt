package dev.akinom.isod.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.TimetableEntry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NextClassWidget : GlanceAppWidget(), KoinComponent {
    private val timetableRepo: TimetableRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val monday = currentWeekMonday()
            val timetable by timetableRepo.getTimetable("2026L", monday).collectAsState(emptyList())
            val nextClasses = TimetableWidgetUtils.getNextClasses(timetable)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "Next Classes",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )

                if (nextClasses.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No upcoming classes")
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(nextClasses) { entry ->
                            val now = TimetableWidgetUtils.getCurrentTime()
                            val today = TimetableWidgetUtils.getTodayDayOfWeek()
                            val isCurrent = entry.dayOfWeek == today && entry.startTime <= now && entry.endTime > now
                            NextClassItemContent(entry, isCurrent = isCurrent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextClassItemContent(entry: TimetableEntry, isCurrent: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isCurrent) "NOW: ${entry.endTime}" else "${entry.startTime} - ${entry.endTime}",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Day ${entry.dayOfWeek}",
                style = TextStyle(fontSize = 10.sp)
            )
        }
        Text(
            text = entry.courseNameShort,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
        )
        Text(
            text = "${entry.buildingShort} ${entry.room}",
            style = TextStyle(fontSize = 12.sp)
        )
    }
}

class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()
}
