package dev.akinom.isod.android.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.unit.ColorProvider
import dev.akinom.isod.MainTab
import dev.akinom.isod.android.MainActivity
import dev.akinom.isod.android.R
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.TimetableEntry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TodayScheduleWidget : GlanceAppWidget(), KoinComponent {
    private val timetableRepo: TimetableRepository by inject()

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("tab", MainTab.Schedule.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val action = actionStartActivity(intent)

        provideContent {
            GlanceTheme {
                val monday = currentWeekMonday()
                val semester = currentSemester()
                val timetable by timetableRepo.getTimetable(semester, monday).collectAsState(emptyList())
                val currentWeek = AcademicCalendar.getCurrentWeek(semester)
                val (isAfterLessons, dashboardEntries) = TimetableWidgetUtils.getDashboardSchedule(timetable, currentWeek)
                val size = LocalSize.current
                val context = LocalContext.current

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                            } else {
                                GlanceModifier.cornerRadius(16.dp)
                            }
                        )
                        .clickable(action)
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        if (dashboardEntries.isEmpty()) {
                            Column(
                                modifier = GlanceModifier.fillMaxSize().defaultWeight(), 
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.baseline_nights_stay_24),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(64.dp).padding(bottom = 12.dp),
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                                )
                                Text(
                                    text = context.getString(if (isAfterLessons) R.string.no_classes_tomorrow else R.string.no_classes_today),
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = GlanceModifier.fillMaxSize()
                            ) {
                                items(dashboardEntries) { entry ->
                                    TimetableItemWidget(entry, size, action)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableItemWidget(entry: TimetableEntry, size: DpSize, action: androidx.glance.action.Action) {
    val accentColor = TimetableWidgetUtils.widgetTypeToColor(entry.courseType)
    val useFullName = size.width > 250.dp
    
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(45.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = entry.startTime,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = GlanceModifier.width(4.dp))
        
        Box(
            modifier = GlanceModifier
                .height(40.dp)
                .width(3.dp)
                .background(accentColor)
                .cornerRadius(2.dp)
        ) {}
        
        Spacer(modifier = GlanceModifier.width(12.dp))

        Column {
            Text(
                text = if (useFullName) entry.courseName else entry.courseNameShort,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.shortType,
                    style = TextStyle(color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = " • ${entry.buildingShort} ${entry.room}",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                )
            }
        }
    }
}

class TodayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayScheduleWidget()
}
