package com.example.markdownwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader

class MarkdownWidgetConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var editText: EditText
    private val FILE_SELECT_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.markdown_widget_configure)

        editText = findViewById(R.id.editText)

        findViewById<Button>(R.id.browse_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/markdown"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Select a Markdown file"), FILE_SELECT_CODE)
        }

        findViewById<Button>(R.id.add_button).setOnClickListener {
            val context = this@MarkdownWidgetConfigureActivity

            val markdownFilePath = editText.text.toString()
            if (markdownFilePath.isNotEmpty()) {
                val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
                prefs.putString(PREF_PREFIX_KEY + appWidgetId, markdownFilePath)
                prefs.apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                MarkdownWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            } else {
                Toast.makeText(context, "Please select a valid file", Toast.LENGTH_SHORT).show()
            }
        }

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            uri?.let {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.use { it.readText() }

                val prefs = getSharedPreferences(PREFS_NAME, 0).edit()
                prefs.putString(PREF_PREFIX_KEY + appWidgetId, content)
                prefs.apply()

                val appWidgetManager = AppWidgetManager.getInstance(this)
                MarkdownWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }

    companion object {
        const val PREFS_NAME = "com.example.markdownwidget.MarkdownWidget"
        const val PREF_PREFIX_KEY = "appwidget_"

        fun loadMarkdownFilePath(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }
    }
}