package com.adamdlewis.markdownviewer

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.adamdlewis.markdownviewer.R
import java.io.BufferedReader
import java.io.InputStreamReader

class MarkdownWidgetConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val FILE_SELECT_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.markdown_widget_configure)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/markdown"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(Intent.createChooser(fileIntent, "Select a Markdown file"), FILE_SELECT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val inputStream = contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.use { it.readText() }

                val prefs = getSharedPreferences(PREFS_NAME, 0).edit()
                prefs.putString(PREF_PREFIX_KEY + appWidgetId, content)
                prefs.putString(PREF_URI_KEY + appWidgetId, it.toString())
                prefs.apply()

                val appWidgetManager = AppWidgetManager.getInstance(this)
                MarkdownWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        } else {
            finish()
        }
    }

    companion object {
        const val PREFS_NAME = "com.adamdlewis.markdownviewer.MarkdownWidget"
        const val PREF_PREFIX_KEY = "appwidget_"
        const val PREF_URI_KEY = "appwidget_uri_"

        fun loadMarkdownFilePath(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        fun loadMarkdownFileUri(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_URI_KEY + appWidgetId, null)
        }
    }
}