package com.adamdlewis.markdownwidget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.util.concurrent.TimeUnit
import android.widget.Toast
import com.example.markdownwidget.R

class MarkdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("MarkdownWidgetProvider", "onUpdate called")
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        // Schedule the alarm to periodically check for updates
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Cancel any existing alarm
        alarmManager.cancel(pendingIntent)

        // Set a new repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            pendingIntent
        )

        Log.d("MarkdownWidgetProvider", "Alarm set to trigger periodically")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0).edit()
        for (appWidgetId in appWidgetIds) {
            prefs.remove(MarkdownWidgetConfigureActivity.PREF_PREFIX_KEY + appWidgetId)
            prefs.remove(MarkdownWidgetConfigureActivity.PREF_URI_KEY + appWidgetId)
            prefs.remove("last_modified_$appWidgetId")
        }
        prefs.apply()
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val intent = Intent(context, MdRemoteViewsService::class.java).apply {
                data = Uri.parse("content://com.example.markdownwidget/$appWidgetId")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setRemoteAdapter(R.id.widget_list, intent)

            // Create a template for item clicks
            val itemClickIntent = Intent(context, UpdateReceiver::class.java).apply {
                action = "com.example.markdownwidget.ACTION_UPDATE_WIDGET"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set up click handler for both the layout and list items
            views.setPendingIntentTemplate(R.id.widget_list, itemClickPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_layout, itemClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun checkFileUpdates(context: Context, appWidgetId: Int): Boolean {
            val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0)
            val contentUriString = prefs.getString(MarkdownWidgetConfigureActivity.PREF_URI_KEY + appWidgetId, null)
            val lastModifiedPrefKey = "last_modified_$appWidgetId"
            val lastKnownModified = prefs.getLong(lastModifiedPrefKey, 0)

            if (contentUriString != null) {
                val contentUri = Uri.parse(contentUriString)
                val documentFile = DocumentFile.fromSingleUri(context, contentUri)
                val lastModified = documentFile?.lastModified() ?: 0

                Log.d("MarkdownWidgetProvider", "appWidgetId: ${appWidgetId}")
                Log.d("MarkdownWidgetProvider", "Content URI: $contentUri")
                Log.d("MarkdownWidgetProvider", "Last modified: $lastModified")
                Log.d("MarkdownWidgetProvider", "Last known modified: $lastKnownModified")
                if (lastModified > lastKnownModified) {
                    val inputStream = context.contentResolver.openInputStream(contentUri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.use { it.readText() }

                    val editor = prefs.edit()
                    editor.putString(MarkdownWidgetConfigureActivity.PREF_PREFIX_KEY + appWidgetId, content)
                    editor.putLong(lastModifiedPrefKey, lastModified)
                    editor.apply()

                    // Notify the AppWidgetManager that the data has changed
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)

                    Log.d("MarkdownWidgetProvider", "File updated for widget ID: $appWidgetId")
                    return true
                } else {
                    Log.d("MarkdownWidgetProvider", "No updates for file for widget ID: $appWidgetId")
                }
            } else {
                Log.d("MarkdownWidgetProvider", "Content URI not found for widget ID: $appWidgetId")
            }
            return false
        }
    }
}

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("UpdateReceiver", "Update received with action: ${intent.action}")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MarkdownWidgetProvider::class.java))

        for (appWidgetId in appWidgetIds) {
            val fileUpdated = MarkdownWidgetProvider.checkFileUpdates(context, appWidgetId)
            if (fileUpdated) {
                MarkdownWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
                // display toast notification only if user triggered the update
                if (intent.action == "com.example.markdownwidget.ACTION_UPDATE_WIDGET") {
                    Toast.makeText(context, "Content refreshed", Toast.LENGTH_SHORT).show()
                }
            } else {
                // display toast notification only if user triggered the update
                if (intent.action == "com.example.markdownwidget.ACTION_UPDATE_WIDGET") {
                    Toast.makeText(context, "Already up to date", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class MdRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return MarkdownRemoteViewsFactory(applicationContext, appWidgetId)
    }
}
