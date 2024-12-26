package com.example.markdownwidget

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
//            if (checkFileUpdates(context, appWidgetId)) {
//                updateAppWidget(context, appWidgetManager, appWidgetId)
//            }
        }

        // Schedule the alarm to periodically check for updates
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Cancel any existing alarm
        alarmManager.cancel(pendingIntent)

        // Set a new repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1),
            TimeUnit.SECONDS.toMillis(1),
            pendingIntent
        )

        Log.d("MarkdownWidgetProvider", "Alarm set to trigger every second")
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

            // Retrieve the file URI from shared preferences
            val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0)
            val fileUriString = prefs.getString(MarkdownWidgetConfigureActivity.PREF_URI_KEY + appWidgetId, null)
            if (fileUriString != null) {
                val fileUri = Uri.parse(fileUriString)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = fileUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Check if there is an app to handle the intent
                if (viewIntent.resolveActivity(context.packageManager) != null) {
                    val pendingIntent = PendingIntent.getActivity(context, appWidgetId, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
                } else {
                    Log.d("MarkdownWidgetProvider", "No app available to handle ACTION_VIEW for URI: $fileUri")
                    val chooserIntent = Intent.createChooser(viewIntent, "Choose an app to open the file")
                    val pendingIntent = PendingIntent.getActivity(context, appWidgetId, chooserIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
                }
            }

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

class MdRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return MarkdownRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("UpdateReceiver", "Alarm received")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MarkdownWidgetProvider::class.java))
        for (appWidgetId in appWidgetIds) {
            val fileUpdated = MarkdownWidgetProvider.checkFileUpdates(context, appWidgetId)
            if (fileUpdated) {
                MarkdownWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}