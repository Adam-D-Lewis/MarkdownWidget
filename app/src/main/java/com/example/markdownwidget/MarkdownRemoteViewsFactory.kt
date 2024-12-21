package com.example.markdownwidget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import android.text.Html

class MarkdownRemoteViewsFactory(private val context: Context, private val appWidgetId: Int) : RemoteViewsService.RemoteViewsFactory {

    private var markdownContent: String = ""

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0)
        markdownContent = prefs.getString(MarkdownWidgetConfigureActivity.PREF_PREFIX_KEY + appWidgetId, "# Hello, Markdown!\nThis is a **markdown** text.") ?: ""
    }

    override fun onDestroy() {}

    override fun getCount(): Int = 1

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val parser = Parser.builder().build()
        val document = parser.parse(markdownContent)
        val renderer = HtmlRenderer.builder().build()
        val html = renderer.render(document)
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        views.setTextViewText(R.id.widget_item_text, spanned)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}