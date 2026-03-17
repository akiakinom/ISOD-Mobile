package dev.akinom.isod.android.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val scope = MainScope()
        scope.launch {
            try {
                TodayScheduleWidget().updateAll(context)
                NextClassWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
