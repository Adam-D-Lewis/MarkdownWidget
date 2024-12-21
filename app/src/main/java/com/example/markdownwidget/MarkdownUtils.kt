package com.example.markdownwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService

class MarkdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return MarkdownRemoteViewsFactory(applicationContext, appWidgetId)
    }
}