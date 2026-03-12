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
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import dev.akinom.isod.MainTab
import dev.akinom.isod.android.MainActivity
import dev.akinom.isod.android.R
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.TimetableEntry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NextClassWidget : GlanceAppWidget(), KoinComponent {
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
                val today = TimetableWidgetUtils.getTodayDayOfWeek()
                val nextClasses = TimetableWidgetUtils.getNextClasses(timetable)
                val nextClass = nextClasses.firstOrNull()
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
                    if (nextClass == null) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.take_rest),
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                            )
                        }
                    } else {
                        NextClassHero(nextClass, today, size, action)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassHero(entry: TimetableEntry, today: Int, size: DpSize, action: androidx.glance.action.Action) {
    val context = LocalContext.current
    val accentColor = TimetableWidgetUtils.widgetTypeToColor(entry.courseType)
    val useFullName = size.width > 250.dp
    
    val timeLabel = run {
        val days = listOf(
            context.getString(R.string.day_mon),
            context.getString(R.string.day_tue),
            context.getString(R.string.day_wed),
            context.getString(R.string.day_thu),
            context.getString(R.string.day_fri),
            context.getString(R.string.day_sat),
            context.getString(R.string.day_sun)
        )
        when {
            entry.dayOfWeek == today -> entry.startTime
            entry.dayOfWeek == (today % 7) + 1 -> context.getString(R.string.tomorrow_at, entry.startTime)
            else -> context.getString(R.string.on_day_at, days[entry.dayOfWeek - 1], entry.startTime)
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(accentColor)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                } else {
                    GlanceModifier.cornerRadius(24.dp)
                }
            )
            .padding(16.dp)
            .clickable(action)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .background(GlanceTheme.colors.onPrimary)
                    .cornerRadius(4.dp)
            ) {}
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.upcoming),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Text(
            text = if (useFullName) entry.courseName else entry.courseNameShort,
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (useFullName) 18.sp else 28.sp
            ),
            maxLines = 3
        )
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        Text(
            text = "${entry.buildingShort} ${entry.room}",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimary,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = GlanceModifier.defaultWeight())
        
        Row(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.onPrimary)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = timeLabel,
                style = TextStyle(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
        }
    }
}

class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()
}
