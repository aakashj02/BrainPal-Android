package com.brainpal.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.brainpal.app.data.BrainPalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { updateAll(context, manager, ids) }
    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            updateAll(context, manager, ids)
        }
        private fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = BrainPalDatabase.getInstance(context).brainPalDao()
                val count = dao.countForDate(LocalDate.now().toString()) + dao.correctionForDate(LocalDate.now().toString())
                val goal = dao.getDailyGoal()?.reelsTarget
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_today)
                    views.setTextViewText(R.id.widget_count, count.toString())
                    views.setTextViewText(R.id.widget_subtitle, if (goal == null) "Reels / Shorts today" else "$count of $goal today")
                    val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_root, intent)
                    manager.updateAppWidget(id, views)
                }
            }
        }
    }
}
