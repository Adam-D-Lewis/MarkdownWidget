package com.example.markdownwidget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import android.text.Html
import android.util.Log
import com.example.markdownwidget.R

class MarkdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
//            val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0)
//            val markdownContent = prefs.getString(MarkdownWidgetConfigureActivity.PREF_PREFIX_KEY + appWidgetId, "# Hello, Markdown!\nThis is a **markdown** text.")

//            val parser = Parser.builder().build()
//            val document = parser.parse(markdownContent)
//            val renderer = HtmlRenderer.builder().build()
//            val html = renderer.render(document)
//            val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)

            val intent = Intent(context, MarkdownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
//            Log.i("MarkdownWidget", "My File content: ${spanned}")
            views.setRemoteAdapter(R.id.widget_list, intent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}