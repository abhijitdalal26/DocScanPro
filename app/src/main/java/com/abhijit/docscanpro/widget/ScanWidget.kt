package com.abhijit.docscanpro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.abhijit.docscanpro.MainActivity
import com.abhijit.docscanpro.R

/**
 * Homescreen widget — one-tap scan button.
 * Tapping opens MainActivity and immediately navigates to the scanner.
 */
class ScanWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.abhijit.docscanpro.ACTION_SCAN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_scan).apply {
            setOnClickPendingIntent(R.id.widget_scan_btn, pendingIntent)
            setOnClickPendingIntent(R.id.widget_icon, pendingIntent)
        }

        manager.updateAppWidget(widgetId, views)
    }
}
